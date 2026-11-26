# 网易云播放日志

## 入口与所有权

MusicService 创建 PlaybackLogController，监听逻辑播放器的事件；手机、平板、通知栏等控制入口共用这一实例。PlaybackLogSession 是纯计时状态机，页面不负责记录。

只有已登录、正整数网易云歌曲 ID、实际选中官方音源时才记录。QueueSource.LOCAL、MusicFree 和其他备用音源不向网易云上报。官方 full-v1 完整磁盘缓存可继续记录；若该 ID 实际关联本地文件则不记录。开始时保存队列 occurrence、歌曲信息与来源快照，歌单／专辑／艺人使用真实来源 ID，其他尚未核对的来源回退为 track，不编造来源编码。

## 播放会话

- 第一次 isPlaying=true 时写开始事件，预加载和首次缓冲不产生播放记录。
- 单调时钟累计可听时间 realtime；time 另外按各时间段的播放速度折算。暂停、缓冲和播放抑制不推进计时，seek 不使用进度差累加。
- 暂停后恢复沿用会话。切歌、单曲循环和队列中的重复歌曲分别结束旧会话并开始新会话。
- 自然结束／循环为 playend，主动切歌为 ui，播放错误为 exception，移除／停止／销毁为 interrupt。
- 服务销毁前结束会话，最终写入由应用级 IO scope 执行，不随服务 scope 取消。
- _sessid 属于应用进程会话，不是每首歌生成一个 UUID。UA 构建时间 logtime 和序列化时间 _logTime 同时保留，单位毫秒；startlogtime 和记录行前缀为秒。
- 现有 PauseListenHistoryKey=true 时停用远端记录并清理待发数据。

智能过渡在双 deck 交接前明确以 playend 结束旧会话，按逻辑播放器的当前曲目归属时间，不把双解码器重叠期间同时算给两首歌。系统直接杀进程而未调用 onDestroy 时，当前尚未结束的会话可能没有结束事件；已落盘的完整记录仍可恢复，本版不通过伪造周期性结束事件填补时长。

## 协议

本版依据官方静态调用链同时生成 UA 的 _plv/_pld 和 BI 的 startplay/play，分别上传对应 channel 的 NCBL 文件。两类事件均使用已在抓包见到的：

POST https://clientlogsf.music.163.com/api/clientlog/encrypt/upload?multiupload=true

不额外调用尚未确认的 EAPI feedback/weblog 兼容路径，避免同一组 BI 事件重复投递。官方存在可配置域名和 v2 路由，本版不推测或自动切换到这些未验证分支。

PlaybackLogClient 使用隔离的 Ktor/OkHttp 传输，绕过 NeteaseInterceptor 对 JSON、EAPI 和账号 Cookie 的改写；关闭重定向，不增加伪造 IP。Android 版本和客户端配置复用现有 XEAPI profile。

记录行是 秒级时间戳 + U+0001 + 事件名 + U+0001 + JSON + LF。NcblCodec 根据官方 bilog 布局独立实现：NCBL v3、70 字节固定头、CC 元数据、Zstd、原始 ChaCha20、历史协议的 RSA 密钥封装。两个 channel 使用不同的随机输入。编码有独立 Python cryptography/libzstd fixture 和 RFC 8439 向量验证，不直接复制 MeiloX 的 GPL 实现。

zstd-jni 使用 Android AAR，JVM 测试额外使用桌面 JNI artifact，R8 保留原始 JNI 名称。依赖官方说明：https://github.com/luben/zstd-jni

## 上报域名解析

`PlaybackLogClient` 单独装配 `PlaybackLogDns`，只处理 `clientlogsf.music.163.com`。先使用系统 DNS；失败或仅返回空地址、本机、内网等非公网地址时，向 AliDNS 的 HTTPS JSON 接口查询 A 记录。解析器使用公开的引导地址连接 `dns.alidns.com`，不依赖被 hosts 拦截的目标域名；解析请求不携带账号、Cookie 或设备信息。其他域名原样交给系统 DNS，不修改系统、root 模块或应用其他接口的解析行为。

