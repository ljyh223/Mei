package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.GetSearch
import com.ljyh.mei.data.model.api.GetSearchSuggest
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.api.SearchSuggest
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    val apiService: ApiService
) {
    suspend fun search( keyword: String, type: Int, limit: Int): Resource<SearchResult> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.search(
                    GetSearch(
                        s = keyword,
                        type = type,
                        limit = limit
                    )
                )
            }
        }
    }

    suspend fun searchSuggest( keyword: String): Resource<SearchSuggest> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.searchSuggest(
                    GetSearchSuggest(
                        s = keyword
                    )
                )
            }
        }
    }
}