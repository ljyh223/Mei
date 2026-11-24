# 听歌档案与个人中心

音乐库右上角显示当前账号头像，缺图或未登录显示人形占位。入口复用顶栏按钮，图片使用应用级 Coil 及自动尺寸策略。

## 展示与数据归属

手机按资料、音乐足迹、常听排行、最近收听排列；账号与会员默认收起。平板由 `TabletPageContent` 注入双栏：左侧资料和账号操作，右侧听歌档案，各自滚动。普通字体下从 600dp 起分栏；左侧至少 224dp、右侧至少 320dp，空间不足或大字体时回退单栏。

个人中心沿用 `library/profile` 路由。`LibraryViewModel` 归属音乐库父 entry，继续提供资料、会员及已加载的周/月报告。`ListeningArchiveViewModel` 同样归属父 entry，仅在档案页面需要时加载排行与云端历史。切换周期、返回重进和主 tab 切换复用内存状态；进程重启后重新请求，不声称这些新增数据有磁盘缓存。

账号或 Cookie 变化会取消旧任务并清空档案缓存；迟到响应不能写入新账号状态。主动刷新保留已有内容，失败显示提示与重试；失败不会当成空列表，也不会在每次重进时自动重试。

## 网易云播放历史

个人中心「最近收听」保留最多 6 张预览，点击「查看全部」进入 `library/profile/history`。音乐库不增加历史入口，本地 `history` 页面保持独立。新页使用公共集合页顶栏与安全区，手机横屏沿用常驻侧栏的返回规则；宽度和字体允许时增加专辑、时长列。

历史页与档案通过 `ListeningArchiveBinding` 复用音乐库父 entry 的 `ListeningArchiveViewModel`。打开、返回及旋转不额外请求已加载的数据，恢复到历史页时也会重新绑定账号。手动刷新仅更新最近收听，不重拉排行；失败保留原列表并提示，账号切换仍清空旧缓存。

列表沿服务端历史的时间顺序显示歌曲与收听时间，点击从对应歌曲开始播放，队列携带网易云历史来源和歌曲元数据，不把收听时间当作播放进度。当前接口上限为最近 100 条，不提供虚假的分页或完整历史承诺。

## 已核对并接入的接口

| 内容 | 接口与参数 | 协议及响应 |
| --- | --- | --- |
| 周/月收听时长 | `/eapi/content/activity/listen/data/realtime/report`，`type=week/month` | 复用项目现有 EAPI；`data.listenTimeDistributionBlock` |
| 周偏好 | `/eapi/content/activity/listen/data/report`，`type=week` | 复用项目现有 EAPI；`topStyleBlock/topArtistBlock` |
| 歌曲排行 | `/weapi/v1/play/record`，`uid`、`type=1` 一周或 `0` 累计 | WEAPI；`weekData/allData`，保持服务端次序 |
| 最近收听 | `/weapi/play-record/song/list`，`limit=100` | WEAPI；`data.list[].data` 为歌曲，`playTime` 为时间 |

排行与历史分别核对了本地 MeiloX 的 `MeloXRepository.userPlayRecords/recentSongs`、解析器以及外部 api-enhanced 的 `user_record.js/record_recent_song.js`。这里使用已有 `WeApiService` 与拦截器，不仅凭 `/api` 前缀推断加密模式。曲目同时兼容 `ar/al/dt` 与 `artists/album/duration`。

歌曲行与最近收听可直接播放，队列沿展示来源生成，点击位置与歌曲对应。重复云端历史保留较新的时间，秒时间戳归一为毫秒。排行中的 `score` 不冒充播放次数；仅实际返回 `playCount` 时展示次数。常听歌手根据返回的歌曲排行中出现的歌曲数整理，并标明依据，不能解释为官方歌手播放量排行。

## 暂未接入的候选

api-enhanced 另有 `/api/content/activity/listen/data/total`（总时长）、`song/play/rank`（周/月 Top20）、年度报告。它们与当前排行接口不是同一响应结构，没有以猜测字段接入。需要扩展时逐个核对返回结构、单位、账号条件和协议。

## 验证边界

编译与定向测试覆盖路由参数、响应解析、缓存与账号隔离、刷新保留和分栏尺寸；不以 MockEngine 测试代替真实接口成功。按协作约定未安装 APK、未运行设备视觉测试，也未为本次改动发起真实账号接口请求。
