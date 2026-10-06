# UnblockNeteaseMusic

Pure Kotlin/JVM library for finding playable alternatives to unavailable tracks.
It has no Android or Media3 dependency.

## Modules inside this library

- `model`: source-neutral track, album, artist, lyric and playable-audio values.
- `provider`: separate catalog, lyric and playable-audio capabilities. A source implements only the capabilities it supports.
- `qq`: QQ Music search and lyric implementation. Its wire DTOs are internal and only the source-neutral models are public.
- `unblock`: matching and fallback coordination. A provider must implement both catalog search and playable-audio resolution to participate in playback fallback.

The Android app owns playback, persistence, user settings and credentials. New music sources can be added under their own package without depending on the app. QQ Music currently supplies search and lyrics; it does not yet supply a playable-audio URL. QQ lyric text is returned in its encoded wire form; the library's `QRCUtils` decodes it for playback and display.
