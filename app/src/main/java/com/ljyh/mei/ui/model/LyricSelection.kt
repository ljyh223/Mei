package com.ljyh.mei.ui.model

enum class LyricVariant(val label: String) {
    Auto("自动选择"),
    AppleMusic("Apple Music · TTML"),
    NeteaseYrc("网易云 · 逐字"),
    NeteaseLrc("网易云 · 逐行"),
    QqQrc("QQ 音乐 · 逐字"),
    QqLrc("QQ 音乐 · 逐行"),
}

data class LyricCandidate(
    val variant: LyricVariant,
    val data: LyricData,
    val preview: String,
)

data class LyricSelectionState(
    val songId: String? = null,
    val requested: LyricVariant = LyricVariant.Auto,
    val active: LyricVariant? = null,
    val candidates: List<LyricCandidate> = emptyList(),
    val isLoading: Boolean = true,
    val preferenceLoaded: Boolean = false,
)
