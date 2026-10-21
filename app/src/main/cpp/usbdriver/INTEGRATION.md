# Mei USB integration

The native transport is vendored so builds do not depend on a local NeriPlayer checkout.
See NOTICE.md for revision, author, modifications and licensing. The Java/Kotlin USB
session and AudioOutput adapter are implemented in Mei, rather than copied from NeriPlayer.

## Playback flow

- MusicService enumerates USB host AudioStreaming OUT interfaces and requests USB permission.
- Host attach/detach events drive reconfiguration. AudioManager route changes are not physical
  detach events: libusb claims can temporarily remove the system audio route.
- With a USB output attached, compressed passthrough/offload is rejected; the decoder supplies PCM.
- UsbExclusiveOutputProvider probes actual PCM alternate/clock-range tuples without claiming interfaces,
  applies the sample-rate, bit-depth and channel compatibility switches, then opens one native UAC session.
  Exact source sample rate is preferred; compatible resampling uses the native PCM resampler.
  UsbPcmAudioOutput queues PCM before starting transfers. No AudioTrack owns the same interfaces.
- The transport completion counter and actual output rate drive the playback clock. End-of-stream drains short queues;
  pause preserves pending audio; flush clears counters for seeks.
- A failed or stalled transport pauses playback and blocks repeated native attempts for that
  connection. Toggling exclusive output or physically reconnecting permits a fresh attempt.
- Disabling exclusive output closes native ownership before rebuilding ordinary audio, allowing
  650 ms for Android to rediscover the kernel audio route. Reconnecting never starts paused music.

Hardware volume is used when the DAC offers writable playback Feature Units. Otherwise the PCM
pipeline uses software volume and the status says so; raw bit-perfect output is not claimed in
that case. Decoded integer 32-bit PCM uses ordinary output because DefaultAudioSink's float
conversion cannot retain all 32 source bits. Mono/stereo PCM is accepted; multichannel input
keeps Mei's existing ordinary stereo downmix.

## Settings and background playback

Playback settings links to a dedicated USB settings screen. Presets apply both durations atomically:

| Preset | Foreground | Background |
| --- | --- | --- |
| Low latency | 100 ms | 400 ms |
| Balanced | 250 ms | 800 ms |
| Stable (default) | 500 ms | 1500 ms |

Foreground is adjustable from 100–1000 ms and background from 200–3000 ms, in 50 ms steps.
Custom values are retained until another preset is chosen. These values adapt the reference project's
buffer choices to Mei's queue. A fixed 3000 ms native ring reserves space; each write is limited by the
current duration's watermark, measured in output frames. Switching to a smaller watermark drains the
existing queue without resizing or dropping audio. Changing durations or app visibility never restarts
the current track. A format compatibility change reopens the output, preserving pause/play intent.

All three compatibility options default on. When disabled, the corresponding output property must
match the decoded source. Descriptor tuples are kept together so unsupported cross-products are
never invented. Unknown clock ranges allow only an exact source-rate attempt. Format rejection
pauses playback and rebuilds ordinary output; changing a compatibility option enables a fresh attempt.
Multichannel source downmix and integer 32-bit source limitations above still apply.

Background guidance opens Android application/battery settings and refreshes reported restrictions
on resume. It cannot verify vendor autostart policies. Optional floating keepalive is off by default,
requires overlay permission, and adds an untouchable, unfocusable transparent 1 px window only during
active native USB background playback. It is separate from desktop lyrics and removed on pause,
foreground return, USB failure/release, permission revocation or service destruction. Its status is
shown in the USB settings screen. Vendor background behavior still requires device verification.

## Validation

```
ANDROID_HOME=/path/to/sdk ./gradlew :app:compileDebugKotlin :app:externalNativeBuildDebug
ANDROID_HOME=/path/to/sdk ./gradlew :app:testDebugUnitTest --tests 'com.ljyh.mei.playback.Usb*' --tests 'com.ljyh.mei.playback.usb.*'
cmake -S app/src/test/cpp -B app/build/usb-host-tests -DCMAKE_BUILD_TYPE=Debug
cmake --build app/build/usb-host-tests
ctest --test-dir app/build/usb-host-tests --output-on-failure
```

Host tests use AddressSanitizer and UndefinedBehaviorSanitizer for the control adapter and
native PCM codec. ABI builds and unit tests cannot establish real DAC playback or reconnect
behavior. Installation and on-device playback verification remain separate steps.
