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

## 持久队列与重试

PlaybackLogRepository 在应用启动时恢复工作，使用 WorkManager 的联网约束、15 秒初始等待和指数退避；记录入队后追加工作，避免入队与正在运行的 worker 最后一次读取之间发生丢失。播放线程只提交不可变事件，不进行压缩、加密或网络 I/O。

outbox 位于 noBackupFilesDir/playback-logs/outbox.json，AtomicFile 写入，不改 Room 版本。最多保存 256 条事件、2MiB，超过容量时移除最旧记录；7 天过期，单条最多 8 次失败后丢弃，防止无限增长或请求风暴。

队列不保存登录 Cookie。所有权是 userId、token 和 deviceId 的 SHA-256 指纹；出队前重新读取身份，身份不符的记录会清理，网络请求只使用当次显式快照，不能串用新账号凭据。文件名、编码随机输入和记录正文持久化，正常重试保持同一文件和 envelope；UA／BI 接受状态分别落盘，已确认的 channel 不因另一 channel 失败而重发。

只有 HTTP 成功、业务 code=200 且 data.successfiles 包含精确文件名才确认接受。429、5xx、网络异常和缺少确认按退避重试；明确拒绝会丢弃记录，不显示虚假的成功提示。接口接受文件不能证明服务端已更新听歌次数、排行榜或年度统计；未知响应和断连后的服务端去重仍需真实账户验证。

## 验证

./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests 'com.ljyh.mei.playback.log.*' --tests 'com.ljyh.mei.data.network.netease.*' --tests 'com.ljyh.mei.data.network.NeteaseHttpTest' --tests 'com.ljyh.mei.playback.queue.*'

测试覆盖可听计时、倍速、墙上时钟变化、重复歌曲、切号、来源 ID、协议字段、独立加密样本、多帧、HTTP 假成功、持久恢复、分别确认和重试边界。不在单元测试中请求真实网易云接口，不安装 APK，不把编译通过视为设备或账号统计验收。
