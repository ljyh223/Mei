package com.ljyh.mei.data.model.qq.c

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
@XmlSerialName("QrcInfos")
data class QrcInfos(
    @XmlElement(true) val QrcHeadInfo: QrcHeadInfo,
    @XmlElement(true) val LyricInfo: LyricInfo
)

@Serializable
@XmlSerialName("QrcHeadInfo")
data class QrcHeadInfo(
    val SaveTime: Long,
    val Version: Int
)

@Serializable
@XmlSerialName("LyricInfo")
data class LyricInfo(
    val LyricCount: Int,
    @XmlElement(true) val lyric: Lyric_1
)

@Serializable
@XmlSerialName("Lyric_1")
data class Lyric_1(
    val LyricType: Int,
    val LyricContent: String
)
