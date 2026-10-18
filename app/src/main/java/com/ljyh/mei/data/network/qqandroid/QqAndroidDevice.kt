package com.ljyh.mei.data.network.qqandroid

import android.os.Build
import java.security.SecureRandom
import kotlinx.serialization.Serializable

/**
 * QQ 音乐 Android 协议的设备档案（`comm` 里的 35 个字段）。
 *
 * 结论来自在线探测：服务端只要求 `QIMEI36` 非空，其余标识并不校验真实性，
 * 所以这里全部用合成值——不需要抓包里的 uid/udid，也不需要游客登录。
 *
 * `comm` 里**故意不放 `br`**：不放 brotli 能力声明时服务端返回明文 JSON，
 * 客户端就不必引入 brotli 解码。
 */
@Serializable
data class QqAndroidDevice(val comm: Map<String, String>) {

    val uid: String get() = comm["uid"] ?: "0"
    val udid: String get() = comm["udid"].orEmpty()
    val aid: String get() = comm["aid"].orEmpty()

    /** 刷新每次请求的字段，并让已缓存的设备档案使用当前协议版本。 */
    fun refreshed(nowMs: Long = System.currentTimeMillis()): QqAndroidDevice =
        copy(
            comm = LinkedHashMap(comm).apply {
                put("M-Value", QqAndroidCrypto.mValue(aid))
                put("traceid", "11_0${uid}_${nowMs / 1000}")
                put("cv", QQ_ANDROID_VERSION_CODE.toString())
                put("v", QQ_ANDROID_VERSION_CODE.toString())
            },
        )

    /** sign 之外那段参与 mask 的明文。 */
    fun custom(tsMs: Long = System.currentTimeMillis()): String =
        "11&$QQ_ANDROID_VERSION_CODE&$uid&null&$tsMs&$udid&android"

    companion object {
        private val rng = SecureRandom()

        private fun hex(n: Int): String =
            buildString(n) { repeat(n) { append("0123456789abcdef"[rng.nextInt(16)]) } }

        /** OpenUDID = UUID(符号扩展的 Java hashCode(aid), hashCode("")<<32 | hashCode("null"))。 */
        fun openUdid(androidId: String): String {
            fun javaHash(s: String): Int {
                var h = 0
                for (c in s) h = h * 31 + c.code
                return h
            }

            val most = javaHash(androidId)
            val hi = if (most < 0) (most.toLong() and 0xFFFFFFFFL) or -0x100000000L else most.toLong()
            val lo = (javaHash("").toLong() shl 32) or (javaHash("null").toLong() and 0xFFFFFFFFL)
            return "%016x%016x".format(hi, lo)
        }

        fun synthetic(osVer: String = Build.VERSION.RELEASE ?: "", model: String = Build.MODEL ?: ""): QqAndroidDevice {
            val aid = hex(16)
            val udid = openUdid(aid)
            val uid = (1_000_000_000L + (rng.nextLong() ushr 1) % 9_000_000_000L).toString()
            val sid = "%s%s".format(
                java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date()),
                uid,
            )
            val comm = linkedMapOf(
                "OpenUDID" to udid,
                "udid" to udid,
                "ct" to "11",
                "cv" to QQ_ANDROID_VERSION_CODE.toString(),
                "v" to QQ_ANDROID_VERSION_CODE.toString(),
                "chid" to "60009",
                "os_ver" to osVer,
                "aid" to aid,
                "phonetype" to model,
                "OpenUDID2" to hex(32),
                "devicelevel" to "50",
                "newdevicelevel" to "40",
                "deviceScore" to "773.5",
                "QIMEI36" to hex(36),
                "oaid" to hex(64),
                "taid" to hex(64),
                "tmeAppID" to "qqmusic",
                "tid" to (1_000_000_000_000_000_000L + (rng.nextLong() ushr 1) % 8_000_000_000_000_000_000L).toString(),
                "modeSwitch" to "6",
                "teenMode" to "0",
                "M-Value" to "",
                "ui_mode" to "1",
                "nettype" to "1030",
                "wid" to uid,
                "rom" to "unknown",
                "uid" to uid,
                "sid" to sid,
                "tmeLoginType" to "0",
                "tmeLoginMethod" to "0",
                "fPersonality" to "0",
                "tyt_exp_env" to "0",
                "v4ip" to "",
                "hotfix" to "",
                "traceid" to "",
            )
            return QqAndroidDevice(comm).refreshed()
        }
    }
}
