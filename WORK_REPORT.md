# Mei 音乐播放器 - 项目技术分析报告

## 一、项目概览

| 项目 | 详情 |
|------|------|
| **名称** | Mei (梅) - 第三方网易云音乐客户端 |
| **包名** | `com.ljyh.mei` |
| **最低SDK** | Android 10 (API 29) |
| **目标SDK** | Android 16 (API 36) |
| **当前版本** | v1.53 |
| **开发语言** | Kotlin |
| **UI框架** | Jetpack Compose |
| **构建工具** | Gradle KTS + Version Catalog |

---

## 二、技术栈清单

### 2.1 核心框架

| 分类 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **UI** | Jetpack Compose (BOM) | 2026.04.01 | 声明式UI |
| **UI** | Material3 | 1.4.0 | Material Design 3 组件 |
| **DI** | Hilt (Dagger) | 2.58 | 依赖注入 |
| **DB** | Room | 2.8.4 | 本地数据库 |
| **网络** | Retrofit | 3.0.0 | HTTP API 客户端 |
| **网络** | OkHttp | 5.3.2 | HTTP 底层 + 日志拦截 |
| **播放器** | Media3 (ExoPlayer) | 1.10.0 | 音频播放引擎 |
| **图片** | Coil 3 | 3.4.0 | 图片加载 (Compose原生) |
| **序列化** | Gson | - | JSON 序列化/反序列化 |
| **序列化** | Kotlinx Serialization | 1.11.0 | Kotlin原生序列化 |
| **异步** | Kotlin Coroutines | 1.10.2 | 异步编程 |
| **导航** | Navigation Compose | 2.9.8 | 页面导航 |
| **分页** | Paging 3 | 3.4.2 | 列表分页加载 |
| **存储** | DataStore | 1.2.1 | 键值对偏好存储 |
| **任务** | WorkManager | 2.11.2 | 后台下载任务 |
| **日志** | Timber | 5.0.1 | 结构化日志 |

### 2.2 UI/视觉库

| 库 | 版本 | 用途 |
|---|---|---|
| Material Icons Extended | - | 扩展图标库 |
| Material Kolor | 4.0.4 | 动态主题色生成 |
| kmpalette | 3.1.0 | 图片主色提取 |
| compose-shimmer | 1.4.0 | 骨架屏加载效果 |
| compose-cloudy | 0.5.0 | 模糊效果 (Android 12以下兼容) |
| Compose-Colorful-Sliders | 1.2.2 | 彩色滑块组件 |
| zoomable | 2.11.1 | 图片缩放查看 |
| accompanist-lyrics-core/ui | 0.4.5/1.0.19 | 逐字歌词渲染引擎 |
| reorderable | 3.1.0 | 列表拖拽排序 |

### 2.3 加密/工具库

| 库 | 版本 | 用途 |
|---|---|---|
| korlibs-crypto | 6.0.1 | AES/DES加密 (排除了Android端冲突) |
| kotlin-reflect | 2.1.21 | Kotlin反射 |
| jaudiotagger | 3.0.1 | 音频文件标签写入 (FLAC/MP3) |

### 2.4 构建配置

| 配置 | 值 |
|------|-----|
| AGP | 8.12.0 |
| Kotlin | 2.3.0 |
| KSP | 2.3.9 |
| Java兼容 | 21 |
| 混淆 | 开启 (release) |
| 资源压缩 | 开启 (release) |

---

## 三、架构设计

### 3.1 整体架构

项目采用 **MVVM + Repository** 分层架构：

```
┌─────────────────────────────────────────────┐
│                   UI Layer                   │
│  Composable Screens ← ViewModels ← StateFlow│
├─────────────────────────────────────────────┤
│               Domain Layer                   │
│            Repository (数据协调)              │
├──────────────┬──────────────────────────────┤
│  Local Data  │       Remote Data            │
│  Room DB     │  Retrofit (网易云/QQ音乐)     │
│  DataStore   │  OkHttp + NeteaseInterceptor  │
│  File Cache  │  加密层 (WeApi/EApi)          │
└──────────────┴──────────────────────────────┘
```

### 3.2 依赖注入 (Hilt)

两个核心 Hilt Module：

