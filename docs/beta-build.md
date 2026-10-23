# Beta 测试包构建

合并到默认分支后，在 GitHub **Actions → Beta Build → Run workflow** 中选择要构建的分支（通常为 `main`），手动启动构建。

完成后，在该次运行的 **Artifacts** 下载 `Mei_beta_<运行序号>_<提交短号>`。压缩包包含以下两个独立的签名 APK 及各自的 SHA-256 校验文件，保留 14 天：

- `Mei_beta_<运行序号>_<提交短号>_arm64-v8a.apk`：64 位 ARM 手机、平板。
- `Mei_beta_<运行序号>_<提交短号>_x86_64.apk`：64 位 x86 设备、模拟器。

每个 APK 仅包含对应架构的原生 `.so` 库，不生成 32 位或通用 APK。构建后会检查两个包内的实际原生库架构，防止混包或漏包。

工作流仅构建测试 APK 并上传 Actions 产物，没有发布或创建标签步骤，不会在合并或推送时自动运行。

构建沿用现有 Rust USB 检查、项目 Gradle Wrapper 和签名配置，使用 Secrets：`SIGNING_KEY`、`ALIAS`、`KEY_STORE_PASSWORD`、`KEY_PASSWORD`。APK 使用应用自身版本号和包名。

原有正式发布工作流保持原样，本次没有改动。

本地构建相同分包时执行：

```bash
./gradlew -PmeiAbiSplits=true :app:assembleRelease
```

该选项仅由 Beta Build 显式开启，普通本地构建与原正式发布工作流的输出方式保持不变。
