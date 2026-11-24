# Apple Music iPad 艺人背景研究

研究日期：2026-11-24。目标是解释 iPad 艺人页的人像、底色与正文之间的衔接，给 Mei 的平板背景提供可追溯参数。这里记录函数行为和独立实现思路，不把 Apple 二进制作为应用依赖。

## 来源与边界

样本来自 Apple 官方 iPad14,6 的 iPadOS 27.2.1（24B156）恢复固件，非收费下载站的 IPA。[Apple 发布记录](https://developer.apple.com/news/releases/?id=11092026c)。通过 [ipsw](https://github.com/blacktop/ipsw) 的 ranged APFS reader 读取所需文件，再从系统 shared cache 导出 MusicUI；没有安装应用或启动模拟器。

| 文件 | SHA-256 |
| --- | --- |
| Music.app/Music | c9ef76e0674b785fcebec515c643eec4f325f2d5b87698c796c8cbd22578259b |
| MusicApplication.framework/MusicApplication | cf9a9004e64db6f0c6007b52b65f8a0285aab29b35c6fb9a3479c16abe07347e |
| MusicUI.framework/MusicUI | a004533b56adc007bb387cc64c3916f6731db8cb0ef39823b3aefcbf3a8e5573 |

本次机器上的原始证据保存在 `/tmp/mei-am-ui-research/`，该临时路径不是持久化交付或运行依赖。关键文件为 `MusicUI-confirmed-functions.json`（反编译与汇编）、`MusicUI-imports-demangled.json`（导入符号）、`MusicUI-struct-fields.json` 与 `ReflectionContainerView-fields.json`（Swift 反射字段）。二进制、完整反编译、令牌和账号信息均不入库。

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
- 常规宽图使用 2:1 区域。只有普通人像时沿用完整适配和侧缘渐隐，避免把方形人像裁成狭长横幅；这是 Mei 的资源回退，不宣称与官方素材裁切相同。
- Android 12 支持滚动阶段的额外平台模糊，但没有径向可变模糊；更早版本保留反射与渐隐。没有在 CPU 上反复生成模糊位图。
- 手机竖屏、手机横屏、网易云平板资料头、AM 默认关闭的设置保持原有行为。
- 官方原生 variableBlur 的采样核、motion 视频、全部 size class／非零安全区分支及 overscroll 尚未逐像素等价。当前可复刻的是已确认的常规 iPad 背景结构和参数，不是 Apple 私有渲染器。

[Android RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect) 和 [AGSL shader sampling](https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference) 为 Android 实现依据。本地测试通过原生 Skia 验证 shader 编译、uniform 与双 pass 绑定，并检查横图失败后使用人像及滚动／反射不重复发起图片请求；Robolectric 的 SDK 33/34 使用旧 SkSL 而不支持 `.eval`，因此该检查使用 SDK 36，见 [Robolectric issue 9691](https://github.com/robolectric/robolectric/issues/9691)。这不是模拟器或设备视觉测试，也不能证明 GPU 性能和最终像素已经一致。
