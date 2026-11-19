package com.ljyh.mei.playback.source.musicfree

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.BuildConfig
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.playback.source.lx.LxScriptDownloader
import com.ljyh.mei.playback.source.lx.readLimited
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicFreeStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloader: LxScriptDownloader,
    private val songs: SongRepository,
) {
    private val key = stringPreferencesKey("musicfree_plugins_v1")
    private val lock = Mutex()
    private val resolutionLocks = Array(16) { Mutex() }
    private val directory get() = File(context.noBackupFilesDir, "musicfree").apply { mkdirs() }
    private val tracksDirectory get() = File(directory, "tracks").apply { mkdirs() }
    private val audioCache = ConcurrentHashMap<String, Pair<Long, MusicFreeAudio>>()
    val plugins: Flow<List<MusicFreePlugin>> = context.dataStore.data.map {
        it[key]?.let { value -> musicFreeJson.decodeFromString<List<MusicFreePlugin>>(value) } ?: emptyList()
    }.distinctUntilChanged()

    private val runtime by lazy {
        fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
        MusicFreeRuntime(asset("lx/preload.js"), asset("musicfree/modules.js"), asset("musicfree/preload.js"), BuildConfig.VERSION_NAME)
    }

    suspend fun snapshot(): List<MusicFreePlugin> = plugins.first()

    suspend fun importFile(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(MAX_SCRIPT_BYTES + 1) }
            ?: error("无法打开插件文件")
        importBytes(bytes)
    }

    suspend fun importUrl(url: String) = withContext(Dispatchers.IO) {
        importBytes(downloader.download(url).bytes)
    }

    suspend fun readImportUrl(url: String): MusicFreeImportDocument = withContext(Dispatchers.IO) {
        val document = downloader.download(url, allowJson = true)
        musicFreeImportDocument(document.bytes, document.url)
    }

    /** Each plugin commits separately; a failed download must not discard other successful imports. */
    suspend fun importSubscription(
        entries: List<MusicFreeSubscriptionEntry>,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): MusicFreeBatchImportResult {
        val selection = entries.distinctBy { it.url }
        if (selection.isEmpty() || selection.size > MAX_PLUGINS) throw MusicFreeImportException("每次请选择 1 至 20 个插件")
        val imported = mutableListOf<MusicFreeSubscriptionEntry>()
        val failures = mutableListOf<MusicFreeImportFailure>()
        selection.forEachIndexed { index, entry ->
            currentCoroutineContext().ensureActive()
            try {
                importUrl(entry.url)
                imported += entry
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                failures += MusicFreeImportFailure(entry, musicFreeImportError(timeout))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failures += MusicFreeImportFailure(entry, musicFreeImportError(error))
            }
            onProgress(index + 1, selection.size)
        }
        return MusicFreeBatchImportResult(imported, failures)
    }

    internal suspend fun importBytes(bytes: ByteArray): MusicFreePlugin = withContext(Dispatchers.IO) {
        require(bytes.size in 1..MAX_SCRIPT_BYTES) { "插件必须为 1 MB 以内的 UTF-8 文件" }
        val code = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        val id = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        val inspected = runtime.use(code) { inspect() }
        val plugin = musicFreeJson.decodeFromJsonElement<MusicFreePlugin>(
            JsonObject(inspected + ("id" to JsonPrimitive(id)))
        )
        currentCoroutineContext().ensureActive()
        lock.withLock {
            val current = snapshot()
            val existing = current.firstOrNull { it.id == id }
            if (existing == null && current.size >= MAX_PLUGINS) throw MusicFreeImportException("最多导入 20 个插件，请先删除不需要的插件")
            write(AtomicFile(File(directory, "$id.js")), code.toByteArray())
            val updated = plugin.copy(enabled = existing?.enabled ?: true, variables = existing?.variables.orEmpty())
            context.dataStore.edit { prefs ->
                prefs[key] = musicFreeJson.encodeToString(
                    if (existing == null) current + updated else current.map { if (it.id == id) updated else it }
                )
            }
        }
        plugin
    }

    suspend fun enable(id: String, enabled: Boolean) = update(id) { it.copy(enabled = enabled) }

    suspend fun configure(id: String, variables: Map<String, String>) = update(id) { plugin ->
        plugin.copy(variables = variables.filterKeys { name -> plugin.userVariables.any { it.key == name } }
            .mapValues { it.value.take(8192) })
    }

    private suspend fun update(id: String, change: (MusicFreePlugin) -> MusicFreePlugin) = lock.withLock {
        context.dataStore.edit { prefs ->
            val list = prefs[key]?.let { musicFreeJson.decodeFromString<List<MusicFreePlugin>>(it) }.orEmpty()
            prefs[key] = musicFreeJson.encodeToString(list.map { if (it.id == id) change(it) else it })
        }
        audioCache.clear()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            context.dataStore.edit { prefs ->
                val list = prefs[key]?.let { musicFreeJson.decodeFromString<List<MusicFreePlugin>>(it) }.orEmpty()
                prefs[key] = musicFreeJson.encodeToString(list.filterNot { it.id == id })
            }
            if (id.matches(Regex("[a-f0-9]{64}"))) AtomicFile(File(directory, "$id.js")).delete()
            audioCache.clear()
        }
    }

    suspend fun check(id: String) {
        val plugin = snapshot().firstOrNull { it.id == id } ?: error("插件已删除")
        runtime.use(code(plugin), plugin.variables) { inspect() }
    }

    suspend fun search(pluginId: String, query: String, page: Int): MusicFreeSearchPage {
        require(page > 0 && query.isNotBlank())
        val plugin = enabledPlugin(pluginId)
        require(plugin.canSearch) { "此插件不支持歌曲搜索" }
        return runtime.use(code(plugin), plugin.variables) {
            musicFreePage(plugin.id, call("search", buildJsonArray {
                add(query); add(page); add("music")
            }))
        }
    }

    /** Save the original plugin fields before queuing, so playback survives process restoration. */
    suspend fun prepareQueue(tracks: List<MusicFreeTrack>) = withContext(Dispatchers.IO) {
        lock.withLock {
            val enabledIds = snapshot().filter { it.enabled }.map { it.id }.toSet()
            require(tracks.all { it.pluginId in enabledIds }) { "插件已停用或删除" }
            for (track in tracks) {
                currentCoroutineContext().ensureActive()
                val existing = trackFor(track.mediaId.toString())
                require(existing == null || (existing.pluginId == track.pluginId && existing.originalId == track.originalId)) {
                    "歌曲标识冲突，请重新搜索"
                }
                val data = musicFreeJson.encodeToString(track).toByteArray()
                require(data.size <= MAX_TRACK_BYTES) { "插件返回的歌曲信息过大" }
                write(AtomicFile(File(tracksDirectory, "${track.mediaId}.json")), data)
                val metadata = track.metadata()
                songs.insertSong(Song(
                    id = metadata.id.toString(), title = metadata.title, artist = metadata.artists.map { it.name },
                    album = metadata.album.title, cover = metadata.coverUrl, duration = metadata.duration / 1000,
                ))
            }
        }
    }

    suspend fun trackFor(mediaId: String): MusicFreeTrack? = withContext(Dispatchers.IO) {
        if (mediaId.toLongOrNull()?.let { it < 0 } != true) return@withContext null
        val file = File(tracksDirectory, "$mediaId.json")
        if (!file.exists()) return@withContext null
        file.inputStream().use {
            val bytes = it.readLimited(MAX_TRACK_BYTES + 1)
            require(bytes.size <= MAX_TRACK_BYTES)
            musicFreeJson.decodeFromString<MusicFreeTrack>(bytes.toString(Charsets.UTF_8))
                .also { track -> require(track.mediaId.toString() == mediaId) }
        }
    }

    suspend fun resolve(mediaId: String, requestedQuality: String): MusicFreeAudio {
        val track = trackFor(mediaId) ?: error("MusicFree 歌曲信息已失效，请重新搜索")
        val plugin = enabledPlugin(track.pluginId)
        val quality = musicFreeQuality(requestedQuality)
        val cacheKey = "$mediaId|$quality"
        return resolutionLocks[Math.floorMod(cacheKey.hashCode(), resolutionLocks.size)].withLock {
            audioCache[cacheKey]?.takeIf { it.first > System.currentTimeMillis() }?.let { return@withLock it.second }
            val result = runtime.use(code(plugin), plugin.variables) {
                call("getMediaSource", buildJsonArray { add(track.data); add(quality) })
            } as? JsonObject ?: error("插件未返回播放地址")
            val url = result.text("url").toHttpUrlOrNull() ?: error("插件未返回有效的 HTTP 播放地址")
            val headers = (result["headers"] as? JsonObject)?.mapValues {
                (it.value as? JsonPrimitive)?.contentOrNull ?: error("插件返回的请求头无效")
            }.orEmpty().toMutableMap()
            result.text("userAgent").takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
            require(headers.size <= 64 && headers.all { (key, value) ->
                key.length <= 256 && value.length <= 8192 && '\r' !in value && '\n' !in value
            }) { "插件返回的请求头无效" }
            check(enabledPlugin(plugin.id) == plugin) { "插件配置已变化，请重试" }
            val audio = MusicFreeAudio(url.toString(), headers, result.text("quality").ifBlank { quality })
            if (audioCache.size > 128) audioCache.clear()
            audioCache[cacheKey] = System.currentTimeMillis() + 60_000 to audio
            audio
        }
    }

    fun invalidate(mediaId: String) { audioCache.keys.removeIf { it.startsWith("$mediaId|") } }

    private suspend fun enabledPlugin(id: String): MusicFreePlugin =
        snapshot().firstOrNull { it.id == id && it.enabled } ?: error("MusicFree 插件已停用或删除")

    private suspend fun code(plugin: MusicFreePlugin): String = withContext(Dispatchers.IO) {
        require(plugin.id.matches(Regex("[a-f0-9]{64}")))
        File(directory, "${plugin.id}.js").inputStream().use {
            val bytes = it.readLimited(MAX_SCRIPT_BYTES + 1)
            require(bytes.size <= MAX_SCRIPT_BYTES)
            bytes.toString(Charsets.UTF_8)
        }
    }

    private fun write(file: AtomicFile, bytes: ByteArray) {
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
    }

    companion object {
        const val MAX_PLUGINS = 20
        private const val MAX_SCRIPT_BYTES = 1024 * 1024
        private const val MAX_TRACK_BYTES = 256 * 1024
    }
}
