package com.ljyh.mei.playback.source.musicfree

import android.util.AtomicFile
import com.ljyh.mei.playback.source.lx.readLimited
import java.io.File
import java.io.FileNotFoundException

/** File writes and eviction are serialized by MusicFreeStore. Reads see the last committed file. */
internal class MusicFreeTrackCache(
    private val directory: File,
    private val maxBytes: Long = MAX_CACHE_BYTES,
    private val maxTracks: Int = 2048,
) {
    fun read(mediaId: String, touch: Boolean = true): MusicFreeTrack? {
        val file = file(mediaId)
        val track = try {
            file.inputStream().use { input ->
                val bytes = input.readLimited(MAX_TRACK_BYTES + 1)
                require(bytes.size <= MAX_TRACK_BYTES)
                musicFreeJson.decodeFromString<MusicFreeTrack>(bytes.decodeToString())
                    .also { require(it.mediaId.toString() == mediaId) }
            }
        } catch (error: FileNotFoundException) {
            if (file.exists()) throw error
            return null
        }
        if (touch) file.setLastModified(System.currentTimeMillis())
        return track
    }

    fun writeBatch(records: Map<String, ByteArray>, retained: Set<String>) {
        val existing = entries().filterNot { it.nameWithoutExtension in records }
        val (protected, removable) = existing.partition { it.nameWithoutExtension in retained }
        val incomingBytes = records.values.sumOf { it.size.toLong() }
        require(protected.size + records.size <= maxTracks &&
            protected.sumOf(File::length) + incomingBytes <= maxBytes) { "MusicFree 队列超过歌曲资料缓存容量" }

        var count = existing.size + records.size
        var bytes = existing.sumOf(File::length) + incomingBytes
        for (file in removable.sortedBy(File::lastModified)) {
            if (count <= maxTracks && bytes <= maxBytes) break
            val size = file.length()
            AtomicFile(file).delete()
            check(!file.exists()) { "无法回收 MusicFree 歌曲资料" }
            count--
            bytes -= size
        }
        directory.mkdirs()
        for ((id, data) in records) {
            val target = AtomicFile(file(id))
            val output = target.startWrite()
            try {
                output.write(data)
                target.finishWrite(output)
            } catch (error: Exception) {
                target.failWrite(output)
                throw error
            }
        }
    }

    fun removePlugin(pluginId: String, retained: Set<String>) {
        for (file in entries()) {
            val id = file.nameWithoutExtension
            if (id !in retained && runCatching { read(id, touch = false)?.pluginId }.getOrNull() == pluginId) {
                AtomicFile(file).delete()
            }
        }
    }

    private fun file(mediaId: String) = File(directory, "$mediaId.json")

    private fun entries() = directory.listFiles { file ->
        file.isFile && file.extension == "json" && file.nameWithoutExtension.toLongOrNull()?.let { it < 0 } == true
    }.orEmpty()

    companion object {
        const val MAX_TRACK_BYTES = 256 * 1024
        const val MAX_CACHE_BYTES = 32L * 1024 * 1024
    }
}
