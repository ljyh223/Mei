# LX 新样本黑盒测试：2026-10-22

样本：用户新增的 `hunayin.js`、`qdy.js`、`xinghai.js`。样本只由测试进程加载执行，没有查看、解混淆或输出源码。本文不包含播放 URL、原始接口响应、Cookie 或令牌。用户转述的 GitHub 有效性说明没有作为测试依据。

测试使用生产 LX 宿主、目录适配和 Media3 探测器，在 QuickJS-KT JVM 原生库及 Robolectric 环境中执行；没有安装 APK 或在设备上播放。本次没有修改应用播放逻辑；已核对测试分支与当前本地 main 的 LX 生产源码及宿主预加载脚本一致。

## 结论

| 样本 | 初始化 | 实测结果 |
| --- | --- | --- |
| hunayin.js | 成功；wy / tx / kw / mg | 多个返回地址在当前网络下域名解析失败；未确认可用音频 |
| qdy.js | 成功；wy / tx / kw / kg / mg | 海阔天空的网易云、酷我结果通过时长校验；其他抽测结果失败，属于部分可用 |
| xinghai.js | 成功；wy / tx / kw / kg / mg | 海阔天空的网易云 320k 请求通过时长校验；另一次 128k 请求失败，存在波动；其他平台未确认可用 |

`AUDIO_VALIDATED` 要求识别到音频时长、大于 30 秒、且与目标歌曲时长差不超过 15 秒。仅返回 URL 或 HTTP 200 不算通过。没有在这一批样本中检出已确认的 30 秒以内试听音频；无法访问、返回非音频或无法确认时长的结果也没有被采用。

## 实际取链与音频校验

曲库目标：《海阔天空》— Beyond，326 秒；《晴天》— 周杰伦，269 秒。

下表音质为请求参数，不保证音源返回文件的实际码率。

| 样本 | 歌曲 / 平台 | 请求音质 | 结果 |
| --- | --- | --- | --- |
| hunayin.js | 海阔天空、晴天 / 网易云 | 320k、128k | 返回 URL，但均发生 UnknownHostException，未确认时长 |
| hunayin.js | 海阔天空 / 酷我、QQ | 320k | 返回 URL，域名解析失败 |
| hunayin.js | 海阔天空 / 咪咕 | 320k | 取链阶段失败 |
| hunayin.js | 海阔天空 / 酷狗 | — | 脚本未声明支持 |
| qdy.js | 海阔天空 / 网易云 | 320k、128k | HTTP 206，识别为 MP3，约 326.063 秒，通过 |
| qdy.js | 海阔天空 / 酷我 | 320k | HTTP 206，识别为 MP4，约 324.940 秒，通过 |
| qdy.js | 海阔天空 / QQ | 320k、128k | 返回 URL，音频访问 HTTP 403 |
| qdy.js | 海阔天空 / 酷狗 | 320k | 返回 URL，HTTP 200 但正文是 JSON，拒绝采用 |
| qdy.js | 海阔天空 / 咪咕 | 320k | 返回 URL，音频访问 HTTP 404 / HTML |
| qdy.js | 晴天 / 网易云 | 320k、128k | 返回 URL，音频访问 HTTP 404 / HTML |
| xinghai.js | 海阔天空 / 网易云 | 320k | HTTP 206，识别为 MP3，约 326.061 秒，通过 |
| xinghai.js | 海阔天空 / 网易云 | 128k | 请求头对照时曾返回 MP3；后续时长测试未取得音频 URL，并观察到接口 403，未判定为稳定可用 |
| xinghai.js | 晴天 / 网易云 | 320k、128k | 取链失败；128k 测试观察到接口 403 |
| xinghai.js | 海阔天空 / 酷我、QQ、酷狗、咪咕 | 320k | 未取得可验证的音频 URL |

额外对《海阔天空》的网易云 320k、128k 和 QQ 128k 比较了默认请求头与播放器 User-Agent：qdy 和 xinghai 的网易云返回地址在该次对照中均响应 206 / MP3；hunayin 仍域名解析失败，qdy 的 QQ 仍为 403。更换请求头没有解决这些失败。

这些结果仅覆盖本次文件、歌曲、时间和网络。初始化通过不能保证所有歌曲可用，HTTP 403 / 404 或取链失败也不足以单独判断脚本永久失效。

## 测试工具改进及复测

手动测试支持逗号分隔的样本文件名、指定目标音质，并输出脱敏的脚本 HTTP 状态码。失败结果区分未返回音频 URL；请求头对照会跳过脚本未声明的音质。未启用外部样本目录时，黑盒用例继续默认跳过。

```sh
MEI_LX_SAMPLE_DIR=/path/to/lx_temp \
MEI_LX_SAMPLE_FILTER=hunayin.js,qdy.js,xinghai.js \
./gradlew :app:testDebugUnitTest \
  --tests 'com.ljyh.mei.playback.source.lx.LxBlackBoxTest.initialization' \
  --tests 'com.ljyh.mei.playback.source.lx.LxBlackBoxTest.publicTrackUrls' \
  --tests 'com.ljyh.mei.playback.source.lx.LxBlackBoxTest.otherPlatforms'
```

指定 `MEI_LX_TARGET_QUALITY=standard` 后，`publicTrackUrls` / `otherPlatforms` 请求 128k；默认请求最高不超过 320k。`lowerQualityAndPlaybackHeaders` 用例另行比较固定的请求头场景。JUnit 通过表示诊断流程完成，音源可用性以脱敏报告中的结果为准。

## 回归验证

`:app:compileDebugKotlin` 通过。LX 定向回归共 35 个用例：31 个通过，4 个需要外部样本的手动用例按预期跳过，0 失败。此前的新样本手动诊断均执行完成；音源的部分失败如上表记录，不由 JUnit 通过掩盖。
