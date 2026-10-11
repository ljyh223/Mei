package com.ljyh.mei.utils.encrypt

import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptorCompatibilityTest {
    @Test
    fun weApiMatchesProtocolVector() {
        val result = encryptWeApiWithKey("{\"hello\":\"world\"}", "0123456789abcdef")
        assertEquals(
            "TcbemrtD8hz1QWG5puftqG7VTrMIXd/pgeDMPGhd97cIZe6xr/T3vmpPN25H7Zqy",
            result.params,
        )
    }

    @Test
    fun eApiMatchesProtocolVector() {
        val result = encryptEApi(
            "/api/song/enhance/player/url",
            "{\"ids\":\"[123]\",\"br\":320000}",
        )
        assertEquals(
            "fa90b329e9614f79e79598f37dc2edb430f8378d2a2796338f0bfdeaef824a22975cda9d96d79e6dc4a59218cdb8199f43d1b451df264aab885392fbd9bae38b69297106fba90eee42cee9221728150304402328c166454425787e96a42f51130c1a6a40656f8d4fda74a84de353884b52369bf75cd6308d9fe58b15c63068d9",
            result.params,
        )
    }

    @Test
    fun encryptedIdMatchesProtocolVector() {
        assertEquals("SOJgz_ObU1tgyRJptBgDTA==", encryptId("12345"))
    }
}
