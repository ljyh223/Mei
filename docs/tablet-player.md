# 平板全屏播放器

平板全屏播放器从 `ui/tablet/player/` 接入独立内容结构。手机与平板复用播放服务、连接、ViewModel、状态采集及公共操作；各自决定全屏布局和展开几何。当前结构取代旧 `ClassicTabletLayout`，没有在手机播放器中继续增加平板判断。

## 接入与职责

```text
MeiAppState / TabletApp
└── TabletPlayerHost                    # 同一个展开锚点；折叠后移除命中区域
    └── PlayerHost                     # 共享会话入口、动态资源与公共弹层
        └── TabletPlayerScreen         # 状态绑定、行为、背景及复杂内容槽位
            └── TabletPlayerContent    # 标题、操作入口与自适应排列
                ├── TabletPlayerLayout # 纯尺寸规则
                └── TabletPlayerPanels # 歌词／队列／评论的展示转场
```

- `ui/component/player/PlayerHost.kt` 持有现有 `PlayerStateContainer`，沿用原 `PlayerViewModel`、`PlaylistViewModel` 与 `LocalPlayerConnection`。动态封面设置、屏幕常亮、喜欢反馈和 `CommonOverlayHandler` 在这里统一绑定，每个活动外层只有一个播放器入口。
- 原 `BottomSheetPlayer` 是手机外层适配入口：竖向沿用 `AppleMusicPlayer`，手机横向沿用经典沉浸布局。平板从 `TabletPlayerHost` 直接注入自己的展示，不经过旧经典平板分支。
- `TabletPlayerScreen` 是带业务依赖的绑定层。它读取状态、组装回调，并提供封面、控制区、歌词、队列和评论槽位。导航、ViewModel 与播放连接停留在这一层，不传入平板纯布局函数。
- `TabletPlayerContract.kt` 中的 `TabletPlayerUiState` 与 `TabletPlayerActions` 只描述展示所需的标题、喜欢状态、面板、颜色与用户操作。`TabletPlayerSlots` 承接已有复杂组件的渲染，不创建第二套音频引擎或数据源。
- `TabletPlayerContent` 负责几何和交互排列；`TabletPlayerPanels` 只处理转场。旧 `ClassicTabletLayout.kt`、`PlayerTableControls.kt` 和 `PlayerLayoutMode.kt` 已移除。

## 布局规则

尺寸规则接收扣除系统安全区、顶部收起按钮和公共左右边距后的实际内容宽高，以及字体比例。普通字体下，正文从 840dp 起允许两栏：扣除 40dp 间隔后左右各占一半，每栏至少 400dp；字体增大时提高两栏所需宽度。外层内容最大宽度为 1400dp，不以横竖屏或设备型号决定排列。

两栏时，左边为封面、标题、喜欢／更多、进度和播放操作；右边为歌词／队列／评论。左右栏保持等宽，左栏内的封面、标题及操作区、进度条与播放控制共用一个居中的有效内容宽度。该宽度按视口高度和栏宽计算，普通字体时以 280–460dp 为目标范围，空间不足时受栏宽限制，字体增大时适度放宽；短窗口通过左栏整组滚动承载内容，不单独挤压或截断操作。

单栏时，默认显示正在播放。队列或评论操作切入详情；详情顶部保留小封面与歌曲信息，底部保留同一组自定义 action，不设置另一组面板标签。歌词可从 more 的“显示歌词”打开，并可返回正在播放。歌词模式在正文高度至少为 460dp × 字体比例时保留紧凑播放控制，较短窗口通过“正在播放”返回完整控制，优先保证面板可读；评论和队列以内容浏览为主，不通过上滑重新浮出播放控制。

## 与手机端共用的视觉与行为

- 标题、喜欢和更多直接复用手机 `Title`／`PlayerControlButton`。进度条、上一首／播放／下一首和用户配置的操作栏复用 `PlayerControlsSection`。
- `PlayerControlsSection` 提供内容内边距和导航栏预留参数，默认值保持手机行为；平板已经消费系统安全区，使用 0dp 内容内缩及单次安全区处理。
- 封面使用共同的高清请求策略与 `PlayerArtworkImage`，沿用静态图片保留和像素边缘修复。方形／圆形设置、暂停缩放、长按预览、封面横滑切歌和动态封面失败回退继续可用。平板采用已有方形动态封面资源；手机的竖版动态视频背景几何仍归手机布局。
- 背景复用 `FluidBackground` 的画面保留与切歌过渡。封面底色扩展沿用已有分析设置、缓存和 `CoverEdgeExtensionBackdrop`；黑白前景选择由手机和平板共同的 `playerForegroundForBackground` 决定。
- 歌词、队列、评论复用原组件。队列与评论在平板背景上使用对应文字对比度；音质、更多、收藏歌单、睡眠定时、分享和下载等弹层继续统一交给 `CommonOverlayHandler`。
- 面板转场继续使用现有 `TabletAnimationStyle` 设置，包括滑动、淡入淡出、缩放和翻转；系统动画时长缩放由 Compose 动画处理。

