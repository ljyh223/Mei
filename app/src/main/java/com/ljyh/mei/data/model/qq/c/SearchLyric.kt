package com.ljyh.mei.data.model.qq.c

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

//<command-lable-xwl78-qq-music>
//<cmd value="4105" verson="7">
//<result>0</result>
//<reason></reason>
//<qq uin="" />
//<songcount>3</songcount>
//<songname><![CDATA[Surges]]></songname>
//<singer><![CDATA[orangestar]]></singer>
//<result_type>1</result_type>
//<rangemin>1</rangemin>
//<rangemax>20</rangemax>
//<songinfo id="321253074" scroll="1">
//<seqnum>0</seqnum>
//<name><![CDATA[Surges]]></name>
//<singername><![CDATA[Orangestar]]></singername>
//<albumname><![CDATA[Surges]]></albumname>
//</songinfo>
//<songinfo id="393758921" scroll="1">
//<seqnum>1</seqnum>
//<name><![CDATA[Surges]]></name>
//<singername><![CDATA[Orangestar]]></singername>
//<albumname><![CDATA[Surges]]></albumname>
//</songinfo>
//<songinfo id="430626622" scroll="1">
//<seqnum>2</seqnum>
//<name><![CDATA[Surges]]></name>
//<singername><![CDATA[Orangestar]]></singername>
//<albumname><![CDATA[And+So+Henceforth%2C]]></albumname>
//</songinfo>
//</cmd>
//</command-lable-xwl78-qq-music>


@Serializable
@XmlSerialName("command-lable-xwl78-qq-music", "", "")
data class SearchLyricCmd(
    @XmlElement(true) val cmd: SCmd
)


@Serializable
@XmlSerialName("cmd", "", "")
data class SCmd(
    val value: Int,
    val verson: Int,

    @XmlElement(true) val result: Int,

    @XmlElement(true) val reason: String,

    @XmlElement(true) val qq: String,


    @XmlElement(true)
    @XmlSerialName("songcount","","")
    val songCount: String,


    @XmlElement(true)
    @XmlSerialName("songname","","")
    val songName: String,


    @XmlElement(true) val singer: String,


    @XmlElement(true)
    @XmlSerialName("result_type")
    val resultType: Int,


    @XmlElement(true)
    @XmlSerialName("rangemin","","")
    val rangeMin: Int,

    @XmlElement(true)
    @XmlSerialName("rangemax","","")
    val rangeMax: Int,


    @XmlElement(true)
    @XmlSerialName("songinfo","","")
    val songInfo: List<SongInfo>
)


@Serializable
data class SongInfo(
    val id: String,
    val scroll:Int,

    @XmlElement(true)
    @XmlSerialName("seqnum","","")
    val seqNum: Int,

    @XmlElement(true) val name: String,

    @XmlElement(true)
    @XmlSerialName("singername","","")
    val singerName: String,

    @XmlElement(true)
    @XmlSerialName("albumname","","")
    val albumName: String
)