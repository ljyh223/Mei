package com.ljyh.mei.utils.lyric


/**
 * 歌词精准匹配算法
 * 从 LDDC 项目移植: https://github.com/chenmozhijin/LDDC
 * 实现自动匹配最佳歌词的核心算法
 */
object LyricMatchAlgorithm {

    /**
     * 符号映射表（全角转半角）
     */
    private val SYMBOL_MAP = mapOf(
        "（" to "(",
        "）" to ")",
        "：" to ":",
        "！" to "!",
        "？" to "?",
        "／" to "/",
        "＆" to "&",
        "＊" to "*",
        "＠" to "@",
        "＃" to "#",
        "＄" to "$",
        "％" to "%",
        "＼" to "\\",
        "｜" to "|",
        "＝" to "=",
        "＋" to "+",
        "－" to "-",
        "＜" to "<",
        "＞" to ">",
        "［" to "[",
        "］" to "]",
        "｛" to "{",
        "｝" to "}"
    )

    /**
     * 标题标签正则模式（需要过滤的版本标签等）
     */
    private val TITLE_TAG_PATTERNS = listOf(
        Regex("""[-<(\[～]([～\]^)>-]*)[～\]^)>-]"""),
        Regex("""(\w+ ?(?:(?:solo |size )?ver(?:sion)?\.?|size|style|mix(?:ed)?|edit(?:ed)?|版|solo))"""),
        Regex("""(纯音乐|inst\.?(?:rumental)|off ?vocal(?: ?[Vv]er.)?)""")
    )

    /**
     * 统一符号（全角转半角，去除多余空格）
     */
    fun unifySymbol(text: String): String {
        var result = text.trim()
        for ((key, value) in SYMBOL_MAP) {
            result = result.replace(key, value)
        }
        return result.replace(Regex("""\s+"""), " ")
    }

    /**
     * 计算两个文本的相似度（0.0 - 1.0）
     * 使用编辑距离算法（Levenshtein Distance）
     */
    fun textDifference(text1: String, text2: String): Float {
        if (text1 == text2) return 1.0f

        val len1 = text1.length
        val len2 = text2.length

        // 创建动态规划表
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // 初始化第一行和第一列
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // 填充动态规划表
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (text1[i - 1] == text2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }

        val maxLen = maxOf(len1, len2)
        return if (maxLen == 0) 1.0f else 1.0f - dp[len1][len2].toFloat() / maxLen
    }

    /**
     * 计算两个列表中字符串的最大相似度
     */
    fun listMaxDifference(
        list1: List<String>,
        list2: List<String>,
        filterEmpty: Boolean = true
    ): Float {
        val l1 = if (filterEmpty) list1.filter { it.isNotEmpty() } else list1
        val l2 = if (filterEmpty) list2.filter { it.isNotEmpty() } else list2

        if (l1.isEmpty() || l2.isEmpty()) return 0.0f

        // 计算所有配对的相似度
        val scores = mutableListOf<Triple<Int, Int, Float>>()
        for (i1 in l1.indices) {
            for (i2 in l2.indices) {
                scores.add(Triple(i1, i2, textDifference(l1[i1], l2[i2])))
            }
        }

        // 按相似度降序排序
        scores.sortByDescending { it.third }

        // 贪心匹配：选择相似度最高的配对
        var totalScore = 0.0f
        val usedI1 = mutableSetOf<Int>()
        val usedI2 = mutableSetOf<Int>()

        for ((i1, i2, score) in scores) {
            if (i1 !in usedI1 && i2 !in usedI2) {
                usedI1.add(i1)
                usedI2.add(i2)
                totalScore += score
                if (usedI1.size == l1.size || usedI2.size == l2.size) {
                    break
                }
            }
        }

        return totalScore / maxOf(l1.size, l2.size)
    }

    /**
     * 歌手信息数据类
     */
    data class ArtistInfo(
        val groups: List<String> = emptyList(),
        val artists: List<List<String>> = emptyList()
    )

    /**
     * 将歌手字符串转换为结构化数据
     * 支持复杂格式：组织名(角色1・角色2)CV:歌手1・歌手2
     */
    fun artistStrToList(artist: String): ArtistInfo {
        if (artist.split(" ").all { it.length == 1 }) {
            val compactArtist = artist.replace(" ", "")
            return parseArtistString(compactArtist)
        }

        return parseArtistString(artist)
    }

