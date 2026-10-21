#![forbid(unsafe_code)]
use crate::{codec, resampler::Resampler};
use std::sync::{
    atomic::{AtomicBool, AtomicU32, Ordering},
    Mutex,
};
use std::time::Instant;

pub type Result<T> = std::result::Result<T, &'static str>;
#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct Config {
    pub output_rate: i32,
    pub output_channels: i32,
    pub output_bytes: i32,
    pub output_bits: i32,
    pub frame_bytes: i32,
    pub input_rate: i32,
    pub input_channels: i32,
    pub input_encoding: i32,
    pub ring_ms: i32,
    pub transfer_bytes: i32,
    pub transfer_count: i32,
}
impl Config {
    fn validate(self) -> Result<Self> {
        if !(8000..=768000).contains(&self.output_rate)
            || !(8000..=768000).contains(&self.input_rate)
            || !(1..=8).contains(&self.output_channels)
            || !(1..=8).contains(&self.input_channels)
            || !(1..=4).contains(&self.output_bytes)
            || !(8..=32).contains(&self.output_bits)
            || self.output_bits > self.output_bytes * 8
            || self.frame_bytes != self.output_bytes * self.output_channels
            || codec::width(self.input_encoding).is_none()
            || !(1..=3000).contains(&self.ring_ms)
            || self.transfer_bytes <= 0
            || self.transfer_count <= 0
        {
            return Err("invalid_pcm_configuration");
        }
        Ok(self)
    }
    fn ring_bytes(self, ms: i32, transfer_bytes: i32, transfer_count: i32) -> Result<usize> {
        if !(1..=3000).contains(&ms) || transfer_bytes <= 0 || transfer_count <= 0 {
            return Err("invalid_pcm_buffer");
        }
        let requested = self.output_rate as u64 * self.frame_bytes as u64 * ms as u64 / 1000;
        let floor = (transfer_bytes as u64)
            .saturating_mul(transfer_count as u64)
            .saturating_mul(3);
        let bytes = requested
            .max(floor)
            .max(self.frame_bytes as u64)
            .min(64 * 1024 * 1024);
        Ok((bytes - bytes % self.frame_bytes as u64) as usize)
    }
    fn input_frame_bytes(self) -> usize {
        codec::width(self.input_encoding).unwrap() * self.input_channels as usize
    }
}
#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct Snapshot {
    pub integers: [u64; 17],
    pub floats: [f32; 8],
}
#[derive(Default)]
struct Stats {
    input: u64,
    output: u64,
    dropped: u64,
    underrun: u64,
    zero: u64,
    paused: u64,
    signal_frames: u64,
    silent_frames: u64,
    signal_bytes: u64,
    backpressure_events: u64,
    backpressure_total: u64,
    backpressure_max: u64,
    pressure_at: Option<Instant>,
    max_level: usize,
    peak: f32,
    last_peak: f32,
    channel0_peak: f32,
    channel1_peak: f32,
    last0: f32,
    last1: f32,
}
#[derive(Default)]
struct Writer {
    config: Option<Config>,
    resampler: Option<Resampler>,
    converted: Vec<u8>,
    samples: Vec<f32>,
    filtered: Vec<f32>,
}
#[derive(Default)]
struct Output {
    config: Option<Config>,
    ring: Vec<u8>,
    read: usize,
    level: usize,
    stats: Stats,
    applied: f32,
    gain_target: f32,
    gain_remaining: usize,
    start_total: usize,
    start_remaining: usize,
}
fn allocate(bytes: usize) -> Result<Vec<u8>> {
    let mut result = Vec::new();
    result
        .try_reserve_exact(bytes)
        .map_err(|_| "pcm_allocation_failed")?;
    result.resize(bytes, 0);
    Ok(result)
}
impl Output {
    fn pressure(&mut self) {
        if self.stats.pressure_at.is_none() {
            self.stats.pressure_at = Some(Instant::now());
            self.stats.backpressure_events += 1;
        }
    }
    fn end_pressure(&mut self) {
        if let Some(start) = self.stats.pressure_at.take() {
            let us = start.elapsed().as_micros().min(u64::MAX as u128) as u64;
            self.stats.backpressure_total = self.stats.backpressure_total.saturating_add(us);
            self.stats.backpressure_max = self.stats.backpressure_max.max(us);
        }
    }
    fn free(&self) -> usize {
        self.ring.len() - self.level
    }
    fn push(&mut self, bytes: &[u8]) -> Result<()> {
        if bytes.len() > self.free() {
            self.pressure();
            return Err("pcm_backpressure");
        }
        self.end_pressure();
        if bytes.is_empty() {
            return Ok(());
        }
        let write = (self.read + self.level) % self.ring.len();
        let first = bytes.len().min(self.ring.len() - write);
        self.ring[write..write + first].copy_from_slice(&bytes[..first]);
        self.ring[..bytes.len() - first].copy_from_slice(&bytes[first..]);
        self.level += bytes.len();
        self.stats.max_level = self.stats.max_level.max(self.level);
        Ok(())
    }
    fn pop(&mut self, bytes: &mut [u8]) -> usize {
        let count = bytes.len().min(self.level);
        if count == 0 {
            return 0;
        }
        let first = count.min(self.ring.len() - self.read);
        bytes[..first].copy_from_slice(&self.ring[self.read..self.read + first]);
        bytes[first..count].copy_from_slice(&self.ring[..count - first]);
        self.read = (self.read + count) % self.ring.len();
        self.level -= count;
        if self.free() > 0 {
            self.end_pressure();
        }
        count
    }
    fn silent(&mut self) {
        self.applied = 0.0;
        self.gain_target = 0.0;
        self.gain_remaining = 0;
    }
    fn gain(&mut self, bytes: &mut [u8], target: f32, perfect: bool) {
        if perfect {
            let muted = target <= 0.000001;
            if muted {
                bytes.fill(0);
            }
            self.applied = if muted { 0.0 } else { 1.0 };
            self.gain_target = self.applied;
            self.gain_remaining = 0;
            return;
        }
        let config = self.config.unwrap();
        if (target - self.gain_target).abs() > 0.000001 {
            self.gain_target = target;
            self.gain_remaining = (config.output_rate as usize * 80 / 1000).max(1);
        }
        if self.gain_remaining == 0
            && (self.applied - 1.0).abs() <= 0.000001
            && (self.gain_target - 1.0).abs() <= 0.000001
        {
            return;
        }
        for frame in bytes.chunks_exact_mut(config.frame_bytes as usize) {
            if self.gain_remaining > 0 {
                self.applied += (self.gain_target - self.applied) / self.gain_remaining as f32;
                self.gain_remaining -= 1;
            } else {
                self.applied = self.gain_target;
            }
            for sample in frame.chunks_exact_mut(config.output_bytes as usize) {
                codec::encode(
                    sample,
                    config.output_bits as u32,
                    codec::decode_output(sample, config.output_bits as u32) * self.applied,
                );
            }
        }
    }
    fn fade_tail(&self, bytes: &mut [u8]) {
        let config = self.config.unwrap();
        let frames = bytes.len() / config.frame_bytes as usize;
        let ramp = (config.output_rate as usize * 5 / 1000).max(1).min(frames);
        for (index, frame) in bytes
            .chunks_exact_mut(config.frame_bytes as usize)
            .enumerate()
            .skip(frames - ramp)
        {
            let gain = if ramp > 1 {
                (frames - index - 1) as f32 / (ramp - 1) as f32
            } else {
                0.0
            };
            for sample in frame.chunks_exact_mut(config.output_bytes as usize) {
                codec::encode(
                    sample,
                    config.output_bits as u32,
                    codec::decode_output(sample, config.output_bits as u32) * gain,
                );
            }
        }
    }
    fn signal_stats(&mut self, bytes: &[u8]) {
        let config = self.config.unwrap();
        let mut peak = 0.0f32;
        let mut first = 0.0f32;
        let mut second = 0.0f32;
        for frame in bytes.chunks_exact(config.frame_bytes as usize) {
            let mut signal = false;
            for (channel, sample) in frame.chunks_exact(config.output_bytes as usize).enumerate() {
                let value = codec::decode_output(sample, config.output_bits as u32).abs();
                peak = peak.max(value);
                if channel == 0 {
                    first = first.max(value);
                } else if channel == 1 {
                    second = second.max(value);
                }
                signal |= value > 0.000001;
            }
            if signal {
                self.stats.signal_frames += 1;
                self.stats.signal_bytes += config.frame_bytes as u64;
            } else {
                self.stats.silent_frames += 1;
            }
        }
        self.stats.last_peak = peak;
        self.stats.peak = self.stats.peak.max(peak);
        self.stats.last0 = first;
        self.stats.last1 = second;
        self.stats.channel0_peak = self.stats.channel0_peak.max(first);
        self.stats.channel1_peak = self.stats.channel1_peak.max(second);
    }
}

