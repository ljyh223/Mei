<div align="center">

# Mei

一个使用 Jetpack Compose 构建的网易云音乐第三方播放器

<img src="./screenshot/logo.png" width="120px" style="border-radius:12px"/>

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-orange)
![Status](https://img.shields.io/badge/Development-In%20Progress-yellow)

</div>

## 关于 Mei

Mei 是一款面向 Android 的音乐播放器，围绕日常听歌、歌单管理和歌词阅读设计。应用支持手机和平板，根据可用空间调整页面布局，并提供多种播放器样式与个性化设置。

## 功能与特色

### 播放与音频

- **LX 自定义音源**：支持从本地文件或链接导入 JS 脚本，管理多个音源的启用状态和顺序。网易云音源不可用或仅提供试听时，可按设置尝试备用音源。[了解音源配置](docs/lx-sources.md)
- **智能歌曲过渡**：根据歌曲的节拍和小节强拍尝试衔接，也可选择固定时长淡化。
- **USB 独占播放**：通过 USB 直连音频设备播放；提供参数均衡器、频响曲线、预设和混响设置。
- **本地与下载**：扫描本地音乐，管理下载音质、保存位置和音乐缓存。

### 封面、歌词与播放器

- **动态封面**：支持匹配 Apple Music 和网易云的动态封面，可分别选择来源；无可用动态封面时回退至静态封面。
- **歌词体验**：支持逐字歌词、翻译和音译，可点击歌词跳转播放进度，也可开启桌面歌词。
- **ColorOS 锁屏歌词**：向支持的系统提供歌词数据，实际显示取决于系统版本及兼容情况。[了解支持范围](docs/coloros-lockscreen-lyrics.md)
- **播放器个性化**：提供不同播放器样式、动态背景、封面样式、进度条和播放操作设置。

### 发现与界面

- **音乐发现**：浏览推荐、每日推荐、排行榜，搜索歌曲、歌手、专辑与歌单，使用听歌识曲。
- **音乐库与歌单**：浏览自建和收藏歌单、最近播放及本地音乐，支持歌单歌曲排序。
- **手机与平板适配**：根据可用窗口空间调整歌单、播放器和设置页面布局。
- **主题外观**：支持动态主题、主题色、玻璃材质及动画设置。

动态封面、备用音源和歌词的可用性取决于内容及服务；USB 独占播放取决于设备兼容性，启用后部分音效和歌曲过渡不可用。请仅导入可信来源的音源脚本。

## 安装

从项目的 [Releases](https://github.com/ljyh223/Mei/releases) 页面下载适合设备架构的 APK。应用最低支持 Android 10（API 29）。

## 登录网易云音乐

在应用中打开 **设置 → 账号 → 扫码登录**，使用网易云音乐 App 扫描二维码并确认。只有一台设备时，可以先截取二维码，再从网易云音乐 App 的扫一扫相册中识别。

如果扫码不可用，也可以在同一页面手动填写 `MUSIC_U`：只填写字段的值，不要包含 `MUSIC_U=`、`cookie=`、分号、空格或其他字段。填写后可使用“测试 Cookie”检查登录状态。

手动获取时，可登录网易云音乐网页版，在浏览器开发者工具的网络请求中找到 `MUSIC_U` 并复制其值。Cookie 相当于账号凭据，请妥善保管，不要将其分享或提交到公开仓库。Cookie 失效时需要重新获取。

## 界面预览

<table>
  <tr>
    <td><img src="./screenshot/2026-10-28/home.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/find.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/library.jpg" width="200"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-10-28/player_cover.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/player_lyric.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/player_comment.jpg" width="200"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2026-10-28/player_dynamic_cover.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/player_pure_cover.jpg" width="200"></td>
    <td><img src="./screenshot/2026-10-28/setting.jpg" width="200"></td>
  </tr>
</table>

## 从源码构建

项目使用 Gradle Wrapper。构建前请准备 JDK 21、Android SDK、NDK、CMake 3.22.1，以及 Rust 和 Cargo；原生 USB 模块的工具链与目标架构要求见 [Rust USB 构建说明](app/src/main/rust/mei-usb-core/README.md)。

```bash
./gradlew :app:assembleDebug
```

项目结构与各功能的修改入口见 [整体架构与修改入口](docs/architecture.md)。正式发布与测试包的构建流程分别见 [正式发布构建](docs/release-build.md) 和 [Beta 构建](docs/beta-build.md)。

## 开源致谢

感谢以下项目及其贡献者：

- [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db)：歌词数据。
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui.git)：歌词组件。
- [qrcDecrypt](https://github.com/TLittlePrince/qrcDecrypt)：QRC 解密算法。
- [BeatNet](https://github.com/mjhydri/BeatNet)：用于智能过渡的节拍与小节强拍识别；模型署名和转换说明见 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES)。

其他第三方组件及其许可说明见项目中的 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES)。Mei 的应用代码采用 [Apache License 2.0](LICENSE)。

---

Mei 仍在持续开发。感谢你使用 Mei，愿我们终将相遇。

Mei 2026（梅）
