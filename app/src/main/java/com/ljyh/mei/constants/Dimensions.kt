package com.ljyh.mei.constants

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.room.Playlist

val RecommendCardWidth = 160.dp
val RecommendCardHeight = 160.dp

val RecommendCardSpacing = 24.dp
val PlaylistCardSpacing = 20.dp

val PlaylistCardSize = 120.dp

val NavigationBarHeight = 80.dp
val MiniPlayerHeight = 64.dp
val QueuePeekHeight = 64.dp
val AppBarHeight = 64.dp

val ListItemHeight = 64.dp
val SuggestionItemHeight = 56.dp
val SearchFilterHeight = 48.dp
val ListThumbnailSize = 48.dp
val GridThumbnailHeight = 128.dp
val SmallGridThumbnailHeight = 92.dp
val PlayerHorizontalPadding = 24.dp

val AlbumThumbnailSize = 56.dp
val ArtistThumbnailSize = 56.dp
val PlaylistThumbnailSize = 56.dp
val TrackThumbnailSize = 56.dp
val CommonImageRadius = 8.dp
val ThumbnailCornerRadius = 6.dp

val NavigationBarAnimationSpec = spring<Dp>(stiffness = Spring.StiffnessMediumLow)

const val UserAgent="Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0"
const val AndroidUserAgent = "Mozilla/5.0 (Linux; Android 10; Mi A3 Build/QQ3A.200705.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/143.0.7499.34 Mobile Safari/537.36 NeteaseMusic/9.4.32.251222163637"
const val Github="https://github.com/ljyh223/Mei"

const val checkToken="9ca17ae2e6ffcda170e2e6ee8af14fbabdb988f225b3868eb2c15a879b9a83d274a790ac8ff54a97b889d5d42af0feaec3b92af58cff99c470a7eafd88f75e839a9ea7c14e909da883e83fb692a3abdb6b92adee9e"