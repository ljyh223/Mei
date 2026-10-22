# 发现页接口调查与适配方案

调查日期：2026-10-22。当前 UI 分支：`test/meilox-mini-player`。

本轮已调整首页标题；发现页尚未改动。以下基于本地 API Enhanced 的模块源码和文档，以及 MeiloX、NeriPlayer 的实现进行静态核对，没有进行真实上游请求，也未验证游客／登录态可用性或实际响应字段。

## 当前发现页的限制

- `FindMusicViewModel` 把普通歌单分类写成固定长串，却只请求精品歌单列表。普通分类与精品标签不能视为相同集合。
- 当前每分类只加载 30 条。DTO 已保留 `more`、`lasttime`，请求 DTO 也有 `lastTime`，但 Repository 没有暴露分页游标。
- 切换分类的请求没有取消和版本校验；旧结果可能覆盖新分类。缓存也没有区分排序、游标等条件。
- 页面只有分类横条与两列歌单，缺少按曲风、榜单、新发行内容探索的路径。

## 候选接口

下表“接口路由”是 API Enhanced 的服务路由；“上游路径／加密”来自对应 `module/*.js`，不可把服务路由直接作为 Android 上游路径。

| 用途 | 接口路由 | 上游路径／加密 | 筛选与分页 | 建议 |
| --- | --- | --- | --- | --- |
| 全部歌单分类 | `/playlist/catlist` | `/api/playlist/catalogue`，eapi | 无分页 | 用于完整分类面板，核对响应中的分组、名称和分类关联 |
| 热门歌单分类 | `/playlist/hot` | `/api/playlist/hottags`，weapi | 无分页 | 常用分类候选；仍需与实际列表模式匹配 |
| 普通分类歌单 | `/top/playlist` | `/api/playlist/list`，weapi | `cat`、`order=hot/new`、`limit`、`offset` | 按语种、场景、情绪等主动浏览的主数据源 |
| 精品支持的标签 | `/playlist/highquality/tags` | `/api/playlist/highquality/tags`，weapi | 无分页 | 精品模式单独使用，不能直接套全部分类 |
| 精品歌单 | `/top/playlist/highquality` | `/api/playlist/highquality/list`，weapi | `cat`、`limit`；服务 `before` 映射上游 `lasttime` | 保留为精品浏览；按真实响应游标续页，不使用 offset |
| 榜单摘要 | `/toplist/detail` | `/api/toplist/detail`，weapi | 无分页 | 展示真实榜单；具体摘要字段、榜单 ID 与歌曲预览需先核对响应 |
| 榜单摘要 v2 | `/toplist/detail/v2` | `/api/toplist/detail/v2`，weapi | 无分页 | 与旧版分别验证，不预设响应结构相同 |
| 新歌速递 | `/top/song` | `/api/v1/discovery/new/songs`，weapi | `type` 映射 `areaId`：0/7/96/8/16 | 适合紧凑歌曲列表；模块没有 limit/offset 分页参数 |
| 最新专辑 | `/album/newest` | `/api/discovery/newAlbum`，weapi | 模块无筛选、分页参数 | 适合少量新碟预览，点入现有专辑详情 |
| 全部新碟 | `/album/new` | `/api/album/new`，weapi | `area`、`limit`、`offset` | 地区筛选与续页；文档注明需登录，游客策略需验证 |
| 新碟上架 | `/top/album` | `/api/discovery/new/albums/area`，weapi | `area`、`type`、`year`、`month`、`limit`、`offset` | 可按发行月份浏览；不与最新专辑 DTO 混用 |
| 曲风目录 | `/style/list` | `/api/tag/list/get`，weapi | 无分页 | 曲风 ID 来源；先核对目录层级，避免硬编码 tagId |
| 曲风介绍 | `/style/detail` | `/api/style-tag/home/head`，weapi | `tagId` | 曲风详情的简介与头部候选，字段待验证 |
| 曲风歌单 | `/style/playlist` | `/api/style-tag/home/playlist`，weapi | `tagId`、`size`、`cursor`；模块固定 sort=0 | 用于更细的曲风浏览；cursor 按响应原样传递 |
| 曲风歌曲 | `/style/song` | `/api/style-tag/home/song`，weapi | `tagId`、`size`、`cursor`、`sort=0/1` | 曲风页的歌曲内容；与歌单接口独立映射 |

