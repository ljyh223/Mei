//! Rust usbfs transport for an already-authorized Android/Linux device fd.
//! The kernel owns submitted URBs until REAP returns their exact address. Pending
//! buffers are held privately; callers receive them back only after completion.
mod descriptors;
pub mod os;
pub use descriptors::{Alt, Configuration, Endpoint};
pub use os::Packet;
use std::{
    ffi::c_void,
    io,
    mem::MaybeUninit,
    os::fd::{AsRawFd, BorrowedFd, OwnedFd},
    ptr,
    sync::{Arc, Mutex},
    time::{Duration, Instant},
};
pub const COMPLETED: i32 = 0;
pub const CANCELLED: i32 = -os::ENOENT;
pub const NO_DEVICE: i32 = -os::ENODEV;
fn normalize(status: i32) -> i32 {
    if status == -os::ECONNRESET {
        CANCELLED
    } else {
        status
    }
}
fn poisoned() -> io::Error {
    io::Error::other("usbfs_pending_state_poisoned")
}
pub struct Request {
    pub token: usize,
    pub data: Vec<u8>,
    pub packets: Vec<Packet>,
    pub status: i32,
    storage: Vec<MaybeUninit<usize>>,
    packet_count: usize,
}
impl Request {
    pub fn new(token: usize, capacity: usize, packets: usize) -> io::Result<Self> {
        if !(1..=128).contains(&packets) || !(1..=49152).contains(&capacity) {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "usbfs_request_geometry",
            ));
        }
        let bytes = std::mem::size_of::<os::Urb>() + std::mem::size_of::<Packet>() * packets;
        Ok(Self {
            token,
            data: vec![0; capacity * packets],
            packets: vec![Packet::default(); packets],
            status: 0,
            storage: vec![MaybeUninit::zeroed(); bytes.div_ceil(std::mem::size_of::<usize>())],
            packet_count: packets,
        })
    }
    fn address(&self) -> usize {
        self.storage.as_ptr() as usize
    }
    fn prepare(&mut self, endpoint: u8) -> io::Result<()> {
        if self.packets.len() != self.packet_count {
            return Err(io::Error::other("usbfs_packet_count_changed"));
        }
        let length = self
            .packets
            .iter()
            .try_fold(0usize, |sum, p| sum.checked_add(p.length as usize))
            .ok_or_else(|| io::Error::other("usbfs_packet_length_overflow"))?;
        if length > self.data.len() || length > i32::MAX as usize {
            return Err(io::Error::other("usbfs_packet_exceeds_buffer"));
        }
        // SAFETY: usize storage is aligned for the UAPI header and packet descriptors,
        // sized once, and never resized. This request has not yet been submitted.
        unsafe {
            let p = self.storage.as_mut_ptr().cast::<os::Urb>();
            ptr::write(
                p,
                os::Urb {
                    kind: 0,
                    endpoint,
                    status: 0,
                    flags: 2,
                    buffer: self.data.as_mut_ptr(),
                    length: length as i32,
                    actual: 0,
                    start_frame: 0,
                    packets: self.packets.len() as i32,
                    errors: 0,
                    signal: 0,
                    user: ptr::null_mut(),
                },
            );
            let iso = p
                .cast::<u8>()
                .add(std::mem::size_of::<os::Urb>())
                .cast::<Packet>();
            for (i, packet) in self.packets.iter().enumerate() {
                ptr::write(
                    iso.add(i),
                    Packet {
                        length: packet.length,
                        actual: 0,
                        status: 0,
                    },
                );
            }
        }
        Ok(())
    }
    fn finish(&mut self) -> io::Result<()> {
        // SAFETY: called only after the kernel has returned ownership through REAP.
        unsafe {
            let p = self.storage.as_ptr().cast::<os::Urb>();
            let header = ptr::read(p);
            if header.packets != self.packets.len() as i32 {
                return Err(io::Error::other("usbfs_reaped_packet_count_changed"));
            }
            self.status = normalize(header.status);
            let iso = p
                .cast::<u8>()
                .add(std::mem::size_of::<os::Urb>())
                .cast::<Packet>();
            for (i, packet) in self.packets.iter_mut().enumerate() {
                let done = ptr::read(iso.add(i));
                if done.length != packet.length || done.actual > packet.length {
                    return Err(io::Error::other("usbfs_reaped_packet_length_invalid"));
                }
                packet.actual = done.actual;
                packet.status = normalize(done.status);
            }
        }
        Ok(())
    }
}
// Err returns the unsubmitted request to its owner, so no accepted kernel buffer
// can accidentally be freed on an error path.
pub struct SubmitError {
    pub error: io::Error,
    pub request: Request,
}
pub struct UsbFs {
    fd: OwnedFd,
    kernel: Arc<dyn os::Kernel>,
    wake: os::Wake,
    pending: Mutex<Vec<Request>>,
    speed: i32,
}
impl UsbFs {
    pub fn open(fd: i32) -> io::Result<Self> {
        if fd < 0 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "invalid_usb_fd",
            ));
        }
        // SAFETY: JNI caller retains its UsbDeviceConnection while we duplicate its fd.
        let fd = unsafe { BorrowedFd::borrow_raw(fd) }.try_clone_to_owned()?;
        Self::from_fd(fd, Arc::new(os::System))
    }
    fn from_fd(fd: OwnedFd, kernel: Arc<dyn os::Kernel>) -> io::Result<Self> {
        let mut caps = 0u32;
        unsafe {
            kernel.call(
                fd.as_raw_fd(),
                os::CAPABILITIES,
                (&mut caps as *mut u32).cast(),
            )
        }?;
        // API 29+ kernels normally provide this capability. Without it, disconnect
        // cannot safely return submitted buffers, so reject the route explicitly.
        if caps & os::REAP_AFTER_DISCONNECT == 0 {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "usbfs_reap_after_disconnect_required",
            ));
        }
        let speed = unsafe { kernel.call(fd.as_raw_fd(), os::SPEED, ptr::null_mut()) }?;
        if !matches!(speed, 1 | 2 | 3 | 5 | 6) {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "usbfs_unknown_device_speed",
            ));
        }
        Ok(Self {
            fd,
            kernel,
            wake: os::Wake::new()?,
            pending: Mutex::new(Vec::with_capacity(32)),
            speed,
        })
    }
    pub fn speed(&self) -> i32 {
        self.speed
    }
    fn call<T>(&self, request: u32, arg: &mut T) -> io::Result<i32> {
        // SAFETY: each caller selects the ioctl matching T and keeps arg alive.
        unsafe {
            self.kernel
                .call(self.fd.as_raw_fd(), request, (arg as *mut T).cast())
        }
    }
    pub fn control(
        &self,
        kind: u8,
        request: u8,
        value: u16,
        index: u16,
        data: &mut [u8],
        timeout: u32,
    ) -> io::Result<usize> {
        if data.len() > u16::MAX as usize {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "usbfs_control_length",
            ));
        }
        let mut setup = os::Control {
            kind,
            request,
            value,
            index,
            length: data.len() as u16,
            timeout,
            data: data.as_mut_ptr(),
        };
        let result = self.call(os::CONTROL, &mut setup)? as usize;
        if result > data.len() {
            return Err(io::Error::other("usbfs_control_overflow"));
        }
        Ok(result)
    }
    pub fn configuration(&self) -> Result<Configuration, String> {
        let mut device = [0; 18];
        if self
            .control(0x80, 6, 0x100, 0, &mut device, 250)
            .map_err(|e| format!("usbfs_device_descriptor:{e}"))?
            != 18
            || device[0] != 18
            || device[1] != 1
            || device[17] == 0
        {
            return Err("invalid_usb_device_descriptor".into());
        }
        let mut active = [0];
        let active = if self.control(0x80, 8, 0, 0, &mut active, 250).ok() == Some(1) {
            active[0]
        } else {
            0
        };
        for index in 0..device[17] {
            let mut head = [0; 9];
            if self
                .control(0x80, 6, 0x200 | index as u16, 0, &mut head, 250)
                .map_err(|e| format!("usbfs_configuration_header:{e}"))?
                != 9
                || head[0] != 9
                || head[1] != 2
            {
                return Err("invalid_usb_configuration_header".into());
            }
            if active != 0 && head[5] != active {
                continue;
            }
            let length = u16::from_le_bytes([head[2], head[3]]) as usize;
            if length < 9 {
                return Err("invalid_usb_configuration_length".into());
            }
            let mut data = vec![0; length];
            if self
                .control(0x80, 6, 0x200 | index as u16, 0, &mut data, 250)
                .map_err(|e| format!("usbfs_configuration_descriptor:{e}"))?
                != length
            {
                return Err("truncated_usb_configuration".into());
            }
            return descriptors::parse(&data, self.speed).map_err(str::to_owned);
        }
        Err("usbfs_active_configuration_missing".into())
    }
    pub fn claim(&self, number: u8) -> io::Result<bool> {
        let mut driver = os::GetDriver {
            interface: number as u32,
            driver: [0; 256],
        };
        let active = match self.call(os::GET_DRIVER, &mut driver) {
            Ok(_) => true,
            Err(e) if e.raw_os_error() == Some(os::ENODATA) => false,
            Err(e) => return Err(e),
        };
        if active {
            let mut claim = os::DisconnectClaim {
                interface: number as u32,
                flags: 0,
                driver: [0; 256],
            };
            match self.call(os::DISCONNECT_CLAIM, &mut claim) {
                Ok(_) => return Ok(true),
                Err(e) if matches!(e.raw_os_error(), Some(os::ENOTTY | os::EINVAL)) => {}
                Err(e) => {
                    let _ = self.driver(number, os::CONNECT);
                    return Err(e);
                }
            }
            self.driver(number, os::DISCONNECT)?;
        }
        let result = self.call(os::CLAIM, &mut (number as u32));
        if let Err(e) = result {
            if active {
                let _ = self.driver(number, os::CONNECT);
            }
            return Err(e);
        }
        Ok(active)
    }
    fn driver(&self, number: u8, code: u32) -> io::Result<()> {
        let mut request = os::DriverIoctl {
            interface: number as i32,
            code: code as i32,
            data: ptr::null_mut(),
        };
        self.call(os::DRIVER_IOCTL, &mut request).map(|_| ())
    }
    pub fn release(&self, number: u8, restore: bool) -> io::Result<()> {
        self.call(os::RELEASE, &mut (number as u32))?;
        if restore {
            self.driver(number, os::CONNECT)?;
        }
        Ok(())
    }
    pub fn set_alt(&self, number: u8, alternate: u8) -> io::Result<()> {
        self.call(
            os::SET_INTERFACE,
            &mut os::SetInterface {
                interface: number as u32,
                alternate: alternate as u32,
            },
        )
        .map(|_| ())
    }
    pub fn submit(&self, mut request: Request, endpoint: u8) -> Result<(), SubmitError> {
        if let Err(error) = request.prepare(endpoint) {
            return Err(SubmitError { error, request });
        }
        let mut pending = match self.pending.lock() {
            Ok(p) => p,
            Err(_) => {
                return Err(SubmitError {
                    error: poisoned(),
                    request,
                })
            }
        };
        // Reserve before exposing memory to the kernel. Moving ownership records never moves the URB or payload allocations.
        pending.reserve(1);
        pending.push(request);
        let address = pending.last().expect("request inserted").address();
        let result = unsafe {
            self.kernel
                .call(self.fd.as_raw_fd(), os::SUBMIT, address as *mut c_void)
        };
        match result {
            Ok(_) => Ok(()),
            Err(error) => Err(SubmitError {
                error,
                request: pending.pop().expect("request inserted"),
            }),
        }
    }
    pub fn cancel_all(&self) -> io::Result<()> {
        let pending = self.pending.lock().map_err(|_| poisoned())?;
        let mut error = None;
        for request in pending.iter() {
            let result = unsafe {
                self.kernel.call(
                    self.fd.as_raw_fd(),
                    os::DISCARD,
                    request.address() as *mut c_void,
                )
            };
            if let Err(e) = result {
                if !matches!(e.raw_os_error(), Some(os::EINVAL | os::ENOENT | os::ENODEV)) {
                    error = Some(e);
                }
            }
        }
        error.map_or(Ok(()), Err)
    }
    pub fn pending(&self) -> usize {
        self.pending.lock().unwrap_or_else(|p| p.into_inner()).len()
    }
    pub fn signal(&self) {
        self.wake.signal();
    }
    pub fn reap(&self, complete: &mut Vec<Request>) -> io::Result<()> {
        let mut pending = self.pending.lock().map_err(|_| poisoned())?;
        loop {
            // Reserve completion ownership before REAP so allocation cannot panic
            // between the kernel returning a buffer and our recording its owner.
            complete.reserve(1);
            let mut address = ptr::null_mut::<c_void>();
            let result = self.call(os::REAP, &mut address);
            match result {
                Err(e) if e.raw_os_error() == Some(os::EINTR) => continue,
                Err(e) if e.raw_os_error() == Some(os::EAGAIN) => return Ok(()),
                Err(e) if e.raw_os_error() == Some(os::ENODEV) && pending.is_empty() => {
                    return Ok(())
                }
                Err(e) => return Err(e),
                Ok(_) => {}
            }
            let Some(index) = pending.iter().position(|r| r.address() == address as usize) else {
                return Err(io::Error::other("usbfs_unknown_reaped_urb"));
            };
            let mut request = pending.swap_remove(index);
            let result = request.finish();
            complete.push(request);
            result?;
        }
    }
    pub fn wait(&self, timeout: Duration) -> io::Result<()> {
        let until = Instant::now() + timeout;
        loop {
            let remaining = until.saturating_duration_since(Instant::now());
            match self.kernel.wait(
                self.fd.as_raw_fd(),
                self.wake.fd(),
                remaining.as_millis().min(i32::MAX as u128) as i32,
            ) {
                Err(e) if e.raw_os_error() == Some(os::EINTR) => {
                    if Instant::now() >= until {
                        return Ok(());
                    }
                    continue;
                }
                result => {
                    self.wake.drain();
                    return result;
                }
            }
        }
    }
}
impl Drop for UsbFs {
    fn drop(&mut self) {
        let pending = self.pending.get_mut().unwrap_or_else(|p| p.into_inner());
        if !pending.is_empty() {
            // Never free a kernel-visible buffer, including on worker unwinding.
            // Device's quarantine holds the fd/lease; isolated owners still leak buffers safely.
            std::mem::forget(std::mem::take(pending));
        }
    }
}
#[cfg(test)]
pub(super) mod tests;
