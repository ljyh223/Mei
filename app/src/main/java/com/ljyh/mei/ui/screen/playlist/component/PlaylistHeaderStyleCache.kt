package com.ljyh.mei.ui.screen.playlist.component

import com.ljyh.mei.ui.model.PlaylistHeaderStyle

/** A best-effort hint only; the next successful detail response remains authoritative. */
internal class PlaylistHeaderStyleCache(private val capacity: Int = 64) {
    private val values = object : LinkedHashMap<Long, PlaylistHeaderStyle>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, PlaylistHeaderStyle>): Boolean =
            size > capacity
    }

    @Synchronized
    fun get(id: Long): PlaylistHeaderStyle? = values[id]

    @Synchronized
    fun put(id: Long, style: PlaylistHeaderStyle) {
        values[id] = style
    }
}

internal val playlistHeaderStyleCache = PlaylistHeaderStyleCache()
