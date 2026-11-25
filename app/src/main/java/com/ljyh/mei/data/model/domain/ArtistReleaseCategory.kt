package com.ljyh.mei.data.model.domain

import java.util.Locale

/** Catalog categories use the server's release type, never a guessed track-count threshold. */
enum class ArtistReleaseCategory(val routeValue: String) {
    All("all"), Albums("albums"), SinglesAndEps("singles");

    fun matches(type: String): Boolean {
        val singleOrEp = type.trim().lowercase(Locale.ROOT).replace(" ", "") in
            setOf("single", "ep", "ep/single", "single/ep", "单曲", "单曲/ep", "ep/单曲")
        return when (this) {
            All -> true
            Albums -> !singleOrEp
            SinglesAndEps -> singleOrEp
        }
    }

    companion object {
        fun fromRoute(value: String?): ArtistReleaseCategory = entries.firstOrNull { it.routeValue == value } ?: All
    }
}
