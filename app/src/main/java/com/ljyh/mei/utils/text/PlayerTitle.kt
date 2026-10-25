package com.ljyh.mei.utils.text

private val titleBracketPairs = mapOf(
    ')' to '(', '）' to '（',
    ']' to '[', '］' to '［', '】' to '【', '〕' to '〔',
    '}' to '{', '｝' to '｛',
    '>' to '<', '＞' to '＜', '〉' to '〈', '》' to '《',
    '」' to '「', '』' to '『',
)
private val titleBracketOpeners = titleBracketPairs.values.toSet()
private val repeatedTitleWhitespace = Regex("\\s{2,}")

/** Display only: preserve malformed brackets and never replace the actual song metadata. */
fun formatPlayerTitle(title: String, hideBracketedContent: Boolean): String {
    if (!hideBracketedContent || title.isEmpty()) return title
    val openings = ArrayDeque<Pair<Char, Int>>()
    val removals = IntArray(title.length + 1)
    var hasMatch = false
    title.forEachIndexed { index, char ->
        when {
            char in titleBracketOpeners -> openings.addLast(char to index)
            titleBracketPairs[char] == openings.lastOrNull()?.first && openings.isNotEmpty() -> {
                val start = openings.removeLast().second
                removals[start]++
                removals[index + 1]--
                hasMatch = true
            }
        }
    }
    if (!hasMatch) return title
    val result = buildString {
        var depth = 0
        title.forEachIndexed { index, char ->
            depth += removals[index]
            if (depth == 0) append(char)
        }
    }.replace(repeatedTitleWhitespace, " ").trim()
    return result.ifBlank { title }
}
