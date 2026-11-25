# Apple Music iPad 艺人背景研究

研究日期：2026-11-24。目标是解释 iPad 艺人页的人像、底色与正文之间的衔接，给 Mei 的平板背景提供可追溯参数。这里记录函数行为和独立实现思路，不把 Apple 二进制作为应用依赖。

## 来源与边界

样本来自 Apple 官方 iPad14,6 的 iPadOS 27.2.1（24B156）恢复固件，非收费下载站的 IPA。[Apple 发布记录](https://developer.apple.com/news/releases/?id=11092026c)。通过 [ipsw](https://github.com/blacktop/ipsw) 的 ranged APFS reader 读取所需文件，再从系统 shared cache 导出 MusicUI；没有安装应用或启动模拟器。

| 文件 | SHA-256 |
| --- | --- |
| Music.app/Music | c9ef76e0674b785fcebec515c643eec4f325f2d5b87698c796c8cbd22578259b |
| MusicApplication.framework/MusicApplication | cf9a9004e64db6f0c6007b52b65f8a0285aab29b35c6fb9a3479c16abe07347e |
| MusicUI.framework/MusicUI（首轮含 local symbols 导出） | a004533b56adc007bb387cc64c3916f6731db8cb0ef39823b3aefcbf3a8e5573 |
| MusicUI.framework/MusicUI（本轮未附加 local symbols） | c6475ac85c410e1a1d5f689a5381348c9c0f752700ca1652c5144ac7ed91516a |
| MediaCoreUI.framework/MediaCoreUI（本轮） | a1003056fc93c73a60061a2a2c64465188a1e89f3449c5aed1ca469cc1ec6e44 |

首轮 `/tmp/mei-am-ui-research/` 临时证据已丢失；首轮结论保留在本文，不能再把该路径当作可用附件。本轮从同一官方固件重新导出 MusicUI 和 MediaCoreUI，复核下述回退分支，持久证据位于 `/home/ljyh/tools/re/mei-am-ui-research/`。`evidence-manifest.json` 记录来源、导出方法和哈希；`MusicUI-nonwide-functions.json`、`MusicUI-nonwide-fields.json`、`MediaCoreUI-ColorBackdrop.json`、`MediaCoreUI-shared-stubs.json`、`ColorBackdrop-types-resolved.json` 记录相关反编译、汇编、字段和符号对应关系。新旧 MusicUI 导出的符号表不同，文件哈希不能直接比较；本文地址仍对应同一固件中的虚拟地址。二进制、完整反编译、令牌和账号信息均不入库。

MusicApplication 内旧的 `MusicCoreUI.ProgressiveBlurView` 确实调用 UIKit variable blur，但其默认 cutoff=0.3 **不是**新版艺人页参数。下面结论均追到 MusicUI 的 `ArtistDetailBackgroundView` 与 `ReflectionContainerView`。

## 已确认的常规宽布局

用 `W` 表示背景内容宽度，`s` 表示向上滚动距离（iOS point；Mei 对应 dp）。以下简式省略零值左右安全区及 compact 分支，不能直接套给所有窗口。

1. 原始艺术图像区域高度为 `H = W / 2`，背景容器高度为 `2H`。有 motion 资源时，常规宽布局选择 `motionArtistFullscreen16x9`；其他 size class 分支可选 `motionArtistSquare1x1`。
2. `ReflectionContainerView` 的底部副本使用 `CATransform3DMakeScale(1, -3, 1)`。底部区域高度为 `3H`，从原图下沿延伸；左右还有水平翻转副本，处理 safeInsets 与 layoutDirection。Swift 反射字段进一步确认这些是 `CAPortalLayer`：`bottomPortal`、`leadingPortal`、`trailingPortal` 及两个底部角落 portal，共享 hostingController 的源图层。不是把整张图片统一拉伸，也不是先换成纯色。
3. 反射图像经过 `SwiftUI.View.variableBlur(maxRadius: 160, mask: ..., opaque: true)`。
4. 模糊遮罩是**径向渐变**。遮罩图像以半宽绘制，再扩展回原宽；中心位于顶部中央，横向比纵向更宽。因此中央上部清晰、两侧和下方逐渐柔化，不能用纵向线性渐变代替。
5. 独立的透明度遮罩使用纵向 LinearGradient：位置 0 与 0.25 为不透明，0.65 为透明；整个遮罩高度是 `2H = W`。该遮罩控制人像向底色的衔接，和模糊强度不是同一件事。
6. 模糊、透明度遮罩、缩放、偏移、整体透明度与额外模糊依次作用在背景上。标题、艺术字与播放操作不在被模糊的图层内。

径向遮罩的 stop（位置为渐变半径归一化距离，不是屏幕高度百分比）：

| 位置 | 白色 alpha |
| --- | --- |
| 0 | 0 |
| 0.4 | 0 |
| 0.5 | 0.0625 |
| 0.75 | 0.4 |
| 1 | 0.625 |

零左右安全区时，半宽遮罩展开对应的距离表达式为 `length(((x-W/2)/2, y)) / W`。将 mask alpha 线性映射为 Gaussian radius 是 Mei 的 Android 近似；尚未分析 SwiftUICore/渲染服务内部的实际采样核。

滚动参数：

| 通道 | 已确认公式 |
| --- | --- |
| 向上滚动偏移 | `-0.4 * max(s, 0)` |
| 背景缩放 | `1`，`s > 180` 后为 `1 + (s-180)*0.00025` |
| 背景整体透明度 | `clamp(1 - max(s-180,0)*0.005, 0, 1)` |
| 额外固定模糊 | `clamp((s-180)/6, 0, 100)` |
| 反向拉伸分支 | `s < 0` 时缩放为 `1 - s*0.002`；Mei 当前未另接过度下拉手势 |

背景色先取 artwork/video 的背景色。`0x21f3a07fc` 解析颜色后计算相对亮度 `L`；`L > 0.1` 时，通过 `0x21f9e6128` 对每个 sRGB 通道减去 `0.65*L` 并限制下界为 0。不是把亮度统一限制到 0.065。`0x21f9e64fc` 使用 sRGB transfer function 与 0.2126/0.7152/0.0722 权重计算相对亮度。SwiftUI Resolved 的 red/green/blue getter 与 linearRed 等属性需区分，参见 [Apple Color.Resolved](https://developer.apple.com/documentation/swiftui/color/resolved)。

## 核对地址

这些是该样本导出后二进制的虚拟地址，仅用于复核，升级后不可当作稳定地址。

| 地址 | 用途 |
| --- | --- |
| 0x21f39f7ac | GeometryProxy、安全区与常规尺寸装配；滚动透明度 |
| 0x21f39f920 | 背景主体；variableBlur 的 160、偏移、缩放及额外模糊 |
| 0x21f3a17d4 | BlurMaskInputs 变化后的遮罩重建 |
| 0x21f3a2d98、0x21f3a31c8 | 半宽遮罩绘制与 GraphicsContext.radialGradient |
| 0x21f3a5084 | 径向模糊遮罩 stops |
| 0x21f3a51d0 | 透明度遮罩 stops |
| 0x21f3a16c0 | 滚动缩放公式 |
| 0x21f40ac5c | 反射容器初始化与 CATransform3DMakeScale |
| 0x21f40b264 | 原图、底部与左右副本的布局 |
| 0x21f3a07fc、0x21f9e6128、0x21f9e64fc | 背景色选择、调整与亮度 |

导入 stub 也已对应到实际导出符号：`0x220044a10` 为 variableBlur，`0x220042e90` 为 radialGradient，`0x220042e80` 为 linearGradient；这一步用于避免只凭反编译中的参数形状猜测渐变类型。

## Mei 中的实现与未确认项

- `TabletArtistBackdrop` 独立于列表渲染背景；使用同一个 Coil painter 绘制原图和底部反射，不另建 loader、缓存或额外取色请求。
- `component/ArtistBackdropEffects` 在 Android 13+ 使用两个 AGSL Gaussian pass。每像素半径由上述径向 mask 控制；每个 pass 最多 41 次采样，清晰区域直接返回输入。这里没有玻璃材质、折射或高光。
- 遮罩合成后再对整体背景执行滚动变换和额外模糊；前景列表仍使用单一 LazyColumn，操作与队列索引沿用原业务入口。
- 常规 hero 使用 2:1 区域。后续已将普通人像的 Fit／侧缘渐隐替换为 ColorBackdrop 近似和前景圆形头像，见下文与 [艺人详情](artist-detail.md)。
- Android 12 支持滚动阶段的额外平台模糊，但没有径向可变模糊；更早版本保留反射与渐隐。没有在 CPU 上反复生成模糊位图。
- 手机竖屏、手机横屏、网易云平板资料头、AM 默认关闭的设置保持原有行为。
- 官方原生 variableBlur 的采样核、全部 size class／非零安全区分支及 overscroll 尚未逐像素等价。motion 现已接入单纹理视频及反射，仍未做设备输出与像素等价验证。当前可复刻的是已确认的常规 iPad 背景结构和参数，不是 Apple 私有渲染器。

[Android RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect) 和 [AGSL shader sampling](https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference) 为 Android 实现依据。本地测试通过原生 Skia 验证 shader 编译、uniform 与双 pass 绑定，并检查横图失败后使用人像及滚动／反射不重复发起图片请求；Robolectric 的 SDK 33/34 使用旧 SkSL 而不支持 `.eval`，因此该检查使用 SDK 36，见 [Robolectric issue 9691](https://github.com/robolectric/robolectric/issues/9691)。这不是模拟器或设备视觉测试，也不能证明 GPU 性能和最终像素已经一致。


## 缺少 2:1 静态背景时的实际分支

“目录接口没有 2:1 图片”不等于“原生艺人页没有 hero 资源”。原生 `ArtistDetailHeader` 分别持有 `circleArtwork`、`colorBackdropArtwork`、`artwork`、`videoArtwork`、`wideArtwork`、`wideVideoArtwork`；艺术字 `artistLogo` 也是独立字段。当前 Mei 直接从目录的 editorialArtwork 选择横图，并不能证明该选择等价于官方的 `wideArtwork`。

### 资源选择与布局条件

- 普通艺人详情的 provider 标记固定为 false；歌手简介页 `ArtistBioHeader` 才为 true。它不是图片宽高比标记，不能解释成“缺少宽图就改方形头部”。
- 常规 iPad（横向和纵向 size class 都非 compact）的详情背景读取 `wideArtwork` / `wideVideoArtwork`。横向或纵向 compact 时读取 `artwork` / `videoArtwork`。`0x21f3a2774` 读取的是 **verticalSizeClass**；不要把其几何分支写成横向 compact。
- 所选 artwork 或 video 非空时，进入已有的反射与可变模糊分支；不能仅按 JPEG 的尺寸决定走向。
- `0x21f3a3800` 检查所选 VideoArtwork 中的 `motionArtistFullscreen16x9`，存在时构造 `MediaCoreUI.MotionView(url:previewFrame:fallback:)`。preview 来自 `VideoArtwork.previewFrame`，失败内容来自所选静态 Artwork，再共同进入背景效果。没有所需 motion flavor 时使用静态资源。
- 因此，没有 2:1 静态字段的艺人仍可能拥有适合该分支的 16:9 motion 及预览帧。周杰伦、BTS 的目录响应曾返回这类资源；不能把其 4:1 宣传图直接当作官方平板背景。

### 没有 hero 时：ColorBackdrop

`0x21f39eaac` / `0x21f39eb1c` 检查所选 artwork 和 video；两者都为空时，进入 `0x21f39f554`：

1. 增强对比度分支填黑色背景。
2. 常规分支铺黑色 backing，同时从 provider 的 `colorBackdropArtwork` 加载图像。`0x21f3a22ac` 构造宽高均为 **256** 的 ArtworkImage，遵循资源 cropStyle；通过图像代理取得 UIImage 并缓存到 `__colorBackdropImage`。
3. UIImage 就绪后，`0x21f3a1b6c` 调用 `MediaCoreUI.ColorBackdrop(image:)`，不是把普通方形头像直接延展为清晰的全宽人像。

本轮继续追入 MediaCoreUI，已确认 ColorBackdrop 的具体组成：

| 层或修饰器 | 确认值 |
| --- | --- |
| 输入图像 | `Image(uiImage:)`，`resizable` 的模式为 `stretch`，capInsets 全零 |
| 图像框 | `_AspectRatioLayout` 为 1:1、ContentMode tag 1（`fill`） |
| 图像变换 | 中心锚点，横纵均放大 **2** 倍 |
| 图像对比度 | `_ContrastEffect` **0.5** |
| 覆盖图像的矩形 | 填充由 `ImageTreatment` 提供的 Material |
| Material backdrop | scale **0.1**，颜色为透明 sRGB |
| Material filters，按构造顺序 | `gaussianBlur(radius: 200, isOpaque: true, isDithered: false)`；`saturation(amount: 4)` |
| 后续覆盖 | 白色 opacity **0.08**，再黑色 opacity **0.35** |
| 离屏合成 | `drawingGroup(opaque: false, colorMode: .nonLinear)` |

这是对底图进行 backdrop 采样的材质层；上表的 0.1 是 `Material.Layer.backdrop` 的参数，200 是其滤镜参数。未追入底层渲染器前，不把它们合并宣称为某个确定的屏幕像素模糊半径，也不能把 200 原样套用成 Android dp 后宣称像素等价。此处描述 Apple 的图片效果，不代表 Mei 应在内容区使用项目约定禁止的玻璃容器。

符号核对经过 IDA 解码 shared stub，再按目标地址匹配 SwiftUI / SwiftUICore 的导出表。例如 `0x1d0163c80 → 0x188848024` 对应 gaussianBlur，`0x1d0163c70 → 0x18924a0bc` 对应 saturation。泛型符号引用还确认 `_ScaleEffect`、`_ContrastEffect`、`_OverlayModifier`，避免只凭寄存器里的常数猜效果名称。ContentMode 的枚举字段顺序为 `fit`、`fill`，此处存储 tag 1。

### 前景头像与艺术字

`ContentStack` 的反射字段为 `model`、`hasHeroArtwork`、`_verticalSizeClass`、`_pageGeometry`。`0x21f38bcf0` 将 `0x21f38c4a4` 的结果赋给 `hasHeroArtwork`；后者按布局读取相应 artwork / video，并考虑 motion 内容状态。

`0x21f39111c` 先检查这个标记：有 hero 时不构造独立头像；没有 hero 且存在 `circleArtwork` 时构造 **192×192** ArtworkImage。泛型符号引用已核对为 `_ClipEffect<Circle>`，并附加 Stroke。常规竖向排列中，头像与后续标题之间有 32 point 底部间距（`0x21f390c8c`）。艺术字或文本标题是另一条显示路径，不能用“有无艺术字体图片”决定是否使用 AM 背景。

### 回退分支核对地址

| 地址 | 用途 |
| --- | --- |
| 0x21f3a1260 / 0x21f3a1394 | 按 size class 选择静态 / motion 资源 |
| 0x21f3a2774 / 0x21f3a3318 | vertical compact 与 horizontal compact / provider 条件 |
| 0x21f39eaac / 0x21f39eb1c / 0x21f39f554 | 有 hero 与 ColorBackdrop 分支选择 |
| 0x21f3a1d68 / 0x21f3a22ac / 0x21f3a1b6c | colorBackdropArtwork 加载与 UIImage 消费 |
| 0x1ced37eb8 / 0x1ced37ee0 / 0x1ced37fe8 | ColorBackdrop 初始化、body、图像修饰器装配 |
| 0x1ced38b48 | ImageTreatment 的 Material.Layer 和 filters |
| 0x21f38bcf0 / 0x21f38c4a4 / 0x21f39111c | hasHeroArtwork 与独立圆形头像 |

### 对 Mei 的结论与剩余边界

研究定位到旧实现仅有人像时的 Fit、侧缘渐隐是 Mei 自己的回退，不能算作官方普通艺人方案。后续 UI 修改已区分宽幅静态 hero、motion 视频与静态预览，以及两者均不可用时的 ColorBackdrop 近似 + 独立头像。艺术字可选，不能当作背景可用性的开关；ColorBackdrop 在 Android 12 以下保留页面配色与圆形头像；有有效视频时，首帧后由单纹理动态背景接管。具体图层降采样与渐隐边界见 [艺人详情](artist-detail.md)。

本轮已确认原生展示分支和 ColorBackdrop 的构造参数；**尚未证明目录 API 的每个 editorialArtwork 字段如何映射到原生 `wideArtwork` / `colorBackdropArtwork` / `circleArtwork`**，尤其不能认定 subscriptionHero 总会成为 wideArtwork。4:1 图和 motion 的服务端映射规则、Material 的底层采样实现及最终像素效果仍未完整核对。该研究轮次没有修改应用 UI、安装 APK、启动模拟器或进行视觉测试；后续 UI 实现另按上面的实现记录说明。


后续视频实现复用 Media3 的装饰视频配置，使用 [TextureView](https://developer.android.com/reference/android/view/TextureView) 的纹理输出及 [RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect) 合成，不回读每帧位图。音频焦点显式关闭，参见 [ExoPlayer.Builder.setAudioAttributes](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer.Builder#setAudioAttributes(androidx.media3.common.AudioAttributes,boolean))。暂停、首帧和失败行为见 [艺人详情](artist-detail.md)；该实现不把 Android 的原生图层等同于 Apple 的 CAPortalLayer，也不据此宣称已验证真实设备像素输出。


视频接缝修正：用户设备截图出现原图与倒影的硬切换后，将外围 View 效果改为显式录制／绘制 RenderNode。原图和倒影先合并为单一输入，统一可变模糊后，再在隔离图层上进行 DST_IN 遮罩，最后施加额外滚动模糊；两次纹理绘制各自裁剪，源图下沿在两次变换后映射到相同的接缝坐标。依据 [RenderNode](https://developer.android.com/reference/android/graphics/RenderNode) 与 RenderEffect 的显式节点应用方式；新的定向测试确认显示列表与几何约束，仍未宣称已验证该设备的视频像素效果。
