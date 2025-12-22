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