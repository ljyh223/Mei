package com.ljyh.mei.constants

import androidx.datastore.preferences.core.stringPreferencesKey

val AlbumArtworkModeKey = stringPreferencesKey("albumDetail.artworkMode")

enum class AlbumArtworkMode(val label: String) {
    Auto("自动"), Motion("动态封面"), Model("年代模型"), Static("静态封面")
}

val AlbumModelModeKey = stringPreferencesKey("albumDetail.modelMode")

enum class AlbumModelMode(val label: String) {
    Auto("自动判断"), Vinyl("黑胶唱片"), Cassette("盒式磁带"), CD("CD／DVD 光盘")
}
