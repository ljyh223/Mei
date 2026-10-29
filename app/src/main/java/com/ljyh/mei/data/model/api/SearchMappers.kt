package com.ljyh.mei.data.model.api

import com.ljyh.mei.data.model.domain.SearchSuggestions

internal fun SearchSuggest.toDomain(): SearchSuggestions = SearchSuggestions(
    songs = result.songs.orEmpty().map { SearchSuggestions.Suggestion(it.id, it.name) },
    artists = result.artists.orEmpty().map { SearchSuggestions.Suggestion(it.id.toLong(), it.name) },
    albums = result.albums.orEmpty().map { SearchSuggestions.Suggestion(it.id.toLong(), it.name) },
)
