# 外接键盘媒体控制

适用于手机、平板与 Waydroid 向 Android 传递的标准媒体键（包括生成这些键码的 Fn 组合键）。
不新增 SDK、全局按键权限或普通方向键／空格快捷键。

## 事件处理

- 前台：`MainActivity` 在可见期间连接现有 `MusicService` 的 MediaController，
  将专用媒体键交给 `MediaKeyboardControls`；退到后台时释放连接。
- 后台：媒体键仍由 Android 路由至现有 MediaSession，沿用 Media3 的处理与耳机双击语义。
- `mediaControllerCommands` 给非 trusted 控制器开放读取和基本播放控制，
  不授予队列修改、设备音量、释放播放器等能力。最终可用命令仍受播放器当前能力限制。

| 媒体键 | 操作 |
| --- | --- |
| 播放／暂停 | 切换播放状态；停止态有队列时先 prepare |
| 播放、暂停、停止 | 执行对应操作 |
| 下一首／上一首，包括 SKIP_FORWARD / SKIP_BACKWARD | 交给媒体会话切歌，沿用队列与上一首规则 |
| 快进／快退 | 使用播放器配置的步长，目前为 5 秒；前台允许长按连续步进 |

按键松开不重复操作；切歌和播放暂停忽略长按重复事件。没有队列或命令不可用时，
前台按下事件交回系统处理。普通输入、音量、方向键和焦点导航不被拦截。

## Waydroid 边界

宿主桌面如果截获了媒体键，或键盘 Fn 层没有产生 Android 媒体键，应用收不到事件。
本改动处理已经送达 Activity 或 MediaSession 的事件，不能改变宿主桌面全局快捷键配置。

## 从宿主发送测试事件

先启动 Waydroid，使用包含修复的 Mei 并加载播放队列。以桌面普通用户执行：

```sh
# 先预览命令，不会发送事件，也不要求会话已启动
python3 tools/waydroid-media-test.py next --dry-run

# 发送单次下一首／快进；只对 waydroid shell 使用 sudo
python3 tools/waydroid-media-test.py next --sudo
python3 tools/waydroid-media-test.py fast-forward --sudo

# 每隔一秒快进一次，共三次；这是独立按键，不是长按事件
python3 tools/waydroid-media-test.py fast-forward --count 3 --interval 1 --sudo
```

可用动作：`play-pause`、`play`、`pause`、`stop`、`next`、`previous`、`fast-forward`、`rewind`。
脚本默认只发送一次，不自动启动容器、安装 APK、修改桌面快捷键或 sudo 策略。
Waydroid 未运行或任一命令失败时停止；命令完成后需要观察播放器是否响应。

此方法直接注入 Android 媒体键，可分别在 Mei 前台及其他 Android 页面前台时测试。
若注入有效、物理键盘无效，继续排查宿主快捷键到 Waydroid 的转发。

验证：

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests 'com.ljyh.mei.playback.MediaKeyboardControlsTest' --tests 'com.ljyh.mei.playback.MediaControllerCommandsTest'
```

按项目约定不自动安装 APK 或做视觉测试。真实键盘与 Waydroid 宿主到容器的按键转发仍需用户设备验证。
