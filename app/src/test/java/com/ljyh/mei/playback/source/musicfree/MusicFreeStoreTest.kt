package com.ljyh.mei.playback.source.musicfree

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.playback.source.lx.LxScriptDownloader
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class MusicFreeStoreTest {
    private val pluginCode = """
        module.exports={
          platform:'Fixture',version:'1',supportedSearchType:['music'],
          userVariables:[{key:'key',name:'API Key'}],
          async search(query,page,type){
            return {isEnd:page>=2,data:[{id:'opaque:'+page,title:query,artist:'Artist',album:'Album',
              duration:123,artwork:'https://example.com/cover.jpg',opaque:{needed:true}}]};
          },
          async getMediaSource(song,quality){
            if(!song.opaque?.needed) throw new Error('lost metadata');
            return {url:'https://example.com/'+song.id+'?quality='+quality,
              headers:{Referer:'https://example.com'},userAgent:'Fixture-Agent'};
          }
        };
    """.trimIndent()

    @Test fun importedPluginSearchQueueAndAudioSurviveStoreRecreation() = runBlocking {
        fixture { store, recreate, songs ->
            val plugin = store.importBytes(pluginCode.toByteArray())
            assertTrue(plugin.canSearch)
            assertTrue(plugin.canResolve)
            val first = store.search(plugin.id, "Song", 1)
            assertFalse(first.isEnd)
            val second = store.search(plugin.id, "Song", 2)
            assertTrue(second.isEnd)
            store.prepareQueue(first.tracks + second.tracks)
            val id = first.tracks.single().mediaId.toString()
            assertEquals("Song", songs.getSong(id).first()?.title)
            val restored = recreate()
            assertEquals(first.tracks.single(), restored.trackFor(id))
            val audio = restored.resolve(id, "exhigh")
            assertEquals("standard", audio.quality)
            assertTrue(audio.url.contains("opaque:1"))
            assertEquals("Fixture-Agent", audio.headers["User-Agent"])
            assertEquals("https://example.com", audio.headers["Referer"])
        }
    }

    @Test fun disabledAndDeletedPluginsCannotResolveCachedAudio() = runBlocking {
        fixture { store, _, _ ->
            val plugin = store.importBytes(pluginCode.toByteArray())
            val track = store.search(plugin.id, "Song", 1).tracks.single()
            store.prepareQueue(listOf(track))
            store.resolve(track.mediaId.toString(), "standard")
            store.enable(plugin.id, false)
            assertTrue(runCatching { store.resolve(track.mediaId.toString(), "standard") }.isFailure)
            store.enable(plugin.id, true)
            store.resolve(track.mediaId.toString(), "standard")
            store.delete(plugin.id)
            assertTrue(runCatching { store.resolve(track.mediaId.toString(), "standard") }.isFailure)
            assertNotNull(store.trackFor(track.mediaId.toString()))
        }
    }

    @Test fun invalidImportKeepsExistingPluginsAndConfigurationAffectsRequests() = runBlocking {
        fixture { store, _, _ ->
            val plugin = store.importBytes(pluginCode.toByteArray())
            assertTrue(runCatching { store.importBytes("module.exports={}".toByteArray()) }.isFailure)
            assertEquals(listOf(plugin.id), store.snapshot().map { it.id })
            store.configure(plugin.id, mapOf("key" to "test-input", "unused" to "discard"))
            assertEquals(mapOf("key" to "test-input"), store.snapshot().single().variables)
            store.importBytes(pluginCode.toByteArray())
            assertEquals(1, store.snapshot().size)
            assertEquals(mapOf("key" to "test-input"), store.snapshot().single().variables)
        }
    }

    private suspend fun fixture(block: suspend (MusicFreeStore, () -> MusicFreeStore, SongRepository) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val preferences = stringPreferencesKey("musicfree_plugins_v1")
        context.dataStore.edit { it.remove(preferences) }
        java.io.File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val songs = SongRepository(db.songDao())
        val create = { MusicFreeStore(context, LxScriptDownloader(), songs) }
        try { block(create(), create, songs) }
        finally {
            db.close()
            context.dataStore.edit { it.remove(preferences) }
            java.io.File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        }
    }
}
