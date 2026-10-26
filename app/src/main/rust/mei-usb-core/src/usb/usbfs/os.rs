//! Linux/Android usbfs UAPI, described by linux/usbdevice_fs.h (Linux syscall ABI).
//! No libusb code or C helper is linked. Unsafe operations stay at this syscall boundary.
#[cfg(not(target_os = "android"))]
use std::ffi::c_ulong;
use std::{
    ffi::{c_int, c_long, c_uint, c_void},
    io,
    os::fd::{AsRawFd, FromRawFd, OwnedFd, RawFd},
};
#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct Packet {
    pub length: u32,
    pub actual: u32,
    pub status: i32,
}
#[repr(C)]
#[derive(Clone, Copy)]
pub struct Urb {
    pub kind: u8,
    pub endpoint: u8,
    pub status: i32,
    pub flags: u32,
    pub buffer: *mut u8,
    pub length: i32,
    pub actual: i32,
    pub start_frame: i32,
    pub packets: i32,
    pub errors: i32,
    pub signal: u32,
    pub user: *mut c_void,
}
#[repr(C)]
pub struct Control {
    pub kind: u8,
    pub request: u8,
    pub value: u16,
    pub index: u16,
    pub length: u16,
    pub timeout: u32,
    pub data: *mut u8,
}
#[repr(C)]
pub struct SetInterface {
    pub interface: u32,
    pub alternate: u32,
}
#[repr(C)]
pub struct GetDriver {
    pub interface: u32,
    pub driver: [u8; 256],
}
#[repr(C)]
pub struct DriverIoctl {
    pub interface: i32,
    pub code: i32,
    pub data: *mut c_void,
}
#[repr(C)]
pub struct DisconnectClaim {
    pub interface: u32,
    pub flags: u32,
    pub driver: [u8; 256],
}
#[repr(C)]
#[derive(Clone, Copy)]
struct PollFd {
    fd: c_int,
    events: i16,
    revents: i16,
}
const fn ioc(direction: u32, n: u32, size: usize) -> u32 {
    (direction << 30) | ((size as u32) << 16) | (0x55 << 8) | n
}
pub const CONTROL: u32 = ioc(3, 0, std::mem::size_of::<Control>());
pub const SET_INTERFACE: u32 = ioc(2, 4, 8);
pub const GET_DRIVER: u32 = ioc(1, 8, std::mem::size_of::<GetDriver>());
pub const SUBMIT: u32 = ioc(2, 10, std::mem::size_of::<Urb>());
pub const DISCARD: u32 = ioc(0, 11, 0);
pub const REAP: u32 = ioc(1, 13, std::mem::size_of::<usize>());
pub const CLAIM: u32 = ioc(2, 15, 4);
pub const RELEASE: u32 = ioc(2, 16, 4);
pub const DRIVER_IOCTL: u32 = ioc(3, 18, std::mem::size_of::<DriverIoctl>());
pub const DISCONNECT: u32 = ioc(0, 22, 0);
pub const CONNECT: u32 = ioc(0, 23, 0);
pub const CAPABILITIES: u32 = ioc(2, 26, 4);
pub const DISCONNECT_CLAIM: u32 = ioc(2, 27, std::mem::size_of::<DisconnectClaim>());
pub const SPEED: u32 = ioc(0, 31, 0);
pub const REAP_AFTER_DISCONNECT: u32 = 0x10;
pub const EINTR: i32 = 4;
pub const EAGAIN: i32 = 11;
pub const ENODEV: i32 = 19;
pub const ENOENT: i32 = 2;
pub const EINVAL: i32 = 22;
pub const ENOTTY: i32 = 25;
pub const ENODATA: i32 = 61;
pub const ECONNRESET: i32 = 104;
#[cfg(target_os = "android")]
type IoctlRequest = c_int;
#[cfg(not(target_os = "android"))]
type IoctlRequest = c_ulong;
#[cfg(target_os = "android")]
type Nfds = c_uint;
#[cfg(not(target_os = "android"))]
type Nfds = c_ulong;
extern "C" {
    fn ioctl(fd: c_int, request: IoctlRequest, ...) -> c_int;
    fn poll(fds: *mut PollFd, nfds: Nfds, timeout: c_int) -> c_int;
    fn eventfd(initial: c_uint, flags: c_int) -> c_int;
    fn read(fd: c_int, buffer: *mut c_void, count: usize) -> isize;
    fn write(fd: c_int, buffer: *const c_void, count: usize) -> isize;
    #[cfg(target_os = "android")]
    fn setpriority(which: c_int, who: c_uint, priority: c_int) -> c_int;
}
/// Implementations must obey ioctl memory ownership, including asynchronous URBs.
/// A successful REAP returns ownership of that exact URB and buffer to the caller.
///
/// # Safety
/// Implementations must never access an URB or its payload after returning it
/// through REAP, and must not retain buffers after a rejected SUBMIT.
pub unsafe trait Kernel: Send + Sync {
    /// # Safety
    /// `arg` must point to the initialized UAPI object required by `request`, for
    /// the duration of this call (or until REAP for SUBMIT). Never free pending URBs.
    unsafe fn call(&self, fd: RawFd, request: u32, arg: *mut c_void) -> io::Result<i32>;
    fn wait(&self, fd: RawFd, wake: RawFd, timeout: i32) -> io::Result<()>;
}
pub struct System;
unsafe impl Kernel for System {
    unsafe fn call(&self, fd: RawFd, request: u32, arg: *mut c_void) -> io::Result<i32> {
        // SAFETY: caller supplies a valid UAPI object. ioctl borrows it as documented.
        let result = unsafe { ioctl(fd, request as IoctlRequest, arg) };
        if result < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(result)
        }
    }
    fn wait(&self, fd: RawFd, wake: RawFd, timeout: i32) -> io::Result<()> {
        // usbfs signals completed URBs with POLLOUT; eventfd is readable on commands.
        let mut fds = [
            PollFd {
                fd,
                events: 4,
                revents: 0,
            },
            PollFd {
                fd: wake,
                events: 1,
                revents: 0,
            },
        ];
        let result = unsafe { poll(fds.as_mut_ptr(), fds.len() as Nfds, timeout) };
        if result < 0 {
            return Err(io::Error::last_os_error());
        }
        // POLLNVAL means a bad ownership invariant; don't spin on an invalid fd.
        if fds.iter().any(|f| f.revents & 0x20 != 0) {
            return Err(io::Error::from_raw_os_error(9));
        }
        Ok(())
    }
}
pub struct Wake(OwnedFd);
impl Wake {
    pub fn new() -> io::Result<Self> {
        let fd = unsafe { eventfd(0, 0x80000 | 0x800) }; // CLOEXEC | NONBLOCK on Android/Linux ABIs.
        if fd < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(Self(unsafe { OwnedFd::from_raw_fd(fd) }))
        }
    }
    pub fn fd(&self) -> RawFd {
        self.0.as_raw_fd()
    }
    pub fn signal(&self) {
        let value = 1u64;
        loop {
            let result = unsafe { write(self.fd(), (&value as *const u64).cast(), 8) };
            if result >= 0 || io::Error::last_os_error().raw_os_error() != Some(EINTR) {
                break;
            }
        }
    }
    pub fn drain(&self) {
        let mut value = 0u64;
        loop {
            let result = unsafe { read(self.fd(), (&mut value as *mut u64).cast(), 8) };
            if result < 0 && io::Error::last_os_error().raw_os_error() != Some(EINTR) {
                break;
            }
        }
    }
}
pub fn audio_priority() -> bool {
    #[cfg(target_os = "android")]
    {
        unsafe { setpriority(0, 0, -19) == 0 }
    }
    #[cfg(not(target_os = "android"))]
    {
        false
    }
}
// Compile-time UAPI layout checks run for every Android target, including 32-bit.
const _: () = {
    assert!(std::mem::size_of::<Packet>() == 12);
    assert!(
        std::mem::size_of::<Urb>()
            == if std::mem::size_of::<usize>() == 8 {
                56
            } else {
                44
            }
    );
    assert!(
        std::mem::offset_of!(Urb, buffer)
            == if std::mem::size_of::<usize>() == 8 {
                16
            } else {
                12
            }
    );
    assert!(
        std::mem::offset_of!(Urb, user)
            == if std::mem::size_of::<usize>() == 8 {
                48
            } else {
                40
            }
    );
    assert!(
        std::mem::size_of::<Control>()
            == if std::mem::size_of::<usize>() == 8 {
                24
            } else {
                16
            }
    );
    assert!(
        std::mem::size_of::<DriverIoctl>()
            == if std::mem::size_of::<usize>() == 8 {
                16
            } else {
                12
            }
    );
    assert!(std::mem::size_of::<c_long>() == std::mem::size_of::<usize>());
};
