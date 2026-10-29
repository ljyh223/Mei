# 歌单页面架构与自适应布局

本文描述当前实现。应用外层仍有独立的 `PhoneApp` / `TabletApp`；歌单功能内部按页面、布局和组件组织，两种外层进入同一个歌单页面。歌单的单双栏选择不再依赖 `isTablet` 或横竖屏。

## 1. 从路由到页面

```text
MeiApp → PhoneApp / TabletApp
                ↓
MeiAppPages → NavigationBuilder：playlist/{id}
                ↓
MeiPageContent.Playlist(id) → PlaylistScreen(id)
                ↓
PlaylistContent(state, actions)
  ├── 头部、背景、纯菜单、语义列表锚点
  └── 测量可用宽度与字体大小
        ├── SinglePanePlaylistLayout
        └── TwoPanePlaylistLayout
```

路由注册和 ID 解析由 `NavigationBuilder` 负责。`PlaylistScreen` 是页面状态与操作的接入入口，获取当前导航 entry 的 `PlaylistViewModel`，并绑定播放服务、下载、菜单等操作。

`PlaylistContent` 是展示边界：只接收展示数据、操作回调及可选头部，不接收 ViewModel，也不读取导航控制器或播放连接。它提供共享 Scaffold，并在自身的 `BoxWithConstraints` 中根据实际宽度选择布局。

手机和平板的歌单不再分别经过转发入口。`TabletPageContent` 继续委托共享页面集合，留给以后确实需要不同页面结构的目的地使用。

## 2. 实际文件结构

以下路径相对于 `app/src/main/java/com/ljyh/mei/`：

```text
ui/screen/playlist/
├── PlaylistScreen.kt                 页面入口、服务绑定、下载与收藏等操作
├── PlaylistViewModel.kt              数据请求、搜索状态、分页缓存及任务生命周期
├── PlaylistUiState.kt                展示数据、操作回调、布局输入
├── PlaylistMenuActions.kt            菜单操作契约与入口侧服务绑定
├── PlaylistOrderEditor.kt            可恢复排序草稿、准备与保存任务
├── PlaylistDownloadController.kt     下载意图恢复、准备、确认与提交任务
├── PlaylistOrderItem.kt              排序列表模型
├── PlaylistScrollState.kt            可保存歌曲锚点、布局切换与分页恢复
├── PlaylistContent.kt                无 ViewModel 依赖的自适应页面内容
├── layout/
│   ├── PlaylistLayout.kt             双栏最小宽度与字体适配策略
│   ├── SinglePanePlaylistLayout.kt    头部和歌曲共用一列滚动
│   └── TwoPanePlaylistLayout.kt       信息区与歌曲区独立滚动
└── component/
    ├── PlaylistTracks.kt             共用分页、重排与菜单行为绑定
    ├── PlaylistTrackList.kt           实际歌曲列表和表格外观
    ├── PlaylistActionMenu.kt          只接收数据与操作的菜单
    ├── PlaylistDetailHeader.kt        喜欢、雷达、排行等头部选择
    ├── PlaylistHeader.kt             普通头部
    ├── PlaylistHeaderStyleCache.kt    头部样式的临时提示缓存
    ├── PlaylistBackground.kt
    ├── ImageCollageContent.kt
    └── TiltedCroppedImageCollage.kt

ui/screen/daily/
├── EveryDayScreen.kt                 每日推荐状态与操作入口
├── DailyRecommendationHeader.kt      每日推荐专属头部
└── DailyCoverWall.kt                 每日推荐封面墙

data/paging/
└── PlaylistTrackPagingSource.kt      歌曲分页数据源
```

功能目录中不再混用 `phone/`、`detail/`、`state/` 作为分类。根目录放页面接入及有明确职责的状态持有者；`layout/` 放排列方式；`component/` 放页面使用的具体 UI。每日推荐在自己的功能目录，通过自定义头部和状态回调复用 `PlaylistContent`。

## 3. 命名的对应关系

| 原名称或分组 | 当前名称或位置 | 原因 |
| --- | --- | --- |
| `PlaylistRoute` | `PlaylistScreen` | 它接入状态与操作，不负责注册路由 |
| `PhonePlaylistContent` | `layout/SinglePanePlaylistLayout` | 单栏可以用于任意设备或窗口 |
| `TabletPlaylistContent` | `layout/TwoPanePlaylistLayout` | 双栏表达布局能力，不表达硬件类型 |
| 共享 `PlaylistDetailScaffold` | `PlaylistContent` | 它是完整的共享展示边界，包含 Scaffold 和内容布局选择 |
| `PlaylistDetailContract` | `PlaylistUiState` | 提供实际页面数据和操作，不另立一个空泛的“契约层” |
| `state/PlaylistTrackSource` | `data/paging/PlaylistTrackPagingSource` | 明确属于分页数据源 |
| `state/PlaylistOrderItem` | `PlaylistOrderItem` | 排序模型与实际编辑器在同一功能中 |
| `state/PlaylistHeaderStyleCache` | `component/PlaylistHeaderStyleCache` | 属于头部展示的提示缓存 |
| `DailySongListScreen` | 已移除 | 每日推荐直接装配共享展示内容，减少一次参数转发 |

