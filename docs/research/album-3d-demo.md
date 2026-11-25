# 专辑 3D Demo 实验记录（已迁移）

最初的 CD、磁带和黑胶模型通过 Debug 构建中的独立演示页验证。模型现已迁入正式专辑详情；2026-11-25 清理了 `app/src/debug` 下的演示页面、三个桌面入口、入口名称和本地演示封面。旧 Demo 不再提供单独的启动方式。

## 保留的正式实现

模型代码位于 `app/src/main/java/com/ljyh/mei/ui/component/artwork/album/`：

- `AlbumCdRenderer`、`AlbumCassetteRenderer`、`AlbumVinylRenderer`：CD 盒、磁带收藏盒及黑胶封套与唱片。
- `AlbumPackageRendering`：共用实体造型、材质、投影及遮挡规则。
- `CassettePackageMotion`：可逆的开盒、取出与收纳时间轴。
- `AlbumMediaArtwork`：加载真实专辑封面，装配旋转、开合、内页与倒影。

专辑详情的展示偏好、年代分界、交互和验证入口见 [专辑详情封面展示与年代模型](../album-era-styles.md)。演示页的移除不改变这些正式组件。
