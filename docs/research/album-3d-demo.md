# 独立专辑 3D Demo：CD、磁带与黑胶

三个版本都只参与 Debug 构建，不接入应用导航、专辑详情或播放器。正式版不包含 Demo 入口、代码和封面素材。

## 入口

Debug 构建提供三个桌面入口，也可以在任一 Demo 顶部切换 CD、磁带和黑胶：

| 桌面入口 | Activity | 默认版本 |
| --- | --- | --- |
| Mei 专辑 3D Demo | `.demo.album.Album3dDemoActivity` | CD |
| Mei 磁带 3D Demo | `.demo.album.Cassette3dDemoActivity` | Cassette |
| Mei 黑胶 3D Demo | `.demo.album.Vinyl3dDemoActivity` | Vinyl |

安装对应 Debug 构建后，可手动启动：

```sh
adb shell am start -n com.ljyh.mei/.demo.album.Album3dDemoActivity
adb shell am start -n com.ljyh.mei/.demo.album.Cassette3dDemoActivity
adb shell am start -n com.ljyh.mei/.demo.album.Vinyl3dDemoActivity
```

本次实现不包含安装、启动或设备视觉验证。

## 共用操作与状态

- 在模型区域横向拖动，绕竖轴旋转；纵向拖动，调整俯仰角。
- “旋转”每次转动 90°；“复位”恢复当前版本的初始视角与机构状态。
- 系统返回或左上角返回关闭 Demo。

三个版本各自保存旋转、俯仰、开合／抽出／带盘位置，切换回来不会串用另一版本的状态。状态使用 `rememberSaveable` 和按版本区分的 `SaveableStateHolder`，由 Demo 自己管理。按钮和滑杆提供旋转及机构操作的替代入口；无障碍自定义操作支持向上／向下倾斜。

所有动画由用户操作触发，遵循 Compose 的系统动画时长设置，没有持续自动旋转。窄窗口纵向排列并允许滚动，横屏宽窗口使用模型与控件两栏布局。内容复用公共手机／平板边距，并消费系统安全区。

## 各版本交互

### CD

点击模型或“打开盒盖 / 合上盒盖”按钮切换状态，滑杆连续调整 0–160°。`AlbumCaseRenderer` 将盒盖和底盒作为两个独立的有厚度网格，绕左侧铰链开合，展示封面、内页、托盘与光盘。

### 磁带 / Cassette

磁带版以竖放的翻盖收藏盒作为初始形态：正面展示专辑封面与标题，下方保留纸卡信息；盒脊、扣位、两个铰链和塑料边框围住纸质 J-card。打开后，左侧是纸卡内页，右侧托盘固定竖放的磁带；取出后磁带转为横向，展示在盒旁。

点击模型或主按钮按“打开包装 → 取出磁带 → 装回包装”推进。滑杆连续调整整个展开过程；“合上包装”从当前状态收回。“翻面”转动整个模型 180°，取出后可“转动带盘”。包装内的磁带保留双面标签、带窗、磁头开口、导带轮和螺丝细节。

拆盒与收纳共用一个可逆时间轴：先打开盒盖，再抬离托盘与边框，最后平移并转为横向；收回时按相反次序执行。单独的 `CassettePackageMotion.kt` 定义姿态，防止拖动滑杆或中途反向操作时横移穿过盒壁、盒盖先于磁带收纳而闭合。关闭、开盒停靠与完全取出都保存在 Demo 局部状态中。

这是带 J-card、托盘和磁带的风格化收藏盒设计，不宣称复刻官方实体版本，也不模拟播放或真实机械传动。

### 黑胶 / Vinyl

点击模型或“抽出唱片 / 收回唱片”改变唱片位置，滑杆连续调整抽出量。“转动唱片”使标签有限转动一圈，继续保留全模型的拖动旋转。

纸质封套和唱片分别建模，唱片具有正反标签、沟槽、高光、透明中心孔和 64 段圆形侧边。唱片位于封套夹层，仍在封套内的部分在透视投影前按开口位置裁切，防止斜视时因面中心深度排序而穿过封面。

## 实现与素材

所有代码位于 `app/src/debug/java/com/ljyh/mei/demo/album/`，入口在 `app/src/debug/AndroidManifest.xml`。

- `Album3dDemoActivity.kt`：三个独立 Activity，共用 Demo 主题与界面。
- `Album3dDemoScreen.kt`：版本选择、局部保存状态、手势、按钮和滑杆。
- `AlbumCaseRenderer.kt`：原 CD 盒渲染。
- `CassetteDemoRenderer.kt`：磁带收藏盒、纸卡、托盘与磁带机构。
- `CassettePackageMotion.kt`：可逆的开盒、抬起、横移与转向顺序。
- `VinylDemoRenderer.kt`：封套、唱片抽出及转动。
- `MediaDemoRendering.kt`：磁带和黑胶共用的透视投影、背面剔除、深度排序、画布边界限制和绘制工具。

三个版本使用 Compose Canvas 与 Android Canvas，不引入额外 3D 引擎。本地纹理 `app/src/debug/res/drawable-nodpi/album_demo_cover.webp` 提取自用户提供的参考视频 `0bc32ma42aab7iadwoxh6rvfdu6dzxjqdtia.f10002.mp4` 首帧封面区域，仅用于 Demo。Coil 通过应用现有加载器读取这一资源，不需要联网、重新生成纹理或新增 ImageLoader。

内页、磁带标签和封套背面是演示设计，并非官方实体版本、接口提供的背封或真实曲目单。三个 Demo 都没有播放功能。

## 验证

```sh
./gradlew :app:compileDebugKotlin :app:processDebugMainManifest
./gradlew :app:testDebugUnitTest --tests 'com.ljyh.mei.demo.album.CassettePackageMotionTest'
```

如当前工作树没有 SDK 配置，可为该次命令设置 `ANDROID_HOME` 指向本机 SDK，无需修改仓库配置。定向 JVM 测试检查开合端点、盒盖／盒壁避让及收纳顺序；几何边界另做数值检查。编译、测试与入口合并不能代替设备上的视觉与手势验收。
