# UnblockNeteaseMusic

Pure Kotlin/JVM fallback audio library. It has no Android, Media3, or app storage dependency.

## Structure

- `model`: source-neutral track, album, artist, lyric, and playable-audio values.
- `provider`: separate catalog, lyric, and playable-audio capabilities. A source implements only what it supports.
- `qq`, `kuwo`, `migu`: provider-specific requests and response models, kept internal where possible. QQ also supplies lyrics; `QRCUtils` decodes its lyric payload for playback and display.
- `unblock`: matching and fallback coordination. A source needs both catalog search and playable-audio resolution to participate.

## Flow

1. The app asks NetEase for its normal playback URL.
2. When that URL is empty, the app converts song detail to `MusicTrack` and calls `UnblockResolver`.
3. Each registered `MusicCatalogProvider` searches another platform. `TrackMatcher` checks the title, artist, and available duration to avoid playing a different recording.
4. The corresponding `PlayableAudioProvider` obtains and probes an audio URL. The app plays it under a source-specific disk cache key.

The first registered sources are QQ Music, Kuwo Music, and Migu Music. Add another source by implementing `MusicCatalogProvider` and `PlayableAudioProvider`, then register both with `UnblockResolver`. Lyric support is independent through `MusicLyricProvider`.

QQ Music may require a logged-in QQ Music Cookie. The Android app reads the optional Cookie from **Content settings → QQ Music Cookie** and supplies it to the QQ provider. Kuwo and Migu do not use the NetEase account Cookie.

The provider protocols were informed by the locally cloned [UnblockNeteaseMusic/server](https://github.com/UnblockNeteaseMusic/server) project (LGPL-3.0-only). Migu's old search endpoint now returns a web page, so its implementation uses the current H5 search/listen protocol. These are third-party endpoints and can change independently of this library.
