package com.ljyh.mei.data.repository.artwork

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.data.network.AppleMusicCatalogClient
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/** Optional presentation enrichment. Playback and the artist's catalogue remain owned by NetEase. */
@Singleton
class AppleArtistArtworkRepository @Inject constructor(
    private val catalog: AppleMusicCatalogClient,
    @param:ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("appleArtistArtwork.v3")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cache = AppleArtistArtworkCache(
        read = { context.dataStore.data.first()[key]?.let { json.decodeFromString<Map<String, ArtistArtworkEntry>>(it) }.orEmpty() },
        write = { entries -> context.dataStore.edit { it[key] = json.encodeToString(entries) } },
    )

    suspend fun resolve(identity: String, name: String, aliases: List<String>): AppleArtistArtwork? = withContext(Dispatchers.IO) {
        val names = (listOf(name) + aliases).map(String::trim).filter(String::isNotBlank).distinctBy(::normalizedArtistName).take(6)
        if (names.isEmpty()) return@withContext null
        val cacheKey = identity + ":" + names.map(::normalizedArtistName).sorted().joinToString("|")
        cache.get(cacheKey) { find(names) }
    }

    private suspend fun find(names: List<String>): AppleArtistArtwork? {
        // A Latin alias often disambiguates classical composers and translated Chinese names.
        val queries = names.sortedByDescending { name -> name.any { it in 'A'..'Z' || it in 'a'..'z' } }.take(2)
        for (region in listOf("us", "cn")) for (query in queries) {
            val url = "https://amp-api.music.apple.com/v1/catalog/$region/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", query).addQueryParameter("types", "artists")
                .addQueryParameter("limit", "10").addQueryParameter("l", "en-US").build()
            val response = json.parseToJsonElement(catalog.get(url.toString()).toString()).jsonObject
            val candidates = response.obj("results")?.obj("artists")?.get("data") as? JsonArray ?: continue
            val id = matchAppleArtist(candidates, names) ?: continue
            val detail = catalog.get("https://amp-api.music.apple.com/v1/catalog/$region/artists/$id?extend=editorialArtwork,editorialVideo")
            val item = (json.parseToJsonElement(detail.toString()).jsonObject["data"] as? JsonArray)?.firstOrNull() as? JsonObject ?: continue
            val artwork = parseAppleArtistArtwork(item) ?: continue
            if (normalizedArtistName(artwork.name) !in names.map(::normalizedArtistName)) continue
            return artwork
        }
        return null
    }
}