上传 URL、Host、TLS SNI 和证书校验仍保留原域名，不固定网易云服务器 IP，不关闭证书校验。独立解析的缓存有效期取回答中最短 TTL（包含 CNAME），上限 10 分钟；TTL 为零不缓存。并发查询合并，解析失败冷却 10 秒，单次 HTTPS 解析总超时 6 秒；独立解析不可用时保留待发记录并进入后台重试。当前独立查询为 IPv4，正常系统解析仍可返回 IPv6；纯 IPv6 网络的独立回退尚未验证。

这是参考官方独立 DNS 思路的等价实现，不声称移植了官方私有 HTTPDNS SDK。官方 9.6.31 的静态调用链中，`c42.f.v()` 从 `b32.v.i()` 获取客户端，`b32.b.i()` 选择 `s32.a`（CloudMusicDns）；后者通过 `s32.i` 接入网易云 HTTPDNS，同时保留系统解析回退。静态存在这条路径不等于已经证明用户设备上官方每次上报都使用它。分析 APK SHA-256：`2ceb9cf643284d9e71775383bc0298fb35a450da22642cd61c112e7debc25c3e`。

接口格式参考 [AliDNS DoH JSON API](https://help.aliyun.com/en/dns/httpdns-doh-json-api)。实际 DNS 查询已验证可返回目标域名的公网地址，不代表已验证真实播放日志上传或官方账号统计。

## 持久队列与重试

PlaybackLogRepository 负责账号、持久事件入口与装配；PlaybackLogDrainer 负责读取、发送及逐 channel 确认；PlaybackLogScheduler 负责后台工作合并。播放线程只提交不可变事件，不进行压缩、加密或网络 I/O。

WorkManager 使用联网约束、15 秒初始等待和 30 秒线性退避。同一时间最多保留两个未结束工作：当前工作与覆盖末次读取竞争的后继工作。新增事件和后继调度在同一互斥锁内检查并入队，避免每个事件追加一个工作而堆积依赖。每轮最多读取 8 条记录；还有记录时交接后继，失败连续运行 4 次后也交接后继，避免整个链长期卡在一个工作退避上。仍沿用单条记录最多 8 次失败的总上限。

新队列名为 `netease-playback-logs-v2`。首次调度取消旧版 `netease-playback-logs` 工作链，保留原 outbox 文件以及已确认 channel，新工作继续读取原记录。

outbox 位于 noBackupFilesDir/playback-logs/outbox.json，AtomicFile 写入，不改 Room 版本。最多保存 256 条事件、2MiB，超过容量时移除最旧记录；7 天过期，单条最多 8 次失败后丢弃，防止无限增长或请求风暴。

队列不保存登录 Cookie。所有权是 userId、token 和 deviceId 的 SHA-256 指纹；出队前重新读取身份，身份不符的记录会清理，网络请求只使用当次显式快照，不能串用新账号凭据。文件名、编码随机输入和记录正文持久化，正常重试保持同一文件和 envelope；UA／BI 接受状态分别落盘，已确认的 channel 不因另一 channel 失败而重发。

只有 HTTP 成功、业务 code=200 且 data.successfiles 包含精确文件名才确认接受。429、5xx、网络异常和缺少确认按退避重试；明确拒绝会丢弃记录，不显示虚假的成功提示。接口接受文件不能证明服务端已更新听歌次数、排行榜或年度统计；未知响应和断连后的服务端去重仍需真实账户验证。

## 验证

./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests 'com.ljyh.mei.playback.log.*' --tests 'com.ljyh.mei.data.network.netease.*' --tests 'com.ljyh.mei.data.network.NeteaseHttpTest' --tests 'com.ljyh.mei.playback.queue.*'

测试覆盖可听计时、倍速、墙上时钟变化、重复歌曲、切号、来源 ID、协议字段、独立加密样本、多帧、HTTP 假成功、持久恢复、分别确认、解析回退／TTL／并发、后台工作合并／旧链迁移、分批交接和重试边界。不在单元测试中请求真实网易云接口，不安装 APK，不把编译通过视为设备或账号统计验收。
