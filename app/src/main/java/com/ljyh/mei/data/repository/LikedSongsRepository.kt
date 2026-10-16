package com.ljyh.mei.data.repository

import android.content.Context
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.di.repository.LikeRepository
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the remote like list and the account-scoped Room cache in sync. */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LikedSongsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val local: LikeRepository,
) {
    private val syncLock = Mutex()
    private var syncedAccount: Pair<String, String>? = null

    fun observeLiked(songId: String): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserIdKey].orEmpty().takeIf {
                it.isNotBlank() && !preferences[CookieKey].isNullOrBlank()
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(false) else local.observeLiked(userId, songId)
        }

    suspend fun isLiked(songId: String): Boolean {
        val userId = currentUserId() ?: return false
        return local.isLiked(userId, songId)
    }

    suspend fun refresh(userId: String, force: Boolean = true): Resource<Int> = withContext(Dispatchers.IO) {
        safeApiCall {
            require(userId.isNotBlank()) { "用户 ID 为空" }
            syncLock.withLock {
                val cookie = accountCookie(userId)
                if (!force && syncedAccount == (userId to cookie)) {
                    return@withLock local.getAllForUser(userId).size
                }
                val response = apiService.getLikedSongs(userId)
                check(response.code == 200) { "喜欢歌曲列表请求失败 (${response.code})" }
                check(accountCookie(userId) == cookie) { "登录账号已变化" }
                val ids = response.ids.map(Long::toString).distinct()
                local.replaceAllForUser(userId, ids)
                syncedAccount = userId to cookie
                ids.size
            }
        }
    }

    suspend fun setLiked(songId: String, liked: Boolean) = withContext(Dispatchers.IO) {
        syncLock.withLock {
            val userId = currentUserId() ?: error("请先登录")
            val cookie = accountCookie(userId)
            val previous = local.isLiked(userId, songId)
            local.setLiked(userId, songId, liked)
            try {
                val response = apiService.like(Like(trackId = songId, like = liked))
                check(response.code == 200) { "红心操作失败 (${response.code})" }
                check(accountCookie(userId) == cookie) { "登录账号已变化" }
            } catch (error: Throwable) {
                withContext(NonCancellable) {
                    local.setLiked(userId, songId, previous)
                }
                throw error
            }
        }
    }

    private suspend fun currentUserId(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[UserIdKey]?.takeIf {
            it.isNotBlank() && !preferences[CookieKey].isNullOrBlank()
        }
    }

    private suspend fun accountCookie(userId: String): String {
        val preferences = context.dataStore.data.first()
        check(preferences[UserIdKey] == userId) { "登录账号已变化" }
        return preferences[CookieKey]?.takeIf(String::isNotBlank) ?: error("请先登录")
    }
}
