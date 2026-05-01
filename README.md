<div align="center">

# Mei

### 一个仍在成长，但更新随缘的第三方网易云音乐播放器

<img src="./screenshot/logo.png" width="120px" style="border-radius:12px"/>

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-orange)
![Status](https://img.shields.io/badge/Development-In%20Progress-yellow)

</div>

---
## 项目介绍

Mei 是一款使用 **Jetpack Compose** 开发的 **网易云音乐第三方客户端**。
好久不见，最近会整合代码，并带来一些新UI，比如将applemusic-liek-lyric那样的效果迁移到本项目中
<table>
  <tr>
    <td><img src="./screenshot/2026-05-01/player1.jpg" width="200"></td>
    <td><img src="./screenshot/2026-05-01/player2.jpg" width="200"></td>
    <td><img src="./screenshot/2026-05-01/player3.jpg" width="200"></td>
  </tr>
</table>
---

## 功能进展

### 已实现

* 逐字歌词（支持：**网易云** / **99音乐** / **TMLL**）
* 播放列表管理(应该比较稳定)
* 喜欢音乐(似乎能用，但是不太稳定)
* 随机播放 & 播放顺序优化(算是稳定吧，修复了很多次)
* 定时播放
* 历史播放记录(本地存储)
* 专辑详情页
* 自选背景 & 个人中心 UI 迭代

---

### TODO 清单

| 任务         | 状态   |
| ---------- |------|
| 搜索功能    | 已完成  |
| 播放记录    | 已添加  |
| 性能优化    | 持续中  |
| 专辑页面    | 已完成  |
| 歌单管理    | 已完成  |
| 歌手主页 | 已完成  |
| FM 播放   | 以完成  |
| 红心模式    | 以完成  |

> 已完成 | 未来更新（不保证啥时候）

---

## 登录说明

⚠️ **本软件目前仅支持 Cookie 登录**

* 只需要 `MUSIC_U` 字段的 **值**
* ⚠️ 仅保留纯值 `xxxxx`，不要包含 `cookie=`, `MUSIC_U=`, `;`, `空格`, `其它字段` 等内容

✅ 正确示例：

```
xxxxx（仅纯值）
```

❌ 以下仍然**错误**的：

```
cookie=MUSIC_U=xxxxx
MUSIC_U=xxxxx
cookie: MUSIC_U=xxxxx;
```

以后可能会考虑添加更多登录方式，比如扫码、密码登录。

### 获取方式

1. 通过 **网页版登录网易云**
2. 打开浏览器开发者工具（F12）
3. 在任意请求的 Request Header 中找到 `MUSIC_U`
4. 复制它的 **纯值** 即可 ✅
5. Cookie 一般长期有效，无需频繁更新

---


## 开源致谢

感谢以下两位开源圣人：

* 提供高质量歌词库 [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db)
* 提供精美歌词组件 [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui.git)
* 提供qrc解密算法 [qrcDecrypt](https://github.com/TLittlePrince/qrcDecrypt)




## 软件界面预览

> 更多截图请看仓库 `screenshot` 目录

```
screenshot
```

<table>
  <tr>
    <td><img src="./screenshot/2025-12-22/Screenshot_2025-12-22-16-27-39-23_1f30cde8653eb5f00e783c830c9ae6c6.jpg" width="200"></td>
    <td><img src="./screenshot/2025-12-22/Screenshot_2025-12-22-16-27-45-49_1f30cde8653eb5f00e783c830c9ae6c6.jpg" width="200"></td>
    <td><img src="./screenshot/2025-12-01/Screenshot_2025-12-01-11-37-53-613_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-12-01/Screenshot_2025-12-01-11-38-04-398_com.ljyh.mei.jpg" width="200"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-27-33-154_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-27-35-720_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-27-39-301_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-27-43-241_com.ljyh.mei.jpg" width="200"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-28-20-878_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-28-22-374_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-12-22/Screenshot_2025-12-22-16-28-32-34_1f30cde8653eb5f00e783c830c9ae6c6.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-28-33-984_com.ljyh.mei.jpg" width="200"></td>
  </tr>
  <tr>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-29-00-715_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-29-20-876_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-29-24-064_com.ljyh.mei.jpg" width="200"></td>
    <td><img src="./screenshot/2025-11-26/Screenshot_2025-11-26-13-29-28-763_com.ljyh.mei.jpg" width="200"></td>
  </tr>

  <tr>
    <td><img src="./screenshot/2026-02-01/Screenshot_2026-01-31-20-39-33-53_1f30cde8653eb5f.jpg" width="200"></td>
    <td><img src="./screenshot/2026-02-01/Screenshot_2026-01-31-20-39-29-59_1f30cde8653eb5f.jpg" width="200"></td>
    <td><img src="./screenshot/2026-02-01/Screenshot_2026-01-31-20-39-39-14_1f30cde8653eb5f.jpg" width="200"></td>
  </tr>

<tr>
    <td><img src="./screenshot/2026-02-14/tab_playlist.png" width="400"></td>
    <td><img src="./screenshot/2026-02-14/tab_library.png" width="400"></td>
  </tr>
</table>

---


## 写在最后

嗯… 可能真的有点荒凉。但如果你读到这里，谢谢你。

Mei 还在写、还能听歌、歌词还能逐字滚动、感情虽然滚动不了了但代码还能动。让我们坚信，**我们终将相遇**。


Mei 2025(梅)

---