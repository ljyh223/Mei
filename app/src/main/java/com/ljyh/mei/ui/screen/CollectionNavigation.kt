package com.ljyh.mei.ui.screen

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun discoverPlaylistsRoute(category: String = "全部"): String =
    "${Screen.DiscoverPlaylists.route}?category=${URLEncoder.encode(category, StandardCharsets.UTF_8.name()).replace("+", "%20")}"
