#![deny(unsafe_op_in_unsafe_fn)]
//! Native PCM core for Mei. USB interface/transfer ownership remains in the C++ transport.
//! Unsafe code is limited to ffi.rs; signal processing and queue management are safe Rust.
mod codec;
mod descriptor;
mod ffi;
mod pipeline;
mod resampler;
#[cfg(test)]
mod tests;
