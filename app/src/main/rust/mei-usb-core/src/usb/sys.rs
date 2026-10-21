//! Minimal ABI for the bundled libusb 1.0 header. No second libusb implementation.
use std::ffi::{c_int, c_long, c_uchar, c_uint, c_ushort, c_void};
pub enum Context {}
pub enum Handle {}
pub enum Device {}
#[repr(C)]
pub struct Endpoint {
    pub length: u8,
    pub kind: u8,
    pub address: u8,
    pub attributes: u8,
    pub max_packet: u16,
    pub interval: u8,
    pub refresh: u8,
    pub sync_address: u8,
    pub extra: *const u8,
    pub extra_len: c_int,
}
#[repr(C)]
pub struct Alt {
    pub length: u8,
    pub kind: u8,
    pub number: u8,
    pub alternate: u8,
    pub endpoint_count: u8,
    pub class: u8,
    pub subclass: u8,
    pub protocol: u8,
    pub string: u8,
    pub endpoints: *const Endpoint,
    pub extra: *const u8,
    pub extra_len: c_int,
}
#[repr(C)]
pub struct Interface {
    pub alts: *const Alt,
    pub count: c_int,
}
#[repr(C)]
pub struct Configuration {
    pub length: u8,
    pub kind: u8,
    pub total: u16,
    pub count: u8,
    pub value: u8,
    pub string: u8,
    pub attributes: u8,
    pub max_power: u8,
    pub interfaces: *const Interface,
    pub extra: *const u8,
    pub extra_len: c_int,
}
#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct Packet {
    pub length: c_uint,
    pub actual: c_uint,
    pub status: c_int,
}
#[repr(C)]
pub struct Transfer {
    pub handle: *mut Handle,
    pub flags: u8,
    pub endpoint: u8,
    pub kind: u8,
    pub timeout: c_uint,
    pub status: c_int,
    pub length: c_int,
    pub actual: c_int,
    pub callback: Option<unsafe extern "C" fn(*mut Transfer)>,
    pub user: *mut c_void,
    pub buffer: *mut u8,
    pub packet_count: c_int,
    pub packets: [Packet; 0],
}
#[repr(C)]
pub struct Timeval {
    pub seconds: c_long,
    pub micros: c_long,
}
pub const COMPLETED: i32 = 0;
pub const CANCELLED: i32 = 3;
pub const NO_DEVICE: i32 = -4;
#[cfg_attr(not(target_os = "android"), link(name = "usb-1.0"))]
extern "C" {
    pub fn libusb_set_option(ctx: *mut Context, option: c_int, ...) -> c_int;
    pub fn libusb_init(ctx: *mut *mut Context) -> c_int;
    pub fn libusb_exit(ctx: *mut Context);
    pub fn libusb_wrap_sys_device(ctx: *mut Context, fd: isize, handle: *mut *mut Handle) -> c_int;
    pub fn libusb_close(handle: *mut Handle);
    pub fn libusb_get_device(handle: *mut Handle) -> *mut Device;
    pub fn libusb_get_device_speed(device: *mut Device) -> c_int;
    pub fn libusb_get_active_config_descriptor(
        device: *mut Device,
        config: *mut *const Configuration,
    ) -> c_int;
    pub fn libusb_get_config_descriptor(
        device: *mut Device,
        index: u8,
        config: *mut *const Configuration,
    ) -> c_int;
    pub fn libusb_free_config_descriptor(config: *const Configuration);
    pub fn libusb_get_max_alt_packet_size(
        device: *mut Device,
        interface: c_int,
        alternate: c_int,
        endpoint: u8,
    ) -> c_int;
    pub fn libusb_kernel_driver_active(handle: *mut Handle, interface: c_int) -> c_int;
    pub fn libusb_detach_kernel_driver(handle: *mut Handle, interface: c_int) -> c_int;
    pub fn libusb_attach_kernel_driver(handle: *mut Handle, interface: c_int) -> c_int;
    pub fn libusb_claim_interface(handle: *mut Handle, interface: c_int) -> c_int;
    pub fn libusb_release_interface(handle: *mut Handle, interface: c_int) -> c_int;
    pub fn libusb_set_interface_alt_setting(
        handle: *mut Handle,
        interface: c_int,
        alternate: c_int,
    ) -> c_int;
    pub fn libusb_control_transfer(
        handle: *mut Handle,
        request_type: c_uchar,
        request: c_uchar,
        value: c_ushort,
        index: c_ushort,
        data: *mut u8,
        length: c_ushort,
        timeout: c_uint,
    ) -> c_int;
    pub fn libusb_alloc_transfer(packets: c_int) -> *mut Transfer;
    pub fn libusb_free_transfer(transfer: *mut Transfer);
    pub fn libusb_submit_transfer(transfer: *mut Transfer) -> c_int;
    pub fn libusb_cancel_transfer(transfer: *mut Transfer) -> c_int;
    pub fn libusb_interrupt_event_handler(ctx: *mut Context);
    #[cfg(target_os = "android")]
    pub fn setpriority(which: c_int, who: c_uint, priority: c_int) -> c_int;
    pub fn libusb_handle_events_timeout_completed(
        ctx: *mut Context,
        timeout: *const Timeval,
        completed: *mut c_int,
    ) -> c_int;
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn libusb_layout_matches_header_contract() {
        let wide = std::mem::size_of::<usize>() == 8;
        assert_eq!(std::mem::size_of::<Endpoint>(), if wide { 32 } else { 20 });
        assert_eq!(std::mem::size_of::<Alt>(), if wide { 40 } else { 24 });
        assert_eq!(
            std::mem::size_of::<Configuration>(),
            if wide { 40 } else { 24 }
        );
        assert_eq!(std::mem::size_of::<Transfer>(), if wide { 64 } else { 40 });
        assert_eq!(
            std::mem::offset_of!(Transfer, packets),
            if wide { 60 } else { 40 }
        );
        assert_eq!(std::mem::size_of::<Packet>(), 12);
    }
}
