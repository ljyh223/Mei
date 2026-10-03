package com.ljyh.mei.playback

/** Reuses covers shared by songs in one download batch without keeping them indefinitely. */
internal class DownloadCoverCache(
    private val fetch: suspend (String) -> ByteArray?,
    private val maxEntries: Int = 8,
    private val maxBytes: Int = 4 * 1024 * 1024,
) {
    private val covers = LinkedHashMap<String, ByteArray>(maxEntries, 0.75f, true)
    private var bytesInCache = 0

    suspend fun get(url: String): ByteArray? {
        synchronized(covers) { covers[url] }?.let { return it }
        val downloaded = fetch(url)?.takeIf { it.isNotEmpty() } ?: return null
        if (downloaded.size > maxBytes) return downloaded

        synchronized(covers) {
            covers.put(url, downloaded)?.let { bytesInCache -= it.size }
            bytesInCache += downloaded.size
            while (covers.size > maxEntries || bytesInCache > maxBytes) {
                val oldest = covers.entries.first()
                bytesInCache -= oldest.value.size
                covers.remove(oldest.key)
            }
        }
        return downloaded
    }
}
