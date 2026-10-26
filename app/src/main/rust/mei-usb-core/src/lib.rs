#![deny(unsafe_op_in_unsafe_fn)]
//! Native USB audio backend for Mei. `full-usb` owns JNI, UAC controls and transport.
//! Android/Linux usbfs supplies the OS transport; audio and session logic use safe Rust.
mod codec;
mod descriptor;
#[cfg(any(not(feature = "full-usb"), test))]
mod ffi;
mod pipeline;
mod resampler;
#[cfg(test)]
mod tests;

#[cfg(feature = "full-usb")]
mod usb;
