package com.ljyh.mei.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.response.AlbumDynamic
import com.ljyh.mei.data.model.room.AlbumSubscription
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.dao.AlbumSubscriptionDao
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Account-scoped confirmed state: Room first, remote revalidation in the background. */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumSubscriptionRepository internal constructor(
    private val dao: AlbumSubscriptionDao,
    private val preferences: Flow<Preferences>,
    private val load: suspend (Long) -> Resource<AlbumDynamic>,
    private val change: suspend (Long, Boolean) -> Resource<BaseResponse>,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        database: AppDatabase,
        repository: PlaylistRepository,
    ) : this(database.albumSubscriptionDao(), context.dataStore.data, repository::getAlbumDynamic,
        { id, subscribed ->
            if (subscribed) repository.subscribeAlbum(id.toString())
            else repository.unsubscribeAlbum(id.toString())
        })

    private class Account(val userId: String, val cookie: String) {
        fun matches(other: Account?): Boolean = other?.userId == userId && other.cookie == cookie
        override fun equals(other: Any?): Boolean = other is Account && matches(other)
        override fun hashCode(): Int = 31 * userId.hashCode() + cookie.hashCode()
        override fun toString(): String = "AlbumSubscriptionAccount(redacted)"
    }

    private fun Preferences.account(): Account? {
        val userId = this[UserIdKey]?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }
        val cookie = this[CookieKey]?.takeIf(String::isNotBlank)
        return if (userId != null && cookie != null) Account(userId, cookie) else null
    }

    private val locks = ConcurrentHashMap<Pair<String, Long>, Mutex>()
    private fun lock(account: Account, albumId: Long) = locks.getOrPut(account.userId to albumId) { Mutex() }
    private suspend fun checkAccount(account: Account) {
        check(account.matches(preferences.first().account())) { "登录账号已变化" }
    }

    fun observe(albumId: Long): Flow<Resource<Boolean>> = preferences
        .map { it.account() }.distinctUntilChanged()
        .flatMapLatest { account ->
            if (account == null) flowOf(Resource.Success(false)) else channelFlow<Resource<Boolean>> {
                val cached = dao.get(account.userId, albumId)
                send(cached?.let { Resource.Success(it.subscribed) } ?: Resource.Loading)
                launch {
                    dao.observe(account.userId, albumId).filterNotNull().collect {
                        send(Resource.Success(it.subscribed))
                    }
                }
                val result = refresh(account, albumId)
                // A failed revalidation never discards a known true OR false state.
                if (result is Resource.Error && dao.get(account.userId, albumId) == null) send(result)
                awaitCancellation()
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)

    private suspend fun refresh(account: Account, albumId: Long): Resource<Unit> = safeApiCall {
        lock(account, albumId).withLock {
            checkAccount(account)
            when (val result = load(albumId)) {
                is Resource.Success -> {
                    val response = result.data
                    check(response.code == 200 && response.isSubscribed != null) { "获取专辑收藏状态失败" }
                    checkAccount(account)
                    dao.upsert(AlbumSubscription(account.userId, albumId, requireNotNull(response.isSubscribed), System.currentTimeMillis()))
                }
                is Resource.Error -> error(result.message)
                Resource.Loading -> error("获取专辑收藏状态失败")
            }
        }
    }

    suspend fun setSubscribed(albumId: Long, subscribed: Boolean): Resource<BaseResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            val account = preferences.first().account() ?: error("请先登录后收藏专辑")
            lock(account, albumId).withLock {
                checkAccount(account)
                val response = when (val result = change(albumId, subscribed)) {
                    is Resource.Success -> result.data
                    is Resource.Error -> error(result.message)
                    Resource.Loading -> error("专辑收藏操作失败")
                }
                check(response.code == 200) { "专辑收藏操作失败 (${response.code})" }
                checkAccount(account)
                dao.upsert(AlbumSubscription(account.userId, albumId, subscribed, System.currentTimeMillis()))
                response
            }
        }
    }
}
