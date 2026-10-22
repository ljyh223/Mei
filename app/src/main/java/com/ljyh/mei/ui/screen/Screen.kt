package com.ljyh.mei.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

sealed class Screen(val route:String) {
    data object Home:Screen("home")
    data object Library:Screen("library")
    data object LibraryCollection:Screen("library/collection")
    data object FindMusic: Screen("find_music")
    data object DiscoverPlaylists: Screen("discover/playlists")
    data object DiscoverAlbums: Screen("discover/albums")
    data object DiscoverCharts: Screen("discover/charts")
    data object AudioMatch: Screen("audio_match")
    data object SearchResult:Screen("search_result")
    data object PlayList:Screen("playlist")
    data object Setting:Screen("setting")
    data object LxSources:Screen("setting/sources")
    data object ContentSettings:Screen(("setting/content"))
    data object AppearanceSettings:Screen("setting/appearance")
    data object GlassSettings:Screen("setting/appearance/glass")
    data object BetaSettings:Screen("setting/beta")
    data object LyricsSettings:Screen("setting/lyrics")
    data object PlaySettings:Screen("setting/play")
    data object UsbExclusiveSettings:Screen("setting/play/usb")
    data object DeveloperSettings:Screen("setting/developer")
    data object Equalizer:Screen("setting/play/equalizer")
    data object ReverbSettings:Screen("setting/play/reverb")
    data object DownloadSettings:Screen("setting/download")
    data object CacheSettings:Screen("setting/cache")
    data object DownloadManage:Screen("download_manage")
    data object LocalMusic:Screen("local_music")
    data object LocalSongList:Screen("local_songs")
    data object EveryDay:Screen("everyday")
    data object Album: Screen("album")
    data object Artist: Screen("artist")
    data object ArtistSongs: Screen("artist_songs")
    data object ArtistAlbums: Screen("artist_albums")
    data object History: Screen("history")
    data object Comment: Screen("comment")
    data object Test:Screen("test")
    data object About: Screen("about")
    data object Log: Screen("log")

    inline fun navigate(
        navController: NavController,
        builder: NavigationBuilder.() -> Unit = {}
    ) {
        navController.navigate(NavigationBuilder(route).apply(builder).build())
    }

    companion object {
        // Nested route objects can initialize this base class first; defer reading their instances.
        val MainScreens: List<Screen> by lazy { listOf(Home, FindMusic, Library, Setting) }
    }
}

enum class Index(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "推荐", Icons.Rounded.Recommend),
    FindMusic("find_music","发现",Icons.Rounded.MusicVideo),
    Library("library", "音乐库", Icons.Rounded.LibraryMusic),
    Setting("setting", "设置", Icons.Rounded.Settings),
}



class NavigationBuilder(
    route: String
) {
    private var finalRoute: String = route
    private val query: MutableMap<String, String> = hashMapOf()

    fun addPath(path: String) {
        finalRoute += "/$path"
    }

    fun addQuery(key: String, value: String) {
        query += key to value
    }

    fun build(): String = if (query.isEmpty()) {
        finalRoute
    } else {
        "$finalRoute${
            query.entries.joinToString(
                separator = "&",
                prefix = "?"
            ) { "${it.key}=${it.value}" }
        }"
    }
}
