# Mei 项目协作说明

## 项目结构

- 这是 Kotlin / Jetpack Compose Android 应用，主模块为 `app`；Gradle 配置使用 Kotlin DSL，依赖版本集中在 `gradle/libs.versions.toml`。
- 页面和可复用组件在 `app/src/main/java/com/ljyh/mei/ui/`；网络接口在 `data/network/` 与 `data/network/api/`；播放服务在 `playback/`；Room 数据库及迁移在 `di/AppDatabase.kt`。
- 图片 URL 的尺寸处理统一放在 `app/src/main/java/com/ljyh/mei/utils/image/ImageUrls.kt`。添加同类逻辑前先查已有工具方法。
- `tools/reverse-engineering/` 和外部 `api-enhanced` 是协议研究参考；应用实现以 `app/` 内代码为准。不要把抓包中的 Cookie、令牌或设备标识写入源码、测试或文档。

## 修改约定

- 开始编辑前检查当前分支和工作区状态；保留与当前任务无关的修改，不自行清理、重置或提交。
- UI 图片优先使用项目现有的 Coil 组件和缓存。列表或封面墙应让图片逐张呈现；避免为了显示首张图而等待全部请求完成。
- 调整 EAPI / XEAPI 接口时，逐个核对请求路径、参数、加密方式和响应结构；不要根据路径前缀推断所有接口都可用同一种协议替换。
- 改动 Room 数据库版本或表结构时，检查已发布版本到新版本的迁移路径，避免用户升级后启动崩溃或丢失数据。
- 播放相关的歌词、封面等预加载由播放服务触发，结果进入共享数据源；页面负责消费状态。

## 验证与交付

- 小型 UI 修改先运行 `./gradlew :app:compileDebugKotlin`；数据、协议或播放行为变化再运行相关的定向测试。
- 按当前协作约定，不主动安装或更新 APK，也不进行视觉测试；需要时由用户另行提出。
- 汇报实际修改、验证结果和仍未确认的行为。编译通过不等于已在设备上验证视觉效果。
