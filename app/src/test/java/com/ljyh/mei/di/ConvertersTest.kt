package com.ljyh.mei.di

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun readsLegacyDatabaseArrayWithUnicodeAndHtmlEscapes() {
        assertEquals(
            listOf("歌手甲", "A & B", "<艺名>", "O'Name"),
            converters.toStringList("""["歌手甲","A \u0026 B","\u003c艺名\u003e","O\u0027Name"]"""),
        )
    }

    @Test
    fun roundTripPreservesNamesAndSpecialCharacters() {
        val names = listOf("歌手甲", "A \"quoted\" name", "back\\slash", "line\nbreak", "🎵")
        assertEquals(names, converters.toStringList(converters.fromStringList(names)))
    }

    @Test
    fun emptyListKeepsExistingDatabaseFormat() {
        assertEquals("[]", converters.fromStringList(emptyList()))
        assertEquals(emptyList<String>(), converters.toStringList("[]"))
    }

    @Test
    fun corruptOrNullDatabaseValuesFallBackToEmptyList() {
        for (value in listOf("", "{", "null", "{}", "[null]")) {
            assertEquals(value, emptyList<String>(), converters.toStringList(value))
        }
    }
}
