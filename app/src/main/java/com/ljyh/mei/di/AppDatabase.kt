package com.ljyh.mei.di

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ljyh.mei.data.model.room.AlbumArtistCrossRef
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.CachedLyric
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.PlaybackHistory
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.ScanFolder
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.di.dao.AlbumsDao
import com.ljyh.mei.di.dao.CachedLyricDao
import com.ljyh.mei.di.dao.ColorDao
import com.ljyh.mei.di.dao.DownloadDao
import com.ljyh.mei.di.dao.HistoryDao
import com.ljyh.mei.di.dao.LikeDao
import com.ljyh.mei.di.dao.PlaylistDao
import com.ljyh.mei.di.dao.PlaylistSongCrossRefDao
import com.ljyh.mei.di.dao.QQSongDao
import com.ljyh.mei.di.dao.ScanFolderDao
import com.ljyh.mei.di.dao.SongDao

@Database(
    entities = [
        CacheColor::class, Song::class, Like::class, QQSong::class, Playlist::class,
        PlaybackHistory::class, AlbumEntity::class, ArtistEntity::class, AlbumArtistCrossRef::class,
        CachedLyric::class, DownloadTask::class, PlaylistSongCrossRef::class, ScanFolder::class
    ],
    version = 13
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
    abstract fun songDao(): SongDao
    abstract fun likeDao(): LikeDao
    abstract fun qqSongDao(): QQSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun AlbumsDao(): AlbumsDao
    abstract fun cachedLyricDao(): CachedLyricDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playlistSongCrossRefDao(): PlaylistSongCrossRefDao
    abstract fun scanFolderDao(): ScanFolderDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_lyric (
                        songId TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL,
                        translation TEXT, isVerbatim INTEGER NOT NULL DEFAULT 0,
                        isPureMusic INTEGER NOT NULL DEFAULT 0,
                        sourceName TEXT NOT NULL DEFAULT 'Empty',
                        parserType TEXT NOT NULL DEFAULT 'LRC',
                        aiProcessed INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS download_task (
                        songId TEXT NOT NULL PRIMARY KEY, url TEXT NOT NULL DEFAULT '',
                        fileName TEXT NOT NULL DEFAULT '', fileType TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING', progress INTEGER NOT NULL DEFAULT 0,
                        songTitle TEXT NOT NULL DEFAULT '', songArtist TEXT NOT NULL DEFAULT '',
                        songAlbum TEXT NOT NULL DEFAULT '', songCover TEXT NOT NULL DEFAULT '',
                        quality TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE song ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'STREAM'")
                db.execSQL("ALTER TABLE song ADD COLUMN fileHash TEXT")
                db.execSQL("ALTER TABLE song ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE song ADD COLUMN fileFormat TEXT")
                db.execSQL("ALTER TABLE song ADD COLUMN bitrate INTEGER")
                db.execSQL("ALTER TABLE song ADD COLUMN sampleRate INTEGER")
                db.execSQL("ALTER TABLE song ADD COLUMN folderPath TEXT")
                db.execSQL("ALTER TABLE song ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE song ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlist ADD COLUMN type TEXT NOT NULL DEFAULT 'NETEAST'")
                db.execSQL("ALTER TABLE playlist ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE playlist ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlist ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_song_cross_ref (playlistId TEXT NOT NULL, songId TEXT NOT NULL, sortOrder INTEGER NOT NULL DEFAULT 0, addedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(playlistId, songId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pscr_playlist ON playlist_song_cross_ref(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pscr_song ON playlist_song_cross_ref(songId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS scan_folder (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, path TEXT NOT NULL, label TEXT, isDefault INTEGER NOT NULL DEFAULT 0, enabled INTEGER NOT NULL DEFAULT 1, lastScanAt INTEGER, songCount INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Clean up URL-encoded data from broken SAF scanner
                db.execSQL("DELETE FROM scan_folder")
                db.execSQL("DELETE FROM playlist_song_cross_ref WHERE playlistId LIKE 'folder_%'")
                db.execSQL("DELETE FROM playlist WHERE type = 'FOLDER'")
                db.execSQL("DELETE FROM song WHERE sourceType = 'LOCAL'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove all SAF-based content:// data, keep only real file paths
                db.execSQL("DELETE FROM song WHERE path LIKE 'content://%'")
                db.execSQL("DELETE FROM scan_folder WHERE path LIKE 'content://%'")
                db.execSQL("DELETE FROM playlist_song_cross_ref WHERE playlistId LIKE 'folder_%'")
                db.execSQL("DELETE FROM playlist WHERE type = 'FOLDER'")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
