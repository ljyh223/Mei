# 正式发布构建

`Release` 工作流（`.github/workflows/main.yml`）支持以下触发方式：

- 推送以 `v` 开头的标签。
- 手动运行工作流，选择构建分支或标签，并提供 `tag_name`；构建完成后会创建该标签并发布 Release。

这两种操作都会正式发布。只需测试安装包时使用 **Beta Build**，不要启动 Release 工作流。

## 下载文件

发布页直接提供两个独立 APK 附件：

- `Mei_<标签>_arm64-v8a.apk`
- `Mei_<标签>_x86_64.apk`

每个 APK 只包含对应 ABI 的原生库，不额外生成 32 位或通用 APK，也不把安装包装成 ZIP 上传。GitHub 自动提供的源码压缩包不属于应用安装包。

工作流沿用现有签名 Secrets；在上传前，公共脚本 `tools/ci/prepare-apks.py` 校验两个 APK 的签名及原生库架构。手动填写的发布说明优先于 `.github/release_notes.md`；保留预发布标签判断和已有 Release 跳过行为。

构建使用项目 Gradle Wrapper，并开启 `-PmeiAbiSplits=true`。普通本地构建默认仍沿用原有通用 APK 输出。
