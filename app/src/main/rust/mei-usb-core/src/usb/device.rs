#![forbid(unsafe_code)]
//! USB fd/claim/control ownership. Adapted from NeriPlayer 113699bb; see NERI_LICENSE.md.
use super::{
    protocol::{self, RateRange},
    usbfs::{self, Alt, Endpoint, UsbFs},
};
use crate::descriptor;
use std::{
    collections::{HashMap, HashSet},
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc, Mutex,
    },
};
static LEASE: AtomicBool = AtomicBool::new(false);
static RELEASED: Mutex<Option<std::time::Instant>> = Mutex::new(None);
#[derive(Clone)]
pub struct Candidate {
    pub stream: Alt,
    pub control: Alt,
    pub format: descriptor::Format,
    pub output: Endpoint,
    pub feedback: Option<Endpoint>,
    pub version: u8,
    pub clock: Option<(u8, bool)>,
    pub ranges: Vec<RateRange>,
    pub function: Vec<u8>,
}
impl Candidate {
    pub fn frame_bytes(&self) -> usize {
        (self.format.channels * self.format.subslot) as usize
    }
}
struct Saved {
    feature: protocol::Feature,
    bytes: Vec<u8>,
}
struct Volume {
    feature: protocol::Feature,
    min: i16,
    max: i16,
    step: i16,
}
#[derive(Default)]
struct Controls {
    saved: Vec<Saved>,
    volume: Vec<Volume>,
    mute: Vec<protocol::Feature>,
}
pub struct Device {
    pub io: Arc<UsbFs>,
    pub speed: i32,
    alts: Vec<Alt>,
    extra: Vec<u8>,
    claims: Vec<(u8, bool)>,
    selected: Option<Candidate>,
    controls: Mutex<Controls>,
    pub online: AtomicBool,
    leased: bool,
}
impl Device {
    pub fn probe(fd: i32, leased: bool) -> Result<Self, String> {
        if leased
            && LEASE
                .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
                .is_err()
        {
            return Err("usb_session_still_owned".into());
        }
        if leased {
            if let Ok(last) = RELEASED.lock() {
                if let Some(last) = *last {
                    if let Some(wait) =
                        std::time::Duration::from_millis(120).checked_sub(last.elapsed())
                    {
                        std::thread::sleep(wait);
                    }
                }
            }
        }
        let result = Self::wrap(fd, false).map(|mut device| {
            device.leased = leased;
            device
        });
        if result.is_err() && leased {
            LEASE.store(false, Ordering::Release);
        }
        result
    }
    fn wrap(fd: i32, leased: bool) -> Result<Self, String> {
        if fd < 0 {
            return Err("invalid_usb_fd".into());
        }
        let io = Arc::new(UsbFs::open(fd).map_err(|e| format!("usbfs_open_failed:{e}"))?);
        Self::from_io(io, leased)
    }
    fn from_io(io: Arc<UsbFs>, leased: bool) -> Result<Self, String> {
        let config = io.configuration()?;
        if config.value == 0 {
            return Err("usbfs_configuration_not_set".into());
        }
        Ok(Self {
            speed: io.speed(),
            io,
            alts: config.alts,
            extra: config.extra,
            claims: vec![],
            selected: None,
            controls: Mutex::new(Controls::default()),
            online: AtomicBool::new(true),
            leased,
        })
    }
    #[cfg(test)]
    pub(super) fn test_device(io: UsbFs) -> Result<Self, String> {
        Self::from_io(Arc::new(io), false)
    }
    pub fn control(
        &self,
        input: bool,
        endpoint: bool,
        request: u8,
        value: u16,
        index: u16,
        data: &mut [u8],
    ) -> Result<usize, String> {
        if !self.online.load(Ordering::Acquire) {
            return Err("usb_device_detached".into());
        }
        if data.len() > u16::MAX as usize {
            return Err("usb_control_too_large".into());
        }
        let ty = if input { 0xa0 } else { 0x20 } | if endpoint { 2 } else { 1 };
        match self.io.control(ty, request, value, index, data, 250) {
            Ok(n) => Ok(n),
            Err(e) => {
                if e.raw_os_error() == Some(usbfs::os::ENODEV) {
                    self.online.store(false, Ordering::Release);
                }
                Err(format!("usbfs_control_failed:{e}"))
            }
        }
    }
    fn clock(&self, ac: &Alt, terminal: u8) -> Option<(u8, bool)> {
        let start = protocol::terminal_clock(&ac.extra, terminal)?;
        protocol::resolve_clock(
            &protocol::clocks(&ac.extra),
            start,
            |id| {
                let mut d = [0];
                (self
                    .control(
                        true,
                        false,
                        1,
                        0x100,
                        ((id as u16) << 8) | ac.number as u16,
                        &mut d,
                    )
                    .ok()?
                    == 1)
                    .then_some(d[0])
            },
            |id| {
                let mut n = [0; 2];
                let mut d = [0; 2];
                let index = ((id as u16) << 8) | ac.number as u16;
                if self.control(true, false, 1, 0x100, index, &mut n).ok()? != 2
                    || self.control(true, false, 1, 0x200, index, &mut d).ok()? != 2
                {
                    return None;
                }
                Some((u16::from_le_bytes(n), u16::from_le_bytes(d)))
            },
        )
    }
    fn rate_ranges(&self, ac: u8, clock: u8) -> Vec<RateRange> {
        let index = ((clock as u16) << 8) | ac as u16;
        let mut header = [0; 2];
        if self.control(true, false, 2, 0x100, index, &mut header).ok() != Some(2) {
            return vec![];
        }
        let count = u16::from_le_bytes(header) as usize;
        if count == 0 || count > 256 {
            return vec![];
        }
        let mut d = vec![0; 2 + 12 * count];
        if self.control(true, false, 2, 0x100, index, &mut d).ok() != Some(d.len()) {
            return vec![];
        }
        protocol::ranges(&d).unwrap_or_default()
    }
    pub fn candidates(&self) -> Vec<Candidate> {
        let mut result = Vec::new();
        let mut clock_cache = HashMap::new();
        let mut range_cache = HashMap::new();
        for stream in self
            .alts
            .iter()
            .filter(|a| a.class == 1 && a.subclass == 2 && a.alternate > 0)
        {
            let version = match stream.protocol {
                0 => 1,
                0x20 => 2,
                _ => continue,
            };
            let Some(format) = descriptor::parse(&stream.extra, version as u32) else {
                continue;
            };
            if format.format_type > 1
                || (version == 1 && format.format != 1)
                || (version == 2 && format.format & 1 == 0)
                || format.channels > 8
                || format.subslot > 4
                || format.bits < 8
            {
                continue;
            }
            let Some(output) = stream.endpoints.iter().find(|e| {
                e.address & 0x80 == 0 && e.attributes & 3 == 1 && (e.attributes >> 4) & 3 == 0
            }) else {
                continue;
            };
            let feedback = stream
                .endpoints
                .iter()
                .find(|e| {
                    e.address & 0x80 != 0
                        && e.attributes & 3 == 1
                        && ((e.attributes >> 4) & 3 == 1 || e.address == output.sync_address)
                })
                .cloned();
            // Implicit feedback needs a paired capture stream; reject rather than free-run it.
            if (output.attributes >> 2) & 3 == 1 && feedback.is_none() {
                continue;
            }
            let mut controls: Vec<(&Alt, Vec<u8>)> = Vec::new();
            for ac in self.alts.iter().filter(|a| {
                a.class == 1 && a.subclass == 1 && a.alternate == 0 && a.protocol == stream.protocol
            }) {
                let mut function = vec![];
                if version == 1 {
                    for d in protocol::descriptors(&ac.extra).unwrap_or_default() {
                        if d.len() >= 8 && d[1] == 0x24 && d[2] == 1 && d.len() >= 8 + d[7] as usize
                        {
                            function.extend_from_slice(&d[8..8 + d[7] as usize]);
                        }
                    }
                } else {
                    for d in std::iter::once(&self.extra)
                        .chain(self.alts.iter().map(|a| &a.extra))
                        .flat_map(|data| protocol::descriptors(data).unwrap_or_default())
                    {
                        if d.len() >= 8 && d[1] == 0x0b && d[4] == 1 && d[2] == ac.number {
                            function.extend((0..d[3]).filter_map(|n| d[2].checked_add(n)));
                        }
                    }
                    if function.is_empty()
                        && protocol::terminal_clock(&ac.extra, format.terminal as u8).is_some()
                    {
                        function.push(stream.number);
                    }
                }
                if function.contains(&stream.number) {
                    controls.push((ac, function));
                }
            }
            if controls.len() != 1 {
                continue;
            }
            let (ac, function) = controls.remove(0);
            let clock = if version == 2 {
                *clock_cache
                    .entry((ac.number, format.terminal as u8))
                    .or_insert_with(|| self.clock(ac, format.terminal as u8))
            } else {
                None
            };
            let ranges = if version == 1 {
                if format.rate_kind == 1 {
                    format.rates[..format.rate_count as usize]
                        .iter()
                        .map(|r| RateRange {
                            min: *r,
                            max: *r,
                            step: 0,
                        })
                        .collect()
                } else {
                    vec![RateRange {
                        min: format.minimum,
                        max: format.maximum,
                        step: 0,
                    }]
                }
            } else {
                clock
                    .map(|(id, _)| {
                        range_cache
                            .entry((ac.number, id))
                            .or_insert_with(|| self.rate_ranges(ac.number, id))
                            .clone()
                    })
                    .unwrap_or_default()
            };
            result.push(Candidate {
                stream: stream.clone(),
                control: ac.clone(),
                format,
                output: output.clone(),
                feedback,
                version,
                clock,
                ranges,
                function,
            });
        }
        result
    }
    fn claim(&mut self, number: u8) -> Result<(), String> {
        if self.claims.iter().any(|(n, _)| *n == number) {
            return Ok(());
        }
        let detached = self
            .io
            .claim(number)
            .map_err(|e| format!("usbfs_claim_failed:{number}:{e}"))?;
        self.claims.push((number, detached));
        Ok(())
    }
    pub fn select(
        &mut self,
        rate: u32,
        channels: u32,
        bits: u32,
        subslot: u32,
    ) -> Result<Candidate, String> {
        let mut candidates = self.candidates();
        candidates.retain(|c| {
            c.format.channels == channels
                && c.format.bits == bits
                && c.format.subslot == subslot
                && (c.ranges.is_empty() || c.ranges.iter().any(|r| r.accepts(rate)))
                && protocol::Scheduler::new(
                    rate,
                    protocol::intervals(self.speed, c.output.interval),
                    c.output.capacity / c.frame_bytes(),
                )
                .is_some()
        });
        candidates.sort_by_key(|c| {
            (
                std::cmp::Reverse(c.version),
                c.stream.number,
                c.stream.alternate,
            )
        });
        let mut c = candidates
            .into_iter()
            .next()
            .ok_or("no_compatible_usb_audio_format:pcm_or_feedback_unavailable")?;
        self.claim(c.control.number)?;
        let audio: HashSet<u8> = self
            .alts
            .iter()
            .filter(|a| a.class == 1 && a.subclass == 2)
            .map(|a| a.number)
            .collect();
        for n in &c.function {
            if audio.contains(n) && *n != c.stream.number {
                self.claim(*n)?;
            }
        }
        self.claim(c.stream.number)?;
        self.selected = Some(c.clone());
        // Refresh controls once interfaces are exclusively owned (some Android drivers reject probes).
        if c.version == 2 {
            c.clock = self.clock(&c.control, c.format.terminal as u8);
            let (id, writable) = c
                .clock
                .ok_or("sample_rate_negotiation_failed:unresolved_uac2_clock")?;
            let ranges = self.rate_ranges(c.control.number, id);
            if !ranges.is_empty() && !ranges.iter().any(|r| r.accepts(rate)) {
                return Err("sample_rate_negotiation_failed:clock_range".into());
            }
            let index = ((id as u16) << 8) | c.control.number as u16;
            let mut current = [0; 4];
            let mut verified = self
                .control(true, false, 1, 0x100, index, &mut current)
                .ok()
                == Some(4)
                && u32::from_le_bytes(current) == rate;
            if !verified && writable {
                if self
                    .control(false, false, 1, 0x100, index, &mut rate.to_le_bytes())
                    .ok()
                    != Some(4)
                {
                    return Err("sample_rate_negotiation_failed:set_clock".into());
                }
                for _ in 0..4 {
                    std::thread::sleep(std::time::Duration::from_millis(20));
                    if self
                        .control(true, false, 1, 0x100, index, &mut current)
                        .ok()
                        == Some(4)
                        && u32::from_le_bytes(current) == rate
                    {
                        verified = true;
                        break;
                    }
                }
            }
            if !verified {
                return Err("sample_rate_negotiation_failed:clock_readback".into());
            }
        }
        self.io
            .set_alt(c.stream.number, c.stream.alternate)
            .map_err(|e| format!("usbfs_set_alt_failed:{e}"))?;
        if c.version == 1 {
            let writable = protocol::descriptors(&c.output.extra)
                .unwrap_or_default()
                .iter()
                .any(|d| d.len() >= 4 && d[1] == 0x25 && d[2] == 1 && d[3] & 1 != 0);
            let fixed = c.ranges.len() == 1 && c.ranges[0].min == rate && c.ranges[0].max == rate;
            if writable {
                let mut value = rate.to_le_bytes();
                if self
                    .control(
                        false,
                        true,
                        1,
                        0x100,
                        c.output.address as u16,
                        &mut value[..3],
                    )
                    .ok()
                    != Some(3)
                    && !fixed
                {
                    return Err("sample_rate_negotiation_failed:uac1_set".into());
                }
                let mut read = [0; 3];
                for attempt in 0..4 {
                    let got =
                        self.control(true, true, 0x81, 0x100, c.output.address as u16, &mut read);
                    if got.as_ref().ok() != Some(&3) {
                        break;
                    }
                    if u32::from_le_bytes([read[0], read[1], read[2], 0]) == rate {
                        break;
                    }
                    if attempt == 3 {
                        return Err("sample_rate_negotiation_failed:uac1_readback".into());
                    }
                    std::thread::sleep(std::time::Duration::from_millis(20));
                }
            } else if !fixed {
                return Err("sample_rate_negotiation_failed:uac1_rate_not_writable".into());
            }
        }
        self.selected = Some(c.clone());
        self.prepare_controls(&c);
        Ok(c)
    }
    fn feature_io(
        &self,
        c: &Candidate,
        f: &protocol::Feature,
        input: bool,
        request: u8,
        d: &mut [u8],
    ) -> Result<usize, String> {
        self.control(
            input,
            false,
            request,
            ((f.selector as u16) << 8) | f.channel as u16,
            ((f.unit as u16) << 8) | c.control.number as u16,
            d,
        )
    }
    fn prepare_controls(&self, c: &Candidate) {
        let Ok(mut controls) = self.controls.lock() else {
            return;
        };
        for f in protocol::playback_features(&c.control.extra, c.version) {
            let width = if f.selector == 1 { 1 } else { 2 };
            let mut old = vec![0; width];
            if self
                .feature_io(c, &f, true, if c.version == 1 { 0x81 } else { 1 }, &mut old)
                .ok()
                != Some(width)
            {
                continue;
            }
            if f.selector == 1 {
                if self.feature_io(c, &f, false, 1, &mut [0]).ok() == Some(1) {
                    controls.mute.push(f.clone());
                    controls.saved.push(Saved {
                        feature: f,
                        bytes: old,
                    });
                }
            } else {
                // One hardware volume stage: multiple feature units in series would
                // multiply the attenuation and make the system volume curve incorrect.
                if controls
                    .volume
                    .first()
                    .is_some_and(|v| v.feature.unit != f.unit)
                {
                    continue;
                }
                let range = if c.version == 1 {
                    let mut min = [0; 2];
                    let mut max = [0; 2];
                    let mut step = [0; 2];
                    if self.feature_io(c, &f, true, 0x82, &mut min).ok() == Some(2)
                        && self.feature_io(c, &f, true, 0x83, &mut max).ok() == Some(2)
                        && self.feature_io(c, &f, true, 0x84, &mut step).ok() == Some(2)
                    {
                        Some((
                            i16::from_le_bytes(min),
                            i16::from_le_bytes(max),
                            i16::from_le_bytes(step),
                        ))
                    } else {
                        None
                    }
                } else {
                    let mut d = [0; 8];
                    if self.feature_io(c, &f, true, 2, &mut d).ok() == Some(8)
                        && u16::from_le_bytes([d[0], d[1]]) == 1
                    {
                        Some((
                            i16::from_le_bytes([d[2], d[3]]),
                            i16::from_le_bytes([d[4], d[5]]),
                            i16::from_le_bytes([d[6], d[7]]),
                        ))
                    } else {
                        None
                    }
                };
                let Some((min, max, step)) = range else {
                    continue;
                };
                if min >= max {
                    continue;
                }
                let mut unity = protocol::volume_value(1., min, max, step).to_le_bytes();
                if self.feature_io(c, &f, false, 1, &mut unity).ok() == Some(2) {
                    controls.volume.push(Volume {
                        feature: f.clone(),
                        min,
                        max,
                        step,
                    });
                    controls.saved.push(Saved {
                        feature: f,
                        bytes: old,
                    });
                }
            }
        }
    }
    pub fn has_volume(&self) -> bool {
        self.controls.lock().is_ok_and(|c| !c.volume.is_empty())
    }
    pub fn set_volume(&self, fraction: f32) -> bool {
        if !fraction.is_finite() {
            return false;
        }
        let Some(c) = self.selected.as_ref() else {
            return false;
        };
        let Ok(controls) = self.controls.lock() else {
            return false;
        };
        if controls.volume.is_empty() {
            return false;
        }
        let mut success = true;
        for v in &controls.volume {
            let mut d = protocol::volume_value(fraction, v.min, v.max, v.step).to_le_bytes();
            success &= self.feature_io(c, &v.feature, false, 1, &mut d).ok() == Some(2);
        }
        for m in &controls.mute {
            success &= self
                .feature_io(c, m, false, 1, &mut [u8::from(fraction <= 0.)])
                .ok()
                == Some(1);
        }
        success
    }
}
impl Drop for Device {
    fn drop(&mut self) {
        if self.io.pending() > 0 {
            // Worker panic: retain fd, kernel-visible buffers and the exclusive lease.
            // Dropping a duplicate while Java still holds the original fd cannot cancel URBs.
            std::mem::forget(self.io.clone());
            return;
        }
        if self.online.load(Ordering::Acquire) {
            if let Some(c) = &self.selected {
                if let Ok(controls) = self.controls.lock() {
                    for saved in controls.saved.iter().rev() {
                        let _ =
                            self.feature_io(c, &saved.feature, false, 1, &mut saved.bytes.clone());
                    }
                }
                let _ = self.io.set_alt(c.stream.number, 0);
            }
        }
        for (number, detached) in self.claims.iter().rev() {
            let _ = self
                .io
                .release(*number, *detached && self.online.load(Ordering::Acquire));
        }
        if self.leased {
            if let Ok(mut last) = RELEASED.lock() {
                *last = Some(std::time::Instant::now());
            }
            LEASE.store(false, Ordering::Release);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn invalid_fd_does_not_leave_exclusive_lease_owned() {
        for _ in 0..2 {
            let error = Device::probe(-1, true).err().expect("invalid fd must fail");
            assert_eq!(error, "invalid_usb_fd");
            assert!(!LEASE.load(Ordering::Acquire));
        }
    }
}
