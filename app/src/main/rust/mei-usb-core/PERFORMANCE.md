# USB backend comparison plan

Keep the known libusb build as a device comparison baseline. The new direct-usbfs
backend needs its own device tests before merging.

| Version | Branch | Reference revision |
| --- | --- | --- |
| C++ USB / PCM + libusb | codex/usb-hotplug-status | 5946fef |
| C++ USB / Rust PCM + libusb | codex/usb-exclusive-rust | 5b9b3e7 |
| Rust USB / PCM + libusb | codex/usb-exclusive-full-rust | 5a73cfc |
| Rust USB / PCM + direct usbfs | codex/usbfs-rust | record tested commit/APK |

First test audible playback, pause/resume, seek, track changes, EOS/drain, unplug
during playback/pause/open/close, repeated reopen and screen-off operation. Include
hardware volume changes/restoration, UAC1/UAC2 DACs, explicit feedback and adaptive/
synchronous endpoints, 44.1/48 kHz families, matching source/output rates and actual
resampling. Failed or silent runs are failures, not low-CPU successes. APK installation
and physical testing require a separate user-requested step in this project.

The pure Rust backend directly uses the granted Android fd. It requires the kernel's
reap-after-disconnect capability, validates UAPI layouts on all four Android ABIs,
and retains native ownership on a stuck cancellation. Verify these behaviors on the
actual phone/vendor kernel; host simulation cannot establish hardware compatibility.

Use the same phone, Android build, DAC/cable, local file, output tuple, volume route,
release build/ABI, screen/network state and thermal conditions. Warm up, collect at
least five ten-minute runs per scenario and interleave backend order. Add a separate
longer screen-off reliability run. Do not compare debug with release.

Transfer windows and the -19 audio priority request are retained: explicit feedback
uses 16 audio requests and four feedback requests, normally 8 ms/audio request;
full-speed synchronous/adaptive uses 32 packets/request and 10 requests at 1000
intervals/s; high-speed uses 64 packets/request and 16 requests at 8000 intervals/s.
The previous 100 ms event-wait ceiling remains, with explicit eventfd wakeup.

A queue-unit bug was corrected during the usbfs migration: `Core::queued()` already
returns output frames; the previous session divided it by frame size again. For a
fair latency/CPU comparison, backport that correction to reference test builds or
measure actual queue duration. The same nominal buffer preset alone is insufficient.

Measure CPU time per played minute, USB worker scheduling delays, steady/peak RSS,
audio-path allocations, underruns, time to audible output, pause/seek/close latency,
completed frames versus expected duration, APK/native-library size and matched-device
power. Process CPU includes decoder/UI work. Report medians, tails and spread.

The new native report identifies `backend=rust-usbfs transport=usbfs pcmBackend=rust`.
It retains completed/queued frames, staged/replay bytes, transfer count, feedback Hz/
samples/rejections, urgentAudio, underrun bytes, backpressure events and terminal/
transport failure fields. Kotlin currently polls for health and logs failures; a
successful periodic metrics series still requires a benchmark/debug harness. Use
the same sampling frequency on every build, outside the audio I/O loop.

Rust usbfs removes the app's C libusb library and C callback layer. JNI and kernel
ABI operations remain. This does not prove lower CPU, better sound or lower power.
Accept the backend based on device reliability and measured tradeoffs.
