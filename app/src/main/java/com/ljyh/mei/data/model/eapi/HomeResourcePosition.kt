package com.ljyh.mei.data.model.eapi

private fun positionPriority(positionCode: String): Int = when (positionCode) {
    "PAGE_RECOMMEND_DAILY_RECOMMEND" -> 1
    "PAGE_RECOMMEND_RADAR" -> 2
    "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" -> 3
    "PAGE_RECOMMEND_RANK" -> 4
    "PAGE_RECOMMEND_PRIVATE_RCMD_SONG" -> 5
    else -> Int.MAX_VALUE
}

val homeResourcePositionComparator = Comparator<HomePageResourceShow.Data.Block> { a, b ->
    positionPriority(a.positionCode).compareTo(positionPriority(b.positionCode))
}
