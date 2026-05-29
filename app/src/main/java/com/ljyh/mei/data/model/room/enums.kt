package com.ljyh.mei.data.model.room

enum class SourceType { STREAM, DOWNLOAD, LOCAL }

enum class DownloadStatus { PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED }

enum class PlaylistType {
    USER, NETEAST, AUTO_RECENT, AUTO_DOWNLOAD, AUTO_MOST, FOLDER, LOCAL_ALL, FAVORITES, LOSSLESS
}
