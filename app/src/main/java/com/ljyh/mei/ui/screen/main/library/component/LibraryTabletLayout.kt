package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.ui.component.home.PlaylistCard
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.main.library.LibraryContentUiState
import com.ljyh.mei.ui.screen.main.library.LibraryEvent
import com.ljyh.mei.ui.screen.main.library.LibraryProfileUi
import com.ljyh.mei.ui.screen.main.library.LibrarySection
import com.ljyh.mei.ui.screen.main.library.ListeningFootprintUi
import com.ljyh.mei.ui.screen.main.library.ListeningPeriod
import com.ljyh.mei.ui.screen.main.library.ListeningPeriodUi
import com.ljyh.mei.utils.color.darken
import com.ljyh.mei.utils.color.isGrayscale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.palette.graphics.Palette
import java.time.LocalTime
import kotlin.math.ceil

@Composable
fun LibraryTabletLayout(
    state: LibraryContentUiState,
    backgroundUrl: String,
    onEvent: (LibraryEvent) -> Unit,
) {
    val assets = state.assetsForSelectedSection()
    val bottomPadding = LocalPlayerAwareWindowInsets.current
        .asPaddingValues()
        .calculateBottomPadding()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                top = 24.dp,
                end = 28.dp,
                bottom = bottomPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProfileSidebar(
            profile = state.profile,
            backgroundUrl = backgroundUrl,
            listeningFootprint = state.listeningFootprint,
            createdCount = state.createdCount,
            collectedCount = state.collectedCount,
            albumCount = state.albumCount,
            onChangePhoto = { onEvent(LibraryEvent.ChangeProfilePhoto) },
            modifier = Modifier
                .width(318.dp)
                .fillMaxHeight(),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { LibraryHeading(state) }
            item(span = { GridItemSpan(maxLineSpan) }) { LibraryQuickActions(onEvent) }
            item(span = { GridItemSpan(maxLineSpan) }) { LibrarySectionHeader(state, onEvent) }
            if (assets.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryTabletEmptyState(state.section)
                }
            } else {
                items(assets, key = { "${state.section}-${it.id}" }) { asset ->
                    PlaylistCard(
                        id = asset.id,
                        title = asset.title,
                        coverImg = asset.cover,
                        subTitle = listOf(asset.subtitle),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onEvent(
                                if (state.section == LibrarySection.Albums) {
                                    LibraryEvent.OpenAlbum(asset.id)
                                } else {
                                    LibraryEvent.OpenPlaylist(asset.id)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberProfileColors(backgroundUrl: String): List<Color> {
    val context = LocalContext.current
    val fallbackColor = MaterialTheme.colorScheme.primary
    var seedColor by remember(backgroundUrl) { mutableStateOf(fallbackColor) }

    LaunchedEffect(backgroundUrl, fallbackColor) {
        if (backgroundUrl.isBlank()) {
            seedColor = fallbackColor
            return@LaunchedEffect
        }
        seedColor = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(backgroundUrl)
                    .size(160)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    Palette.from(result.image.toBitmap())
                        .maximumColorCount(64)
                        .generate()
                        .dominantSwatch
                        ?.rgb
                        ?.let(::Color)
                        ?: fallbackColor
                } else {
                    fallbackColor
                }
            }.getOrDefault(fallbackColor)
        }
    }

    val targetColors = remember(seedColor, fallbackColor) {
        val darkSeed = if (seedColor.isGrayscale()) {
            val neutralLevel = when {
                seedColor.luminance() > 0.72f -> 0xFF46464A
                seedColor.luminance() > 0.30f -> 0xFF343438
                else -> 0xFF242428
            }
            Color(neutralLevel)
        } else {
            when {
                seedColor.luminance() > 0.62f -> seedColor.darken(0.52f)
                seedColor.luminance() > 0.34f -> seedColor.darken(0.32f)
                else -> seedColor.darken(0.10f)
            }
        }
        listOf(
            darkSeed,
            darkSeed.darken(0.56f),
            darkSeed.darken(0.24f),
        )
    }
    val first by animateColorAsState(targetColors[0], label = "profileGradientPrimary")
    val second by animateColorAsState(targetColors[1], label = "profileGradientBackground")
    val third by animateColorAsState(targetColors[2], label = "profileGradientAccent")
    return listOf(first, second, third)
}

@Composable
private fun ProfileSidebar(
    profile: LibraryProfileUi,
    backgroundUrl: String,
    listeningFootprint: ListeningFootprintUi,
    createdCount: Int,
    collectedCount: Int,
    albumCount: Int,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileColors = rememberProfileColors(backgroundUrl)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = profileColors[1]),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (backgroundUrl.isNotBlank()) {
                AsyncImage(
                    model = backgroundUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.34f),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                profileColors[0].copy(alpha = 0.54f),
                                profileColors[2].copy(alpha = 0.68f),
                                profileColors[1].copy(alpha = 0.88f),
                                Color(0xFF101013).copy(alpha = 0.97f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "MY MUSIC PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            onClick = onChangePhoto,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Wallpaper, null, Modifier.size(16.dp))
                                Text("更换背景", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = "${profile.nickname}的头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(28.dp)),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = profile.nickname.ifBlank { "Music Lover" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        MembershipBadge(profile)
                    }
                    Text(
                        text = profile.signature.ifBlank { "还没有填写个人签名" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    ProfileStats(profile)
                }

                if (listeningFootprint.hasContent) {
                    ListeningFootprintCard(listeningFootprint)
                } else {
                    LibrarySummaryFallback(createdCount, collectedCount, albumCount)
                }
            }
        }
    }
}

@Composable
private fun MembershipBadge(profile: LibraryProfileUi) {
    when {
        !profile.membershipIconUrl.isNullOrBlank() -> AsyncImage(
            model = profile.membershipIconUrl,
            contentDescription = profile.membershipLabel,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(68.dp)
                .height(24.dp),
        )

        !profile.membershipLabel.isNullOrBlank() -> Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFD99B),
            contentColor = Color(0xFF422710),
        ) {
            Text(
                text = profile.membershipLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun ProfileStats(profile: LibraryProfileUi) {
    Row(
        modifier = Modifier.padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProfileStat(profile.follows?.toString() ?: "—", "关注")
        ProfileStat(profile.followers?.toString() ?: "—", "粉丝")
        ProfileStat(profile.level?.let { "Lv.$it" } ?: "—", "等级")
        ProfileStat(profile.listenSongs?.toString() ?: "—", "累计听歌")
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.42f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ListeningFootprintCard(footprint: ListeningFootprintUi) {
    val initialPeriod = if (footprint.week != null) ListeningPeriod.Week else ListeningPeriod.Month
    var selectedPeriod by rememberSaveable { mutableStateOf(initialPeriod) }
    val selectedData = when (selectedPeriod) {
        ListeningPeriod.Week -> footprint.week ?: footprint.month
        ListeningPeriod.Month -> footprint.month ?: footprint.week
    } ?: return

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.09f),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 252.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Rounded.Headphones, null, Modifier.size(18.dp))
                    Text(
                        text = if (selectedData.period == ListeningPeriod.Week) "本周听歌" else "本月听歌",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ListeningPeriodToggle(
                    selected = selectedData.period,
                    weekEnabled = footprint.week != null,
                    monthEnabled = footprint.month != null,
                    onSelect = { selectedPeriod = it },
                )
            }
            Text(
                text = formatListenDuration(selectedData.totalMinutes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            ListeningBars(selectedData)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ListeningMetric("${selectedData.activeDays} 天", "活跃天数")
                selectedData.todaySongCount?.let { ListeningMetric("$it 首", "今日收听") }
                if (selectedData.todaySongCount == null) {
                    footprint.weekInsight?.topStyle?.let { ListeningMetric(it, "本周偏好") }
                }
            }
            val insightText = listOfNotNull(
                footprint.weekInsight?.title,
                footprint.weekInsight?.topStyle,
                footprint.weekInsight?.topArtist,
            ).filter { it.isNotBlank() }.joinToString(" · ")
            Text(
                text = insightText.takeIf { it.isNotBlank() }?.let { "最近报告 · $it" }.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.56f),
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ListeningPeriodToggle(
    selected: ListeningPeriod,
    weekEnabled: Boolean,
    monthEnabled: Boolean,
    onSelect: (ListeningPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(2.dp),
    ) {
        PeriodButton("周", selected == ListeningPeriod.Week, weekEnabled) {
            onSelect(ListeningPeriod.Week)
        }
        PeriodButton("月", selected == ListeningPeriod.Month, monthEnabled) {
            onSelect(ListeningPeriod.Month)
        }
    }
}

@Composable
private fun PeriodButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ListeningBars(data: ListeningPeriodUi) {
    val values = data.dailyMinutes.ifEmpty { listOf(0) }
    val chartValues = if (data.period == ListeningPeriod.Month && values.size > 7) {
        values.chunked(7).map { week -> week.sum() }
    } else {
        values
    }
    val maxValue = chartValues.maxOrNull()?.coerceAtLeast(1) ?: 1
    val description = chartValues.joinToString(prefix = "听歌时长分布：") { "$it 分钟" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(top = 12.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        chartValues.forEach { value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((value.toFloat() / maxValue).coerceAtLeast(0.08f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFF958A), Color(0xFFFFD8BE)))),
                )
            }
        }
    }
}

@Composable
private fun ListeningMetric(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.42f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LibrarySummaryFallback(created: Int, collected: Int, albums: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.09f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(18.dp))
                Text("我的音乐", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ListeningMetric(created.toString(), "创建")
                ListeningMetric(collected.toString(), "收藏")
                ListeningMetric(albums.toString(), "专辑")
            }
        }
    }
}

@Composable
private fun LibraryHeading(state: LibraryContentUiState) {
    val greeting = remember { greetingForHour(LocalTime.now().hour) }
    Column {
        Text(
            text = "$greeting，继续听点什么？",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${state.createdCount + state.collectedCount} 个歌单 · ${state.albumCount} 张收藏专辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LibraryQuickActions(onEvent: (LibraryEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickActionCard(
            icon = Icons.Rounded.History,
            title = "最近播放",
            subtitle = "查看本机播放记录",
            prominent = true,
            onClick = { onEvent(LibraryEvent.OpenHistory) },
            modifier = Modifier.weight(1.45f),
        )
        QuickActionCard(
            icon = Icons.Rounded.Folder,
            title = "本地音乐",
            subtitle = "管理设备歌曲",
            onClick = { onEvent(LibraryEvent.OpenLocalMusic) },
            modifier = Modifier.weight(1f),
        )
        QuickActionCard(
            icon = Icons.Rounded.Download,
            title = "下载管理",
            subtitle = "离线音乐与任务",
            onClick = { onEvent(LibraryEvent.OpenDownloads) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (prominent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (prominent) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(96.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (prominent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun LibrarySectionHeader(
    state: LibraryContentUiState,
    onEvent: (LibraryEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Text(
            text = "我的音乐收藏",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SectionButton("创建 ${state.createdCount}", state.section == LibrarySection.Created) {
                onEvent(LibraryEvent.SelectSection(LibrarySection.Created))
            }
            SectionButton("收藏 ${state.collectedCount}", state.section == LibrarySection.Collected) {
                onEvent(LibraryEvent.SelectSection(LibrarySection.Collected))
            }
            SectionButton("专辑 ${state.albumCount}", state.section == LibrarySection.Albums) {
                onEvent(LibraryEvent.SelectSection(LibrarySection.Albums))
            }
        }
    }
}

internal fun greetingForHour(hour: Int): String = when (hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

@Composable
private fun SectionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
        )
    }
}

private fun LibraryContentUiState.assetsForSelectedSection(): List<LibraryAsset> = when (section) {
    LibrarySection.Created -> createdPlaylists.map {
        LibraryAsset(it.id, it.title, it.cover, "${it.count} 首 · ${formatPlayCount(it.playCount)}")
    }
    LibrarySection.Collected -> collectedPlaylists.map {
        LibraryAsset(it.id, it.title, it.cover, "${it.count} 首 · ${it.authorName}")
    }
    LibrarySection.Albums -> albums.map {
        LibraryAsset(
            id = it.id.toString(),
            title = it.title,
            cover = it.cover,
            subtitle = "${it.size} 首 · ${it.artist.joinToString { artist -> artist.name }}",
        )
    }
}

private fun formatListenDuration(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours == 0 -> "$minutes 分钟"
        minutes == 0 -> "$hours 小时"
        else -> "$hours 小时 $minutes 分"
    }
}

private fun formatPlayCount(playCount: Long): String = when {
    playCount >= 100_000_000 -> "${ceil(playCount / 10_000_000.0) / 10} 亿次播放"
    playCount >= 10_000 -> "${ceil(playCount / 1_000.0) / 10} 万次播放"
    playCount > 0 -> "$playCount 次播放"
    else -> "暂无播放记录"
}

private data class LibraryAsset(
    val id: String,
    val title: String,
    val cover: String,
    val subtitle: String,
)

@Composable
private fun LibraryTabletEmptyState(section: LibrarySection) {
    val label = when (section) {
        LibrarySection.Created -> "暂无创建歌单"
        LibrarySection.Collected -> "暂无收藏歌单"
        LibrarySection.Albums -> "暂无收藏专辑"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