## 4. 展示边界与菜单

`PlaylistUiState` 包含歌单详情、请求中的稳定歌单 ID、完整歌曲 ID 顺序、分页展示数据、加载状态、搜索状态、排序展示状态，以及菜单所需的用户歌单列表和当前弹层状态。

`PlaylistActions` 包含主要页面操作、弹层切换回调和 `PlaylistMenuActions`。菜单的添加到歌单、删除、下一首、队列和复制等操作都通过回调执行。

```text
PlaylistScreen
  ├── 收集 ViewModel 数据
  ├── rememberPlaylistMenuActions：绑定 ViewModel、播放连接和系统操作
  └── PlaylistContent
        ├── PlaylistActionMenu：只读菜单数据，触发回调
        └── 两种 Layout → PlaylistTracks → PlaylistTrackList
```

为已有专辑页面保留的 `PlaylistActionOverlay` 绑定函数在入口侧的 `PlaylistMenuActions.kt` 中。纯菜单文件不再导入 ViewModel。专辑现有行为继续沿用；歌单的展示容器没有经过这个旧适配入口。

这些参数属于 Compose 展示层，允许包含 `LazyPagingItems`、`ImageVector` 等 UI 类型。布局与菜单可以使用测试数据及空操作实例化，不需要构造业务 ViewModel。它们仍消费主题、字体、Haze 和内容 inset 等展示环境。

## 5. 按可用宽度选择布局

`PlaylistContent` 测量页面实际可用宽度，并读取字体缩放。策略在 `layout/PlaylistLayout.kt`，当前普通字体下的双栏起点为 840dp：

- 信息内容至少 280dp，加现有左右内边距共 56dp。
- 歌曲区至少 480dp。
- 两栏之间 24dp。

信息区宽度随空间增长并设上限，其余空间给歌曲区。较大字体按比例提高两栏最小宽度，因此相同窗口可能回到单栏。

方向、小窗、分屏或设备名称不直接参与歌单分栏。宽度不足就使用单栏；足够宽的竖向窗口也可以使用双栏。两种布局复用列表和头部组件，并消费底部播放器所需的留白。

应用外层在 `MeiApp` 中测量扣除左右系统安全区后的实际宽度，由 `AppWindowLayout` 以 600dp 为底栏布局断点选择 `PhoneApp` / `TabletApp`。公共页面边距跟随此结果；歌单的单双栏仍由页面自己的宽度和字体策略决定，两个断点各自负责不同层次，不互相替代。其他尚未改造页面中的旧设备判断仍保留。

## 6. 搜索与排序草稿的恢复

搜索关键词与搜索展开状态由 `PlaylistViewModel` 的 `SavedStateHandle` 保存。页面重建后消费恢复后的 StateFlow。

`PlaylistOrderEditor` 由 ViewModel 持有，准备与保存任务运行在 ViewModel 的协程作用域中。布局切换和 Activity 的配置重建不会因页面组合销毁而取消这些任务。

排序草稿只把歌曲 ID 顺序写入 `SavedStateHandle`。重建编辑器时，重新取得元数据和服务器顺序，再恢复草稿：

- 歌曲集合一致时恢复本地草稿，不自动提交到服务器。
- 歌曲集合已变化时丢弃不兼容草稿，并给出提示。
- 服务器已经与草稿顺序一致时清除草稿，不重复写入。
- 保存中禁止重复提交和继续移动；失败恢复已确认顺序。
- 保存中的布尔标记没有单独持久化。进程重建后重新核对数据，不恢复一个没有对应任务的“正在保存”状态。

`PlaylistViewModel` 也复用已有详情和分页流，避免 Activity 重建时仅因页面重新组合就清空已加载的分页数据。

## 7. 用歌曲锚点恢复位置

`PlaylistScrollState` 保存歌曲 ID、歌曲序号和像素偏移，并单独表示“正在查看头部”。原始 `LazyListState` 的布局索引不作为持久恢复依据。

