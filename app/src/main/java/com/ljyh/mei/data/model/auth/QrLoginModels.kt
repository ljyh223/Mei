package com.ljyh.mei.data.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QrLoginKeyResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("unikey")
    val unikey: String? = null,
    @SerialName("message")
    val message: String? = null,
)

@Serializable
data class QrLoginCheckResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,
    @SerialName("cookie")
    val cookie: String? = null,
)

data class QrLoginCheck(
    val code: Int,
    val message: String?,
    val nickname: String?,
    val avatarUrl: String?,
    val musicU: String?,
)

enum class QrLoginStatus(val code: Int) {
    Expired(800),
    Waiting(801),
    Scanned(802),
    Success(803),
    Unknown(-1),
    ;

    companion object {
        fun fromCode(code: Int): QrLoginStatus = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

sealed interface QrLoginUiState {
    data object Idle : QrLoginUiState
    data object Loading : QrLoginUiState
    data class Waiting(val loginUrl: String) : QrLoginUiState
    data class Scanned(val loginUrl: String, val nickname: String?) : QrLoginUiState
    data class Expired(val loginUrl: String) : QrLoginUiState
    data class Success(val nickname: String?) : QrLoginUiState
    data class Error(val message: String, val loginUrl: String? = null) : QrLoginUiState
}

internal fun extractMusicU(cookieValues: Iterable<String?>): String? {
    val musicUPattern = Regex("(?:^|[;,\\s])MUSIC_U=([^;,\\s]+)", RegexOption.IGNORE_CASE)
    return cookieValues.asSequence()
        .filterNotNull()
        .mapNotNull { musicUPattern.find(it)?.groupValues?.getOrNull(1) }
        .map { it.trim().trim('"') }
        .firstOrNull { it.isNotEmpty() }
}
