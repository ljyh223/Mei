package com.ljyh.mei.ui.screen.search

import android.app.Application
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.data.network.NeteaseHttp
import com.ljyh.mei.data.network.XeApiClient
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.repository.SearchRepository
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.playback.source.lx.LxScriptDownloader
import com.ljyh.mei.playback.source.musicfree.MusicFreeStore
import com.ljyh.mei.utils.preferences.dataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class SearchMusicFreeTest {
    @Test fun standardSearchPaginatesPluginResultsAndPersistsOriginalObjectsBeforeQueueActions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val preferences = stringPreferencesKey("musicfree_plugins_v1")
        context.dataStore.edit { it.remove(preferences) }
        File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val store = MusicFreeStore(context, LxScriptDownloader(), SongRepository(db.songDao()))
        val viewModels = ViewModelStore()
        var catalogueCalls = 0
        // There is no upstream transport in this test: every catalogue invocation fails locally.
        val client = HttpClient(MockEngine {
            catalogueCalls++
            error("Catalogue requests are disabled in this test")
        })
        val json = Json { ignoreUnknownKeys = true }
        val http = NeteaseHttp(client, json)
        val api = ApiService(http, XeApiClient(context, http, json))
        try {
            val plugin = store.importBytes("""
                module.exports={platform:'Search fixture',supportedSearchType:['music'],
                  search(query,page){return {isEnd:page>=2,data:[{id:'item:'+page,title:query+page,
                    artist:'Artist',album:'Album',opaque:{required:true}}]}},
                  getMediaSource(song){if(!song.opaque.required)throw new Error('missing original');
                    return {url:'https://example.com/'+song.id}}
                };
            """.trimIndent().toByteArray())
            val saved = SavedStateHandle(mapOf("source" to plugin.id))
            val vm = SearchViewModel(SearchRepository(api), store, saved)
            viewModels.put("search", vm)
            await { vm.sources.value.size == 1 }
            vm.onSearchInit("Song", SearchType.Song.type)
            await { !vm.pageState.value.loading }
            assertFalse(vm.pageState.value.error)
            assertEquals("Song1", vm.pageState.value.data!!.songs.single().title)
            assertTrue(vm.pageState.value.data!!.hasMore)
            vm.loadMore()
            await { !vm.pageState.value.appending }
            val songs = vm.pageState.value.data!!.songs
            assertEquals(listOf("Song1", "Song2"), songs.map { it.title })
            assertTrue(songs.all { it.id < 0 && it.neteaseId == 0L })
            assertFalse(vm.pageState.value.data!!.hasMore)
            var prepared = false
            vm.prepareSongs(songs) { prepared = true }
            await { prepared }
            songs.forEach { song ->
                assertNotNull(store.trackFor(song.id.toString()))
                assertTrue(store.resolve(song.id.toString(), "standard").url.startsWith("https://example.com/item:"))
            }
            assertEquals(0, catalogueCalls)
            store.enable(plugin.id, false)
            await { vm.currentSource.value == null && !vm.pageState.value.loading }
            assertNull(saved.get<String>("source"))
            assertEquals(SearchType.Song, vm.currentTab.value)
            assertTrue(vm.pageState.value.error)
            assertEquals(1, catalogueCalls)
        } finally {
            viewModels.clear()
            client.close()
            db.close()
            context.dataStore.edit { it.remove(preferences) }
            File(context.noBackupFilesDir, "musicfree").deleteRecursively()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(10_000) {
        while (!condition()) { shadowOf(Looper.getMainLooper()).idle(); delay(10) }
    }
}
