package com.ljyh.mei.playback.usb

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val UsbBufferProfileKey = stringPreferencesKey("usb.bufferProfile")
val UsbForegroundBufferKey = intPreferencesKey("usb.foregroundBufferMs")
val UsbBackgroundBufferKey = intPreferencesKey("usb.backgroundBufferMs")
val UsbSampleRateCompatibilityKey = booleanPreferencesKey("usb.sampleRateCompatibility")
val UsbBitDepthCompatibilityKey = booleanPreferencesKey("usb.bitDepthCompatibility")
val UsbChannelCompatibilityKey = booleanPreferencesKey("usb.channelCompatibility")
val UsbFloatingKeepAliveKey = booleanPreferencesKey("usb.floatingKeepAlive")

/** Presets for Mei's 100–3000 ms native queue; individual durations can override them. */
enum class UsbBufferProfile(val label: String, val foregroundMs: Int, val backgroundMs: Int) {
    LowLatency("低延迟", 100, 400),
    Balanced("均衡", 250, 800),
    Stable("稳定", 500, 1500);
}

data class UsbPlaybackPreferences(
    val profile: UsbBufferProfile = UsbBufferProfile.Stable,
    val foregroundBufferMs: Int = profile.foregroundMs,
    val backgroundBufferMs: Int = profile.backgroundMs,
    val sampleRateCompatibility: Boolean = true,
    val bitDepthCompatibility: Boolean = true,
    val channelCompatibility: Boolean = true,
    val floatingKeepAlive: Boolean = false,
) {
    fun bufferMs(foreground: Boolean): Int = if (foreground) normalizeForegroundBuffer(foregroundBufferMs)
        else normalizeBackgroundBuffer(backgroundBufferMs)
    val customized: Boolean get() = bufferMs(true) != profile.foregroundMs || bufferMs(false) != profile.backgroundMs
    // Buffer/keepalive changes are applied live. Only format policy needs a new USB session.
    fun formatPolicy() = Triple(sampleRateCompatibility, bitDepthCompatibility, channelCompatibility)
}

fun normalizeForegroundBuffer(value: Int) = value.coerceIn(100, 1000) / 50 * 50
fun normalizeBackgroundBuffer(value: Int) = value.coerceIn(200, 3000) / 50 * 50

fun Preferences.usbPlaybackPreferences(): UsbPlaybackPreferences {
    val profile = UsbBufferProfile.entries.firstOrNull { it.name == this[UsbBufferProfileKey] } ?: UsbBufferProfile.Stable
    return UsbPlaybackPreferences(
        profile = profile,
        foregroundBufferMs = normalizeForegroundBuffer(this[UsbForegroundBufferKey] ?: profile.foregroundMs),
        backgroundBufferMs = normalizeBackgroundBuffer(this[UsbBackgroundBufferKey] ?: profile.backgroundMs),
        sampleRateCompatibility = this[UsbSampleRateCompatibilityKey] ?: true,
        bitDepthCompatibility = this[UsbBitDepthCompatibilityKey] ?: true,
        channelCompatibility = this[UsbChannelCompatibilityKey] ?: true,
        floatingKeepAlive = this[UsbFloatingKeepAliveKey] ?: false,
    )
}

/** One actual USB alternate format plus its rate range, never a cross-product of device capabilities. */
internal data class UsbOutputFormat(
    val bits: Int, val bytes: Int, val channels: Int,
    val minimumRate: Int, val maximumRate: Int, val resolution: Int,
) {
    fun nearestRate(requested: Int): Int {
        if (minimumRate == 0) return requested // A device that does not expose readable clock ranges.
        val bounded = requested.coerceIn(minimumRate, maximumRate)
        if (resolution <= 0) return bounded
        val lower = minimumRate + (bounded - minimumRate) / resolution * resolution
        val upper = (lower.toLong() + resolution).takeIf { it <= maximumRate }?.toInt() ?: lower
        return if (requested.toLong() - lower < upper.toLong() - requested) lower else upper
    }
}
internal data class UsbOutputTarget(val rate: Int, val bits: Int, val bytes: Int, val channels: Int)

internal fun parseUsbOutputFormats(report: String): List<UsbOutputFormat> = report.lineSequence().mapNotNull { line ->
    val raw = line.split(',')
    if (raw.size != 6) return@mapNotNull null
    val fields = raw.map { it.toIntOrNull() ?: return@mapNotNull null }
    val (bits, bytes, channels) = fields
    val min = fields[3]; val max = fields[4]; val step = fields[5]
    if (bits !in 16..32 || bytes !in 2..4 || bits > bytes * 8 || channels !in 1..8 ||
        step < 0 || (min != 0 && min !in 8000..768000) || max < min || max > 768000 ||
        (min == 0 && max != 0)) null
    else UsbOutputFormat(bits, bytes, channels, min, max, step)
}.distinct().toList()

internal fun usbOutputTargets(
    formats: List<UsbOutputFormat>, rate: Int, bits: Int, channels: Int, policy: UsbPlaybackPreferences,
): List<UsbOutputTarget> {
    fun family(value: Int) = when { value % 44100 == 0 -> 44100; value % 48000 == 0 -> 48000; else -> 0 }
    return formats.flatMap { format ->
        val preferred = format.nearestRate(rate)
        // Prefer the same clock family when a continuous/ranged clock permits it.
        val familyRate = family(rate).takeIf { it > 0 && format.minimumRate > 0 }?.let { base ->
            (1..16).map { it * base }.filter { format.nearestRate(it) == it }.minByOrNull { kotlin.math.abs(it - rate) }
        }
        listOfNotNull(rate.takeIf { preferred == rate }, familyRate, preferred).distinct().map {
            UsbOutputTarget(it, format.bits, format.bytes, format.channels)
        }
    }.filter {
        (policy.sampleRateCompatibility || it.rate == rate) &&
            (policy.bitDepthCompatibility || it.bits == bits) &&
            (policy.channelCompatibility || it.channels == channels)
    }.distinct().sortedWith(compareBy<UsbOutputTarget>(
        { if (it.rate == rate) 0 else if (family(rate) != 0 && family(it.rate) == family(rate)) 1 else 2 },
        { kotlin.math.abs(it.rate - rate) },
        { if (it.bits >= bits) 0 else 1 }, { kotlin.math.abs(it.bits - bits) },
        { if (it.channels == 2 && policy.channelCompatibility) 0 else if (it.channels == channels) 1 else 2 },
        { it.channels }, { it.bytes },
    ))
}

internal fun shouldShowUsbKeepAlive(enabled: Boolean, foreground: Boolean, playing: Boolean, nativeActive: Boolean, permitted: Boolean) =
    enabled && !foreground && playing && nativeActive && permitted

/** A watermark in output frames; bytes consumed are always aligned source PCM frames. */
internal fun usbWritableBytes(remaining: Int, queued: Long, capacity: Long, inputRate: Int, outputRate: Int, inputFrameBytes: Int): Int {
    val room = (capacity - queued).coerceAtLeast(0)
    val frames = room * inputRate / outputRate
    return minOf(remaining.toLong() / inputFrameBytes, frames).toInt() * inputFrameBytes
}
