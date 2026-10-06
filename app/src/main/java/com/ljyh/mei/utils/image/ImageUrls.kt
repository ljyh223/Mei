package com.ljyh.mei.utils.image

fun String.smallImage(): String = if (startsWith("/")) this else "$this?param=100y100"

fun String.middleImage(): String = if (startsWith("/")) this else "$this?param=300y300"

fun String.largeImage(): String = if (startsWith("/")) this else "$this?param=500y500"

fun String.size1600(): String = "$this?param=1600y1600"
