package com.ljyh.mei.utils.netease


import timber.log.Timber
import java.util.Random

/**
 * 中国 IP 生成器
 * 复刻自 NeteaseCloudMusicApi (Node.js) 的 index.js 逻辑
 */
object ChineseIpUtils {

    // 数据类：存储处理后的 IP 段信息
    private data class IpRange(
        val start: Long,
        val end: Long,
        val count: Long,
        val location: String
    )

    // 原始数据：开始IP, 结束IP, IP个数, 位置
    private val chinaIPRangesRaw = listOf(
        arrayOf("1.0.1.0", "1.0.3.255", 768, "福州"),
        arrayOf("1.0.8.0", "1.0.15.255", 2048, "广州"),
        arrayOf("1.0.32.0", "1.0.63.255", 8192, "广州"),
        arrayOf("1.1.0.0", "1.1.0.255", 256, "福州"),
        arrayOf("1.1.2.0", "1.1.63.255", 15872, "广州"),
        arrayOf("1.2.0.0", "1.2.2.255", 768, "北京"),
        arrayOf("1.2.4.0", "1.2.127.255", 31744, "广州"),
        arrayOf("1.3.0.0", "1.3.255.255", 65536, "广州"),
        arrayOf("1.4.1.0", "1.4.127.255", 32512, "广州"),
        arrayOf("1.8.0.0", "1.8.255.255", 65536, "北京"),
        arrayOf("1.10.0.0", "1.10.9.255", 2560, "福州"),
        arrayOf("1.10.11.0", "1.10.127.255", 29952, "广州"),
        arrayOf("1.12.0.0", "1.15.255.255", 262144, "上海"),
        arrayOf("1.18.128.0", "1.18.128.255", 256, "北京"),
        arrayOf("1.24.0.0", "1.31.255.255", 524288, "赤峰"),
        arrayOf("1.45.0.0", "1.45.255.255", 65536, "北京"),
        arrayOf("1.48.0.0", "1.51.255.255", 262144, "济南"),
        arrayOf("1.56.0.0", "1.63.255.255", 524288, "伊春"),
        arrayOf("1.68.0.0", "1.71.255.255", 262144, "忻州"),
        arrayOf("1.80.0.0", "1.95.255.255", 1048576, "北京"),
        arrayOf("1.116.0.0", "1.117.255.255", 131072, "上海"),
        arrayOf("1.119.0.0", "1.119.255.255", 65536, "北京"),
        arrayOf("1.180.0.0", "1.185.255.255", 393216, "桂林"),
        arrayOf("1.188.0.0", "1.199.255.255", 786432, "洛阳"),
        arrayOf("1.202.0.0", "1.207.255.255", 393216, "铜仁")
    )

    private val rangeList: List<IpRange>
    private val totalCount: Long
    private val random = Random()

    // init 块相当于 JS 中的 IIFE (Immediately Invoked Function Expression)，类加载时只运行一次
    init {
        val list = mutableListOf<IpRange>()
        var sum = 0L

        for (row in chinaIPRangesRaw) {
            val startIpStr = row[0] as String
            val endIpStr = row[1] as String
            val count = (row[2] as Number).toLong()
            val location = row[3] as String

            val startLong = ipToLong(startIpStr)
            val endLong = ipToLong(endIpStr)

            // JS 逻辑兼容：如果有预设 count 则使用，否则通过 end - start + 1 计算
            // 虽然 rawData 里都有 count，但为了严谨复刻逻辑：
            val finalCount = if (count > 0) count else (endLong - startLong + 1)

            list.add(IpRange(startLong, endLong, finalCount, location))
            sum += finalCount
        }

        rangeList = list
        totalCount = sum
    }

    /**
     * 将 IP 字符串转换为 Long (对应 JS ipToInt)
     * 使用 Long 防止 Java/Kotlin 中 Int 符号位导致的负数问题
     */
    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".").map { it.toLong() }
        if (parts.size != 4) return 0L
        return (parts[0] shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]
    }

    /**
     * 将 Long 转换为 IP 字符串 (对应 JS intToIp)
     */
    private fun longToIp(value: Long): String {
        return buildString {
            append((value ushr 24) and 0xFF)
            append(".")
            append((value ushr 16) and 0xFF)
            append(".")
            append((value ushr 8) and 0xFF)
            append(".")
            append(value and 0xFF)
        }
    }

    private fun getRandomInt(min: Int, max: Int): Int {
        return random.nextInt(max - min + 1) + min
    }

    /**
     * 生成随机中国 IP
     * 对应 Node: generateRandomChineseIP
     */
    fun generateRandomChineseIP(): String {
        // 兜底逻辑：如果初始化失败
        if (totalCount == 0L) {
            val fallback = "116.${getRandomInt(25, 94)}.${getRandomInt(1, 255)}.${getRandomInt(1, 255)}"
            Timber.tag("ChineseIP").i("Generated Random Chinese IP (fallback): $fallback")
            return fallback
        }

        // 选择一个全局随机偏移 ([0, total))
        var offset = (random.nextDouble() * totalCount).toLong()
        var chosen: IpRange? = null

        for (range in rangeList) {
            if (offset < range.count) {
                chosen = range
                break
            }
            offset -= range.count
        }

        // 兜底：如果循环结束也没选中（理论上不应该发生），使用最后一个
        if (chosen == null) {
            chosen = rangeList.last()
        }

        // 在选中的段内随机生成一个 IP
        // JS逻辑：start + floor(random() * segSize)
        val segSize = chosen.end - chosen.start + 1
        val randomOffsetInSeg = (random.nextDouble() * segSize).toLong()
        val ipLong = chosen.start + randomOffsetInSeg

        val ip = longToIp(ipLong)

        Timber.tag("ChineseIP").d("Generated: $ip | Loc: ${chosen.location}")

        return ip
    }
}