- 同一首歌在单栏中有头部索引偏移，在双栏中没有，恢复时分别映射。
- 数据加载中先等待；分页尚未到达目标歌曲时继续请求后续页。
- 歌曲已被删除时，回退到相邻序号的剩余歌曲并清除旧偏移；若需要该歌曲所在页，也先加载。
- 空列表回退到顶部；占位、空状态和错误行不会覆盖已保存的歌曲锚点。
- 头部切到双栏时歌曲区回到顶部；没有手动滚动歌曲区时保留原头部锚点，切回单栏仍可恢复头部位置。
- 恢复期间用户开始滚动时，优先尊重用户的新操作。

锚点通过 `rememberSaveable` 的自定义 Saver 保存，按歌单和搜索条件区分。搜索条件变化时从该查询的默认位置开始。

分页错误时不会假装目标位置已经恢复；可以重试分页，或通过手动滚动采用新的阅读位置。当前分页数据源仍为顺序加载，恢复较远的目标可能需要加载它之前的页。

## 8. 下载确认与任务恢复

`PlaylistDownloadController` 由 `PlaylistViewModel` 持有。准备元数据和提交任务运行在 `viewModelScope`，页面组合销毁不会取消任务；Activity 配置重建继续使用原任务。

`SavedStateHandle` 只保存歌单 ID、标题、所选歌曲 ID、阶段和提交结果不确定标记，不保存歌曲元数据或临时下载链接：

- 准备或待确认期间重建 ViewModel，按保存的 ID 重新准备元数据，然后显示确认框。
- 缺少部分歌曲元数据时不会悄悄缩小本次选择；准备失败结束忙碌状态，允许重新操作。
- 只有点击确认才提交下载，重复确认不会启动第二个提交任务；提交中也不能切换为另一组选曲。
- 提交中进程中断，无法仅凭页面保存状态判断队列是否已经落盘。恢复后显示提示，要求用户自行决定是否重试，不自动重复入队。
- 等待 WorkManager 确认入队后才清除已保存意图。此确认不代表歌曲文件已经下载完成。
- 主动关闭确认框会清除意图；不持久恢复普通菜单的展开状态。

确认框中的歌词选项沿用自身的可保存状态，音质和目录沿用现有偏好设置。本轮没有更改下载音质、目录、WorkManager 同名任务替换策略或超过 500 首的限制。

## 9. 测试与尚未覆盖的范围

新增定向测试覆盖：窗口宽度与大字体分栏、歌曲锚点保存与单双栏索引转换、头部/空列表/加载/删除/重排/筛选回退、草稿恢复、重复准备、保存中重复操作、失败回滚、远端集合变化、服务器已确认草稿，以及过期请求隔离。

同时覆盖下载准备与提交恢复、重复提交、过期请求隔离、失败重试，以及应用外层可用宽度的断点。`MeiShellStateRestorationTest` 使用 Robolectric 和 Compose 测试调度，检查跨外层布局的组合保留、Activity 重建的可保存字段、直接进入另一种布局时的恢复，以及播放器展开锚点；其中生产 `MeiAppPageHost` 用例实际运行导航图，页面内容用测试替身替换，检查详情参数、保存字段和返回栈。

同时保留生产路由图、集合返回栈、头部缓存和排序协议的现有测试。上述检查为编译与 JVM 状态验证，不包含设备视觉、真实网络下载、系统杀进程后的完整设备操作，或所有页面局部状态的恢复保证。

远处歌曲锚点的顺序分页恢复仍可能较慢；其他页面中的旧设备判断，以及全屏播放器内部的临时面板状态，仍需逐页整理。

## 10. 修改入口

| 修改目标 | 对应文件 |
| --- | --- |
| 页面数据和用户操作接入 | `PlaylistScreen.kt`、`PlaylistViewModel.kt` |
| 排序草稿、准备及保存任务 | `PlaylistOrderEditor.kt` |
| 下载准备、确认及提交恢复 | `PlaylistDownloadController.kt` |
| 展示参数及菜单操作边界 | `PlaylistUiState.kt`、`PlaylistMenuActions.kt` |
| 顶栏、背景、自适应内容组合 | `PlaylistContent.kt` |
| 分栏断点及最小宽度 | `layout/PlaylistLayout.kt` |
| 单栏或双栏排列 | `layout/SinglePanePlaylistLayout.kt`、`layout/TwoPanePlaylistLayout.kt` |
| 阅读位置恢复与回退 | `PlaylistScrollState.kt` |
| 每日推荐的专属内容 | `ui/screen/daily/` |
| 应用底栏、输入法避让和全屏播放器层 | `ui/app/PhoneApp.kt`、`ui/tablet/TabletApp.kt` |
