# ColorOS 锁屏歌词

Mei 由播放服务把当前歌曲的完整歌词时间轴发布到现有平台 MediaSession 的
`MediaMetadata["lyricInfo"]`。此字段是 ColorOS 的扩展，不是 Android 通用歌词显示 API。
没有增加 SDK、第二个媒体会话、悬浮窗权限或网络歌词来源。

## 数据流

`MusicService` 创建 `PlaybackLyricsController`，负责当前歌曲加载和实际下一首预加载。
结果仍进入应用共享的 `LyricManager`；播放器页面、桌面歌词和系统歌词都消费同一份选中结果。
下一首仅进入预加载缓存，不发布为正在播放的歌词。

`ColorOsLyricPayload` 把选中结果转换为 JSON：

- `lyric`：逐行 LRC，供原生系统列表使用。
- `rawLyric`：有真实逐字时间时才提供 enhanced LRC；时间为绝对播放位置。
- `translationLyric`：沿用原文行起始时间的翻译，不把音译或注音当成翻译。
- `songId`、`songName`、`artist`、`album`、`lyricType`、`noLyric`：当前歌曲信息。

加载中、空歌词和纯音乐不发布占位文案。切歌、清空队列、无歌词和服务销毁时清理本功能
上次发布的字段。异步结果必须同时匹配当前播放器歌曲、歌词所属歌曲及平台会话的歌曲 ID。

## Media3 兼容

当前依赖为 Media3 1.11.1。该版本的 `MediaMetadata.equals()` 不比较 extras 的内容，
仅修改 extras 中的歌词可能无法触发平台元数据更新。因此由 `ColorOsLyricsPublisher`
直接更新同一个平台会话，并观察宿主后续元数据替换以补回字段。

`androidx.media3.session.PlatformLyricsSessionAccess` 是唯一使用 Media3 包内访问的入口，
没有使用反射；升级 `media3-session` 时需要重新检查此入口并编译 Java/Kotlin。
播放器原有媒体控制、PlaybackState 和播放时钟由 Media3 继续维护。

发布时按类型保留当前元数据、封面 Bitmap / URI、评分及未知字段，不获取额外封面。
歌词 JSON 限制为 256 KiB，完整候选元数据 Parcel 限制为 512 KiB；超限放弃本次写入。
日志不包含完整歌词、私有媒体路径或鉴权信息。

## 系统侧边界

支持 `lyricInfo` 且允许该播放器进入歌词链路的 ColorOS 构建可以直接消费数据。
某些版本仍有包名白名单、歌词入口和 AOD 限制，应用发布字段不能解除这些系统限制。
本实现不修改系统白名单，也不伪装其他播放器包名。

社区 Bridge 可以在用户已有的 Root / LSPosed 环境增强渲染及兼容性，Mei 不依赖其 APK。
逐字、翻译和 AOD 的具体呈现能力以目标系统或 Bridge 为准。本功能不向 OPPO 独立的
“音乐”App 导入歌词，也不保证其他品牌系统媒体卡片会显示此扩展。

## 验证

```sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests 'com.ljyh.mei.playback.lyrics.*' --tests 'com.ljyh.mei.utils.lyric.*'
```

自动测试覆盖序列化、逐字与翻译、占位排除、歌曲归属、宿主元数据刷新、封面保留、
超限和释放清理。按项目协作约定不自动安装 APK 或执行视觉测试。
设备验证需要另行确认机型、ColorOS / SystemUI 版本，并验证后台首次播放、暂停恢复、
拖动进度、快速切歌、歌词版本切换、无歌词歌曲、封面更新和 AOD。

参考：

- [社区播放器接入协议](https://github.com/Andrea-lyz/ColorOS-Live-Lyrics-Bridge/blob/main/docs/PLAYER_INTEGRATION.md)
- [Media3 1.11.1 MediaMetadata](https://github.com/androidx/media/blob/1.11.1/libraries/common/src/main/java/androidx/media3/common/MediaMetadata.java)
- [Media3 1.11.1 平台会话桥接](https://github.com/androidx/media/blob/1.11.1/libraries/session/src/main/java/androidx/media3/session/MediaSessionLegacyStub.java)
