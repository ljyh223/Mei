# 代码目录约定

`app/src/main/java/com/ljyh/mei` 中的包按职责组织：

| 包 | 放置内容 |
| --- | --- |
| `data/model/domain` | 界面与业务逻辑使用的模型，以及从接口响应到模型的映射 |
| `data/model/response` | 网易接口的原始响应数据类 |
| `data/model/api`、`weapi`、`eapi`、`auth` | 现有接口协议、请求参数及鉴权数据类 |
| `data/model/room` | Room 实体及关联对象，保持数据库映射稳定 |
| `data/network`、`data/repository` | 网络访问和数据仓库；封面匹配算法在 `repository/artwork` |
| `download` | 下载调度、文件命名、元数据、歌词文件和动态封面导出 |
| `playback` | 播放服务、连接、音量与定时器等核心入口 |
| `playback/queue`、`source`、`transition`、`lyrics`、`equalizer` | 播放队列、音源缓存、智能过渡、桌面歌词和音效 |
| `utils/image`、`power`、`preferences`、`system` 等 | 单一领域的可复用工具；通用日期、字符串、单位工具保留在 `utils` 顶层 |

新增代码优先放进所属领域包。接口响应数据类不应与应用内部模型混放；下载逻辑不应放进播放服务包。`playback.DownloadWorker` 仅用于兼容旧版本已排队的 WorkManager 任务，新任务使用 `download.DownloadWorker`。
