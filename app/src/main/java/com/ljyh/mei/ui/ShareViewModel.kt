package com.ljyh.mei.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.constants.UserPhotoKey
import com.ljyh.mei.data.model.domain.UserAccountSummary
import com.ljyh.mei.data.model.auth.QrLoginStatus
import com.ljyh.mei.data.model.auth.QrLoginUiState
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.ShareRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
    private val userRepository: UserRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {


    private val _userAccount = MutableStateFlow<Resource<UserAccountSummary>>(Resource.Loading)
    val userAccount: StateFlow<Resource<UserAccountSummary>> = _userAccount

    private val _qrLoginState = MutableStateFlow<QrLoginUiState>(QrLoginUiState.Idle)
    val qrLoginState: StateFlow<QrLoginUiState> = _qrLoginState.asStateFlow()

    private var qrLoginJob: Job? = null

    fun getUserAccount(){
        viewModelScope.launch {
            _userAccount.value = Resource.Loading
            _userAccount.value = userRepository.getUserAccount()
        }
    }

    fun startQrLogin() {
        qrLoginJob?.cancel()
        qrLoginJob = viewModelScope.launch {
            _qrLoginState.value = QrLoginUiState.Loading
            val keyResult = userRepository.createQrLoginKey()
            if (!currentCoroutineContext().isActive) return@launch
            when (keyResult) {
                is Resource.Error -> {
                    _qrLoginState.value = QrLoginUiState.Error(keyResult.message)
                }

                Resource.Loading -> Unit
                is Resource.Success -> {
                    if (keyResult.data.code != 200) {
                        _qrLoginState.value = QrLoginUiState.Error(
                            keyResult.data.message ?: "获取二维码失败（${keyResult.data.code}）",
                        )
                        return@launch
                    }
                    val unikey = keyResult.data.unikey?.takeIf(String::isNotBlank)
                    if (unikey == null) {
                        _qrLoginState.value = QrLoginUiState.Error("获取二维码失败：响应中缺少登录凭据")
                        return@launch
                    }
                    val loginUrl = buildQrLoginUrl(unikey)
                    _qrLoginState.value = QrLoginUiState.Waiting(loginUrl)
                    pollQrLogin(unikey, loginUrl)
                }
            }
        }
    }

    fun refreshQrLogin() = startQrLogin()

    fun stopQrLogin() {
        qrLoginJob?.cancel()
        qrLoginJob = null
        _qrLoginState.value = QrLoginUiState.Idle
    }

    private suspend fun pollQrLogin(unikey: String, loginUrl: String) {
        var consecutiveNetworkErrors = 0
        while (currentCoroutineContext().isActive) {
            delay(QR_POLL_INTERVAL_MS)
            val result = userRepository.checkQrLogin(unikey)
            if (!currentCoroutineContext().isActive) return
            when (result) {
                is Resource.Error -> {
                    consecutiveNetworkErrors += 1
                    if (consecutiveNetworkErrors >= MAX_CONSECUTIVE_NETWORK_ERRORS) {
                        _qrLoginState.value = QrLoginUiState.Error(result.message, loginUrl)
                        return
                    }
                }

                Resource.Loading -> Unit
                is Resource.Success -> {
                    consecutiveNetworkErrors = 0
                    val check = result.data
                    when (QrLoginStatus.fromCode(check.code)) {
                        QrLoginStatus.Expired -> {
                            _qrLoginState.value = QrLoginUiState.Expired(loginUrl)
                            return
                        }

                        QrLoginStatus.Waiting -> {
                            _qrLoginState.value = QrLoginUiState.Waiting(loginUrl)
                        }

                        QrLoginStatus.Scanned -> {
                            _qrLoginState.value = QrLoginUiState.Scanned(loginUrl, check.nickname)
                        }

                        QrLoginStatus.Success -> {
                            val musicU = check.musicU
                            if (musicU.isNullOrBlank()) {
                                _qrLoginState.value = QrLoginUiState.Error(
                                    message = "Login succeeded but MUSIC_U was missing",
                                    loginUrl = loginUrl,
                                )
                                return
                            }
                            context.dataStore.edit {
                                it[CookieKey] = musicU
                                it.remove(UserIdKey)
                                it.remove(UserNicknameKey)
                                it.remove(UserAvatarUrlKey)
                                it.remove(UserPhotoKey)
                            }
                            _qrLoginState.value = QrLoginUiState.Success(check.nickname)
                            refreshUserAccount()
                            return
                        }

                        QrLoginStatus.Unknown -> Unit
                    }
                }
            }
        }
    }

    private suspend fun refreshUserAccount() {
        _userAccount.value = Resource.Loading
        _userAccount.value = userRepository.getUserAccount()
    }

    override fun onCleared() {
        qrLoginJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val QR_POLL_INTERVAL_MS = 1_200L
        const val MAX_CONSECUTIVE_NETWORK_ERRORS = 15

        fun buildQrLoginUrl(unikey: String): String {
            val encodedKey = URLEncoder.encode(unikey, StandardCharsets.UTF_8.toString())
            return "https://music.163.com/login?codekey=$encodedKey"
        }
    }


}
