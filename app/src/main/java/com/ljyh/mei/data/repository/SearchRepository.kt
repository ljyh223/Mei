package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.GetSearch
import com.ljyh.mei.data.model.api.GetSearchSuggest
import com.ljyh.mei.data.model.domain.SearchResults
import com.ljyh.mei.data.model.domain.SearchSuggestions
import com.ljyh.mei.data.model.api.toDomain
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    val apiService: ApiService
) {
    suspend fun search(keyword: String, type: Int, limit: Int): Resource<SearchResults> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.search(
                    GetSearch(
                        s = keyword,
                        type = type,
                        limit = limit
                    )
                ).toDomain()
            }
        }
    }

    suspend fun searchSuggest(keyword: String): Resource<SearchSuggestions> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.searchSuggest(
                    GetSearchSuggest(
                        s = keyword
                    )
                ).toDomain()
            }
        }
    }
}
