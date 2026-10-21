package com.ljyh.mei.playback.source.lx

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LxScriptStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloader: LxScriptDownloader,
) {
    private val key = stringPreferencesKey("lx_sources_v1")
    private val lock = Mutex()
    private val directory get() = File(context.noBackupFilesDir, "lx-sources").apply { mkdirs() }
    val scripts: Flow<List<LxScript>> = context.dataStore.data.map {
        it[key]?.let { value -> lxJson.decodeFromString<List<LxScript>>(value) } ?: emptyList()
    }.distinctUntilChanged()
    private val _status = MutableStateFlow<Map<String, String>>(emptyMap())
    val status = _status.asStateFlow()
    suspend fun snapshot(): List<LxScript> = scripts.first()
    fun status(id: String, message: String) { _status.update { it + (id to message) } }
    fun runtime(): LxRuntime = LxRuntime(context.assets.open("lx/preload.js").bufferedReader().use { it.readText() })

    suspend fun import(uri: Uri): LxScript = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(MAX_SCRIPT_BYTES + 1) }
            ?: error("无法打开文件")
        val filename = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "自定义音源"
        importBytes(bytes, filename)
    }

    suspend fun importUrl(input: String): LxScript = withContext(Dispatchers.IO) {
        val downloaded = downloader.download(input)
        importBytes(downloaded.bytes, downloaded.filename)
    }

    private suspend fun importBytes(bytes: ByteArray, filename: String): LxScript {
        if (bytes.size !in 1..MAX_SCRIPT_BYTES) throw LxScriptImportException("脚本必须为 1 MB 以内的 UTF-8 文件")
        val code = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw LxScriptImportException("脚本不是有效的 UTF-8 文本")
        }
        val id = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        val header = code.take(16_384).trimStart().takeIf { it.startsWith("/*") }?.substringBefore("*/").orEmpty()
        fun tag(name: String): String = Regex("(?m)^\\s*\\*?\\s*@${name}\\s+([^\\r\\n]+)")
            .find(header)?.groupValues?.get(1)?.trim()?.take(512).orEmpty()
        val script = LxScript(id, tag("name").ifBlank { filename.removeSuffix(".js") }.take(128),
            tag("version"), tag("author"), tag("description"), tag("homepage"))
        val capabilities = runtime().use(script, code) { sources }
        val imported = script.copy(sources = capabilities)
        currentCoroutineContext().ensureActive()
        lock.withLock {
            currentCoroutineContext().ensureActive()
            val current = snapshot()
            val existing = current.firstOrNull { it.id == id }
            require(existing != null || current.size < 20) { "最多导入 20 个音源，请先删除不再使用的音源" }
            val file = AtomicFile(File(directory, "$id.js"))
            val hadFile = file.baseFile.exists()
            val output = file.startWrite()
            try { output.write(code.toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
            catch (e: Exception) { file.failWrite(output); throw e }
            val updated = if (existing == null) current + imported else current.map {
                if (it.id == id) imported.copy(enabled = it.enabled) else it
            }
            try { context.dataStore.edit { it[key] = lxJson.encodeToString(updated) } }
            catch (e: Exception) { if (!hadFile) file.delete(); throw e }
        }
        status(id, "初始化成功")
        return imported
    }

    suspend fun code(script: LxScript): String = withContext(Dispatchers.IO) {
        require(script.id.matches(Regex("[a-f0-9]{64}")))
        File(directory, "${script.id}.js").inputStream().use {
            val bytes = it.readLimited(MAX_SCRIPT_BYTES + 1)
            require(bytes.size <= MAX_SCRIPT_BYTES)
            bytes.toString(Charsets.UTF_8)
        }
    }

    suspend fun enable(id: String, enabled: Boolean) = update { list -> list.map { if (it.id == id) it.copy(enabled = enabled) else it } }
    suspend fun move(id: String, offset: Int) = update { list ->
        list.toMutableList().apply {
            val from = indexOfFirst { it.id == id }
            if (from >= 0) { val item = removeAt(from); add((from + offset).coerceIn(0, size), item) }
        }
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        update { it.filterNot { s -> s.id == id } }
        if (id.matches(Regex("[a-f0-9]{64}"))) AtomicFile(File(directory, "$id.js")).delete()
        _status.update { it - id }
    }
    private suspend fun update(change: (List<LxScript>) -> List<LxScript>) = lock.withLock {
        context.dataStore.edit { prefs ->
            val list = prefs[key]?.let { lxJson.decodeFromString<List<LxScript>>(it) } ?: emptyList()
            prefs[key] = lxJson.encodeToString(change(list))
        }
    }
    companion object { const val MAX_SCRIPT_BYTES = 1024 * 1024 }
}
