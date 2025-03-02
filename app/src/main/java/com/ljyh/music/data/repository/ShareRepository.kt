package com.ljyh.music.data.repository

import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.api.GetLyric
import com.ljyh.music.data.model.api.GetLyricV1
import com.ljyh.music.data.model.api.GetSongUrl
import com.ljyh.music.data.model.api.GetSongUrlV1
import com.ljyh.music.data.model.qq.u.GetLyricData
import com.ljyh.music.data.model.qq.u.GetSearchData
import com.ljyh.music.data.model.qq.u.GetSearchData.Comm1
import com.ljyh.music.data.model.qq.u.LyricResult
import com.ljyh.music.data.model.qq.u.MusicU
import com.ljyh.music.data.model.qq.u.Search
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.QQMusicCApiService
import com.ljyh.music.data.network.QQMusicUApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareRepository(
    val apiService: ApiService,
    val qqMusicCApiService: QQMusicCApiService,
    val qqMusicUApiService: QQMusicUApiService
) {
    suspend fun getSongUrl(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrl(
                    GetSongUrl(
                        ids = "[$id]"
                    )
                )
            }
        }
    }

    suspend fun getSongUrlV1(id: String): Resource<SongUrl> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getSongUrlV1(
                    GetSongUrlV1(
                        ids = "[$id]"
                    )
                )
            }
        }
    }


    suspend fun getLyric(id: String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getLyric(
                    GetLyric(
                        id = id
                    )
                )
            }
        }
    }


    suspend fun getLyricV1(id: String): Resource<Lyric> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getLyricV1(
                    GetLyricV1(
                        id = id
                    )
                )
            }
        }
    }


    suspend fun getQQMusicLyric(id: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicCApiService.getQQMusicLyric(id) }
        }
    }


    suspend fun searchLyric(songName: String, singerName: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicCApiService.searchLyric(songName, singerName) }
        }
    }


    suspend fun searchU(keyword: String): Resource<Search> {
        val search = MusicU(
            MusicU.Comm(
                ct = "19",
                cv = "1859",
                uin = "0",
            ),
            MusicU.Req(
                module = "music.search.SearchCgiService",
                method = "DoSearchForQQMusicDesktop",
                param = MusicU.Req.Param(
                    query = keyword,
                    pageNum = 1,
                    numPerPage = 50,
                    grp = 1,
                    searchType = 0
                )
            )
        )
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicUApiService.search(search) }
        }
    }


    suspend fun searchC(keyword: String): Resource<com.ljyh.music.data.model.qq.c.Search> {
        return withContext(Dispatchers.IO) {
            safeApiCall { qqMusicCApiService.search(keyword) }
        }
    }


    suspend fun searchNew(keyword: String): Resource<SearchResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicUApiService.search(
                    GetSearchData(
                        comm = Comm1(),
                        req = GetSearchData.Req(param = GetSearchData.Req.Param(query = keyword))
                    )
                )
            }
        }
    }


    suspend fun getLyricNew(
        title: String,
        album: String,
        artist: String,
        duration: Int,
        id: Int
    ): Resource<LyricResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                qqMusicUApiService.getLyric(
                    GetLyricData(
                        comm = GetLyricData.Comm(),
                        GetLyricData.GetPlayLyricInfo(
                            param = GetLyricData.GetPlayLyricInfo.GetLyric(
                                singerName = artist,
                                songName = title,
                                albumName = album,
                                interval = duration,
                                songID = id

                            )
                        )
                    )
                )
            }
        }
    }

}