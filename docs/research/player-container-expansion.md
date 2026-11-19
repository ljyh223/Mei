# mini player 到全屏播放器的容器展开研究

研究分支：`test/player-container-expansion`。基线：`main` 的 `8c5256c`。初始研究：2026-10-31；原型更新：2026-11-03。

本文记录源码核对、官方 API 限制和实验实现。当前已接入手机竖屏、手机横屏与平板，尚未进行真机验证。

## 结论

建议保留现有 `BottomSheetState` 的连续进度、手势锚点和播放会话，在其上增加一个独立的容器展示层：**外部矩形与圆角连续变化，内部全屏布局保持目标尺寸，封面单独共享位置。**

这里的“同一个容器”首先指屏幕上的视觉连续性，不要求将 mini 行里的文字、图标乃至原生渲染 View 原样拉大。全屏控件可以有自己的布局，并在容器展开后段交接。

不能仅把 `Sheet` 改成 `Morph`，也不能直接把现有 `sharedTransitionKey` 打开就作为完成方案。主要难点是起点坐标、原生背景和中途反向操作，而不是缺少一条尺寸插值公式。

## 当前实现：手机竖屏、手机横屏与平板

- `BottomSheet` 增加可选的 `expansionBounds`／`expansionRadius`；未提供或边界无效时保留原 sheet 行为。手机竖屏和平板从底栏起步，手机横屏从左侧圆形播放器按钮起步。
- `PlayerContainerExpansion.kt` 计算像素矩形和圆角，并通过 `playerContainerViewport` 固定子内容的完整窗口测量。外层实际布局与点击区域随当前矩形变化，子内容反向平移以保持根坐标系一致，不缩放文字。
- `MiniPlayerBarContent` 上报实际底座边界；手机绑定层在收起时记录它，展开后不再更新，并冻结紧凑形态。窗口尺寸改变后旧边界失效，重新收起测量前回退普通 sheet。
- mini 已加载的缩略图 Painter 可交给共享封面使用，高清加载继续复用原请求和缓存；接管后不再绘制 mini 缩略图或它的占位框，不为过渡增加一次缩略图请求。
- 手机竖屏现在通过 `FluidBackground(clipToContainer = true)` 使用新的 `MeshTextureView`。它复用原 `MeshGradientRenderer`、专辑图片缓存和全部 mesh 参数，使用独立渲染线程的 EGL／Choreographer 向 TextureView 输出，参与容器裁剪与透明度合成。竖版动态封面背景也已恢复，封面底色扩展与方形动态封面沿用原组件。
- 原 SurfaceView 路径仍是其他调用方的默认值。Texture 宿主在首次专辑帧提交后才显现，离开窗口时暂停帧回调；销毁时由渲染线程依次释放 EGL、自己的位图副本和 SurfaceTexture，不阻塞 UI 等待渲染线程。初始化失败保留底色回退并记录日志。
- mini 边界使用 `positionInRoot + size`，不再将 `boundsInRoot` 返回的裁剪后可见高度当成真实布局高度。回归检查确认此前 48dp 布局可被误记录为 24dp；修正后过薄或过厚的起点都恢复到 48dp。
- 封面通过 `resolveAppleArtworkBounds` 混合两条完整路径：普通模式沿 mini → 大封面的既有轨迹，header 模式沿 mini → header 的直接轨迹。避免用 sheet 进度再乘 header 进度，将收起的小封面先拉大再缩回 mini；完整展开时切换 header 的轨迹保持不变。
- 手势和返回仍交给同一个 `BottomSheetState`。过渡期间隐藏两套控件的可访问性入口，并用临时点击屏障阻止淡出控件误触；父层垂直拖动继续工作。
- 平板实测起点和共享封面已接入，详见 [平板播放器](../tablet-player.md#容器展开)。接下来仍需真机检查纹理背景首帧、裁剪与性能。未增加正式设置；实现随本次播放器调整合入 main。

## 1. 最初基线与可复用部分

| 入口 | 已核对的行为 | 对实验的意义 |
| --- | --- | --- |
| `ui/component/sheet/BottomSheet.kt` | `AnchoredDraggableState` 管理 Dismissed／Collapsed／Expanded；`progress` 为连续值 | 继续作为唯一运动进度，保留拖动和松手归位 |
| 同文件的 `BottomSheetContainerMotion` | 支持 `Morph` 与 `Sheet`；前者改变外层测量高度，后者保持全屏高度再移动 | 已有尺寸变化能力，但不是完整的胶囊到全屏交接 |
| `BottomSheetCalculations.kt` | 根据边距、居中宽度、高度和圆角计算 morph | 可以参考纯计算分层，当前模型缺少任意起点的水平位置 |
| `player/applemusic/AppleMusicPlayer.kt` | 实际选择 `Sheet`；全屏布局保留组合；封面使用同一进度插值 | 手机已有共享封面基础，不能在外层新增位移后继续重复计算一遍 |
| `player/MiniPlayerBar.kt` | 手机实际底座在内部加左右边距，48dp 高、24dp 圆角；封面 32dp | 实际可见底座不是 BottomSheet 外壳的整个宽度 |
| `tablet/component/TabletMiniPlayer.kt` | 平板底栏有独立的 mini 内容、48dp 封面 | 平板起点与手机不同，应由外层提供几何 |
| `tablet/component/TabletBottomBar.kt` | 导航、mini、搜索整组居中；整组随进度淡出并下移 | mini 不在屏幕正中；动画开始后不能持续采集正在下移的起点 |
| `tablet/player/TabletPlayerScreen.kt` | 独立全屏 sheet，`collapsedContent` 为空 | 当前是两个容器交接，尚无共享容器或封面转场 |
| `ui/motion/PlayerMotionSpec.kt` | 背景、mini、导航和全屏 UI 有分段显隐窗口 | 沿用统一进度的思路；新视觉方案需要重新协调窗口 |

当前没有调用方传入 `sharedTransitionKey`。其现有代码以 `isTargetExpanded` 驱动 `AnimatedContent`，属于目标状态切换；它目前没有接上任意拖动进度，不能直接保证逐帧跟手。

## 2. 为什么简单切换 Morph 不够

### 2.1 外壳与真实 mini 的边界不一致

手机 `AppleMusicPlayer` 的 morph 配置仍是 `collapsedHorizontalMargin = 0.dp`，真实胶囊的边距在 `MiniPlayerBarContent` 内部处理；外壳圆角与内部胶囊也不同。简单改枚举会让错误的起点开始长大。

平板还存在水平偏移。以 1280dp 宽、普通字体的窗口为例，沿现有底栏公式计算：导航 300dp、mini 340dp、搜索 64dp，两处间隔各 12dp，整组宽 728dp。实际 mini 左边为 588dp；将 340dp 容器直接屏幕居中则为 470dp，相差 118dp。这是公式推导的示例，不是设备测量。

因此输入必须是 mini 在共同宿主中的实际矩形，而不只是“宽、高、底部边距”。窗口安全区、页面底栏显隐、手机紧凑形态也不能再靠另一套常量重复推算。

### 2.2 改容器尺寸会改变子内容的布局

现有 Morph 会将中间尺寸传给子内容。手机通过 `BoxWithConstraints` 计算封面和控制区；平板根据可用宽高及字体选择单／双栏。逐帧缩小约束会使这些终态布局重新排列，甚至在动画中跨过分栏断点。

目标应是固定测量终态内容，再改变可见轮廓与位置，避免标题不断换行、两栏变单栏、按钮挤压。也不应非等比缩放整页，否则文字和播放图标会被拉扁。

### 2.3 背景不是纯 Compose 绘制

`FluidBackground` 通过 `AndroidView` 使用 `MeshBackgroundView : GLSurfaceView`；动态封面则使用 `TextureView`。保持全屏 View 尺寸只是必要条件，不能据此承诺父级 Compose 的圆角和透明度一定能控制原生 surface。

Android 官方说明，SurfaceView 从 Android 14 才支持任意 alpha 混合；此前 0 与 1 之间的 alpha 会被忽略。本项目 minSdk 为 29，必须考虑这段兼容范围。[SurfaceView 文档](https://developer.android.com/reference/android/view/SurfaceView)

Compose 共享元素文档也明确列出 View／Compose 互操作限制，并且不会自动完成形状插值。把整个含 AndroidView 的播放器直接交给共享元素 API，不足以解决背景问题。[共享元素文档](https://developer.android.com/develop/ui/compose/animation/shared-elements)

## 3. 推荐的职责边界

以下名称是方案草案，不是现有类：

```text
PhoneApp / TabletApp
├── 底栏：上报 mini 底座与封面在共同宿主中的边界
├── BottomSheetState：唯一连续进度、拖动与锚点
└── PlayerExpansionHost：过渡轮廓、裁剪、内容交接
    ├── mini 展示内容
    ├── 过渡背景与共享封面
    └── 固定目标尺寸的手机／平板播放器内容
```

- `PlayerHost` 继续持有共享会话、ViewModel 和弹层；不创建第二个播放引擎。
- 手机和平板分别提供各自起点、终态布局和封面目标，公共宿主只做运动几何。
- 第一版先接手机竖屏，再接平板；平板因起点偏在右侧，必须同时插值 x、y、宽和高。
- 旧 `Sheet` 保留为独立回退路径；研究分支先验证，不提前增加正式设置项。

### 唯一坐标与进度

将底座矩形 `R0` 和全屏内容宿主矩形 `R1` 转成同一坐标系、同一像素单位。全屏终点取实际窗口宿主，不取设备物理屏幕尺寸，避免分屏和安全区错误。

```text
p = BottomSheetState.progress
R(p) = lerp(R0, R1, p)
r(p) = lerp(miniRadius, expandedRadius, p)
A(p) = lerp(miniArtworkRect, expandedArtworkRect, p)
```

这是外轮廓与封面的基线公式。圆角还需受当前矩形短边限制。点击展开时可由现有弹簧推进 p；拖动时直接使用 p，不再叠一层独立的尺寸动画。

封面若先在根坐标系插值为 `A(p)`，绘制在移动容器内时应减去 `R(p).topLeft`，不能既使用根坐标又再加 sheet 位移。手机现有 `finalStart`／`finalTop` 必须在接入时统一换算；暂停缩放也必须算到实际可见边界里。

### 起点快照与交接

1. 收起时持续记录真实 mini 的布局边界。
2. 点击或开始上拖时，先确认边界有效，再冻结本次起点；无效时走旧 Sheet，不从 `(0,0)` 开始。
3. 动画中的导航隐藏、紧凑形态变化不得反写起点。平板尤其要避免把整组 bar 的下移算进容器轨迹。
4. 原 mini 保留布局占位，在过渡层接管后隐藏实际绘制和交互；任何时刻只显示一份封面和一套有效操作。
5. 收起前获取当前窗口与最终底栏的目标位置，沿同一路径反向返回。不能使用正在隐藏或被裁剪的 mini 边界作为目标。
6. 快速反向时从当前 p 继续，不跳到端点重播；窗口改变时重新测量并明确重定向或回退，不能盲用旧快照。

### 内容不需要全部形变

主要连续元素为容器轮廓和封面。mini 标题／小按钮退场，全屏标题／进度／控制区在后半程进入；分段显隐也从同一 p 推导，收起时自然反向。具体阈值应在原型后决定，不能把现有多个窗口原封不动叠加上去。

## 4. 背景如何推进

第一阶段曾使用纯 Compose 静态底色验证几何；当前已恢复流体背景，采用下面第二条 TextureView／EGL 路线。`MeshStaticModeKey` 由两种宿主共用的 `MeshGradientRenderer` 消费，不是纯 Compose 背景。静态模式冻结流动时间，切歌渐变仍完成；画面稳定后 SurfaceView 使用按需绘制，TextureView 停止提交帧回调。封面、参数、尺寸或可见性变化时重新请求绘制，每次提交都完整填充后缓冲区，不依赖 EGL 交换后的内容保留。恢复动态或重新显示时重置帧计时基准，避免把静止／隐藏时长计入动画。

第二阶段比较两条路线：

| 路线 | 优点 | 尚需确认 |
| --- | --- | --- |
| 转场期间用背景代理，端点交回现有 renderer | 可保留现有渲染实现 | 若要精确匹配收起第一帧，需要已有帧或一次异步取帧；取帧失败有缓存底色回退，不能逐帧回读 GL。全屏首帧就绪后的交接必须检查色差、闪烁及旧 surface 是否露出 |
| 为 mesh 增加可参与常规 View 合成的渲染宿主，例如 TextureView／EGL 适配 | 更适合持续裁剪和容器转场 | 涉及 EGL、线程、生命周期和性能；现有动态封面使用 TextureView 不等于 mesh 可以无成本替换 |

当前已将同一 mesh renderer 接入 TextureView 宿主，不逐帧取截图或重新实现 mesh 算法。TextureView 的合成属性见 [官方文档](https://developer.android.com/reference/android/view/TextureView)。不能仅凭模拟测试认定原生纹理的设备外观正确。

## 5. 路线选择

| 方式 | 判断 |
| --- | --- |
| 仅 Sheet → Morph | 可作为反例对照，不能作为完整实现 |
| 整个播放器套 sharedBounds | 有 AndroidView 限制，也需要处理可寻址进度与手势；暂不作为主路线 |
| 自定义过渡宿主＋固定布局＋共享封面 | 最符合现有播放器的持续状态、原生背景和跟手要求，推荐先验证 |

官方提供的 `ScaleToBounds` 与 `RemeasureToBounds` 说明了“固定目标测量再缩放”与“逐帧重新布局”的区别；本项目可以借鉴这一区分，而不必将整页强行纳入共享元素转场。[自定义共享元素转场](https://developer.android.com/develop/ui/compose/animation/shared-elements/customize)

## 6. 实验顺序与验收

1. 纯几何阶段：记录实际 mini 起点；外轮廓连续展开／收起；内部保持全屏测量。先完成点击、半途反向和手动拖动。
2. 封面与交互阶段：复用手机高清封面请求和缓存；去除双份封面；标题与控件分段交接；导航和搜索分别退场。
3. 原生背景阶段：接入已就绪背景交接，检查首次展开、快速收起再打开、切歌中展开与动态视频，保留失败回退。
4. 平板阶段：接实际非居中 mini 起点，共用运动层，保留现有单双栏终态布局。

可自动检查：端点矩形一致、半途反向连续、无效边界回退、坐标换算、窗口与字体变化、隐藏内容不可点击，以及展开不重复创建播放连接或触发预加载。

设备验收重点：容器边缘与封面无断层、标题无重排跳动、背景无白线或露底、首帧不闪、拖动不与歌词／队列滚动冲突。按项目约定，本次没有安装 APK 或进行视觉测试；当前检查范围为 Kotlin 编译与定向几何／Compose 交互检查，运行结果以本次交付说明为准。
