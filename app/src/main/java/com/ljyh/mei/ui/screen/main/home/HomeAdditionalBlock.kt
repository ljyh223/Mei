package com.ljyh.mei.ui.screen.main.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.eapi.HomeFeedPolicy
import com.ljyh.mei.ui.component.card.PlaylistCard
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.screen.Screen
import kotlinx.serialization.json.*
import kotlinx.coroutines.launch

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

@Composable
internal fun HomeAdditionalBlock(block: HomePageResourceShow.Data.Block, nav: NavController, cardSize: Dp, viewModel: HomeViewModel) {
    val context = LocalContext.current
    val playPlaylist = rememberHomePlaylistPlayback(viewModel)
    val preparingPlaylistId by viewModel.preparingPlaylistId.collectAsState()
    val scope = rememberCoroutineScope()
    var albumJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val shelfCardSize = if (block.positionCode == "PAGE_RECOMMEND_SHORTCUT") cardSize * 0.75f else cardSize
    fun open(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme in listOf("http", "https")) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }
    val data = block.dslData
    val bannerPayload = block.nativeData?.get("banners") as? JsonArray
    if (bannerPayload != null) {
        val banners = remember(bannerPayload) { HomeFeedPolicy.visibleBanners(bannerPayload) }
        if (banners.isEmpty()) return
        LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(homeCardSpacing())) {
            items(banners) { banner ->
                AsyncImage(model = banner.text("pic"), contentDescription = banner.text("typeTitle"), contentScale = ContentScale.Crop,
                    modifier = Modifier.width(cardSize * 2).height(cardSize).clip(RoundedCornerShape(12.dp)).clickable {
                        val id = banner.text("targetId")
                        when (banner.text("targetType")) {
                            "10" -> Screen.Album.navigate(nav) { addPath(id) }
                            "1000" -> Screen.PlayList.navigate(nav) { addPath(id) }
                            "1" -> open("https://music.163.com/#/song?id=$id")
                            else -> open(banner.text("url"))
                        }
                    })
            }
        }
        return
    }
    if (data == null) return
    val content = if (block.positionCode == "PAGE_RECOMMEND_ARTIST_TREND") {
        data.values.filterIsInstance<JsonObject>().firstOrNull { it["items"] is JsonArray } ?: data
    } else selectSpecialField(data) ?: data
    val title = (data["header"] as? JsonObject)?.text("title")?.takeIf { it.isNotBlank() }
        ?: content.text("title").takeIf { it.isNotBlank() }
        ?: when (block.positionCode) {
            "PAGE_RECOMMEND_SHORTCUT" -> "最近访问"
            "PAGE_RECOMMEND_ARTIST_TREND" -> data.values.filterIsInstance<JsonObject>()
                .firstNotNullOfOrNull { it.text("blockTitle").takeIf(String::isNotBlank) } ?: "你关注的艺人新动向"
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM" -> "播客推荐"
            else -> (content["header"] as? JsonObject)?.text("title").orEmpty()
        }
    val cards = remember(block) {
        if (block.positionCode == "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM") {
            (data["items"] as? JsonArray).orEmpty().flatMap {
                ((it as? JsonObject)?.get("items") as? JsonArray).orEmpty()
            }.filterIsInstance<JsonObject>()
        } else {
            ((content["resources"] ?: content["items"]) as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
        }
    }
    if (cards.isEmpty()) return
    if (title.isNotBlank()) Title(title)
    LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(homeCardSpacing())) {
        items(cards) { card ->
            val type = card.text("resourceType")
            val id = card.text("resourceId")
            val onCardClick: () -> Unit = {
                    albumJob?.cancel()
                    if (block.positionCode == "PAGE_RECOMMEND_ARTIST_TREND") {
                        albumJob = scope.launch {
                            when (val result = viewModel.artistAlbumId(type, id)) {
                                is com.ljyh.mei.data.network.Resource.Success -> Screen.Album.navigate(nav) { addPath(result.data) }
                                is com.ljyh.mei.data.network.Resource.Error -> android.widget.Toast.makeText(context, "暂时无法打开专辑，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
                                else -> Unit
                            }
                        }
                    } else when (type) {
                        "album", "digital_album" -> Screen.Album.navigate(nav) { addPath(id) }
                        "list", "playList", "playlist" -> Screen.PlayList.navigate(nav) { addPath(id) }
                        "artist" -> Screen.Artist.navigate(nav) { addPath(id) }
                        "song" -> open("https://music.163.com/#/song?id=$id")
                        "djprogram" -> open("https://music.163.com/#/program?id=$id")
                    }
            }
            if (block.positionCode == "PAGE_RECOMMEND_ARTIST_TREND") {
                VinylAlbumCard(
                    title = card.text("title"), subtitle = card.text("subTitle"),
                    coverUrl = card.text("coverImg").ifBlank { card.text("coverUrl") },
                    modifier = Modifier.width(shelfCardSize * VinylAlbumArtworkRatio),
                    onClick = onCardClick,
                )
            } else {
                PlaylistCard(id = id, title = card.text("title"),
                    coverImg = card.text("coverImg").ifBlank { card.text("coverUrl") }, cardSize = shelfCardSize,
                    showPlay = type in setOf("list", "playList", "playlist"),
                    preparingPlayback = preparingPlaylistId == id,
                    onPlayClick = { playPlaylist(id) }, onClick = onCardClick,
                )
            }
        }
    }
}
