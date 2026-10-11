package com.ljyh.mei.utils.netease

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Base64
import com.ljyh.mei.AppContext
import java.security.MessageDigest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val DEFAULT_WIFI_MAC = "02:00:00:00:00:00"

/**
 * Reproduces the Android NetEase deviceId wire value from l4.e():
 * imei<TAB>wifiMac<TAB>androidId<TAB>croppedLocalId, then Base64 NO_WRAP and
 * form-style URL encoding.
 *
 * The app cannot read NetEase's private NEDeviceID native cache. For a stable
 * compatible local-id shape, use the Android ID as the raw fallback and the
 * SDK's default jarReflectMul value (-2 / 0xFE) before MD5. The resulting
 * 32-hex ID is cropped exactly like l4.f() (substring(8, 24)).
 */
fun getDeviceId(context: Context = AppContext.instance): String {
    val appContext = context.applicationContext
    val androidId = runCatching {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "null"
    val wifiMac = getWifiMac(appContext)

    val localId = MessageDigest.getInstance("MD5").run {
        update(androidId.toByteArray(StandardCharsets.UTF_8))
        update(0xFE.toByte()) // (uint8)(poly/a.o + poly/a.p - 2), default -2
        digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }
    val croppedLocalId = localId.substring(8, 24)
    val raw = listOf("null", wifiMac, androidId, croppedLocalId).joinToString("\t")
    val base64 = Base64.encodeToString(raw.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    return URLEncoder.encode(base64, StandardCharsets.UTF_8.name())
}

private fun getWifiMac(context: Context): String = runCatching {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    wifiManager?.connectionInfo?.macAddress
        ?.takeIf { it.isNotBlank() }
}.getOrNull() ?: DEFAULT_WIFI_MAC


fun cloudmusicDllEncodeId(someId: String, idXorKey1: String): String {
    val xoredString = buildString {
        for (i in someId.indices) {
            val charCode = someId[i].code xor idXorKey1[i % idXorKey1.length].code
            append(charCode.toChar())
        }
    }

    // Step 2: Compute MD5 hash
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(xoredString.toByteArray())

    // Step 3: Encode the hash in Base64
    return Base64.encodeToString(digest, Base64.NO_WRAP)
}
