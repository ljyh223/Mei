# 顶栏与页面基准线

本规则适用于 Mei 的导航页面。保持现有封面、文字层级和页面功能，只统一悬浮操作材质与外层内容边界。

## 顶栏图标

- `MeiGlassIconButton` 与 `MeiGlassBackButton` 共用 48dp 点击区域、40dp 圆形底座和 22dp 实色图标；使用 `ToolbarControl` 材质。返回、搜索、关闭、扫描、清理、分享等同级悬浮操作使用同一组件。
- `MeiTopAppBar` 与 `MeiHazeTopBar` 将当前页面 Haze source 提供给操作区域；按钮光学层与图标分开绘制，不把图标放进采样层，也不在返回按钮外重复套玻璃。
- 关闭玻璃时回退为同尺寸的普通中性按钮；禁用操作同步禁止点击并降低不透明度。保留原操作、长按返回与无障碍描述。
- 搜索框内部按钮、正文操作、卡片内部按钮以及评论排序文字按钮保留所在容器的样式。播放器属于独立沉浸布局。封面预览使用自己的暗色背景与局部采样，关闭和保存按钮共用玻璃控件。

## 页面边距

- `MeiPageLayout` 提供手机 20dp、平板 28dp 的稳定 gutter。`meiPageGutter()` 按设备尺寸返回同一数值，正文、顶栏按钮外边缘、展开的底栏和 mini player 共用它。
- `pageDestination` 在页面根部统一处理并消费横向 safeDrawing 安全区。页面列表不再把相同横向系统 inset 加到 contentPadding；现有使用 windowInsetsPadding 的子组件会尊重已消费的 inset。
- 大标题、分区标题、集合网格、列表封面和页面级卡片边缘对齐 gutter。歌单与本地目录的既有详情头部保留封面、标题、说明和操作组的居中布局，不能为了统一页面边距强行左移。封面右侧文字沿自身内容列对齐；卡片内部 padding、封面与文字间距、播放器布局和居中的详情封面保留。
- 横向歌单、专辑、分类与筛选使用列表内部 contentPadding 对齐首尾，视口可延伸到边缘。滚动内容不跟随底栏收缩移动。
- `MeiPinnedLazyPage` 的标题 padding 只补足内容已有缩进，避免二次相加。设置分组自己负责页面 gutter，玻璃设置页不在外层再叠加一次。歌曲行和表头共用页面 gutter，骨架复用同一布局。
- mini player 外边距与 Apple 风格播放器封面动画共同使用 `miniPlayerHorizontalMargin`，防止平板边距和紧凑形态的封面起点偏移。

## 覆盖与验证

已核对：首页与加载状态、发现与独立集合页、歌单／专辑／每日推荐、歌手及全部歌曲／专辑、本地音乐与目录、搜索结果和建议、音乐库、设置总页和子页、下载管理、历史、评论、关于、日志，以及封面预览。

本轮仅修改 UI 与协作规范，不修改播放、网络、数据库或导入协议。编译与相关既有测试用于验证集成；按项目约定不安装 APK、不进行设备视觉检查，实际材质与边距观感仍由设备验收。

## 关于页的采样范围

关于页使用 `MeiPinnedLazyPage`，沿用大标题、渐进顶栏和统一返回按钮。播放器安全区进入 LazyColumn 的 contentPadding，不能在列表外使用 windowInsetsPadding 或 padding 缩短视口；滚动时内容应可以经过底部悬浮控件背后，同时末项仍可滚动到控件上方。

主要依赖按当前 Gradle 声明和 UI 使用列出 Haze / Haze Glass、Coil、MaterialKolor、Reorderable、Zoomable，并链接各自上游仓库。MeiloX 属于交互参考，单独列在开源致谢中。
