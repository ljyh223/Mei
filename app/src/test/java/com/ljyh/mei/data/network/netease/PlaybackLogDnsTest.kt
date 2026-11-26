package com.ljyh.mei.data.network.netease

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import org.junit.Assert.*
import org.junit.Test

class PlaybackLogDnsTest {
    private fun ip(value: String) = InetAddress.getByName(value)
    private val blocked = Dns { listOf(ip("0.0.0.0"), ip("127.0.0.1"), ip("::1")) }

    @Test fun blockedSystemAnswerUsesIndependentResolverAndHonorsTtl() {
        var now = 0L
        var queries = 0
        val dns = PlaybackLogDns(blocked, {
            queries++
            PlaybackLogDnsAnswer(listOf(ip("8.8.8.8")), 2)
        }, { now })
        assertEquals(listOf(ip("8.8.8.8")), dns.lookup(PlaybackLogDns.HOST))
        now = TimeUnit.SECONDS.toNanos(1)
        dns.lookup(PlaybackLogDns.HOST)
        assertEquals(1, queries)
        now = TimeUnit.SECONDS.toNanos(2)
        dns.lookup(PlaybackLogDns.HOST)
        assertEquals(2, queries)
    }

    @Test fun otherDomainsAndHealthySystemResolutionDoNotUseIndependentDns() {
        val system = Dns { host -> if (host == PlaybackLogDns.HOST) listOf(ip("8.8.4.4")) else listOf(ip("127.0.0.1")) }
        val dns = PlaybackLogDns(system, { error("Unexpected independent DNS query") })
        assertEquals(listOf(ip("8.8.4.4")), dns.lookup(PlaybackLogDns.HOST))
        assertEquals(listOf(ip("127.0.0.1")), dns.lookup("ads.example.test"))
    }

    @Test fun failedSystemDnsCanRecoverButIndependentFailureIsThrottled() {
        var queries = 0
        var now = 0L
        val dns = PlaybackLogDns(Dns { throw UnknownHostException() }, {
            queries++
            if (queries == 1) throw IOException("offline")
            PlaybackLogDnsAnswer(listOf(ip("8.8.8.8")), 0)
        }, { now })
        repeat(2) {
            assertTrue(runCatching { dns.lookup(PlaybackLogDns.HOST) }.exceptionOrNull() is UnknownHostException)
        }
        assertEquals(1, queries)
        now = TimeUnit.SECONDS.toNanos(10)
        assertEquals(listOf(ip("8.8.8.8")), dns.lookup(PlaybackLogDns.HOST))
        dns.lookup(PlaybackLogDns.HOST)
        assertEquals(3, queries) // TTL zero never becomes a persistent pinned address.
    }

    @Test fun concurrentLookupsShareOneIndependentRequest() {
        val pool = Executors.newFixedThreadPool(4)
        var queries = 0
        val dns = PlaybackLogDns(blocked, {
            queries++
            PlaybackLogDnsAnswer(listOf(ip("8.8.8.8")), 60)
        })
        try {
            pool.invokeAll(List(12) { java.util.concurrent.Callable { dns.lookup(PlaybackLogDns.HOST) } })
                .forEach { assertEquals(listOf(ip("8.8.8.8")), it.get()) }
            assertEquals(1, queries)
        } finally { pool.shutdownNow() }
    }

    private fun response(address: String = "8.8.8.8", host: String = PlaybackLogDns.HOST) = """
        {"Status":0,"TC":false,"Question":{"name":"$host.","type":1},"Answer":[
        {"name":"$host.","TTL":15,"type":5,"data":"cdn.example.test."},
        {"name":"cdn.example.test.","TTL":60,"type":1,"data":"$address"}]}
    """.trimIndent()

    @Test fun jsonParserHonorsCnameTtlAndNeverResolvesTextAsAHostname() {
        val result = PlaybackLogDoh.parse(response())
        assertEquals(15L, result.ttlSeconds)
        assertEquals(PlaybackLogDns.HOST, result.addresses.single().hostName)
        assertEquals(ip("8.8.8.8"), result.addresses.single())
        for (invalid in listOf(
            response("127.0.0.1"), response("0.0.0.0"), response("192.168.1.1"),
            response("100.64.0.1"), response("10.0.0.1"), response("ads.example.test"),
            response(host = "another.example.test"), response().replace("\"Status\":0", "\"Status\":3"),
            response().replace("\"TC\":false", "\"TC\":true"), "{}",
        )) assertTrue(runCatching { PlaybackLogDoh.parse(invalid) }.exceptionOrNull() is IOException)
    }
}
