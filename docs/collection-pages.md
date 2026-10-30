# 完整集合页复用

歌曲、专辑、歌单与榜单使用独立导航目的地，页面框架和内容组件复用，不在发现页用枚举替换整页。

## 已接入

| 页面 | 路由 | 数据所有者 | 布局 |
| --- | --- | --- | --- |
| 分类／全部歌单 | `discover/playlists?category={category}` | `DiscoverPlaylistsViewModel` | 手机两列，平板自适应 |
| 发现新碟 | `discover/albums` | `DiscoverAlbumsViewModel` | 手机两列，平板自适应 |
| 全部榜单（保留实现，总览入口已暂停） | `discover/charts` | `DiscoverChartsViewModel` | 单列 |
| 歌手全部歌曲 | `artist_songs/{id}` | `ArtistCatalogViewModel` | 单列 Track，Paging |
| 歌手全部专辑 | `artist_albums/{id}` | `ArtistCatalogViewModel` | 自适应封面网格，Paging |

发现新碟仍使用已核对的最新专辑接口，展示该接口返回的完整集合，不代表网易云的全部专辑库。歌手歌曲、专辑继续使用原有数据接口与搜索流程。

## 页面框架

`app/src/main/java/com/ljyh/mei/ui/component/collection/MeiCollectionPage.kt` 负责大标题、渐进顶栏、返回按钮、页面 Haze source、系统与播放器安全区、网格布局和加载／失败／空内容状态。

- `itemCount`、`itemKey`、`itemContent(index)` 提供惰性内容。Paging 可在 itemContent 内访问索引，不能先把全部项目读取成列表。
- `columns = GridCells.Fixed(1)` 支持歌曲或榜单行；多列支持封面网格。歌曲行可设置 horizontalContentPadding=0.dp、verticalItemSpacing=0.dp，保留 Track 自身布局。
- `controls` 承接分类、排序等操作；`actions` 与 `titleContent` 承接顶栏搜索等操作。搜索时关闭大标题并保持顶栏可见。
- `footer` 承接各数据源的分页状态。普通 offset 分页和 Paging 各自维护协议，通过 `MeiCollectionLoadMore` 统一加载与失败重试展示。
- 搜索、普通列表分别保存滚动状态。空搜索结果可以关闭空状态重试按钮，不强迫所有页面使用同一种恢复动作。
- 卡片网格按集合自身实际宽度设置默认间距：600dp 起横向 24dp／纵向 28dp，窄布局保持横向 12dp／纵向 16dp。发现全部歌单、新碟、歌手全部专辑及音乐库集合共同使用；标题、筛选和页尾继续跨列显示。调用方可通过 `horizontalItemSpacing`／`verticalItemSpacing` 覆盖，单列行列表保留原间距，歌曲显式设置的 0dp 不受影响。
- 框架不获取数据、不持有业务 ViewModel，不给每张内容卡添加 Surface。

## 导航与状态

`PageDestination.kt` 的 `pageDestination` 注册真实返回栈目的地，并为移动中的页面提供独立底色；转场继承主 NavHost 的统一策略。`discoverPlaylistsRoute` 对分类查询值进行编码，导航只解码一次；R&B/Soul 等含保留字符的名称也可传递。

每个 NavBackStackEntry 分别拥有 Hilt ViewModel 与可保存滚动状态。发现总览只保留预览数据，独立列表的加载、筛选和错误不会替换总览。分类与排序保存在 SavedStateHandle；仅实际筛选变化时重置列表位置，详情返回不重新滚动到顶部。

新目的地属于二级页面，沿用 MainShellState 隐藏主 tabbar，并继续保留播放器安全区。不要加入 Screen.MainScreens。

## 公共内容组件

- `ui/component/card/ArtworkCard.kt`：共享封面、标题、副标题与点击布局，首页歌单、发现歌单／新碟、歌手专辑和本地专辑共同复用。普通图片按 ImageUrls 工具处理，只有封面有圆角；标题与副标题不进入封面裁切范围。`artworkContent` 允许专用封面构图替换图片区，继续复用卡片文字及交互。
- `ui/component/card/PlaylistCard.kt`：保留歌单调用接口，只映射数据并补充封面上的播放量、说明与播放图标；不再重复实现卡片布局。
- 现有歌手与本地专辑适配函数只传入数据、尺寸和样式；专辑卡片统一映射到 `VinylAlbumCard`，歌单保留原卡片，歌手条目使用圆形头像。
- `ui/component/card/VinylAlbumCard.kt`：发现、音乐库、歌手作品和本地专辑使用的统一专辑卡片，复用 `ArtworkCard` 的文字与交互。`ui/component/artwork/VinylAlbumArtwork.kt` 为卡片、搜索紧凑行和骨架共用的图片区：正方形封套在前，唱片向侧边露出约封面宽度的 20%，中心圆标完全遮住，构图宽高比为 1.20。横排及紧凑行按原封面大小额外留出唱片宽度，平板网格减少必要的列数来保留封套大小；手机双列网格随单元宽度等比缩放。小尺寸减少唱片纹路，圆角与阴影随尺寸收敛。只发起一个 Coil 封面请求，不为唱片增加图片请求。
- 发现总览的歌单和新碟横排卡片间距为手机 20dp／平板 24dp；分类标签仍用原紧凑间距，横排首尾继续使用公共页面边距。
- `DiscoverCategorySheet`：总览和完整歌单使用同一分类面板，选择回调由页面决定导航或更新当前筛选。

## 验证

定向检查覆盖真实 NavHostController 的路由入栈／出栈、返回同一列表 entry、分类保留字符编码、二级页面主导航状态、歌单筛选恢复与页面隔离、旧请求竞争、分页去重与重试、接口解析，以及既有歌手目录响应。

不安装或更新 APK，不做设备视觉检查；界面观感由用户验收。

## 首页分组布局

首页每个推荐分组使用 Column 承载其输出。歌曲分组的 Title 与 TripleLaneSlider 是顺序内容，不能直接放入 Box，否则二者共享起点并发生重叠。
