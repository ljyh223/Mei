# Mei Rust USB core

This branch uses a hybrid driver. Rust owns the PCM ring, conversion, sinc resampling,
software gain/ramps, statistics and UAC1/UAC2 AudioStreaming Type-I descriptor parsing.
The existing C++ JNI/UAC session, clock controls, isochronous transfers, asynchronous
feedback, cancellation/quarantine and libusb remain the transport. This is not a
complete Rust rewrite of the USB transport and does not replace Android USB permissions.

The signal processing algorithms and parsers are adapted from NeriPlayer's native
sources at revision 113699bb, by cwuom / ouom: https://github.com/cwuom/NeriPlayer.
They retain the NeriPlayer Native Attribution License in ../../cpp/usbdriver/NERI_LICENSE.md.
See ../../cpp/usbdriver/NOTICE.md. No endorsement or certification is implied.

## Build

Rust 1.82+ (validated and used by CI: 1.97.0), Cargo and the project's Android NDK are required.
Install Android standard libraries once:

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
```

Normal Gradle builds invoke Cargo through CMake, one output directory per ABI.
Cargo uses --offline --locked; this crate has no third-party Cargo dependencies.
Cargo.lock is tracked; compiler artifacts are not. Rust is statically linked into
libmei_usb.so, and libusb remains a separate LGPL shared library. Rust symbols are
hidden from the Android shared library's external symbol namespace.

The CMake option MEI_USB_RUST_PCM defaults ON on this branch. Configure a separate
build directory with -DMEI_USB_RUST_PCM=OFF to use the original C++ PCM and descriptor
parsers. Both backends use exactly the same Kotlin interface and settings. Native
runtime reports include pcmBackend=rust or pcmBackend=cpp.

## Lifetime and threading

- C++ PcmPipeline owns one opaque Rust Core and destroys it after session callbacks stop.
  Existing C++ shared_ptr/quarantine ownership still protects late USB cancellation callbacks.
- Rust borrows the JNI input buffer or USB transfer output buffer only for a synchronous call;
  it retains no foreign buffer pointer. ABI structs use fixed-width fields and checked layouts.
- Codec, resampler, descriptor parser and pipeline modules forbid unsafe code. Pointer/slice
  conversion is confined to ffi.rs under an explicit caller contract.
- A writer mutex serializes configure/convert/drain/seek. A separate output mutex protects
  the ring and gain/statistics state; sinc conversion does not hold the consumer's queue lock.
- Shrinking a queue preserves all accepted audio. No producer overwrite is allowed.
- Rust panics are caught at the C ABI boundary. Poisoned state fails closed, JNI writes report
  failure and empty snapshots trigger the existing native failure/pause recovery path.
  Allocation failure at process OOM remains subject to Rust's allocator/Android process policy.

## Verification

```sh
cargo fmt --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml -- --check
cargo test --offline --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml
cargo clippy --offline --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml --all-targets -- -D warnings
cmake -S app/src/test/cpp -B app/build/usb-host-tests -DCMAKE_BUILD_TYPE=Debug
cmake --build app/build/usb-host-tests
ctest --test-dir app/build/usb-host-tests --output-on-failure
```

Host tests compare the Rust C ABI with the original C++ algorithms: integer PCM must
match exactly at unity, resampling/gain must match within the stated numeric tolerance,
frame counts must agree, and descriptor acceptance/fields are compared after truncation
and byte mutation. The real C++ Rust adapter runs the same pipeline behavior suite.
C++ host code and FFI callers use ASan/UBSan; ordinary Cargo tests do not imply that
Rust machine code is sanitizer-instrumented. Rust tests cover ring backpressure,
producer/consumer concurrency, seek, pause, gain, resampling duration/chunk independence,
anti-alias filtering and malformed descriptors. These tests do not verify real DAC or
Android vendor background behavior. APK installation is a separate user-requested step.
