#![deny(unsafe_op_in_unsafe_fn)]
//! Native USB audio backend for Mei. `full-usb` owns JNI, UAC controls and transport.
//! libusb remains C; PCM, resampling and untrusted protocol parsing use safe Rust.
mod codec;
mod descriptor;
mod ffi;
mod pipeline;
mod resampler;
#[cfg(test)]
mod tests;

#[cfg(feature = "full-usb")]
mod usb;
