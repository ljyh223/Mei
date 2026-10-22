# Beta 测试包构建

合并到默认分支后，在 GitHub **Actions → Beta Build → Run workflow** 中选择要构建的分支（通常为 `main`），手动启动构建。

完成后，在该次运行的 **Artifacts** 下载 `Mei_beta_<运行序号>_<提交短号>`。压缩包包含签名后的 APK 和 SHA-256 校验文件，保留 14 天。

工作流仅构建测试 APK 并上传 Actions 产物，没有发布或创建标签步骤，不会在合并或推送时自动运行。

构建沿用现有 Rust USB 检查、项目 Gradle Wrapper 和签名配置，使用 Secrets：`SIGNING_KEY`、`ALIAS`、`KEY_STORE_PASSWORD`、`KEY_PASSWORD`。APK 使用应用自身版本号和包名。

原有正式发布工作流保持原样，本次没有改动。
