//! Rust-owned asynchronous isochronous transport and cancellation lifetime.
//! Ported from NeriPlayer 113699bb; see ../../../../cpp/usbdriver/NERI_LICENSE.md.
use super::{
    device::{Candidate, Device},
    protocol::{self, Scheduler},
    sys,
};
use crate::pipeline::{Config, Core};
use std::{
    collections::VecDeque,
    ptr,
    sync::{
        atomic::{AtomicBool, AtomicU64, Ordering},
        Arc, Condvar, Mutex,
    },
    thread::{self, JoinHandle},
    time::{Duration, Instant},
};
struct Completion {
    done: AtomicBool,
    _device: Arc<Device>,
}
struct Slot {
    transfer: *mut sys::Transfer,
    buffer: Vec<u8>,
    completion: Box<Completion>,
    real: Vec<usize>,
    sequence: u64,
    submitted: bool,
    feedback: bool,
}
impl Slot {
    fn new(
        device: Arc<Device>,
        endpoint: u8,
        capacity: usize,
        packets: usize,
        feedback: bool,
    ) -> Result<Self, String> {
        let transfer = unsafe { sys::libusb_alloc_transfer(packets as i32) };
        if transfer.is_null() {
            return Err("usb_transfer_allocation_failed".into());
        }
        let mut slot = Self {
            transfer,
            buffer: vec![0; capacity * packets],
            completion: Box::new(Completion {
                done: AtomicBool::new(false),
                _device: device.clone(),
            }),
            real: vec![0; packets],
            sequence: 0,
            submitted: false,
            feedback,
        };
        unsafe {
            let t = &mut *transfer;
            t.handle = device.handle;
            t.endpoint = endpoint;
            t.kind = 1;
            t.timeout = 0;
            t.callback = Some(completed);
            t.user = (&mut *slot.completion as *mut Completion).cast();
            t.buffer = slot.buffer.as_mut_ptr();
            t.packet_count = packets as i32;
        }
        Ok(slot)
    }
    fn packets(&self) -> &[sys::Packet] {
        unsafe { std::slice::from_raw_parts((*self.transfer).packets.as_ptr(), self.real.len()) }
    }
    fn packets_mut(&mut self) -> &mut [sys::Packet] {
        unsafe {
            std::slice::from_raw_parts_mut((*self.transfer).packets.as_mut_ptr(), self.real.len())
        }
    }
    fn submit(&mut self) -> Result<(), String> {
        self.completion.done.store(false, Ordering::Release);
        let rc = unsafe { sys::libusb_submit_transfer(self.transfer) };
        if rc < 0 {
            return Err(format!("usb_submit_failed:{rc}"));
        }
        self.submitted = true;
        Ok(())
    }
    fn cancel(&mut self) {
        if self.submitted {
            unsafe {
                sys::libusb_cancel_transfer(self.transfer);
            }
        }
    }
    fn ready(&self) -> bool {
        self.submitted && self.completion.done.load(Ordering::Acquire)
    }
    fn retired(&mut self) {
        self.submitted = false;
    }
}
unsafe extern "C" fn completed(transfer: *mut sys::Transfer) {
    // SAFETY: Completion remains boxed until callback has retired. No locks or panics in callback.
    unsafe {
        let user = (*transfer).user.cast::<Completion>();
        (*user).done.store(true, Ordering::Release);
    }
}
impl Drop for Slot {
    fn drop(&mut self) {
        if self.submitted {
            // A panic or stuck kernel must not turn a late callback into a use-after-free.
            // Leaked Completion retains the device, duplicated fd and global ownership lease.
            std::mem::forget(std::mem::take(&mut self.buffer));
            let replacement = Box::new(Completion {
                done: AtomicBool::new(false),
                _device: self.completion._device.clone(),
            });
            std::mem::forget(std::mem::replace(&mut self.completion, replacement));
        } else {
            unsafe { sys::libusb_free_transfer(self.transfer) }
        }
    }
}
#[derive(Default)]
struct Command {
    playing: bool,
    revision: u64,
    acknowledged: u64,
    flush: bool,
}
pub struct Session {
    pub device: Arc<Device>,
    pub candidate: Candidate,
    pub rate: u32,
    pub core: Core,
    pub api: Mutex<()>,
    command: Mutex<Command>,
    wake: Condvar,
    worker: Mutex<Option<JoinHandle<()>>>,
    pub stop: AtomicBool,
    pub failed: AtomicBool,
    pub prepared: AtomicBool,
    pub completed: AtomicU64,
    staged: AtomicU64,
    replay: AtomicU64,
    transfers: AtomicU64,
    feedback_samples: AtomicU64,
    feedback_rejected: AtomicU64,
    feedback_hz: AtomicU64,
    urgent_audio: AtomicBool,
    errors: Mutex<String>,
    duration: Mutex<i32>,
}
impl Session {
    pub fn open(
        fd: i32,
        rate: u32,
        channels: u32,
        bits: u32,
        subslot: u32,
    ) -> Result<Arc<Self>, String> {
        if !(8000..=768000).contains(&rate)
            || !(1..=8).contains(&channels)
            || !(1..=4).contains(&subslot)
            || !(8..=32).contains(&bits)
            || bits > subslot * 8
        {
            return Err("invalid_usb_output_format".into());
        }
        let mut device = Device::probe(fd, true)?;
        let candidate = device.select(rate, channels, bits, subslot)?;
        let session = Arc::new(Self {
            device: Arc::new(device),
            candidate,
            rate,
            core: Core::default(),
            api: Mutex::new(()),
            command: Mutex::new(Command::default()),
            wake: Condvar::new(),
            worker: Mutex::new(None),
            stop: AtomicBool::new(false),
            failed: AtomicBool::new(false),
            prepared: AtomicBool::new(false),
            completed: AtomicU64::new(0),
            staged: AtomicU64::new(0),
            replay: AtomicU64::new(0),
            transfers: AtomicU64::new(0),
            feedback_samples: AtomicU64::new(0),
            feedback_rejected: AtomicU64::new(0),
            feedback_hz: AtomicU64::new((rate as f64).to_bits()),
            urgent_audio: AtomicBool::new(false),
            errors: Mutex::new("none".into()),
            duration: Mutex::new(3000),
        });
        let worker_session = session.clone();
        let handle = thread::Builder::new()
            .name("mei-usb-rust".into())
            .spawn(move || {
                let result =
                    std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| run(&worker_session)));
                match result {
                    Ok(Ok(())) => {}
                    Ok(Err(e)) => worker_session.fail(e),
                    Err(_) => worker_session.fail("usb_worker_panicked".into()),
                }
                worker_session.wake.notify_all();
            })
            .map_err(|_| "usb_worker_spawn_failed")?;
        *session.worker.lock().map_err(|_| "usb_worker_poisoned")? = Some(handle);
        Ok(session)
    }
    pub fn live(&self) -> bool {
        !self.stop.load(Ordering::Acquire)
            && !self.failed.load(Ordering::Acquire)
            && self.device.online.load(Ordering::Acquire)
    }
    fn fail(&self, error: String) {
        if let Ok(mut e) = self.errors.lock() {
            *e = error;
        }
        self.failed.store(true, Ordering::Release);
        self.wake.notify_all();
    }
    fn plan(&self) -> protocol::TransferPlan {
        protocol::transfer_plan(
            protocol::intervals(self.device.speed, self.candidate.output.interval),
            self.candidate.feedback.is_some(),
        )
    }
    fn transfer_bytes(&self) -> usize {
        self.plan().packets * self.candidate.output.capacity
    }
    pub fn prepare(&self, rate: i32, channels: i32, encoding: i32) -> bool {
        if !self.live() {
            return false;
        }
        if self.prepared.load(Ordering::Acquire) && !self.command(false, true) {
            return false;
        }
        let Ok(duration) = self.duration.lock() else {
            return false;
        };
        let bytes = self.transfer_bytes();
        let f = &self.candidate.format;
        let result = self
            .core
            .configure(Config {
                output_rate: self.rate as i32,
                output_channels: f.channels as i32,
                output_bits: f.bits as i32,
                output_bytes: f.subslot as i32,
                frame_bytes: self.candidate.frame_bytes() as i32,
                input_rate: rate,
                input_channels: channels,
                input_encoding: encoding,
                ring_ms: *duration,
                transfer_bytes: bytes as i32,
                transfer_count: self.plan().slots as i32,
            })
            .is_ok();
        self.prepared.store(result, Ordering::Release);
        result
    }
    pub fn resize(&self, duration: i32) -> bool {
        if !(1..=3000).contains(&duration) || !self.live() {
            return false;
        }
        let Ok(mut ms) = self.duration.lock() else {
            return false;
        };
        let bytes = self.transfer_bytes();
        if self.prepared.load(Ordering::Acquire)
            && self
                .core
                .resize(duration, bytes as i32, self.plan().slots as i32)
                .is_err()
        {
            return false;
        }
        *ms = duration;
        true
    }
    fn signal(&self) {
        self.wake.notify_all();
        // Wake libusb's blocking event poll as well as an idle worker.
        unsafe {
            sys::libusb_interrupt_event_handler(self.device.ctx);
        }
    }
    pub fn command(&self, playing: bool, flush: bool) -> bool {
        if !self.live() || (playing && !self.prepared.load(Ordering::Acquire)) {
            return false;
        }
        let Ok(mut c) = self.command.lock() else {
            return false;
        };
        c.playing = playing;
        c.flush = flush;
        c.revision = c.revision.wrapping_add(1);
        let revision = c.revision;
        self.signal();
        let until = Instant::now() + Duration::from_secs(2);
        while c.acknowledged != revision && self.live() {
            let Some(remaining) = until.checked_duration_since(Instant::now()) else {
                self.fail("usb_command_timeout".into());
                return false;
            };
            let Ok((next, _)) = self.wake.wait_timeout(c, remaining) else {
                return false;
            };
            c = next;
        }
        c.acknowledged == revision && self.live()
    }
    pub fn flush(&self) -> bool {
        // Kotlin resets started on seek and refills preroll before calling play again.
        self.command(false, true)
    }
    pub fn queued_frames(&self) -> u64 {
        (self.core.queued().unwrap_or(0) as u64
            + self.staged.load(Ordering::Acquire)
            + self.replay.load(Ordering::Acquire))
            / self.candidate.frame_bytes() as u64
    }
    pub fn close(&self) {
        self.stop.store(true, Ordering::Release);
        self.signal();
        let handle = self.worker.lock().ok().and_then(|mut w| w.take());
        if let Some(handle) = handle {
            let until = Instant::now() + Duration::from_secs(2);
            while !handle.is_finished() && Instant::now() < until {
                thread::sleep(Duration::from_millis(5));
            }
            if handle.is_finished() {
                let _ = handle.join();
            }
            // Otherwise detach: worker/transfer completions retain Device and its lease.
        }
    }
    pub fn report(&self) -> String {
        let snapshot = self.core.snapshot().unwrap_or_else(|_| {
            self.fail("usb_pcm_state_poisoned".into());
            Default::default()
        });
        let e = self
            .errors
            .lock()
            .map(|e| e.clone())
            .unwrap_or_else(|_| "state_poisoned".into());
        format!("backend=full-rust pcmBackend=rust terminalFailure={} transportFailed={} online={} outputRate={} outputBits={} outputChannels={} completedFrames={} queuedFrames={} stagedBytes={} replayBytes={} transfers={} feedbackHz={:.3} feedbackSamples={} feedbackRejected={} urgentAudio={} underrunBytes={} backpressureEvents={} error={}",self.failed.load(Ordering::Acquire),!self.device.online.load(Ordering::Acquire),self.device.online.load(Ordering::Acquire),self.rate,self.candidate.format.bits,self.candidate.format.channels,self.completed.load(Ordering::Acquire),self.queued_frames(),self.staged.load(Ordering::Acquire),self.replay.load(Ordering::Acquire),self.transfers.load(Ordering::Relaxed),f64::from_bits(self.feedback_hz.load(Ordering::Relaxed)),self.feedback_samples.load(Ordering::Relaxed),self.feedback_rejected.load(Ordering::Relaxed),self.urgent_audio.load(Ordering::Relaxed),snapshot.integers[7],snapshot.integers[13],e)
    }
}
// Returns completed real PCM bytes and preserves only the unsent PCM of each packet.
fn settle(slot: &Slot, frame: usize, preserve: bool, replay: &mut VecDeque<u8>) -> (usize, bool) {
    settle_packets(
        unsafe { (*slot.transfer).status },
        slot.packets(),
        &slot.real,
        &slot.buffer,
        frame,
        preserve.then_some(replay),
    )
}
fn settle_packets(
    status: i32,
    packets: &[sys::Packet],
    real: &[usize],
    buffer: &[u8],
    frame: usize,
    mut replay: Option<&mut VecDeque<u8>>,
) -> (usize, bool) {
    let mut completed = 0;
    let mut offset = 0;
    let mut bad = false;
    for (packet, real) in packets.iter().zip(real) {
        let actual = if (status == sys::COMPLETED || status == sys::CANCELLED)
            && packet.status == sys::COMPLETED
        {
            (packet.actual.min(packet.length) as usize / frame) * frame
        } else {
            0
        };
        let sent = actual.min(*real);
        completed += sent;
        if let Some(replay) = replay.as_mut() {
            if sent < *real {
                replay.extend(&buffer[offset + sent..offset + real]);
            }
        }
        if status == sys::COMPLETED
            && (packet.status != sys::COMPLETED || actual < packet.length as usize)
        {
            bad = true;
        }
        offset += packet.length as usize;
    }
    (completed, bad)
}
fn retire(
    s: &Session,
    slot: &mut Slot,
    preserve: bool,
    replay: &mut VecDeque<u8>,
) -> Result<(), String> {
    slot.retired();
    let status = unsafe { (*slot.transfer).status };
    if slot.feedback {
        return Ok(());
    }
    let (completed, bad) = settle(slot, s.candidate.frame_bytes(), preserve, replay);
    let real: usize = slot.real.iter().sum();
    s.staged.fetch_sub(real as u64, Ordering::AcqRel);
    s.completed.fetch_add(
        (completed / s.candidate.frame_bytes()) as u64,
        Ordering::AcqRel,
    );
    s.replay.store(replay.len() as u64, Ordering::Release);
    s.transfers.fetch_add(1, Ordering::Relaxed);
    if status == 5 || slot.packets().iter().any(|p| p.status == 5) {
        s.device.online.store(false, Ordering::Release);
        return Err("usb_device_detached".into());
    }
    if status != sys::COMPLETED && status != sys::CANCELLED {
        return Err(format!("usb_transfer_failed:{status}"));
    }
    if bad {
        return Err("usb_iso_packet_failed_or_short".into());
    }
    Ok(())
}
fn pump(s: &Session) -> Result<(), String> {
    let rc = unsafe {
        sys::libusb_handle_events_timeout_completed(
            s.device.ctx,
            &sys::Timeval {
                seconds: 0,
                micros: 100000,
            },
            ptr::null_mut(),
        )
    };
    if rc < 0 && rc != -10 {
        Err(format!("usb_event_loop_failed:{rc}"))
    } else {
        Ok(())
    }
}
fn cancel_all(
    s: &Session,
    slots: &mut [Slot],
    preserve: bool,
    replay: &mut VecDeque<u8>,
) -> Result<(), String> {
    for slot in slots.iter_mut() {
        slot.cancel();
    }
    let mut error = None;
    let mut pending = VecDeque::new();
    // Do not free buffers on a timeout. Even if libusb stops delivering callbacks, this
    // thread and its duplicated fd stay quarantined until every submitted transfer retires.
    while slots.iter().any(|slot| slot.submitted && !slot.ready()) {
        if let Err(e) = pump(s) {
            s.fail(e);
            thread::sleep(Duration::from_millis(5));
        }
    }
    slots.sort_by_key(|slot| slot.sequence);
    for slot in slots.iter_mut().filter(|slot| slot.submitted) {
        if let Err(e) = retire(s, slot, preserve, &mut pending) {
            error = Some(e);
        }
    }
    if preserve {
        pending.append(replay);
        *replay = pending;
    }
    s.replay.store(replay.len() as u64, Ordering::Release);
    error.map_or(Ok(()), Err)
}
fn fill(
    s: &Session,
    slot: &mut Slot,
    scheduler: &mut Scheduler,
    replay: &mut VecDeque<u8>,
) -> Result<(), String> {
    let frame = s.candidate.frame_bytes();
    let mut offset = 0;
    for i in 0..slot.real.len() {
        let length = scheduler.next() * frame;
        if offset + length > slot.buffer.len() {
            return Err("usb_packet_capacity_exceeded".into());
        }
        let target = &mut slot.buffer[offset..offset + length];
        target.fill(0);
        let count = target.len().min(replay.len());
        for byte in &mut target[..count] {
            *byte = replay.pop_front().unwrap_or(0);
        }
        let real = count
            + s.core
                .fill(&mut target[count..], true)
                .map_err(str::to_owned)?;
        slot.real[i] = real;
        slot.packets_mut()[i] = sys::Packet {
            length: length as u32,
            actual: 0,
            status: 0,
        };
        offset += length;
    }
    unsafe {
        (*slot.transfer).length = offset as i32;
    }
    s.replay.store(replay.len() as u64, Ordering::Release);
    Ok(())
}
fn start_feedback(slot: &mut Slot) -> Result<(), String> {
    let len = slot.buffer.len().min(4);
    slot.buffer.fill(0);
    slot.packets_mut()[0] = sys::Packet {
        length: len as u32,
        actual: 0,
        status: 0,
    };
    unsafe {
        (*slot.transfer).length = len as i32;
    }
    slot.submit()
}
fn run(s: &Arc<Session>) -> Result<(), String> {
    // Match the original Android audio worker priority; failure is non-fatal.
    #[cfg(target_os = "android")]
    s.urgent_audio.store(
        unsafe { sys::setpriority(0, 0, -19) } == 0,
        Ordering::Release,
    );
    let plan = s.plan();
    let packets = plan.packets;
    let mut slots = Vec::with_capacity(plan.slots + 4);
    for _ in 0..plan.slots {
        slots.push(Slot::new(
            s.device.clone(),
            s.candidate.output.address,
            s.candidate.output.capacity,
            packets,
            false,
        )?);
    }
    if let Some(feedback) = &s.candidate.feedback {
        for _ in 0..4 {
            slots.push(Slot::new(
                s.device.clone(),
                feedback.address,
                feedback.capacity.min(4),
                1,
                true,
            )?);
        }
    }
    let ips = protocol::intervals(s.device.speed, s.candidate.output.interval);
    let mut scheduler = Scheduler::new(
        s.rate,
        ips,
        s.candidate.output.capacity / s.candidate.frame_bytes(),
    )
    .ok_or("usb_packet_capacity_exceeded")?;
    let mut replay = VecDeque::new();
    let mut revision = 0;
    let mut sequence = 0;
    let mut playing = false;
    let mut progress = Instant::now();
    let mut feedback_at = Instant::now();
    let mut rate = s.rate as f64;
    let mut terminal = None;
    while !s.stop.load(Ordering::Acquire)
        && !s.failed.load(Ordering::Acquire)
        && s.device.online.load(Ordering::Acquire)
    {
        let (next_revision, wanted, flush) = match s.command.lock() {
            Ok(c) => (c.revision, c.playing, c.flush),
            Err(_) => {
                terminal = Some("usb_command_poisoned".into());
                break;
            }
        };
        if next_revision != revision {
            if let Err(e) = cancel_all(s, &mut slots, !flush, &mut replay) {
                terminal = Some(e);
                break;
            }
            if flush {
                replay.clear();
                s.replay.store(0, Ordering::Release);
                s.completed.store(0, Ordering::Release);
                if s.core
                    .clear()
                    .and_then(|_| s.core.reset_counters())
                    .is_err()
                {
                    terminal = Some("usb_flush_failed".into());
                    break;
                }
            }
            revision = next_revision;
            playing = wanted;
            progress = Instant::now();
            feedback_at = Instant::now();
            if let Ok(mut c) = s.command.lock() {
                c.acknowledged = revision;
                s.wake.notify_all();
            }
        }
        if !playing {
            if let Ok(c) = s.command.lock() {
                let _ = s.wake.wait_timeout(c, Duration::from_millis(20));
            }
            continue;
        }
        let mut feedback_error = None;
        for slot in slots.iter_mut().filter(|slot| slot.feedback) {
            if slot.ready() {
                slot.retired();
                let t = unsafe { &*slot.transfer };
                let p = slot.packets()[0];
                if t.status == sys::COMPLETED && p.status == sys::COMPLETED {
                    if let Some(sample) = protocol::feedback(
                        &slot.buffer[..(p.actual as usize).min(slot.buffer.len())],
                        s.device.speed >= 3,
                        s.rate,
                    ) {
                        rate += (sample - rate) * 0.125;
                        if scheduler.set_rate(rate) {
                            feedback_at = Instant::now();
                            s.feedback_samples.fetch_add(1, Ordering::Relaxed);
                            s.feedback_hz.store(rate.to_bits(), Ordering::Relaxed);
                        } else {
                            feedback_error = Some("usb_feedback_exceeds_endpoint".into());
                        }
                    } else {
                        s.feedback_rejected.fetch_add(1, Ordering::Relaxed);
                    }
                } else if t.status != sys::CANCELLED {
                    feedback_error = Some(format!("usb_feedback_transfer_failed:{}", t.status));
                }
            }
            if !slot.submitted {
                if let Err(e) = start_feedback(slot) {
                    feedback_error = Some(e);
                }
            }
        }
        if let Some(e) = feedback_error {
            terminal = Some(e);
            break;
        }
        if s.candidate.feedback.is_some() && feedback_at.elapsed() > Duration::from_secs(2) {
            terminal = Some("usb_feedback_timeout".into());
            break;
        }
        // Process in submission order so cancelled replay and completed media clocks agree.
        slots.sort_by_key(|slot| slot.sequence);
        for slot in slots.iter_mut().filter(|slot| !slot.feedback) {
            if slot.ready() {
                if let Err(e) = retire(s, slot, false, &mut replay) {
                    terminal = Some(e);
                    break;
                }
                progress = Instant::now();
            }
            if !slot.submitted {
                if let Err(e) = fill(s, slot, &mut scheduler, &mut replay) {
                    terminal = Some(e);
                    break;
                }
                sequence += 1;
                slot.sequence = sequence;
                let real: usize = slot.real.iter().sum();
                match slot.submit() {
                    Ok(()) => {
                        s.staged.fetch_add(real as u64, Ordering::AcqRel);
                    }
                    Err(e) => {
                        terminal = Some(e);
                        break;
                    }
                }
            }
        }
        if terminal.is_some() {
            break;
        }
        if progress.elapsed() > Duration::from_secs(2) {
            terminal = Some("usb_audio_completion_timeout".into());
            break;
        }
        if let Err(e) = pump(s) {
            terminal = Some(e);
            break;
        }
    }
    if let Some(e) = terminal {
        s.fail(e);
    }
    let result = cancel_all(s, &mut slots, false, &mut replay);
    s.staged.store(0, Ordering::Release);
    s.replay.store(0, Ordering::Release);
    result
}
#[cfg(test)]
mod tests {
    use super::*;
    fn packets() -> [sys::Packet; 3] {
        [
            sys::Packet {
                length: 8,
                actual: 8,
                status: 0,
            },
            sys::Packet {
                length: 8,
                actual: 4,
                status: 0,
            },
            sys::Packet {
                length: 8,
                actual: 0,
                status: 3,
            },
        ]
    }
    #[test]
    fn cancel_preserves_only_unsent_pcm_without_silence() {
        let mut replay = VecDeque::new();
        let buffer: Vec<u8> = (0..24).collect();
        let (sent, bad) = settle_packets(
            sys::CANCELLED,
            &packets(),
            &[8, 8, 4],
            &buffer,
            4,
            Some(&mut replay),
        );
        assert_eq!(sent, 12);
        assert!(!bad);
        assert_eq!(
            replay.into_iter().collect::<Vec<_>>(),
            vec![12, 13, 14, 15, 16, 17, 18, 19]
        );
    }
    #[test]
    fn failed_or_zero_actual_packets_never_advance_clock() {
        let mut ps = packets();
        ps[0].actual = 0;
        ps[1].status = 1;
        let (sent, bad) = settle_packets(sys::COMPLETED, &ps, &[8, 8, 4], &[0; 24], 4, None);
        assert_eq!(sent, 0);
        assert!(bad);
        let (sent, bad) = settle_packets(1, &packets(), &[8, 8, 4], &[0; 24], 4, None);
        assert_eq!(sent, 0);
        assert!(!bad);
    }
    #[test]
    fn completion_clamps_to_packet_and_whole_pcm_frames() {
        let p = [sys::Packet {
            length: 8,
            actual: 100,
            status: 0,
        }];
        assert_eq!(settle_packets(0, &p, &[4], &[0; 8], 4, None), (4, false));
        let p = [sys::Packet {
            length: 8,
            actual: 7,
            status: 0,
        }];
        assert_eq!(settle_packets(0, &p, &[8], &[0; 8], 4, None), (4, true));
    }
}