`/personalized`、`/personalized/newsong` 更偏个性化推荐，本轮不作为发现页主体，以免复制首页职责。

## 参考子项目结论

- MeiloX 的发现页仍基于精品歌单，加入大标题、主推封面和网格；未发现其发现页已整合上述曲风、新碟、榜单数据。它的请求任务取消与 generation 校验可以参考，但不复制整页布局。
- NeriPlayer 的客户端分别封装了精品标签与精品歌单接口，但当前发现界面的标签仍来自固定列表和 TAG_TO_API_CATEGORY 映射，不能据此认定已动态获取全部分类。它同时包含其他来源的搜索，超出本轮发现页范围。
- 接口能力主要来自 `/home/ljyh/tools/o/api-enhanced/module/`。Android 应继续通过本项目的 Service、Repository、domain model 消费，不引入 Node 服务作为运行时依赖。

## 建议页面结构

发现页的任务是主动寻找音乐。与首页共用「大标题 → 紧凑顶栏」过渡、边距及中性底色；封面正常呈现，玻璃留在悬浮控件。

1. 「发现」大标题，下方提供少量常用分类与「全部分类」入口。完整分类按响应支持的分组组织，不能凭空生成不受支持的标签。
2. 主体分为有真实数据支持的少量区块：曲风／场景浏览、新发行内容、榜单。每区只展示适量内容，完整浏览进入相应列表。避免把所有接口同时塞入首屏。
3. 分类歌单页使用稳定两列布局，提供热门／最新排序；精品模式独立使用精品标签和游标。
4. 接口分区独立加载与重试，不因某区失败而整页空白。已有内容刷新时保留；没有数据的可选分区不画空容器。
5. 不增加与底部重复的搜索框，不新增与现有功能重复的大卡入口。

## 实施顺序与验收

1. 先逐条以最小请求核对分类、普通歌单、榜单、新碟的真实响应、认证要求和加密路径；曲风目录作为下一组验证。当前源码仅能证明接口定义存在。
2. DTO 使用 kotlinx.serialization，只映射页面需要的字段。新增 Service 明确 cryptoMode 或使用已核对的 weapi 路径；不能仅因路径以 `/api/` 开头便统一按 eapi 处理。
3. 分页按各自协议实现：offset、lasttime、cursor 分开。分类／排序变化取消旧任务并校验请求版本；缓存 key 包含模式、分类与排序。
4. 加载与导航遵循现有播放器、安全区和底部搜索布局。图片逐张显示，沿用 Coil 和 ImageUrls，不等待全部图片请求完成。
5. 定向测试响应解析、分页终止／去重、分类请求竞争与局部失败；UI 修改编译检查。APK 安装和设备视觉验证仍由用户另行提出。

## 本地源码索引

- API 模块：`/home/ljyh/tools/o/api-enhanced/module/`，文件名为接口路由中的斜杠替换成下划线，如 `playlist_catlist.js`、`top_playlist.js`、`style_playlist.js`。
- API 文档：`/home/ljyh/tools/o/api-enhanced/public/docs/home.md`。
- 当前发现页：`app/src/main/java/com/ljyh/mei/ui/screen/main/findmusic/`。
- 当前精品 Service：`app/src/main/java/com/ljyh/mei/data/network/api/WeApiService.kt`。
- MeiloX：`MeiloX/app/src/main/java/com/ljyh/mei/ui/screen/main/findmusic/`。
- NeriPlayer：`NeriPlayer/modules/platform/src/main/java/moe/ouom/neriplayer/platform/netease/api/client/NeteaseClient.kt` 与 `NeriPlayer/app/src/main/java/moe/ouom/neriplayer/ui/screen/tab/explore/ExploreBrowseContent.kt`。