## 自定义播放操作栏

- 公共功能与保存规则由 `ui/model/PlayerAction.kt` 的 `PlayerAction`／`PlayerActionProfile` 描述。手机继续读取原 `playerBottomAction`，平板单独读取 `tabletPlayerBottomAction`，不根据单双栏临时改配置。
- 平板播放模式固定第一项，最多 5 项。手机保留旧排列（包括旧 more 入口和省略播放模式的设置）；新候选不提供重复的 more，平板候选也不提供歌词开关。未知 ID、重复项与超出上限的项会在读取时整理，无有效项时回到各自默认值。
- `PlayerActionSettingsSheet` 通过同一个偏好入口编辑当前外层对应的排列，支持添加、移除、长按图标拖动、前移／后移按钮及恢复默认。候选列表随选择更新，保存立即生效；不另建每种设备的设置页。
- `PlayerActionToolbar` 负责绑定公共播放状态，`PlayerActionRow` 只展示图标、选中状态、禁用状态和睡眠剩余时间。没有可分享链接时保留分享位置并禁用；无歌曲时禁用依赖歌曲的操作。
- `MoreAction.playerAction` 将菜单映射到同一组命令，`PlayerOverlayHandler.handlePlayerAction` 统一执行。歌词显示在手机仍由手机布局切换回调处理；平板使用公共面板意图。自定义设置入口只保留在 more 中。
- 菜单与 action 的下载都先发出同一种、绑定点击时歌曲的请求；`CommonOverlayHandler` 等已有 sheet 关闭后展示同一个 `DownloadConfirmDialog`。质量、保存路径及后续下载任务沿用现有逻辑，不再由 action 绕过确认直接下载。

## 状态与返回

`PlayerPanel` 是独立于布局的内容选择，公共入口通过 `rememberPlayerPanelState` 保存。公共操作处理器同时保存单栏详情意图；布局在同一入口内改变宽度时，不重建播放状态和数据任务。

默认右栏显示歌词，移除原来的“歌词／队列／评论”顶部切换按钮；默认底部 action 为播放模式、播放队列、评论。点击队列／评论切换右栏，再次点击当前 action 返回歌词；more 中的同名功能始终打开目标面板，不执行关闭切换。

三个面板按名称使用 `SaveableStateHolder`，在切换时保存各自支持保存的局部状态。队列首次定位标记也可保存，避免返回队列时总被当成第一次打开。歌词仍遵循当前播放行定位，评论仍遵循其 ViewModel 和 Paging 的现有生命周期。

返回顺序：普通弹层优先处理自身返回；双栏队列或评论返回歌词，单栏详情返回正在播放；随后返回收起播放器。主动点击顶部收起按钮直接折叠，播放继续。完整评论入口先收起播放器，再进入原评论目的地。

保存面板选择不代表所有播放中对象都可序列化恢复。Activity 重建后，服务连接、ViewModel、Compose 保存状态各自负责自己的生命周期；手机／平板外层之间切换不承诺所有局部滚动像素完全一致。不会保存视频播放器、协程、Painter 或大量歌词内容到状态 Bundle。

## 验证边界

定向 JVM／Robolectric 检查覆盖尺寸与大字体退回、窗口变化中的面板选择、返回顺序、评论详情不展示播放控制、面板选择的 Activity 重建恢复，以及既有 sheet 几何、转场和弹层导航。自定义操作的定向检查覆盖手机兼容与平板默认、固定首项、排序／去重／上限、候选列表更新、分享禁用后的位置稳定及恢复默认。共享组件编译覆盖手机调用。

不安装 APK，不做截图或设备视觉检查。真实播放、动态视频／GPU 背景、拖动与歌词同步的设备表现仍由真机验收。