    private fun parseArtistString(artist: String): ArtistInfo {
        var processedArtist = artist.trim()
            .replace("·", "・")
            .replace("（", "(")
            .replace("）", ")")
            .replace("：", ":")

        val groups = mutableListOf<String>()
        val artists = mutableListOf<List<String>>()

        // 处理 "组织名(角色1・角色2)/CV:歌手1・歌手2" 格式
        val cvPattern = Regex("""^(?<group>.*)\s?\((?<characters>.+)\)/[Cc][Vv][.:]\s?(?<singers>.+)$""")
        val cvMatch = cvPattern.find(processedArtist)

        if (cvMatch != null && "・" in (cvMatch.groups["characters"]?.value ?: "") &&
            "・" in (cvMatch.groups["singers"]?.value ?: "")) {
            val characters = cvMatch.groups["characters"]?.value
                ?.split("・")?.map { unifySymbol(it) } ?: emptyList()
            val singers = cvMatch.groups["singers"]?.value
                ?.split("・")?.map { unifySymbol(it) } ?: emptyList()

            if (characters.size == singers.size) {
                return ArtistInfo(
                    groups = listOf(cvMatch.groups["group"]?.value ?: ""),
                    artists = characters.zip(singers) { c, s -> listOf(c, s).toSet().toList() }
                )
            }
        }

        // 处理多组织格式：组织1(角色1,角色2CV:歌手1,歌手2)/组织2(...)
        val multiCvPattern = Regex("""^(?<group>.*)\s?\((?<characters>.+)[Cc][Vv][.:](?<singers>.+)\)$""")
        val parts = processedArtist.split("/")

        if (parts.size > 1) {
            var allMatched = true
            val tempArtists = mutableListOf<List<String>>()
            val tempGroups = mutableListOf<String>()

            for (part in parts) {
                val match = multiCvPattern.find(part)
                if (match != null && "・" in (match.groups["characters"]?.value ?: "") &&
                    "・" in (match.groups["singers"]?.value ?: "")) {
                    val characters = match.groups["characters"]?.value
                        ?.split("・")?.map { unifySymbol(it) } ?: emptyList()
                    val singers = match.groups["singers"]?.value
                        ?.split("・")?.map { unifySymbol(it) } ?: emptyList()

                    if (characters.size == singers.size) {
                        tempArtists.addAll(characters.zip(singers) { c, s ->
                            listOf(c, s).toSet().toList()
                        })
                        tempGroups.add(match.groups["group"]?.value ?: "")
                    } else {
                        allMatched = false
                        break
                    }
                } else {
                    allMatched = false
                    break
                }
            }

            if (allMatched && tempArtists.isNotEmpty()) {
                return ArtistInfo(groups = tempGroups, artists = tempArtists)
            }
        }

        // 处理 "组织名(歌手1,歌手2)" 格式
        val groupPattern = Regex("""^(?<group>.*)\s?\(+(?<singers>[^)]+)\)+$""")
        val groupMatch = groupPattern.find(processedArtist)

        if (groupMatch != null) {
            val singers = groupMatch.groups["singers"]?.value
                ?.split(Regex("""[,、・]""")) ?: emptyList()
            if (singers.size > 1) {
                return ArtistInfo(
                    groups = listOf(groupMatch.groups["group"]?.value ?: ""),
                    artists = singers.map { listOf(unifySymbol(it)) }
                )
            }
        }

        // 处理组织名与歌手名分离的情况
        val groupSplitPattern = Regex("""^(?<group>.*[^&])\s(?<artist>[^(&a-zA-Z].*)$""")
        val groupSplitMatch = groupSplitPattern.find(processedArtist)

        if (groupSplitMatch != null) {
            groups.add(groupSplitMatch.groups["group"]?.value ?: "")
            processedArtist = groupSplitMatch.groups["artist"]?.value ?: processedArtist
        }

        // 以符号分隔歌手
        val singerParts = processedArtist.split(Regex("""[,、/\\&]"""))
        val singerList = if (singerParts.size > 1) {
            singerParts.map { unifySymbol(it) }
        } else {
            listOf(unifySymbol(processedArtist))
        }

        // 处理每个歌手名
        for (singerStr in singerList) {
            // 处理 "feat.角色(歌手2)" 格式
            val featPattern = Regex("""^(?<singer1>.*)\s?feat\.(?<char>.*)\s?\((?<singer2>.*)\)$""")
            val featMatch = featPattern.find(singerStr)

            if (featMatch != null) {
                artists.add(listOf(unifySymbol(featMatch.groups["singer1"]?.value ?: "")))
                artists.add(listOf(
                    unifySymbol(featMatch.groups["singer2"]?.value?.trim() ?: ""),
                    unifySymbol(featMatch.groups["char"]?.value?.trim() ?: "")
                ).toSet().toList())
                continue
            }

            // 处理 "歌手名(别名)" 或 "角色名(CV:歌手名)" 格式
            val aliasPattern = Regex("""^(?<name1>.*)\s?\((?:[Cc][Vv][.:]|[Vv][Oo][.:])?(?<name2>.*)\)$""")
            val aliasMatch = aliasPattern.find(singerStr)

            if (aliasMatch != null) {
                artists.add(listOf(
                    unifySymbol(aliasMatch.groups["name1"]?.value?.trim() ?: ""),
                    unifySymbol(aliasMatch.groups["name2"]?.value?.trim() ?: "")
                ).toSet().toList())
            } else {
                artists.add(listOf(singerStr))
            }
        }

        return ArtistInfo(groups = groups, artists = artists)
    }

