package com.ljyh.mei.data.repository

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.constants.AppleMusicWebTokenKey
import com.ljyh.mei.constants.AppleMusicWebTokenRefreshAtKey
import com.ljyh.mei.data.network.AppleMusicCatalogClient
import com.ljyh.mei.data.repository.artwork.AppleArtistArtworkRepository
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class AppleArtistArtworkRepositoryTest {
    private val search = """{"results":{"artists":{"data":[{"id":"123","attributes":{"name":"Richard Wagner"}}]}}}"""
    private val detail = """{"data":[{"id":"123","attributes":{"name":"Richard Wagner","artwork":{
        "url":"https://is1-ssl.mzstatic.com/image/thumb/portrait/{w}x{h}bb.jpg","width":1600,"height":1600}}}]}"""

    @Test fun matchesAnExplicitAliasAndRestoresArtworkWithoutAnotherCatalogRequest() = runBlocking {
        fixture { context ->
            val requests = mutableListOf<Request>()
            val catalog = catalog(context) { request ->
                requests += request
                if (request.url.encodedPath.endsWith("/search")) 200 to search else 200 to detail
            }
            val repo = AppleArtistArtworkRepository(catalog, context)
            val result = repo.resolve("netease-1", "理查德·瓦格纳", listOf("Richard Wagner"))!!
            assertEquals("123", result.id)
            assertEquals(2, requests.size)
            assertEquals("Richard Wagner", requests[0].url.queryParameter("term"))
            assertEquals("artists", requests[0].url.queryParameter("types"))
            assertEquals("editorialArtwork", requests[1].url.queryParameter("extend"))
            assertEquals("/v1/catalog/us/artists/123", requests[1].url.encodedPath)
            assertEquals(result, AppleArtistArtworkRepository(catalog, context)
                .resolve("netease-1", "理查德·瓦格纳", listOf("Richard Wagner")))
            assertEquals(2, requests.size)
        }
    }

    @Test fun missingMatchesAreBoundedAndCachedWhileTransportFailuresRemainRetryable() = runBlocking {
        fixture { context ->
            var requests = 0
            var offline = false
            val catalog = catalog(context) { request ->
                requests++
                if (offline) 503 to "{}"
                else if (request.url.encodedPath.endsWith("/search")) 200 to """{"results":{"artists":{"data":[]}}}"""
                else error("Missing names must never resolve another artist's portrait")
            }
            val repo = AppleArtistArtworkRepository(catalog, context)
            assertNull(repo.resolve("missing", "Unknown", listOf("未知艺人")))
            assertEquals(4, requests)
            assertNull(repo.resolve("missing", "Unknown", listOf("未知艺人")))
            assertEquals(4, requests)
            offline = true
            assertTrue(runCatching { repo.resolve("offline", "Other", emptyList()) }.isFailure)
            assertEquals(5, requests)
            offline = false
            assertNull(repo.resolve("offline", "Other", emptyList()))
            assertEquals(7, requests)
        }
    }

    private fun catalog(context: Application, respond: (Request) -> Pair<Int, String>): AppleMusicCatalogClient {
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            check(request.url.host == "amp-api.music.apple.com")
            val (code, body) = respond(request)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return AppleMusicCatalogClient(context, http, "https://music.apple.com", "https://amp-api.music.apple.com")
    }

    private suspend fun fixture(block: suspend (Application) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cache = stringPreferencesKey("appleArtistArtwork.v1")
        fun encode(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())
        val token = encode("{\"alg\":\"fixture\"}") + "." + encode("{\"exp\":${System.currentTimeMillis()/1000+86400}}") + ".fixture"
        context.dataStore.edit {
            it.remove(cache)
            it[AppleMusicWebTokenKey] = token
            it[AppleMusicWebTokenRefreshAtKey] = System.currentTimeMillis()+3600000
        }
        try { block(context) } finally {
            context.dataStore.edit { it.remove(cache); it.remove(AppleMusicWebTokenKey); it.remove(AppleMusicWebTokenRefreshAtKey) }
        }
    }
}
