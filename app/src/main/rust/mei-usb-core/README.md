# Mei Rust USB audio driver: direct usbfs backend

On `codex/usbfs-rust`, Android USB exclusive playback no longer builds or links
libusb or any C/C++/assembly USB source. Cargo directly emits `libmei_usb.so` as a
Rust cdylib. The existing Kotlin USB permission flow supplies an authorized device
fd; the Rust backend duplicates it and uses Linux/Android usbfs ioctls. Android's
runtime, libc and kernel remain platform dependencies. BeatNet is a separate C++
component and is outside this USB migration.

JNI, UAC1/UAC2 Type-I descriptors, clock/rate negotiation, hardware volume, feedback,
PCM conversion/resampling, packet scheduling and playback state remain Rust. The
new `src/usb/usbfs` transport replaces libusb's control/interface/URB/event handling.
It is Android/Linux-specific and does not implement libusb's other platform backends.
The remaining C++ PCM/format/control sources are host parity references only.

## Source provenance

The audio algorithms and UAC/session behavior are adapted from NeriPlayer revision
113699bb, by cwuom / ouom: https://github.com/cwuom/NeriPlayer. The derivative Rust
code retains the NeriPlayer Native Attribution License in
../../cpp/usbdriver/NERI_LICENSE.md and the notices in ../../cpp/usbdriver/NOTICE.md.
The NeriPlayer acknowledgement remains in the app. Replacing libusb does not remove
these obligations. No endorsement is implied.

The usbfs backend is implemented for Mei using the Linux userspace USB ABI,
not translated from the bundled libusb C implementation. The libusb source, dynamic
library, JNI C link anchor and obsolete libusb acknowledgement were removed from
this branch. Original third-party license texts are not edited to rewrite their scope.

## Build and packaging

Rust 1.82+ (CI/validated toolchain: 1.97.0), Cargo and the Android NDK are required.
The only Cargo dependency is pinned `jni-sys` 0.3.0, behind `full-usb`.

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
cargo fetch --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml
```

CMake invokes Cargo with `--offline --locked` and the ABI/API-specific NDK linker.
Rust outputs are isolated per CMake build tree. JNI libraries are copied to
`app/build/generated/usbRustJni/<debug|release>/<ABI>/libmei_usb.so`; Gradle waits for
the native build before merging that variant's generated JNI inputs. Rust's cdylib
exports the 19 existing JNI entry points; the legacy PCM C ABI is compiled only for
the host reference build or tests. Android output uses 16 KiB ELF page alignment.

`mei_add_usb_rust(ON)` builds the Android driver. Host `mei_add_usb_rust()` builds the
PCM static archive for C++ parity tests. No system libusb development package is
needed for any test or Android build. Only x86/x86_64/ARMv7/AArch64 Android and Linux
host testing are covered by this implementation's syscall/layout assumptions.

## Ownership and cancellation

- Device/session/stream modules now forbid unsafe code. Raw JNI access and kernel
  UAPI operations remain isolated in their boundary modules. Protocol, descriptor,
  codec, resampler and PCM ring modules also forbid unsafe code.
- JNI handles are registry IDs, never Rust pointers. Each call retains an Arc. Close
  removes the ID before stopping the worker; stale IDs cannot access freed sessions.
- USB setup uses the authorized fd only: no sysfs discovery, udev, root, or extra
  Android permission is required. The duplicated fd remains owned until release.
- URB headers use aligned `MaybeUninit` storage sized once for their trailing packet
  descriptors. Data allocations remain stable. `submit` consumes a request and the
  transport privately retains it. A caller can access it again only after REAP
  returns that exact kernel address. Rejected submissions return their ownership.
- Kernel completion addresses are checked against the pending registry before
  dereferencing. Packet counts/lengths and output-frame alignment are validated.
- There are no C callbacks. One Rust worker polls usbfs completion readiness and an
  eventfd command wakeup. Synchronous controls do not dispatch audio callbacks on
  the calling JNI thread. The worker requests the previous -19 nice priority.
- Pause cancels and reaps requests, credits only completed real PCM frames, then
  preserves unsent PCM in submission order ahead of the replay tail. Replay does
  not apply software gain twice. Seek cancels first, clears both queues/clocks, and
  waits for Kotlin's next preroll/play command. Core queue sizes are already frames;
  only staged/replay byte counts are divided by output frame size.
- Close waits up to two seconds. If the kernel never returns pending requests, a
  quarantined worker retains buffers/fd/lease until they return. On worker panic,
  pending buffers and the fd/lease are retained rather than freed while kernel I/O
  may still use them. A permanently broken kernel route may need process restart.
- `USBDEVFS_CAP_REAP_AFTER_DISCONNECT` is required before starting any request.
  Unsupported kernels fail explicitly instead of guessing when memory can be freed.
- Claims use atomic disconnect-and-claim when available, with a rollback-capable
  fallback. Known hardware control values and detached drivers are restored on
  normal close. Detached-device close skips hardware-value restoration.

The existing UAC limitations remain: no paired implicit-feedback capture stream,
non-unity clock multiplier conversion, UAC3, DSD or Type-II/III compressed formats.
Errors/feedback loss/stalls still reach Kotlin's terminal-failure/pause path. The
usbfs backend requires new device validation; previous libusb playback results do
not establish its compatibility or performance.

## Verification

```sh
cargo fmt --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml -- --check
cargo test --offline --locked --all-features --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml
cargo clippy --offline --locked --manifest-path app/src/main/rust/mei-usb-core/Cargo.toml --all-targets --all-features -- -D warnings
cmake -S app/src/test/cpp -B app/build/usb-host-tests -DCMAKE_BUILD_TYPE=Debug
cmake --build app/build/usb-host-tests
ctest --test-dir app/build/usb-host-tests --output-on-failure
```

Tests include the actual Rust worker against a simulated kernel: paused replay,
seek/preroll, completed/queued frames, close and disconnect cleanup. Transport tests
cover failed submissions, reordered completions, cancellation ownership, EINTR,
unknown addresses, missing capabilities, control failures and claim rollback.
A real eventfd/poll test checks wakeup without touching USB hardware. Descriptor
truncation and superspeed companions are checked separately. An independent C++
test verifies UAPI constants/layouts against Linux/NDK headers; it is not shipped.

C++ parity tests use ASan/UBSan. Ordinary Cargo tests do not instrument Rust machine
code with those sanitizers. Cross-compilation checks all four Android ABIs; it does
not verify sound, vendor kernels, physical unplug/replug or background behavior.
APK installation and on-device tests remain a separate user-requested step.
See [PERFORMANCE.md](PERFORMANCE.md) for retained baselines and comparison conditions.
