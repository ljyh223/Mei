# 图片加载约定

普通 Compose 页面直接将原始 URL 交给现有 Coil `AsyncImage`。不要在页面拼接 CDN 参数，也不要用自定义缓存键隐藏图片尺寸。

应用级 `ArtworkRequestInterceptor` 在 Coil 缓存查找之前读取组件解析后的像素尺寸（包含屏幕密度），取已知长边并向上归档：192、512、1024px。无法取得尺寸时使用 512px；超过最大档也不会自动请求原图。网易云 URL 统一使用 `imageView`、`thumbnail`、WebP 和质量 80，替换重复的旧尺寸参数，保留其他参数及动态图片。`ImageUrls.kt` 是尺寸和 URL 规则的唯一入口。

全屏播放器、全屏图片预览需要原图时使用 `rememberOriginalArtworkRequest(url)`。它优先使用已有内存缓存，其次只从磁盘缓存查找缩略图，再加载原图；不会为了占位额外下载小图，原图失败时保留占位图。没有占位缓存时沿用组件的空状态。保存图片使用 `ImageRequest.Builder.originalArtwork(url)` 显式绕过缩略规则。

取色、背景采样、通知栏及预加载没有组件尺寸，应在 `ImageRequest.size` 指定处理尺寸，网络图片同样自动归档。下一首图片由播放服务触发预加载，页面消费同一应用级 ImageLoader 的缓存。

本地路径、content URI 和非网易云域名保持原样，不套用网易云参数；其他 CDN 的服务端缩略需要单独适配。这里只统一网络请求规格，不改变组件在界面上的 dp 大小。
