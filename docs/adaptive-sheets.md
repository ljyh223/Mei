# 自适应功能弹层

`ui/component/sheet/MeiAdaptiveSheet.kt` 负责临时功能弹层的展示容器。播放器的 mini → 全屏展开、平板内嵌歌词／队列／评论、确认对话框和系统分享面板不属于这一层。

## 展示规则

- 手机／紧凑外层默认使用同一弹窗内的 Material 3 底部容器与纵向手势。手机横屏全屏播放器的功能弹层从末端侧边滑入，普通字体下需要扣除左右安全区后有 480dp 可用宽度，字体放大时提高门槛，空间不足回到底部。
- 宽外层且实际窗口扣除左右安全区后至少有 600dp 时，从末端侧边滑入；中文与其他从左到右语言为右侧。字体增大时提高所需宽度，最多提高至 780dp；空间不足时回到底部。
- 侧边面板覆盖页面，不挤压正文。普通字体宽 420dp，随字体适度增宽，并受窗口剩余空间约束；距系统安全区 16dp，内部有关闭按钮，嵌套操作有返回按钮。材质使用普通主题 surfaceContainer，不添加玻璃。
- 遮罩、返回键和向外拖动可关闭侧边面板；向内拖动和不足阈值的拖动回到原位。底部仍沿用 Material 3 的纵向手势。
- 统一弹窗通过 safeDrawing、imePadding 和导航栏 inset 统一处理系统安全区与 IME，两种容器不重复避让。内容不再自行叠加 imePadding／navigationBarsPadding。

容器使用 `LocalWindowInfo` 的实际窗口尺寸，外层能力沿用 `LocalAppWindowLayout`。播放器通过共用弹层入口启用横屏侧边样式。尚未接入外层的独立调用使用配置最小宽度作为回退。

## 状态与操作

`MeiAdaptiveSheet` 只负责展示、动画和关闭，不创建 ViewModel 或发起业务请求。内容的 ViewModel、播放连接、弹层目的地和返回关系仍归原入口管理。

- `content(close)` 的 close 请求带动画关闭；`onDismissRequest` 在关闭完成后调用。独立组件也可以使用 `rememberMeiAdaptiveSheetState()`，在操作成功时调用 `hide()`。
- `onNavigateBack` 表示返回当前弹层的上一层，区别于关闭整组弹层；`CommonOverlayHandler` 继续复用 `PlayerSheetNavigation`。
- `dismissEnabled` 与 `onDismissBlocked` 保护菜单排序等编辑过程，优先取消编辑，再返回上一级或关闭。
- 窗口在底部／侧边之间切换时，同一个 Dialog 保持挂载，在其内部移动同一份 movable content，而不是替换两个原生弹窗或新建第二份内容。窗口内的普通布局切换不应丢失草稿或重启内容任务；Activity 重建仍依赖各状态自身的 rememberSaveable、ViewModel 与保存机制，不承诺任意 remember 状态跨重建保留。
- 内容通过 `meiSheetContentHeight` 获得当前宿主可用高度：侧边使用面板空间，底部可以保留较紧凑的高度与上限。不要继续在内容中按屏幕高度写死手机尺寸。

## 已接入范围

播放器弹层统一从 `CommonOverlayHandler` 接入，包括更多、歌曲信息、音质、歌词来源、QQ 歌词匹配、睡眠定时、添加／创建歌单和操作栏自定义。

共用的 `AddToPlaylistSheet`、`CreatePlaylistSheet`、`AlbumArtistBottomSheet`、`DescriptionSheet` 以及播放器各弹层的独立调用也使用同一容器。已有 embedded 内容路径继续使用宿主，不叠加第二层 modal。

本地元数据编辑和图片选择等页面专属弹层尚未迁入；它们后续可接入这个入口，不应各自复制侧边容器。

## 验证边界

定向 JVM／Robolectric 检查覆盖容器切换中的草稿和任务生命周期、嵌套返回与编辑保护、遮罩及侧滑关闭、窄窗口与大字体回退。编译检查覆盖现有业务调用。按项目约定不安装 APK 或做设备视觉检查，输入法动画、系统手势和实际侧滑观感由设备验收。