pub struct Core {
    writer: Mutex<Writer>,
    output: Mutex<Output>,
    target: AtomicU32,
    perfect: AtomicBool,
}
impl Default for Core {
    fn default() -> Self {
        Self {
            writer: Mutex::default(),
            output: Mutex::default(),
            target: AtomicU32::new(1f32.to_bits()),
            perfect: AtomicBool::new(false),
        }
    }
}
impl Core {
    pub fn configure(&self, config: Config) -> Result<()> {
        let config = config.validate()?;
        let ring = allocate(config.ring_bytes(
            config.ring_ms,
            config.transfer_bytes,
            config.transfer_count,
        )?)?;
        let resampler = (config.input_rate != config.output_rate).then(|| {
            Resampler::new(
                config.input_rate as usize,
                config.output_rate as usize,
                config.output_channels as usize,
            )
        });
        let mut writer = self.writer.lock().map_err(|_| "pcm_writer_poisoned")?;
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        *writer = Writer {
            config: Some(config),
            resampler,
            ..Writer::default()
        };
        let target = self.target();
        *output = Output {
            config: Some(config),
            ring,
            applied: target,
            gain_target: target,
            ..Output::default()
        };
        Ok(())
    }
    pub fn resize(&self, ms: i32, transfer_bytes: i32, transfer_count: i32) -> Result<()> {
        let _writer = self.writer.lock().map_err(|_| "pcm_writer_poisoned")?;
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        let config = output.config.ok_or("pcm_not_configured")?;
        // Never drop accepted PCM when shrinking. A subsequent resize can reclaim space after draining.
        let bytes = config
            .ring_bytes(ms, transfer_bytes, transfer_count)?
            .max(output.level);
        if bytes == output.ring.len() {
            return Ok(());
        }
        let mut ring = allocate(bytes)?;
        let level = output.level;
        output.pop(&mut ring[..level]);
        output.ring = ring;
        output.read = 0;
        output.level = level;
        Ok(())
    }
    pub fn write(&self, input: &[u8]) -> Result<usize> {
        let mut writer = self.writer.lock().map_err(|_| "pcm_writer_poisoned")?;
        let config = writer.config.ok_or("pcm_not_configured")?;
        let frame_bytes = config.input_frame_bytes();
        if input.len() % frame_bytes != 0 {
            return Err("unaligned_pcm_input");
        }
        if input.is_empty() {
            return Ok(0);
        }
        let free = {
            let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
            let free = output.free() / config.frame_bytes as usize;
            if free == 0 {
                output.pressure();
            } else {
                output.end_pressure();
            }
            free
        };
        if free == 0 {
            return Ok(0);
        }
        let available = input.len() / frame_bytes;
        let frames = writer
            .resampler
            .as_ref()
            .map_or(available.min(free), |r| r.acceptable_input(available, free));
        if frames == 0 {
            return Ok(0);
        }
        let consumed = frames * frame_bytes;
        let passthrough = config.input_rate == config.output_rate
            && config.input_channels == config.output_channels
            && codec::integer_bits(config.input_encoding) == Some(config.output_bits as u32)
            && codec::width(config.input_encoding) == Some(config.output_bytes as usize)
            && config.input_encoding < 0x10000000;
        if passthrough {
            let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
            output.push(&input[..consumed])?;
            output.stats.input += consumed as u64;
            return Ok(consumed);
        }
        let Writer {
            resampler,
            samples,
            filtered,
            converted,
            ..
        } = &mut *writer;
        converted.clear();
        samples.clear();
        filtered.clear();
        if let Some(resampler) = resampler {
            for frame in input[..consumed].chunks_exact(frame_bytes) {
                for channel in 0..config.output_channels as usize {
                    samples.push(input_sample(frame, config, channel));
                }
            }
            resampler.process(samples, filtered);
            converted.resize(filtered.len() * config.output_bytes as usize, 0);
            for (sample, value) in converted
                .chunks_exact_mut(config.output_bytes as usize)
                .zip(filtered.iter())
            {
                codec::encode(sample, config.output_bits as u32, *value);
            }
        } else {
            converted.resize(frames * config.frame_bytes as usize, 0);
            let exact_integer = config.input_channels == config.output_channels
                && codec::integer_bits(config.input_encoding)
                    .is_some_and(|b| b <= config.output_bits as u32);
            for (source, target) in input[..consumed]
                .chunks_exact(frame_bytes)
                .zip(converted.chunks_exact_mut(config.frame_bytes as usize))
            {
                for (channel, sample) in target
                    .chunks_exact_mut(config.output_bytes as usize)
                    .enumerate()
                {
                    if exact_integer {
                        let width = codec::width(config.input_encoding).unwrap();
                        let value = codec::integer(
                            &source[channel * width..][..width],
                            config.input_encoding,
                        );
                        codec::encode_integer(
                            sample,
                            config.output_bits as u32,
                            value
                                << (config.output_bits as u32
                                    - codec::integer_bits(config.input_encoding).unwrap()),
                        );
                    } else {
                        codec::encode(
                            sample,
                            config.output_bits as u32,
                            input_sample(source, config, channel),
                        );
                    }
                }
            }
        }
        // Only the reader can change ring occupancy while conversion runs, and it only frees space.
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        output.push(converted)?;
        output.stats.input += consumed as u64;
        Ok(consumed)
    }
    pub fn drain(&self) -> Result<bool> {
        let mut writer = self.writer.lock().map_err(|_| "pcm_writer_poisoned")?;
        let config = writer.config.ok_or("pcm_not_configured")?;
        let Writer {
            resampler,
            filtered,
            converted,
            ..
        } = &mut *writer;
        let Some(resampler) = resampler else {
            return Ok(true);
        };
        let rollback = resampler.clone();
        filtered.clear();
        resampler.drain(filtered);
        converted.clear();
        converted.resize(filtered.len() * config.output_bytes as usize, 0);
        for (sample, value) in converted
            .chunks_exact_mut(config.output_bytes as usize)
            .zip(filtered.iter())
        {
            codec::encode(sample, config.output_bits as u32, *value);
        }
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        if converted.len() > output.free() {
            *resampler = rollback;
            output.pressure();
            return Ok(false);
        }
        output.push(converted)?;
        Ok(true)
    }
    pub fn fill(&self, bytes: &mut [u8], playing: bool) -> Result<usize> {
        if bytes.is_empty() {
            return Ok(0);
        }
        bytes.fill(0);
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        let Some(config) = output.config else {
            return Ok(0);
        };
        if bytes.len() % config.frame_bytes as usize != 0 {
            return Err("unaligned_pcm_output");
        }
        let read = if playing { output.pop(bytes) } else { 0 };
        if playing {
            output.stats.underrun += (bytes.len() - read) as u64;
            output.stats.zero += (bytes.len() - read) as u64;
        } else {
            output.stats.paused += bytes.len() as u64;
        }
        output.stats.output += bytes.len() as u64;
        if read == 0 {
            output.silent();
            output.stats.silent_frames += (bytes.len() / config.frame_bytes as usize) as u64;
            output.stats.last_peak = 0.0;
            output.stats.last0 = 0.0;
            output.stats.last1 = 0.0;
        } else {
            let perfect = self.perfect();
            output.gain(&mut bytes[..read], self.target(), perfect);
            if read < bytes.len() {
                if !perfect {
                    output.fade_tail(&mut bytes[..read]);
                }
                output.silent();
            }
            output.signal_stats(bytes);
        }
        Ok(read)
    }
    pub fn clear(&self) -> Result<()> {
        let mut writer = self.writer.lock().map_err(|_| "pcm_writer_poisoned")?;
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        output.read = 0;
        output.level = 0;
        output.end_pressure();
        if let Some(r) = writer.resampler.as_mut() {
            r.reset();
        }
        Ok(())
    }
    pub fn reset_counters(&self) -> Result<()> {
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        output.stats = Stats {
            max_level: output.level,
            ..Stats::default()
        };
        Ok(())
    }
    pub fn add_dropped(&self, frames: u64) -> Result<()> {
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        let config = output.config.ok_or("pcm_not_configured")?;
        output.stats.dropped = output
            .stats
            .dropped
            .saturating_add(frames.saturating_mul(config.frame_bytes as u64));
        Ok(())
    }
    pub fn set_target(&self, gain: f32) {
        self.target
            .store(codec::gain(gain).to_bits(), Ordering::Relaxed);
    }
    pub fn target(&self) -> f32 {
        f32::from_bits(self.target.load(Ordering::Relaxed))
    }
    pub fn set_perfect(&self, perfect: bool) {
        self.perfect.store(perfect, Ordering::Relaxed);
    }
    pub fn perfect(&self) -> bool {
        self.perfect.load(Ordering::Relaxed)
    }
    pub fn arm_ramp(&self) -> Result<()> {
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        if let Some(c) = output.config {
            output.start_total = (c.output_rate as usize * 80 / 1000).max(1);
            output.start_remaining = output.start_total;
        }
        Ok(())
    }
    pub fn start_ramp(&self, bytes: &mut [u8]) -> Result<()> {
        let mut output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        let Some(config) = output.config else {
            return Ok(());
        };
        if self.perfect() {
            output.start_remaining = 0;
            return Ok(());
        }
        for frame in bytes.chunks_exact_mut(config.frame_bytes as usize) {
            if output.start_remaining == 0 {
                break;
            }
            let gain = if output.start_total > 1 {
                (output.start_total - output.start_remaining) as f32
                    / (output.start_total - 1) as f32
            } else {
                0.0
            };
            for sample in frame.chunks_exact_mut(config.output_bytes as usize) {
                codec::encode(
                    sample,
                    config.output_bits as u32,
                    codec::decode_output(sample, config.output_bits as u32) * gain,
                );
            }
            output.start_remaining -= 1;
        }
        Ok(())
    }
    pub fn queued(&self) -> Result<usize> {
        let output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        Ok(output
            .config
            .map_or(0, |c| output.level / c.frame_bytes as usize))
    }
    pub fn snapshot(&self) -> Result<Snapshot> {
        let output = self.output.lock().map_err(|_| "pcm_output_poisoned")?;
        let s = &output.stats;
        let current = s.pressure_at.map_or(0, |at| {
            at.elapsed().as_micros().min(u64::MAX as u128) as u64
        });
        Ok(Snapshot {
            integers: [
                output.level as u64,
                output.ring.len() as u64,
                output.free() as u64,
                s.max_level as u64,
                s.input,
                s.output,
                s.dropped,
                s.underrun,
                s.zero,
                s.paused,
                s.signal_frames,
                s.silent_frames,
                s.signal_bytes,
                s.backpressure_events,
                s.backpressure_total.saturating_add(current),
                current,
                s.backpressure_max.max(current),
            ],
            floats: [
                s.peak,
                s.last_peak,
                s.channel0_peak,
                s.channel1_peak,
                s.last0,
                s.last1,
                self.target(),
                output.applied,
            ],
        })
    }
}
fn input_sample(frame: &[u8], config: Config, output_channel: usize) -> f32 {
    let width = codec::width(config.input_encoding).unwrap();
    let sample =
        |channel: usize| codec::decode(&frame[channel * width..][..width], config.input_encoding);
    if config.output_channels == 1 && config.input_channels >= 2 {
        return (sample(0) + sample(1)) * 0.5;
    }
    if config.input_channels == 1 {
        if output_channel < 2 {
            sample(0)
        } else {
            0.0
        }
    } else if output_channel < config.input_channels as usize {
        sample(output_channel)
    } else {
        0.0
    }
}

#[cfg(test)]
mod failure_tests {
    use super::*;
    #[test]
    fn poisoned_output_fails_closed() {
        let core = Core::default();
        let _ = std::panic::catch_unwind(|| {
            let _guard = core.output.lock().unwrap();
            panic!("injected failure");
        });
        assert!(core.snapshot().is_err());
        let mut bytes = [1u8; 4];
        assert!(core.fill(&mut bytes, true).is_err());
        assert_eq!(bytes, [0; 4]);
    }
    #[test]
    fn empty_transfer_does_not_change_gain_state() {
        let core = Core::default();
        core.output.lock().unwrap().applied = 1.0;
        assert_eq!(core.fill(&mut [], true).unwrap(), 0);
        assert_eq!(core.output.lock().unwrap().applied, 1.0);
    }
}