    /**
     * 计算两个歌手名称的匹配分数（0 - 100）
     */
    fun calculateArtistScore(artist1: String, artist2: String): Float {
        val info1 = artistStrToList(artist1)
        val info2 = artistStrToList(artist2)

        // 如果都是组织名+歌手名格式
        val all1 = info1.groups + info1.artists.flatten()
        val all2 = info2.groups + info2.artists.flatten()

        val baseScore = listMaxDifference(all1, all2)
        if (baseScore == 1.0f) return 100.0f

        val combinedScore = textDifference(
            all1.joinToString("") + info1.artists.flatten().joinToString(""),
            all2.joinToString("") + info2.artists.flatten().joinToString("")
        )

        var score = maxOf(baseScore, combinedScore)

        // 特殊情况处理
        if ((info1.artists.isEmpty() && info1.groups.isNotEmpty() && info2.groups.isNotEmpty()) ||
            (info2.artists.isEmpty() && info2.groups.isNotEmpty() && info1.groups.isNotEmpty())) {
            // 只有组织名
            score = maxOf(score, listMaxDifference(info1.artists.map { it.first() }, info2.artists.map { it.first() }) * 0.6f)
        } else if (info1.artists.isNotEmpty() && info2.artists.isNotEmpty()) {
            // 都有歌手名
            score = maxOf(score, listMaxDifference(info1.artists.flatten(), info2.artists.flatten()))
        }

        return (score * 100).coerceAtLeast(0f)
    }

    /**
     * 从文本中提取标签
     */
    private data class TagInfo(val tags: List<String>, val other: String)

    private fun getTags(notSame: String): TagInfo {
        val notSameTags = mutableListOf<String>()
        var notSameOther = notSame

        for (pattern in TITLE_TAG_PATTERNS) {
            val matches = pattern.findAll(notSame).map { it.value.trim() }.toList()
            notSameTags.addAll(matches)
        }

        // 统一标签格式
        val normalizedTags = notSameTags.map { tag ->
            var normalized = tag
            normalized = Regex("""ver(?:sion)?\.?""").replace(normalized, "ver")
            normalized = Regex("""伴奏|纯音乐|inst\.?(?:rumental)|off ?vocal(?: ?[Vv]er.)?""")
                .replace(normalized, "inst")
            normalized = normalized.replace("mixed", "mix")
                .replace("edited", "edit")
            normalized = Regex("""(solo|mix|edit|style|size) ver""").replace(normalized, "$1")
            normalized = Regex("""(?:tv|anime) ?(?:サイズ|size)?(?: ?edit)?(?: ?ver)?""")
                .replace(normalized, "tv size")
            normalized
        }.filter { it.isNotEmpty() }

        // 移除标签获取其他部分
        for (tag in normalizedTags) {
            notSameOther = notSameOther.replace(tag, "")
        }
        notSameOther = notSameOther.replace(Regex("""[-><)(\]\[～]"""), "")

        return TagInfo(normalizedTags, notSameOther.trim())
    }

