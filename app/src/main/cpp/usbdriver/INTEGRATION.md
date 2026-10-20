# Mei USB integration

The native transport is vendored so builds do not depend on a local NeriPlayer checkout.
See NOTICE.md for revision, author, modifications and licensing. The Java/Kotlin USB
session and AudioOutput adapter are implemented in Mei, rather than copied from NeriPlayer.

## Playback flow

- MusicService enumerates USB host AudioStreaming OUT interfaces and requests USB permission.
- Host attach/detach events drive reconfiguration. AudioManager route changes are not physical
  detach events: libusb claims can temporarily remove the system audio route.
- With a USB output attached, compressed passthrough/offload is rejected; the decoder supplies PCM.
- UsbExclusiveOutputProvider opens one native UAC session with the source sample rate, and
  UsbPcmAudioOutput queues PCM before starting transfers. No AudioTrack owns the same interfaces.
- The transport completion counter drives the playback clock. End-of-stream drains short queues;
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
