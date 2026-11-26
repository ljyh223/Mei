# MusicFree 插件音源

管理入口位于设置 → 音源与平台歌词 → MusicFree 插件。管理页面与 ViewModel 位于 `ui/screen/setting/MusicFreePluginsScreen.kt` 和 `MusicFreePluginsViewModel.kt`，与 LX 设置页同级；运行时、导入与存储保留在 `playback/source/musicfree/`。支持本地 UTF-8 JS 文件和 HTTP(S) 插件／订阅链接导入、启停、删除、初始化检查与插件声明的自定义变量。初始化成功后才保存到应用私有目录。相同文件重复导入保留启停状态和配置。

搜索统一使用标准搜索结果页，不再提供独立的 MusicFree 搜索页。「单曲」右侧的下拉菜单默认选择网易云，并列出已启用且支持歌曲搜索的 MusicFree 插件；菜单也可打开插件管理。选择插件后立即使用当前关键词搜索，复用手机、横屏和平板列表、加载提示、重试与分页。歌曲分区标题显示当前音源；其他分类仍使用网易云。各音源分别缓存结果、分页和滚动位置，切换分类或音源不会混入其他来源的数据；新关键词清空结果缓存。停用、删除或修改插件配置后相应缓存失效。

搜索结果点击后从该歌曲开始播放，并保留当前已加载结果作为队列。播放服务在队列命令中准备插件歌曲，新点播可以取代仍在准备的请求。切换搜索词、分类或音源也会使原结果的准备失效。「下一首播放」和「队尾播放」继续使用共享歌曲菜单，菜单打开前保存该曲目的原始对象。

## 订阅清单导入

「从链接导入」同时接受单个 JS 插件与 MusicFree JSON 订阅清单。清单使用 `{ "desc": "说明", "plugins": [{ "name": "插件名", "url": "JS 下载地址", "version": "版本" }] }` 结构；插件列表按下载地址去重，支持相对地址并以下载重定向后的清单地址为基准解析。代理地址保留原样，不自动绕过代理或改写下载域名。

读取清单后先展示名称和版本，可勾选所需插件，再逐个下载、初始化和保存。单次最多选择 20 个插件，清单最多 100 项；已有插件总数仍限制 20 个，相同脚本不会重复占位。某项失败继续处理其他项，显示逐项结果并可只重试失败项；取消会终止当前请求，已完成导入的插件保留。失败不覆盖现有插件或配置。沿用原有运行时的网络、执行和文件大小限制。

当前按需导入清单快照，不保存订阅地址，不后台自动更新。再次导入相同脚本保留配置；脚本内容变化按新插件处理，旧插件与其队列仍保留。

## 接口与兼容范围

按 MusicFree 官方 CommonJS 插件协议调用：

- 插件信息来自 module.exports 的 platform、version、author、supportedSearchType 和 userVariables。
- 搜索调用 search(query, page, "music")，读取 data / isEnd。
- 播放调用 getMediaSource(originalMusicItem, quality)，读取 url、headers、userAgent、quality。没有该方法时尝试歌曲自带的 url。
- env.os 为 android，env.appVersion 为 Mei 版本；env.getUserVariables() 返回当前插件自己的配置。
- 复用应用级 Coil、现有播放器、HTTP 传输与 QuickJS。支持常用 axios 请求子集，以及 crypto-js、cheerio、qs、he、dayjs、big-integer 和基础 Buffer。版本及许可证在 tools/musicfree-runtime 和 assets/musicfree 中。
- 插件请求使用独立、可取消的 HTTP 客户端，不注入应用账号凭据。音频请求头仅传给该条播放请求。播放请求缺少 `User-Agent` 时才使用应用默认值。每次运行有执行、内存、响应大小与总时限。
- 本次提供歌曲搜索和播放。尚未接入插件的专辑、歌单、榜单、歌词方法。axios 的拦截器、二进制/流式 HTTP、完整 Node API 等超出当前兼容范围的插件可能无法运行；未承诺兼容任意社区脚本。

音质映射为 standard → low、higher/exhigh → standard、lossless → high、其他高清档 → super。界面显示插件返回的音质，缺失时使用请求档位。脚本的声明不构成音频质量验证。

## 歌曲身份与状态

原始歌曲对象按插件 SHA-256 与原始歌曲 ID 生成稳定负数内部 ID，保留原字段用于后续解析，公开网易云 ID 设为 0。加入队列前将原字段写入私有文件，并将标题、封面等元数据写入现有歌曲表。无需变更 Room 表结构或数据库版本。队列恢复按内部 ID 找到原插件与歌曲，普通曲目继续从原目录接口加载；本地文件仍优先。

原始曲目缓存最多保存 2,048 条或 32 MiB，回收时优先移除最久未读且当前队列和恢复快照均未引用的文件。删除插件时清理未引用的原始曲目。较早且已回收的曲目可重新搜索后播放。

插件歌曲按自身插件解析，不受网易云备用音源模式控制。停用、删除或修改配置会清除短期地址缓存。解析失败沿用现有有限重试机制，原始响应和请求头不写入日志或文档。删除插件后，已排队的曲目仍显示已有资料，再次取地址时提示插件不可用。播放器对 MusicFree 歌曲停用网易云歌单、评论、下载和红心操作。

## 验证

定向测试使用合成插件和本机 HTTP 服务，覆盖 CommonJS、字段透传、加密与 HTML 解析、定时器、HTTP/取消、导入失败保护、配置保存、分页、播放地址/请求头及持久化。已运行 Kotlin 编译和相关队列、LX、导航测试。

按协作约定未安装或更新 APK、未做设备视觉验证，也未对第三方音乐服务发起真实请求。真实插件和上游可用性仍需用用户导入的脚本确认。

协议参考：

- https://musicfree.catcat.work/plugin/protocol.html
- https://github.com/maotoumao/MusicFreePlugins/blob/master/types/plugin.d.ts
- https://github.com/maotoumao/MusicFreePlugins/blob/master/types/mediaType.d.ts
