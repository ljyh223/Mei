package com.ljyh.mei.ui.screen.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.response.ListeningTrack
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.screen.main.library.ListeningPeriodUi
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ListeningArchiveContent(state: PersonalCenterState, actions: PersonalCenterActions) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        ListeningOverview(state, actions)
        ListeningRanking(state.archive, actions)
        RecentListening(state.archive, actions)
        state.archive.refreshError?.let { InlineRetry(it, actions.onRefresh) }
    }
}

@Composable
private fun ListeningOverview(state: PersonalCenterState, actions: PersonalCenterActions) {
    var month by rememberSaveable { mutableStateOf(false) }
    val report = if (month) state.footprint.month else state.footprint.week
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeading("音乐足迹")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !month, onClick = { month = false }, label = { Text("本周") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(selected = month, onClick = { month = true }, label = { Text("本月") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        Text(report?.let { formatListeningDuration(it.totalMinutes) } ?: "—",
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(if (month) "本月收听时长" else "本周收听时长", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (report != null) {
            ListeningBars(report)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric("活跃天数", "${report.activeDays} 天")
                if (!month) report.todaySongCount?.let { SummaryMetric("今日收听", "$it 首") }
                SummaryMetric("累计听过", state.profile?.listenSongs?.let { "${formatProfileCount(it)} 首" } ?: "—")
            }
        } else {
            ReportStatus(state.reportStatus, actions)
            state.profile?.listenSongs?.let { SummaryMetric("累计听过", "${formatProfileCount(it)} 首") }
        }
        if (!month) state.footprint.weekInsight?.let { insight ->
            val text = listOfNotNull(insight.topStyle?.takeIf { it.isNotBlank() }?.let { "偏爱 $it" },
                insight.topArtist?.takeIf { it.isNotBlank() }?.let { "常听 $it" }).joinToString("  ·  ")
            if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (report != null && state.reportStatus is Resource.Error) ReportStatus(state.reportStatus, actions)
    }
}

@Composable
private fun ListeningBars(report: ListeningPeriodUi) {
    if (report.dailyMinutes.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val peak = report.dailyMinutes.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(90.dp).semantics {
            contentDescription = "每日收听时长，依次为" + report.dailyMinutes.joinToString { "$it 分钟" }
        }) {
            val step = size.width / report.dailyMinutes.size
            val barWidth = (step * 0.56f).coerceAtMost(22.dp.toPx())
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            report.dailyMinutes.forEachIndexed { index, minutes ->
                if (minutes > 0) {
                    val height = (size.height - 4.dp.toPx()) * minutes / peak
                    drawRoundRect(color, Offset(step * index + (step - barWidth) / 2, size.height - height),
                        Size(barWidth, height), CornerRadius(3.dp.toPx()))
                }
            }
        }
        Text("每日收听时长", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReportStatus(status: Resource<Unit>, actions: PersonalCenterActions) {
    when (status) {
        is Resource.Loading -> LoadingLine("正在同步音乐足迹")
        is Resource.Error -> InlineRetry(status.message, actions.onRefresh)
        is Resource.Success -> Text("这段时间还没有可展示的听歌足迹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListeningRanking(archive: ListeningArchiveState, actions: PersonalCenterActions) {
    var expanded by rememberSaveable(archive.allTime) { mutableStateOf(false) }
    val tracks = (archive.rank as? Resource.Success)?.data.orEmpty()
    val title = if (archive.allTime) "累计听歌排行" else "本周听歌排行"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("反复听的旋律", if (tracks.size > 5) (if (expanded) "收起" else "展开") else null) { expanded = !expanded }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!archive.allTime, { actions.onRankPeriod(false) }, { Text("最近一周") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(archive.allTime, { actions.onRankPeriod(true) }, { Text("所有时间") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        when (val result = archive.rank) {
            is Resource.Loading -> LoadingLine("正在同步听歌排行")
            is Resource.Error -> InlineRetry(result.message, actions.onRefresh)
            is Resource.Success -> {
                if (tracks.isEmpty()) EmptyMusicText("还没有可展示的听歌排行")
                tracks.take(if (expanded) 20 else 5).forEachIndexed { index, track ->
                    RankedTrackRow(track, index, onPlay = { actions.onPlay(tracks, index, title) })
                }
                if (tracks.isNotEmpty()) Text("排行来自网易云账号的听歌记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (tracks.isNotEmpty()) FrequentArtists(tracks, archive.allTime)
    }
}

@Composable
private fun RankedTrackRow(track: ListeningTrack, index: Int, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button,
        onClickLabel = "播放${track.song.title}", onClick = onPlay).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${index + 1}".padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp))
        SongArtwork(track.song, Modifier.size(52.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.song.artists.joinToString(" / ") { it.name }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.playCount != null) Text("${NumberFormat.getIntegerInstance().format(track.playCount)} 次",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

/** This is a projection of the returned song ranking, not an invented artist-play counter. */
internal fun frequentArtists(tracks: List<ListeningTrack>): List<MediaMetadata.Artist> = tracks
    .flatMap { track -> track.song.artists.filter { it.name.isNotBlank() }.distinctBy { it.id to it.name } }
    .groupBy { artist -> if (artist.id > 0) artist.id.toString() else artist.name }
    .values.sortedByDescending { it.size }.map { it.first() }.take(5)

@Composable
private fun FrequentArtists(tracks: List<ListeningTrack>, allTime: Boolean) {
    val artists = remember(tracks) { frequentArtists(tracks) }
    if (artists.isEmpty()) return
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("常听歌手")
        Text(if (allTime) "根据累计听歌排行整理" else "根据最近一周听歌排行整理", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        artists.forEach { artist ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Text(artist.name.take(1), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (!artist.picUrl.isNullOrBlank()) AsyncImage(artist.picUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Text(artist.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecentListening(archive: ListeningArchiveState, actions: PersonalCenterActions) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tracks = (archive.recent as? Resource.Success)?.data.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading("最近收听", if (tracks.size > 6) (if (expanded) "收起" else "更多") else null) { expanded = !expanded }
        when (val result = archive.recent) {
            is Resource.Loading -> LoadingLine("正在同步最近收听")
            is Resource.Error -> InlineRetry(result.message, actions.onRefresh)
            is Resource.Success -> {
                if (tracks.isEmpty()) EmptyMusicText("云端还没有最近收听记录")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(tracks.take(if (expanded) 100 else 6), key = { _, track -> track.song.id }) { index, track ->
                        Column(Modifier.width(116.dp).clickable(role = Role.Button,
                            onClickLabel = "播放${track.song.title}", onClick = { actions.onPlay(tracks, index, "最近收听") }),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SongArtwork(track.song, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(track.song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.song.artists.joinToString(" / ") { it.name }, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            track.playedAt?.let { Text(formatPlayedAt(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                if (tracks.isNotEmpty()) Text("网易云云端记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SongArtwork(song: MediaMetadata, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (song.coverUrl.isNotBlank()) AsyncImage(song.coverUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun SectionHeading(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingLine(label: String) {
    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyMusicText(text: String) {
    Text(text, Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InlineRetry(message: String, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

internal fun formatListeningDuration(minutes: Int): String {
    val value = minutes.coerceAtLeast(0)
    return if (value < 60) "$value 分钟" else "${value / 60} 小时 ${value % 60} 分钟"
}

private fun formatPlayedAt(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
