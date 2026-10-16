package com.ljyh.mei.playback.sleep

import androidx.media3.common.Player
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    @Test
    fun replacingTimerModeClearsThePreviousMode() {
        val pauses = AtomicInteger()
        val player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, _ ->
            if (method.name == "pause") pauses.incrementAndGet()
            null
        } as Player
        val timer = SleepTimer(CoroutineScope(SupervisorJob() + Dispatchers.Default), player)

        timer.start(-1)
        assertTrue(timer.pauseWhenSongEnd)

        timer.start(30)
        assertFalse(timer.pauseWhenSongEnd)
        assertTrue(timer.triggerTime > System.currentTimeMillis())
        timer.onMediaItemTransition(null, 0)
        assertEquals(0, pauses.get())

        timer.start(-1)
        assertEquals(-1L, timer.triggerTime)
        assertTrue(timer.pauseWhenSongEnd)
        timer.clear()
    }

    @Test
    fun detectedSleepPausesImmediatelyAndCleansUpSubscription() {
        val pauses = AtomicInteger()
        val cleanupCalls = AtomicInteger()
        val timer = SleepTimer(testScope(), fakePlayer(pauses))

        timer.startSleepDetection(120) { cleanupCalls.incrementAndGet() }
        assertTrue(timer.sleepDetectionActive)
        assertTrue(timer.triggerTime > System.currentTimeMillis())

        timer.onSleepDetected()
        assertEquals(1, pauses.get())
        assertEquals(1, cleanupCalls.get())
        assertFalse(timer.isActive)
        assertFalse(timer.sleepDetectionActive)

        timer.onSleepDetected()
        assertEquals(1, pauses.get())
        assertEquals(1, cleanupCalls.get())
    }

    @Test
    fun changingModesOrDeadlineKeepsOnlyTheCurrentDetection() {
        val pauses = AtomicInteger()
        val cleanupCalls = AtomicInteger()
        val timer = SleepTimer(testScope(), fakePlayer(pauses))

        timer.startSleepDetection(120) { cleanupCalls.incrementAndGet() }
        val firstDeadline = timer.triggerTime
        timer.updateSleepDetectionDeadline(180)
        assertTrue(timer.triggerTime > firstDeadline)
        assertEquals(0, cleanupCalls.get())
        assertTrue(timer.sleepDetectionActive)

        timer.start(30)
        assertEquals(1, cleanupCalls.get())
        assertFalse(timer.sleepDetectionActive)
        timer.onSleepDetected()
        assertEquals(0, pauses.get())
        timer.clear()
    }

    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun fakePlayer(pauses: AtomicInteger) = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, _ ->
        if (method.name == "pause") pauses.incrementAndGet()
        null
    } as Player
}