    /**
     * 计算两个标题的匹配分数（0 - 100）
     */
    fun calculateTitleScore(title1: String, title2: String): Float {
        val t1 = unifySymbol(title1).lowercase()
        val t2 = unifySymbol(title2).lowercase()

        if (t1 == t2) return 100.0f

        // 计算文本相似度作为底分
        val score0 = maxOf(textDifference(t1, t2), 0f) * 100

        // 找到开头相同的部分
        var sameBegin = ""
        for (i in t1.indices) {
            if (i < t2.length && t1[i] == t2[i]) {
                sameBegin += t1[i]
            } else {
                break
            }
        }

        if (sameBegin.isEmpty() || sameBegin == t1 || sameBegin == t2) {
            return score0
        }

        // 分析不同部分
        val (tags1, other1) = getTags(t1.substring(sameBegin.length))
        val (tags2, other2) = getTags(t2.substring(sameBegin.length))

        // 计算标签匹配
        val tag1NoMatch = mutableListOf<String>()
        val tag2NoMatch = tags2.toMutableList()

        for (tag1 in tags1) {
            if (tag1 in tag2NoMatch) {
                tag2NoMatch.remove(tag1)
            } else if (tag1.matches(Regex("""(solo|mix|edit|style|size|inst)$"""))) {
                tag1NoMatch.add(tag1)
            } else if (tag1 in other2) {
                // tag在other2中，不算其他部分
            }
        }

        // 计算基础分数
        val kp = sameBegin.length.toFloat() / ((other1.length + other2.length) / 2f + sameBegin.length)
        val score1 = 100 * kp + maxOf(textDifference(other1, other2), 0f) * (1 - kp)

        if (tag1NoMatch.isEmpty() && tag2NoMatch.isEmpty()) {
            return maxOf(score1 * 0.7f + 30, score0)
        }

        // 计算未匹配标签的相似度
        var score2 = 0f
        var score3 = 0f

        if (tag1NoMatch.isNotEmpty() && tag2NoMatch.isNotEmpty()) {
            for (tag1 in tag1NoMatch) {
                score2 += tag2NoMatch.maxOf { textDifference(tag1, it) } * (30f / tag1NoMatch.size)
            }

            for (tag2 in tag2NoMatch) {
                score3 += tag1NoMatch.maxOf { textDifference(it, tag2) } * (30f / tag2NoMatch.size)
            }
        }

        return maxOf(score1 * 0.7f + maxOf(score2, score3), score0)
    }

    /**
     * 歌词匹配结果
     */
    data class MatchResult(
        val song: SongMatchInfo,
        val titleScore: Float,
        val artistScore: Float,
        val totalScore: Float
    )

    /**
     * 歌曲匹配信息（需要根据你的实际数据结构调整）
     */
    data class SongMatchInfo(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long
    )

    /**
     * 计算综合匹配分数
     * @param titleScore 标题分数（0-100）
     * @param artistScore 歌手分数（0-100）
     * @param durationDiff 时长差（毫秒）
     * @param maxDuration 最大允许时长差（毫秒）
     */
    fun calculateTotalScore(
        titleScore: Float,
        artistScore: Float,
        durationDiff: Long,
        maxDuration: Long = 5000L
    ): Float {
        // 时长相似度（时长差越小分数越高）
        val durationScore = if (durationDiff <= maxDuration) {
            1.0f - (durationDiff.toFloat() / maxDuration.toFloat())
        } else {
            0.0f
        }

        // 综合分数：标题50% + 歌手40% + 时长10%
        return titleScore * 0.5f + artistScore * 0.4f + durationScore * 10f
    }

    /**
     * 从搜索结果中自动选择最佳匹配
     * @param targetInfo 目标歌曲信息
     * @param searchResults 搜索结果列表
     * @param minScore 最低匹配分数阈值（默认50分）
     * @return 最佳匹配结果，如果没有符合条件的则返回null
     */
    fun findBestMatch(
        targetInfo: SongMatchInfo,
        searchResults: List<SongMatchInfo>,
        minScore: Float = 50f
    ): MatchResult? {
        if (searchResults.isEmpty()) return null

        val scoredResults = searchResults.map { song ->
            val titleScore = calculateTitleScore(targetInfo.title, song.title)
            val artistScore = calculateArtistScore(targetInfo.artist, song.artist)
            val durationDiff = kotlin.math.abs(targetInfo.duration - song.duration)
            val totalScore = calculateTotalScore(titleScore, artistScore, durationDiff)

            MatchResult(song, titleScore, artistScore, totalScore)
        }

        // 按总分降序排序
        scoredResults.sortedByDescending { it.totalScore }

        // 返回最高分且达到阈值的结果
        val best = scoredResults.first()
        return if (best.totalScore >= minScore) best else null
    }
}
