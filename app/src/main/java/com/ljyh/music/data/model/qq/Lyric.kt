package com.ljyh.music.data.model.qq


import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("command-lable-xwl78-qq-music", "", "")
data class LyricCmd(
    @XmlElement(true) val cmd: Cmd
)

@Serializable
@XmlSerialName("cmd", "", "")
data class Cmd(
    val value: Int, // 对应 XML 中的属性 value
    val verson: Int, // 对应 XML 中的属性 verson（注意拼写是否正确）
    @XmlElement(true) val result: Int, // 子元素 <result>
    @XmlElement(true) val uin: Int, // 子元素 <uin>
    @XmlElement(true) val reason: String, // 子元素 <reason>
    @XmlElement(true) val check: Check, // 可选子元素 <check>
    @XmlElement(true) val lyric: Lyric // 可选子元素 <lyric>
)

@Serializable
@XmlSerialName("check", "", "")
data class Check(
    val musicid: String, // 对应 XML 中的属性 musicid
    val songtype: Int, // 对应 XML 中的属性 songtype
    @XmlElement(true) val downloadtag: Int // 子元素 <downloadtag>
)

@Serializable
@XmlSerialName("lyric", "", "")
data class Lyric(
    val hidesearch: Int, // 对应 XML 中的属性 hidesearch
    val musicid: String, // 对应 XML 中的属性 musicid
    val timetag: Long, // 对应 XML 中的属性 timetag
    val scroll: String, // 对应 XML 中的属性 scroll
    val urlhash: String, // 对应 XML 中的属性 urlhash
    val encode: String, // 对应 XML 中的属性 encode
    val kt: String, // 对应 XML 中的属性 kt
    @XmlElement(true) val adoptuser: AdoptUser, // 子元素 <adoptuser>
    @XmlElement(true) val content: Content, // 子元素 <content>
    @XmlElement(true) val contentTs: ContentTs, // 子元素 <contentts>
    @XmlElement(true) val contentRoma: ContentRoma // 子元素 <contentroma>
)

@Serializable
@XmlSerialName("adoptuser", "", "")
data class AdoptUser(
    val id: Int, // 对应 XML 中的属性 id
    @XmlElement(true) val uin: Int // 子元素 <uin>
)

@Serializable
@XmlSerialName("content", "", "")
data class Content(
    val type: String, // 对应 XML 中的属性 type
    val mime: String, // 对应 XML 中的属性 mime
    val timetag: Long, // 对应 XML 中的属性 timetag
    val filescroll: String, // 对应 XML 中的属性 filescroll
    @XmlValue val value: String = ""
)

@Serializable
@XmlSerialName("contentts", "", "")
data class ContentTs(
    val type: String, // 对应 XML 中的属性 type
    val mime: String, // 对应 XML 中的属性 mime
    val timetag: Long, // 对应 XML 中的属性 timetag
    @XmlValue val value: String = ""
)

@Serializable
@XmlSerialName("contentroma", "", "")
data class ContentRoma(
    val type: String, // 对应 XML 中的属性 type
    val mime: String, // 对应 XML 中的属性 mime
    val timetag: Long, // 对应 XML 中的属性 timetag
    @XmlValue val value: String = ""
)


object CDataSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CData", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString()
    }
}