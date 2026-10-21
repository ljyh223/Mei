# USB backend comparison plan

Retain three baselines; do not merge the experimental full Rust driver into main merely
because it compiles:

| Version | Branch | Pinned starting revision |
| --- | --- | --- |
| C++ USB and PCM | codex/usb-hotplug-status | 5946fef |
| C++ USB / Rust PCM | codex/usb-exclusive-rust | 5b9b3e7 |
| Rust USB / PCM + C libusb | codex/usb-exclusive-full-rust | record tested revision |

First verify actual sound, pause/resume, seek, track changes, EOS/drain, unplug during
play/write/pause/open/close, repeated reopen, and screen-off/background operation.
Exercise UAC1 and UAC2 DACs, explicit feedback and adaptive/synchronous routes, 44.1/48 kHz
families, native-rate playback and actual resampling. Failed or silent runs are failures,
not low-CPU successes. Include volume changes and restoration after closing.

Use the same phone, Android build, DAC, cable, local audio file, output tuple, foreground
and background buffer durations, volume route and release build/ABI for each comparison.
Do not compare a debug build with release. Hold thermal/battery/screen/network conditions
steady. Warm up, then collect at least five ten-minute runs per scenario, interleaving
backend order. Run a separate longer screen-off reliability soak. APK installation and
physical device testing require a separate user-requested step in this project.

The Rust transfer plan matches the previous bounded C++ active windows: explicit
feedback uses 16 audio requests, normally 8 ms each, and four feedback requests;
synchronous/adaptive full-speed uses 32 packets/request and 10 active requests at
1000 intervals/s; high-speed uses 64 packets/request and 16 active requests at
8000 intervals/s. Long foreground/background tolerance remains in the PCM ring. The Android worker also
requests the original -19 nice priority (`urgentAudio` reports whether it succeeded);
event waiting uses the original 100 ms ceiling with explicit wakeup for commands.
Record actual bInterval, packet/request counts and queue settings for each run.

Measure app CPU time per played minute, USB worker scheduling delays, steady/peak RSS,
allocations on the audio path, underruns, time to first audible output, pause/seek/close
latency, PCM completed frames versus expected duration, and power under matched device
conditions. Use Android tracing/CPU/memory tools consistently; process CPU alone includes
Kotlin decoder/UI work. Present medians, tail latencies and spread, not just a fastest run.

`nativeRuntimeReport` exposes `backend`, `pcmBackend`, `completedFrames`, `queuedFrames`,
`stagedBytes`, `replayBytes`, `transfers`, `feedbackHz`, `feedbackSamples`,
`feedbackRejected`, `urgentAudio`, `underrunBytes`, `backpressureEvents`, `terminalFailure`,
`transportFailed` and the last error for full Rust. The current Kotlin code polls this
for health and logs failures; collecting successful periodic samples needs a temporary
benchmark/debug harness. Use the same sampling frequency on every backend, outside USB
callbacks. Current code does not claim to measure CPU/power or audible output latency.

Replacing C++ with Rust removes the custom C++ ↔ Rust PCM bridge from the full backend.
The JNI and libusb C ABI remain. This alone does not establish a performance improvement;
accept the rewrite only after compatibility and measurements justify it.