- **`AppModule`** (`SingletonComponent`): 提供 `AppDatabase`、所有本地Repository（Color、Like、Song、QQSong、Playlist、History、Albums、Download、CachedLyric）
- **`RetrofitModule` / `RepositoryModule`**: 提供 OkHttpClient、Retrofit实例（网易云 + QQ音乐）、所有网络Repository

### 3.3 Composition Locals (UI层DI)

| Local | 类型 | 作用 |
|-------|------|------|
| `LocalNavController` | NavController | 全局导航 |
| `LocalPlayerConnection` | PlayerConnection? | 播放器连接 |
| `LocalDatabase` | AppDatabase | 数据库访问 |
| `LocalPlayerAwareWindowInsets` | WindowInsets | 底部播放器适配 |
| `LocalUserData` | UserData | 当前用户信息 |

---

## 四、功能模块详解

### 4.1 音频播放系统

#### 核心组件

| 组件 | 文件 | 职责 |
|------|------|------|
| `MusicService` | `playback/MusicService.kt` | MediaLibraryService，管理ExoPlayer生命周期 |
| `AudioPlayer` | `playback/AudioPlayer.kt` | 音量淡入淡出包装器 (500ms渐变) |
| `PlayerConnection` | `playback/PlayerConnection.kt` | UI层与Service的桥梁，暴露StateFlow |
| `PlaybackQueueManager` | `playback/PlaybackQueueManager.kt` | 播放队列管理（普通模式 + FM模式） |
| `MediaUriProvider` | `playback/MediaUriProvider.kt` | 音频URL解析（本地→缓存→API） |
| `CacheManager` | `playback/CacheManager.kt` | ExoPlayer磁盘缓存 (10GB LRU) |
| `SleepTimer` | `playback/SleepTimer.kt` | 定时停止播放（倒计时/歌曲结束） |
| `MusicPreloadStrategy` | `playback/MusicPreloadStrategy.kt` | 预加载下一首 (5秒缓冲) |
| `DownloadWorker` | `playback/DownloadWorker.kt` | WorkManager后台下载 |

#### 播放队列系统

定义了 `Queue` 接口及实现：
- `ListQueue`: 静态歌曲列表
- `EmptyQueue`: 空队列单例
- 支持FM模式（私人FM）：自动加载推荐歌曲，剩余≤3首时自动补充

#### 播放模式

- 列表循环
- 单曲循环
- 随机播放（FM模式下禁用）
- 上一首行为可配置（回到开头/切换上一首）

#### 错误处理

- 自定义 `MusicLoadErrorHandlingPolicy`：404/SourceNotFound不重试
- 连续错误计数器：5次连续错误后停止播放
- 自动跳过失败曲目

### 4.2 歌词系统

这是项目最复杂的子系统，支持**三源并行获取**和**四种歌词格式**。

#### 歌词源

| 源 | 优先级 | API | 格式 |
|---|--------|-----|------|
| 网易云 | 2 | `/api/song/lyric/v1` | YRC(逐字) + LRC + 翻译 |
| QQ音乐 | 1 | `/cgi-bin/musicu.fcg` | QRC(逐字, 加密) + LRC + 翻译 |
| Apple Music (AMLL) | 3 | `amlldb.bikonoo.com` | TTML (Timed Text) |

#### 歌词解析器

| 解析器 | 格式 | 特点 |
|--------|------|------|
| `YRCParser` | 网易云YRC | 逐字歌词 `[时间,偏移,时长]文字` |
| `QRCParser` | QQ音乐QRC | 逐字歌词，支持背景人声 `[bg:...]` |
| `LRCParser` | 标准LRC | 行级同步歌词 `[MM:SS.ms]文字` |
| `TTMLParser` | Apple Music TTML | XML格式，支持对唱/拼音/背景人声 |

#### QRC解密

`QRCUtils.kt` 实现了完整的 **Triple-DES** 解密流程：
1. Hex解码 → Triple-DES解密（3个密钥）→ zlib解压 → 提取XML歌词内容

#### 对唱检测 (DuetDetector)

- 自动检测歌词中的对唱标记（如 `[男]`/`[女]`、角色前缀等）
- 支持单源检测和双源合并检测
- 对唱行交替使用 `Alignment.Start` / `Alignment.End` 显示

