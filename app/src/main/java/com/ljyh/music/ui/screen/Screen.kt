package com.ljyh.music.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

sealed class Screen(val route:String) {
    data object Home:Screen("home")
    data object Library:Screen("library")
    data object Search:Screen("search")
    data object PlayList:Screen("playlist")
    data object Setting:Screen("setting")
    data object ContentSettings:Screen(("setting/content"))
    data object AppearanceSettings:Screen("setting/appearance")
    data object Test:Screen("test")
    inline fun navigate(
        navController: NavController,
        builder: NavigationBuilder.() -> Unit = {}
    ) {
        navController.navigate(NavigationBuilder(route).apply(builder).build())
    }

    companion object {
        val MainScreens = listOf(Home, Library)
    }
}

enum class Index(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "For You", Icons.Rounded.Recommend),
    Library("library", "Library", Icons.Rounded.LibraryMusic),
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