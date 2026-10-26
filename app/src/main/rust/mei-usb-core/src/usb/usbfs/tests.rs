use super::*;
use std::{collections::VecDeque, fs::File, os::fd::RawFd};
struct State {
    caps: u32,
    pending: Vec<usize>,
    complete: VecDeque<usize>,
    calls: Vec<u32>,
    fail_submit: bool,
    disconnected: bool,
    driver: bool,
    fallback_claim: bool,
    fail_claim: bool,
    control_error: Option<i32>,
    unknown: Option<usize>,
    reap_interrupts: usize,
    configuration: Option<Vec<u8>>,
}
pub(crate) struct Fake(Mutex<State>);
impl Default for Fake {
    fn default() -> Self {
        Self(Mutex::new(State {
            caps: 16,
            pending: vec![],
            complete: VecDeque::new(),
            calls: vec![],
            fail_submit: false,
            disconnected: false,
            driver: false,
            fallback_claim: false,
            fail_claim: false,
            control_error: None,
            unknown: None,
            reap_interrupts: 0,
            configuration: None,
        }))
    }
}
impl Fake {
    pub(crate) fn finish(&self, status: i32) {
        let mut state = self.0.lock().unwrap();
        for address in state.pending.clone() {
            if !state.complete.contains(&address) {
                // A fake kernel accesses exactly the submitted stable UAPI allocation.
                unsafe {
                    let urb = &mut *(address as *mut os::Urb);
                    urb.status = status;
                    let packets = std::slice::from_raw_parts_mut(
                        (address + std::mem::size_of::<os::Urb>()) as *mut Packet,
                        urb.packets as usize,
                    );
                    for p in packets {
                        p.actual = if status == 0 { p.length } else { 0 };
                        p.status = status;
                    }
                    if status == 0 && urb.endpoint & 0x80 != 0 {
                        std::slice::from_raw_parts_mut(urb.buffer, urb.length as usize).fill(0xa5);
                    }
                }
                state.complete.push_back(address);
            }
        }
    }
}
impl Fake {
    pub(crate) fn pending(&self) -> usize {
        self.0.lock().unwrap().pending.len()
    }
    pub(crate) fn disconnect(&self) {
        self.finish(NO_DEVICE);
        self.0.lock().unwrap().disconnected = true;
    }
}
pub(crate) fn audio() -> (UsbFs, Arc<Fake>) {
    let fake = Arc::new(Fake::default());
    let body = [
        9, 4, 0, 0, 0, 1, 1, 0, 0, 9, 0x24, 1, 0, 1, 9, 0, 1, 1, 9, 4, 1, 0, 0, 1, 2, 0, 0, 9, 4,
        1, 1, 1, 1, 2, 0, 0, 7, 0x24, 1, 1, 1, 1, 0, 11, 0x24, 2, 1, 2, 2, 16, 1, 0x80, 0xbb, 0, 7,
        5, 1, 9, 192, 0, 1,
    ];
    let length = body.len() + 9;
    let mut config = vec![9, 2, length as u8, (length >> 8) as u8, 2, 1, 0, 0x80, 50];
    config.extend_from_slice(&body);
    fake.0.lock().unwrap().configuration = Some(config);
    (device(fake.clone()), fake)
}
unsafe impl os::Kernel for Fake {
    unsafe fn call(&self, _fd: RawFd, request: u32, arg: *mut c_void) -> io::Result<i32> {
        let mut s = self.0.lock().unwrap();
        s.calls.push(request);
        // SAFETY: production wrapper constructs the same valid UAPI objects for a
        // fake kernel as for the real kernel; tests keep ownership until REAP.
        unsafe {
            match request {
                os::CAPABILITIES => {
                    *arg.cast::<u32>() = s.caps;
                    Ok(0)
                }
                os::SPEED => Ok(3),
                os::SUBMIT => {
                    if s.fail_submit {
                        return Err(io::Error::from_raw_os_error(12));
                    }
                    let urb = &*arg.cast::<os::Urb>();
                    assert_eq!(urb.kind, 0);
                    assert_eq!(urb.flags, 2);
                    assert!(urb.packets > 0);
                    assert!(!urb.buffer.is_null());
                    s.pending.push(arg as usize);
                    Ok(0)
                }
                os::DISCARD => {
                    let address = arg as usize;
                    if !s.pending.contains(&address) {
                        return Err(io::Error::from_raw_os_error(os::EINVAL));
                    }
                    if !s.complete.contains(&address) {
                        let urb = &mut *arg.cast::<os::Urb>();
                        urb.status = -os::ENOENT;
                        let iso = (address + std::mem::size_of::<os::Urb>()) as *mut Packet;
                        for i in 0..urb.packets as usize {
                            (*iso.add(i)).status = -os::ECONNRESET;
                            (*iso.add(i)).actual = 0;
                        }
                        s.complete.push_back(address);
                    }
                    Ok(0)
                }
                os::REAP => {
                    if s.reap_interrupts > 0 {
                        s.reap_interrupts -= 1;
                        return Err(io::Error::from_raw_os_error(os::EINTR));
                    }
                    if let Some(address) = s.unknown.take() {
                        *arg.cast::<usize>() = address;
                        return Ok(0);
                    }
                    if let Some(address) = s.complete.pop_front() {
                        s.pending.retain(|p| *p != address);
                        *arg.cast::<usize>() = address;
                        Ok(0)
                    } else {
                        Err(io::Error::from_raw_os_error(if s.disconnected {
                            os::ENODEV
                        } else {
                            os::EAGAIN
                        }))
                    }
                }
                os::CONTROL => {
                    if let Some(error) = s.control_error {
                        return Err(io::Error::from_raw_os_error(error));
                    }
                    let c = &mut *arg.cast::<os::Control>();
                    assert_eq!(c.timeout, 250);
                    let target = std::slice::from_raw_parts_mut(c.data, c.length as usize);
                    let config = [9, 2, 18, 0, 1, 1, 0, 0x80, 50, 9, 4, 0, 0, 0, 1, 1, 0, 0];
                    let bytes: Vec<u8> = match (c.kind, c.request, c.value) {
                        (0x80, 6, 0x100) => {
                            let mut d = vec![0; 18];
                            d[0] = 18;
                            d[1] = 1;
                            d[17] = 1;
                            d
                        }
                        (0x80, 6, 0x200) => {
                            s.configuration.clone().unwrap_or_else(|| config.to_vec())
                        }
                        (0x80, 8, 0) => vec![1],
                        _ => vec![0x33; target.len()],
                    };
                    let n = target.len().min(bytes.len());
                    target[..n].copy_from_slice(&bytes[..n]);
                    Ok(n as i32)
                }
                os::GET_DRIVER => {
                    if s.driver {
                        Ok(0)
                    } else {
                        Err(io::Error::from_raw_os_error(os::ENODATA))
                    }
                }
                os::DISCONNECT_CLAIM => {
                    if s.fallback_claim {
                        Err(io::Error::from_raw_os_error(os::ENOTTY))
                    } else if s.fail_claim {
                        Err(io::Error::from_raw_os_error(16))
                    } else {
                        s.driver = false;
                        Ok(0)
                    }
                }
                os::DRIVER_IOCTL => {
                    let d = &*arg.cast::<os::DriverIoctl>();
                    s.driver = d.code as u32 == os::CONNECT;
                    Ok(0)
                }
                os::CLAIM => {
                    if s.fail_claim {
                        Err(io::Error::from_raw_os_error(16))
                    } else {
                        Ok(0)
                    }
                }
                os::RELEASE | os::SET_INTERFACE => Ok(0),
                _ => panic!("unexpected ioctl {request:x}"),
            }
        }
    }
    fn wait(&self, _fd: RawFd, _wake: RawFd, _timeout: i32) -> io::Result<()> {
        std::thread::sleep(Duration::from_millis(1));
        Ok(())
    }
}
fn device(fake: Arc<Fake>) -> UsbFs {
    UsbFs::from_fd(File::open("/dev/null").unwrap().into(), fake).unwrap()
}
fn request(token: usize) -> Request {
    let mut request = Request::new(token, 8, 2).unwrap();
    request.packets[0].length = 8;
    request.packets[1].length = 4;
    request.data[..12].fill(token as u8);
    request
}
fn submit(io: &UsbFs, request: Request, endpoint: u8) {
    if let Err(e) = io.submit(request, endpoint) {
        panic!("submit: {}", e.error)
    }
}
#[test]
fn submit_reap_returns_the_same_buffer_and_packet_results() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    let request = request(7);
    let address = request.data.as_ptr();
    submit(&io, request, 0x81);
    assert_eq!(io.pending(), 1);
    fake.finish(0);
    let mut complete = vec![];
    io.reap(&mut complete).unwrap();
    assert_eq!(io.pending(), 0);
    let done = complete.pop().unwrap();
    assert_eq!(done.token, 7);
    assert_eq!(done.data.as_ptr(), address);
    assert_eq!(&done.data[..12], &[0xa5; 12]);
    assert_eq!((done.packets[0].actual, done.packets[1].actual), (8, 4));
}
#[test]
fn failed_submission_returns_unsubmitted_ownership() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    fake.0.lock().unwrap().fail_submit = true;
    let error = io.submit(request(9), 1).err().unwrap();
    assert_eq!(error.error.raw_os_error(), Some(12));
    assert_eq!(error.request.data[0], 9);
    assert_eq!(io.pending(), 0);
}
#[test]
fn cancelling_never_releases_requests_before_reap() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    submit(&io, request(1), 1);
    submit(&io, request(2), 1);
    io.cancel_all().unwrap();
    assert_eq!(io.pending(), 2);
    fake.0.lock().unwrap().complete.make_contiguous().reverse();
    let mut complete = vec![];
    io.reap(&mut complete).unwrap();
    assert_eq!(io.pending(), 0);
    assert_eq!(
        complete.iter().map(|r| r.token).collect::<Vec<_>>(),
        vec![2, 1]
    );
    assert!(complete.iter().all(|r| r.status == CANCELLED
        && r.packets
            .iter()
            .all(|p| p.actual == 0 && p.status == CANCELLED)));
}
#[test]
fn disconnect_is_drained_before_enodev_is_accepted() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    submit(&io, request(1), 1);
    submit(&io, request(2), 0x81);
    fake.finish(NO_DEVICE);
    fake.0.lock().unwrap().disconnected = true;
    let mut complete = vec![];
    io.reap(&mut complete).unwrap();
    assert_eq!(io.pending(), 0);
    assert_eq!(complete.len(), 2);
    assert!(complete.iter().all(|r| r.status == NO_DEVICE));
}
#[test]
fn unknown_kernel_address_does_not_free_pending_requests() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    submit(&io, request(1), 1);
    fake.0.lock().unwrap().unknown = Some(123);
    let mut complete = vec![];
    assert!(io.reap(&mut complete).is_err());
    assert_eq!(io.pending(), 1);
    io.cancel_all().unwrap();
    io.reap(&mut complete).unwrap();
    assert_eq!(io.pending(), 0);
}
#[test]
fn interrupted_reap_does_not_lose_completions() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    submit(&io, request(1), 1);
    fake.finish(0);
    fake.0.lock().unwrap().reap_interrupts = 2;
    let mut complete = vec![];
    io.reap(&mut complete).unwrap();
    assert_eq!(complete.len(), 1);
    assert_eq!(io.pending(), 0);
}
#[test]
fn failed_claim_reattaches_driver_and_release_restores_successful_claim() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    {
        let mut s = fake.0.lock().unwrap();
        s.driver = true;
        s.fallback_claim = true;
        s.fail_claim = true;
    }
    assert!(io.claim(1).is_err());
    assert!(fake.0.lock().unwrap().driver);
    fake.0.lock().unwrap().fail_claim = false;
    let detached = io.claim(1).unwrap();
    assert!(detached);
    assert!(!fake.0.lock().unwrap().driver);
    io.release(1, detached).unwrap();
    assert!(fake.0.lock().unwrap().driver);
}
#[test]
fn configuration_and_control_use_only_the_granted_fd() {
    let fake = Arc::new(Fake::default());
    let io = device(fake.clone());
    let config = io.configuration().unwrap();
    assert_eq!(config.value, 1);
    assert_eq!(config.alts.len(), 1);
    fake.0.lock().unwrap().control_error = Some(110);
    assert_eq!(
        io.control(0xa1, 1, 0x100, 0, &mut [0; 4], 250)
            .unwrap_err()
            .raw_os_error(),
        Some(110)
    );
}
#[test]
fn missing_safe_disconnect_capability_is_rejected_before_submission() {
    let fake = Arc::new(Fake::default());
    fake.0.lock().unwrap().caps = 0;
    let error = UsbFs::from_fd(File::open("/dev/null").unwrap().into(), fake.clone())
        .err()
        .unwrap();
    assert_eq!(error.kind(), io::ErrorKind::Unsupported);
    assert!(!fake.0.lock().unwrap().calls.contains(&os::SUBMIT));
}
#[test]
fn syscall_numbers_match_linux_uapi() {
    assert_eq!(os::SET_INTERFACE, 0x80085504);
    assert_eq!(os::GET_DRIVER, 0x41045508);
    assert_eq!(os::DISCARD, 0x550b);
    assert_eq!(os::CLAIM, 0x8004550f);
    assert_eq!(os::RELEASE, 0x80045510);
    assert_eq!(os::CAPABILITIES, 0x8004551a);
    assert_eq!(os::SPEED, 0x551f);
    let wide = std::mem::size_of::<usize>() == 8;
    assert_eq!(os::CONTROL, if wide { 0xc0185500 } else { 0xc0105500 });
    assert_eq!(os::SUBMIT, if wide { 0x8038550a } else { 0x802c550a });
    assert_eq!(os::REAP, if wide { 0x4008550d } else { 0x4004550d });
}
#[test]
fn real_wakeup_interrupts_a_blocking_poll_without_a_usb_device() {
    use os::Kernel;
    let wake = Arc::new(os::Wake::new().unwrap());
    let other = wake.clone();
    let thread = std::thread::spawn(move || {
        std::thread::sleep(Duration::from_millis(10));
        other.signal();
    });
    let start = Instant::now();
    os::System.wait(-1, wake.fd(), 1000).unwrap();
    assert!(start.elapsed() < Duration::from_millis(500));
    wake.drain();
    thread.join().unwrap();
}

#[test]
fn mutated_packet_count_is_rejected_before_exposing_any_memory() {
    let fake = Arc::new(Fake::default());
    let io = device(fake);
    let mut request = request(1);
    request.packets.resize(128, Packet::default());
    assert!(io.submit(request, 1).is_err());
    assert_eq!(io.pending(), 0);
}