#### 歌词缓存策略

```
内存缓存 (FIFO, max 5)
    ↓ miss
Room数据库 (cached_lyric表)
    ↓ miss
三源并行获取 → 合并 → 写入缓存
```

#### 歌词预加载

`LyricPreloader` 在当前歌曲播放时预取下一首歌词，静默执行不更新UI状态。

### 4.3 网络与API

#### 网易云API加密 (`NeteaseInterceptor`)

三种加密模式由URL路径自动判断：

| 模式 | URL前缀 | 加密方式 | User-Agent |
|------|---------|----------|------------|
| **WeApi** | `/weapi/` | 双重AES-CBC + RSA | PC浏览器 |
| **EApi** | `/eapi/` | AES-ECB + MD5摘要 | 桌面客户端 |
| **Api** | `/api/` | 明文FormBody | Android |

**WeApi加密流程**:
1. 生成随机16字符密钥
2. AES-CBC加密（固定密钥 `0CoJUm6Qyw8W8jud` + IV `0102030405060708`）
3. AES-CBC二次加密（随机密钥 + 同IV）
4. RSA加密随机密钥 → `encSecKey`

**EApi加密流程**:
1. 构造消息: `"nobody" + url + "use" + data + "md5forencrypt"`
2. MD5哈希
3. 拼接: `url + "-36cd479b6b5-" + data + "-36cd479b6b5-" + digest`
4. AES-ECB加密（密钥 `e82ckenh8dichen8`）

#### CDN资源URL构造

`encryptId()` 函数：XOR（密钥 `3go8&$8*3*3h0k(2)2`）→ MD5 → Base64 → 替换字符

#### 反作弊机制

- 硬编码 `checkToken` 字符串用于写操作（歌单订阅/取消）
- 随机生成中国IP地址（`X-Real-IP`, `X-Forwarded-For`）
- 设备ID生成（从 `assets/devices.txt` 随机选取）

#### API端点清单

**网易云 API (25+ endpoints)**:
- 歌曲: 详情、URL、歌词、喜欢、FM、智能播放列表
- 歌单: 详情、创建、订阅/取消、删除、添加/移除歌曲
- 专辑: 详情、订阅/取消、收藏列表
- 用户: 账户、歌单、照片、订阅统计
- 搜索: 歌曲/歌手/专辑/歌单、搜索建议
- 评论: 评论列表、楼层回复
- 歌手: 详情、专辑、歌曲
- 首页: 资源推荐

**QQ音乐 API (3 endpoints)**:
- 搜索歌曲
- 获取歌词
- 获取LRC歌词

**外部 API**:
- AMLL歌词数据库 (Apple Music TTML歌词)

### 4.4 数据存储

#### Room数据库 (v15, 15张表)

| 表名 | 实体 | 用途 |
|------|------|------|
| `song` | Song | 歌曲元数据（本地/流媒体） |
| `qqSong` | QQSong | QQ音乐ID映射 |
| `playlist` | Playlist | 歌单 |
| `playlist_song_cross_ref` | PlaylistSongCrossRef | 歌单-歌曲关联 |
| `albums` | AlbumEntity | 专辑 |
| `artists` | ArtistEntity | 歌手 |
| `album_artist_cross_ref` | AlbumArtistCrossRef | 专辑-歌手关联 |
| `playback_history` | PlaybackHistory | 播放历史 |
| `like` | Like | 喜欢的歌曲 |
| `download_task` | DownloadTask | 下载任务 |
| `cached_lyric` | CachedLyric | 歌词缓存 |
| `color` | CacheColor | 封面主色缓存 |

#### 数据库迁移

共7次迁移 (v8→v15)，包括：
- 歌词缓存表、下载任务表
- 歌曲/歌单字段扩展
- 歌单-歌曲关联表
- 艺术家JSON数组迁移

#### DataStore偏好

50+ 偏好设置键，涵盖：
- 用户信息（Cookie、头像、昵称）
- 外观（动态主题、播放器风格、封面样式、进度条样式）
- 播放（音质、循环模式、上一首行为）
- 下载（路径、音质）
- 网格背景（流速、渲染精度、低频响应等6项）
- 歌词（字号、加粗、对齐）
- 调试模式

