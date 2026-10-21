# Mei native USB backend in Rust

The default backend on `codex/usb-exclusive-full-rust` implements JNI, USB session/fd
ownership, interface claims, UAC1/UAC2 Type-I descriptors, clock selection and sample
rate controls, hardware volume, isochronous transfers, explicit feedback, cancellation,
PCM conversion/ring/resampling and gain in Rust. Android permissions, settings and
playback integration continue to use the existing Kotlin interface.

libusb remains a separate C shared library (`libmei_libusb.so`), including its Linux
usbfs backend. This is not a rewrite of libusb or of the kernel USB driver. No C++ USB
implementation is compiled into the default `libmei_usb.so`. `rust_usb_link.c` contains
ABI assertions and a link anchor only; the separate BeatNet component still uses C++.
The previous C++ sources remain selectable for comparisons.

The native algorithms and behavior are adapted from NeriPlayer revision 113699bb,
by cwuom / ouom: https://github.com/cwuom/NeriPlayer. The derivative Rust implementation
retains the NeriPlayer Native Attribution License in ../../cpp/usbdriver/NERI_LICENSE.md
and the attribution in ../../cpp/usbdriver/NOTICE.md. No endorsement is implied.
libusb retains its own LGPL-2.1-or-later terms and separate library/source notices.

## Build

Rust 1.82+ (validated and used by CI: 1.97.0), Cargo and the project's Android NDK are
required. Install the four Android standard library targets once:

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
cargo fetch --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml
```

The only Cargo dependency is `jni-sys` 0.3.0 (raw JNI declarations, optional behind
`full-usb`). Cargo.lock pins it. Gradle/CMake builds use `--offline --locked`, a separate
Cargo output directory per ABI, and static Rust linkage. Only the existing JNI entry
points are exported from the full Rust shared library. The libusb ABI uses checked
minimal declarations for the bundled header, avoiding a second libusb build/download.
C and Rust layout assertions cover pointer widths, transfer flexible-array offsets
and event timeouts on all four Android architectures.

CMake options (use separate build directories):

| Backend | MEI_USB_FULL_RUST | MEI_USB_RUST_PCM |
| --- | --- | --- |
| Full Rust, default here | ON | ignored |
| C++ USB transport / Rust PCM | OFF | ON |
| Original C++ USB / PCM | OFF | OFF |

Android full builds enable the crate's `full-usb` feature. Existing host parity tests
build the PCM-only crate to compare the Rust core against the original C++ algorithms.
Host full-driver tests require the system libusb development package (Ubuntu:
`libusb-1.0-0-dev`). Android always links the project's bundled library.

## Ownership, cancellation and failure

- The Java fd is duplicated before libusb wraps it. The native device owns that duplicate,
  claimed audio interfaces and any detached kernel drivers until all transfers retire.
- Kotlin sees opaque registry IDs, never a Rust pointer. JNI obtains an Arc for each call;
  closing removes the ID first. Later calls cannot dereference stale native memory.
- One Rust worker owns transfers and all buffers. Callbacks only mark completion; no
  JNI calls, allocation, conversion or lock acquisition occur inside USB callbacks.
- Pause cancels and retires requests, credits completed whole PCM frames, and preserves
  unsent real audio in submission order, ahead of the existing replay tail. Silence is
  excluded. Replayed PCM is not given software gain twice. Seek clears both queues and
  clocks only after cancellation completes.
- Close waits up to two seconds. If a kernel never returns a cancellation callback,
  the worker remains quarantined with its buffers, fd and interface ownership. A second
  native session is refused until the old owner actually releases them. A worker panic
  also retains any still-submitted buffers and callback/device ownership. Such a stuck
  session can require unplugging the device or restarting the process.
- Device removal, feedback loss, event/packet failures and stalled audio completion
  produce the existing terminal-failure report consumed by Kotlin's pause/recovery path.
- Hardware mute/volume changes require a readable original value and are restored before
  release while the device remains connected. Volume uses one playback feature-unit
  stage, preferring its master control. Unknown controls remain untouched.
- Clock selector cycles, invalid pin selection and non-unity clock multipliers fail
  closed. UAC1 async routes require explicit feedback. Implicit-feedback capture pairing,
  non-PCM/Type-II/III formats and UAC3 are not implemented. Hardware compatibility has
  not been established by host tests or cross-compilation.

Unsafe code is confined to the libusb/JNI/device/transfer boundaries and the retained
PCM C ABI. Codec, resampler, PCM ring and protocol/topology parsing forbid unsafe code.
JNI direct input is borrowed only during the write call, never retained. PCM conversion
uses a writer mutex; the output mutex only protects the ring/gain/stats. Queue shrinking
does not discard accepted audio. Panics are caught at JNI and worker boundaries; process
allocation failure follows Rust/Android allocator behavior.

## Verification

```sh
cargo fmt --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml -- --check
cargo test --offline --locked --all-features --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml
cargo clippy --offline --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml --all-targets --all-features -- -D warnings
cmake -S app/src/test/cpp -B app/build/usb-host-tests -DCMAKE_BUILD_TYPE=Debug
cmake --build app/build/usb-host-tests
ctest --test-dir app/build/usb-host-tests --output-on-failure
```

Host tests cover PCM backpressure/concurrency/resampling, malformed descriptors and rate
ranges, clock cycles/selection, feedback scaling/phase, hardware volume topology, ABI
layouts, partial/cancelled packet accounting and stale handles. C++ parity callers run
under ASan/UBSan; ordinary Cargo tests do not instrument Rust code with those sanitizers.
Android compilation verifies linkage and ABI layouts, not USB hardware behavior.
See [PERFORMANCE.md](PERFORMANCE.md) for the retained baselines and device comparison plan.
