package com.ljyh.mei.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.di.dao.SongDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Scans Android's audio collection and keeps imported files separate from app downloads. */
class LocalMusicScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val songDao: SongDao,
) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        val existing = songDao.getSongsBySourceOnce(SourceType.LOCAL).associateBy { it.id }
        val downloadedPaths = songDao.getLocalSongs().first()
            .asSequence()
            .filter { it.sourceType == SourceType.DOWNLOAD }
            .mapNotNull { it.path }
            .toHashSet()
        val now = System.currentTimeMillis()
        val found = ArrayList<Song>()
        val cursor = context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        ) ?: error("无法读取系统音乐库")

        cursor.use { rows ->
            val idColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val nameColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val folderColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (rows.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val mediaId = rows.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, mediaId).toString()
                if (uri in downloadedPaths) continue

                val songId = "local_-$mediaId"
                val old = existing[songId]
                val filename = rows.getString(nameColumn).orEmpty()
                val matched = old?.matchedNeteaseId != null
                val title = old?.title.takeIf { matched }
                    ?: rows.getString(titleColumn).usableTag()
                    ?: old?.title
                    ?: filename.substringBeforeLast('.', filename)
                val artist = old?.artist.takeIf { matched }
                    ?: rows.getString(artistColumn).usableTag()
                    ?.let(::listOf)
                    ?: old?.artist.orEmpty()
                val album = old?.album.takeIf { matched }
                    ?: rows.getString(albumColumn).usableTag() ?: old?.album.orEmpty()
                val folder = rows.getString(folderColumn).orEmpty().trimEnd('/')
                    .ifBlank { "其他" }

                found += Song(
                    id = songId,
                    title = title.ifBlank { "未知歌曲" },
                    artist = artist,
                    album = album,
                    cover = old?.cover.orEmpty(),
                    duration = rows.getLong(durationColumn).coerceAtLeast(0L) / 1000L,
                    path = uri,
                    sourceType = SourceType.LOCAL,
                    fileSize = rows.getLong(sizeColumn).coerceAtLeast(0L),
                    fileFormat = filename.substringAfterLast('.', "").lowercase().ifBlank { null },
                    folderPath = folder,
                    matchedNeteaseId = old?.matchedNeteaseId,
                    addedAt = old?.addedAt ?: now,
                    updatedAt = now,
                )
            }
        }
        songDao.synchronizeScannedSongs(found)
        found.size
    }

    private fun String?.usableTag(): String? = this?.trim()
        ?.takeIf { it.isNotEmpty() && it != "<unknown>" && it != "未知" }
}
