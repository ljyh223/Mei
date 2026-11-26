package com.ljyh.mei.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.ljyh.mei.data.model.room.AlbumSubscription
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class AlbumSubscriptionMigrationTest {
    @Test fun version19UpgradePreservesExistingDataAndPersistsAccountScopedStates() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "album-migration-${UUID.randomUUID()}"
        // All pre-existing tables are unchanged in v20. Recreate v19 without the new table or
        // Room identity so reopening through Room validates the entire migrated schema.
        val fresh = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val oldSchema = try {
            fresh.openHelper.writableDatabase.query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT IN ('album_subscription', 'room_master_table', 'android_metadata') AND name NOT LIKE 'sqlite_%'").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
        } finally { fresh.close() }
        val old = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(19) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    oldSchema.forEach(db::execSQL)
                    db.execSQL("INSERT INTO albums (albumId, name, cover, publishTime, songCount) VALUES (7, 'existing album', 'cover', 1, 12)")
                    db.execSQL("INSERT INTO `like` (userId, id) VALUES ('123', '42')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("unexpected upgrade")
            }).build())
        try {
            old.writableDatabase
            old.close()
            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .allowMainThreadQueries().addMigrations(*AppDatabase.MIGRATIONS).build()
            val upgraded = open()
            try {
                assertEquals(20, upgraded.openHelper.writableDatabase.version)
                assertTrue(upgraded.AlbumsDao().existsAlbum(7))
                assertTrue(upgraded.likeDao().isLiked("123", "42"))
                assertNull(upgraded.albumSubscriptionDao().get("123", 7))
                upgraded.albumSubscriptionDao().upsert(AlbumSubscription("123", 7, true, 1))
                upgraded.albumSubscriptionDao().upsert(AlbumSubscription("456", 7, false, 2))
                val migrations = RoomDatabase.MigrationContainer().apply { addMigrations(*AppDatabase.MIGRATIONS) }
                for (version in 8..19) {
                    assertNotNull("missing upgrade from $version", migrations.findMigrationPath(version, 20))
                }
            } finally { upgraded.close() }
            val reopened = open()
            try {
                assertEquals(true, reopened.albumSubscriptionDao().get("123", 7)?.subscribed)
                assertEquals(false, reopened.albumSubscriptionDao().get("456", 7)?.subscribed)
                assertTrue(reopened.AlbumsDao().existsAlbum(7))
            } finally { reopened.close() }
        } finally { old.close(); context.deleteDatabase(name) }
    }
}