### 4.5 UI架构

#### 导航结构

```
MainActivity
├── BottomNavigation (3 tabs)
│   ├── Home (首页推荐)
│   ├── FindMusic (发现音乐)
│   └── Library (音乐库)
├── SearchBar (嵌入顶部)
└── BottomSheetPlayer (底部播放器)
    ├── MiniPlayer (折叠态)
    └── FullPlayer (展开态)
        ├── AppleMusicPlayer (苹果风格)
        └── ClassicPlayer (经典风格)
            ├── PhonePortrait
            ├── Tablet
            └── ImmersiveLandscape
```

#### 页面清单 (20+ 页面)

| 页面 | 路由 | 功能 |
|------|------|------|
| HomeScreen | `home` | 首页推荐（日推、雷达、私人推荐） |
| FindMusicScreen | `find_music` | 发现（70+分类、精品歌单） |
| LibraryScreen | `library` | 音乐库（创建/收藏歌单、收藏专辑） |
| PlaylistScreen | `playlist/{id}` | 歌单详情（分页加载） |
| AlbumDetailScreen | `album/{id}` | 专辑详情 |
| ArtistScreen | `artist/{id}` | 歌手主页（头像、简介、热门歌曲、专辑） |
| SearchResultScreen | `search_result/{query}/{type}` | 搜索结果（歌曲/歌手/专辑/歌单 Tab） |
| HistoryScreen | `history` | 播放历史 |
| CommentScreen | `comment/{songId}` | 评论（支持排序、楼层回复） |
| SettingScreen | `setting` | 设置 |
| AppearanceSettings | `setting/appearance` | 外观设置 |
| ContentsSetting | `setting/content` | 内容设置（Cookie输入） |
| PlaySetting | `setting/play` | 播放设置 |
| DownloadSetting | `setting/download` | 下载设置 |
| DownloadManageScreen | `download_manage` | 下载管理（过滤、暂停/恢复/删除） |
| LocalMusicScreen | `local_music` | 本地音乐 |
| EveryDay | `everyday` | 每日推荐 |
| AboutScreen | `about` | 关于（隐藏开发者模式：7次点击） |
| LogScreen | `log` | 日志查看 |

#### 主题系统

- **动态主题**: 基于当前播放歌曲封面提取主色（kmpalette + Palette API）
- **MaterialKolor**: 从种子色生成完整 Material3 色彩方案
- **颜色缓存**: Room数据库持久化，避免重复提取
- **模糊背景**:
  - `FluidBackground`: OpenGL ES网格渐变 + 低频音频可视化
  - `AmbientBackground`: Canvas动画圆 + 模糊效果

#### 播放器样式

1. **Apple Music 风格**: 全屏封面 + 流体背景 + 歌词覆盖
2. **经典风格**: 3种布局模式
   - 手机竖屏：垂直堆叠
   - 平板：左右分栏（3D翻转动画）
   - 横屏沉浸：封面+歌词并排

### 4.6 下载系统

- **WorkManager** 后台下载，支持通知显示进度
- **断点续传**: 暂停/恢复任务状态管理
- **音频标签写入**: jaudiotagger 写入 FLAC/MP3 元数据（标题、歌手、专辑、封面）
- **MediaStore集成**: 下载完成后注册到系统媒体库
- **任务状态**: PENDING → DOWNLOADING → COMPLETED/FAILED

### 4.7 其他功能

| 功能 | 实现方式 |
|------|----------|
| 定时停止 | 倒计时 + 歌曲结束模式 |
| 喜欢音乐 | API调用 + 本地Room同步 |
| 播放历史 | 5秒后记录到Room |
| 音质选择 | 7档: standard → jymaster |
| FM模式 | 私人FM自动加载推荐 |
| 智能播放列表 | 心动模式（基于当前歌曲推荐） |
| 搜索建议 | 300ms防抖 + 实时API |
| 评论浏览 | 分页 + 3种排序 + 楼层回复 |
| 歌单管理 | 创建、订阅/取消、删除、歌曲添加/移除 |
| 专辑收藏 | 订阅/取消 + Room同步 |
| 日志系统 | Timber + 每日文件日志 + 崩溃日志 |
| 音频可视化 | FFT低频检测 → 网格背景响应 |

---

## 五、代码规模统计

| 目录 | 文件数 | 说明 |
|------|--------|------|
| `data/model/` | ~35 | 数据模型（API/DB/UI） |
| `data/network/` | 4 | Retrofit Service接口 |
| `data/repository/` | 8 | Repository实现 |
| `di/` | ~15 | 依赖注入 + 数据库 + DAO |
| `playback/` | 10 | 播放系统核心 |
| `ui/` | ~50 | UI页面 + ViewModel |
| `utils/` | ~25 | 工具类 |
| `extensions/` | 1 | 扩展函数 |
| `constants/` | 2 | 常量 |
| **总计** | **~150** | Kotlin源文件 |

---

## 六、第三方模块

### `applemusic-like-lyrics` 目录

项目根目录包含一个 **Rust + TypeScript** 混合项目（来自 [amll-ttml-db](https://github.com/Steve-xmh/amll-ttml-db)），用于歌词数据处理。这是一个独立的子模块，包含：
- Rust (Cargo.toml) — 歌词数据处理核心
- TypeScript/Node.js (package.json) — 工具链
- pnpm工作区管理

---

## 七、迁移到Flutter的建议

### 7.1 需要重点迁移的模块

| 模块 | 复杂度 | Flutter替代方案 |
|------|--------|-----------------|
| **播放引擎** | 高 | `just_audio` + `audio_service` |
| **歌词渲染** | 高 | 自定义Canvas绘制 或 fork `accompanist-lyrics-ui` 的Flutter版 |
| **数据库** | 中 | `drift` (原moor) 或 `sqflite` |
| **网络加密** | 中 | 直接移植AES/RSA/DES逻辑到Dart |
| **图片加载** | 低 | `cached_network_image` |
| **依赖注入** | 低 | `get_it` + `injectable` |
| **导航** | 低 | `go_router` |
| **主题** | 低 | Flutter Material3 + `dynamic_color` |
| **分页** | 低 | `infinite_scroll_pagination` |
| **后台任务** | 中 | `workmanager` |
| **通知** | 中 | `flutter_local_notifications` |
| **音频可视化** | 中 | FFT数据 + CustomPainter |

### 7.2 加密逻辑迁移清单

需要从以下文件移植核心加密代码：

1. **WeApi加密** (`Encryptor.kt` → Dart): AES-CBC × 2 + RSA
2. **EApi加密** (`Encryptor.kt` → Dart): AES-ECB + MD5
3. **EApi解密** (`Encryptor.kt` → Dart): AES-ECB
4. **QRC解密** (`QRCUtils.kt` → Dart): Triple-DES + zlib
5. **ID加密** (`Encryptor.kt` → Dart): XOR + MD5 + Base64
6. **设备ID生成** (`DeviceId.kt` → Dart)
7. **中国IP生成** (`ChineseIpUtils.kt` → Dart)

### 7.3 歌词系统迁移

这是最复杂的迁移部分：

1. 四种歌词格式解析器 (YRC/QRC/LRC/TTML) 需要完整移植
2. 对唱检测算法需要移植
3. 歌词合并/翻译合并逻辑需要移植
4. 逐字歌词渲染需要自定义实现（Flutter Canvas绘制）

### 7.4 数据库Schema迁移

Room的15张表需要映射到Flutter数据库方案：
- 推荐使用 `drift` (类型安全的SQLite ORM)
- TypeConverter (List<String> ↔ JSON) 需要在Dart中实现

---

## 八、总结

Mei 是一个功能完备的第三方网易云音乐客户端，核心技术亮点包括：

1. **三源歌词系统**: 网易云 + QQ音乐 + Apple Music，支持逐字歌词、对唱检测、翻译合并
2. **多层加密**: WeApi (AES+RSA) + EApi (AES+MD5) + QRC (Triple-DES)
3. **智能缓存**: 内存 → 磁盘 → 数据库 → 网络，四级缓存策略
4. **自适应UI**: 手机/平板/横屏三种布局，Apple Music/经典两种风格
5. **音频可视化**: 实时FFT低频检测驱动背景动画

代码质量较好，架构清晰，注释适度。主要复杂度集中在歌词系统和加密层，这两部分是Flutter迁移的重点和难点。
