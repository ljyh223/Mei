package com.ljyh.music

import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.LocalTime
import com.google.gson.annotations.SerializedName
import java.util.ArrayList

fun main(){
    val testJson="""{"sgc":false,"sfy":false,"qfy":false,"transUser":{"id":74215657,"status":99,"demand":1,"userid":1345789883,"nickname":"夏树遥","uptime":1735116444747},"lrc":{"version":3,"lyric":"[00:00.00] 作词 : 奥華子\n[00:01.00] 作曲 : 奥華子\n[00:16:45]私が産まれた時に買った　ぬいぐるみとか\n[00:22:88]子供の頃に描いてた　クレヨンの絵とか\n[00:29:07]学校でもらってきた　表彰状も通信簿も\n[00:35:38]全部ここに綺麗に　とってあるんだよと\n[00:43:39]まるで宝箱でも開けるように\n[00:49:69]あなたは嬉しそうに　見せてくれた時\n[00:55:64]「なんでそんなの　まだとってあるの\n[00:59:33]もう捨てればいいのに」と　どうして簡単に言ったんだろう\n[01:09:00]何十年も大切に　色褪せた　ガラクタの私の思い出を\n[01:21:92]何十年も変わらずに　持っていてくれる人は　世界中で　あなただけ\n[01:46:56]世間一般に言う　親孝行なことも\n[01:52:71]何一つ出来ないまま　時だけ流れて\n[01:58:89]仕事で成功するとか　そんなことよりもただ\n[02:05:08]私が元気でいることを願ってくれる人\n[02:12:99]寒い冬の朝でも　みんなの部屋に行くと\n[02:19:21]いつもストーブつけて　暖めてあった\n[02:24:88]誰よりも早くに起きて　ご飯を作ってくれた\n[02:31:87]どんな時も　当たり前みたいに\n[02:38:25]何十年もたった今　鮮やかに思い出す　あなたの優しさを\n[02:50:53]何十年もありがとう　厳しさも　うるささも　すべてが私への愛でした\n[03:18:34]何十年も大切に　色褪せた　ガラクタの私の思い出を\n[03:30:43]何十年も変わらずに　持っていてくれる人なんて　あなたしかいない\n[03:42:72]何十年もたった今　鮮やかに思い出す　あなたの優しさを\n[03:54:88]何十年もありがとう　厳しさも　うるささも　すべてが私への愛でした\n"},"klyric":{"version":0,"lyric":""},"tlyric":{"version":4,"lyric":"[by:夏树遥]\n[00:16:45]我出生时买的毛绒玩具\n[00:22:88]儿时手绘的蜡笔画\n[00:29:07]学校颁发的荣誉证书和成绩单\n[00:35:38]都被你整齐地保存在这里\n[00:43:39]仿佛打开一座宝藏之箱\n[00:49:69]你欣喜地向我展示那时\n[00:55:64]「为何这些还留着\n[00:59:33]早该扔掉了」我怎么会轻易这样说\n[01:09:00]几十年来珍藏的，那些褪色的琐碎回忆\n[01:21:92]世界上唯一不变的，是你这么多年来的珍视\n[01:46:56]世俗眼中的孝顺\n[01:52:71]我未能做到一事，时光却悄悄流逝\n[01:58:89]不求事业有成，只愿我安康\n[02:05:08]你总是如此期盼\n[02:12:99]即便是寒冬清晨，走进每一个房间\n[02:19:21]总能见到你早已点燃的暖炉\n[02:24:88]比任何人都早起，为大家准备早餐\n[02:31:87]你的付出仿佛理所当然\n[02:38:25]几十年过去，我依然清晰地记得你的温柔\n[02:50:53]几十年的感谢，你的严厉、唠叨，都是对我的爱\n[03:18:34]几十年来珍藏的，那些褪色的琐碎回忆\n[03:30:43]几十年如一日，世上唯一不变的是你的珍视\n[03:42:72]几十年过去，我依然清晰地记得你的温柔\n[03:54:88]几十年的感谢，你的严厉、唠叨，都是对我的爱\n"},"romalrc":{"version":2,"lyric":"[00:16.450]wa ta shi ga u ma re ta to ki ni ka tta nu i gu ru mi to ka\n[00:22.880]ko do mo no ko ro ni e ga i te ta ku re yo n no e to ka\n[00:29.070]ga kko u de mo ra tte ki ta hyo u sho u jo u mo tsu u shi n bo mo\n[00:35.380]ze n bu ko ko ni ki re i ni to tte a ru n da yo to\n[00:43.390]ma ru de ta ka ra ba ko de mo a ke ru yo u ni\n[00:49.690]a na ta wa u re shi so u ni mi se te ku re ta to ki\n[00:55.640]「na n de so n na no ma da to tte a ru no\n[00:59.330]mo u su te re ba i i no ni」to do u shi te ka n ta n ni i tta n da ro u\n[01:09.000]na n ju u ne n mo ta i se tsu ni i ro a se ta ga ra ku ta no wa ta shi no o mo i de wo\n[01:21.920]na n ju u ne n mo ka wa ra zu ni mo tte i te ku re ru hi to wa se ka i ju u de a na ta da ke\n[01:46.560]se ke n'i ppa n ni yu u o ya ko u ko u na ko to mo\n[01:52.710]na ni hi to tsu de ki na i ma ma to ki da ke na ga re te\n[01:58.890]shi go to de se i ko u su ru to ka so n na ko to yo ri mo ta da\n[02:05.080]wa ta shi ga ge n ki de i ru ko to wo ne ga tte ku re ru hi to\n[02:12.990]sa mu i fu yu no a sa de mo mi n na no he ya ni i ku to\n[02:19.210]i tsu mo su too bu tsu ke te a ta ta me te a tta\n[02:24.880]da re yo ri mo ha ya ku ni o ki te go ha n wo tsu ku tte ku re ta\n[02:31.870]do n na to ki mo a ta ri ma e mi ta i ni\n[02:38.250]na n ju u ne n mo ta tta i ma a za ya ka ni o mo i da su a na ta no ya sa shi sa wo\n[02:50.530]na n ju u ne n mo a ri ga to u ki bi shi sa mo u ru sa sa mo su be te ga wa ta shi e no a i de shi ta\n[03:18.340]na n ju u ne n mo ta i se tsu ni i ro a se ta ga ra ku ta no wa ta shi no o mo i de wo\n[03:30.430]na n ju u ne n mo ka wa ra zu ni mo tte i te ku re ru hi to na n te a na ta shi ka i na i\n[03:42.720]na n ju u ne n mo ta tta i ma a za ya ka ni o mo i da su a na ta no ya sa shi sa wo\n[03:54.880]na n ju u ne n mo a ri ga to u ki bi shi sa mo u ru sa sa mo su be te ga wa ta shi e no a i de shi ta"},"code":200}"""
    val lyric=Gson().fromJson(testJson, Lyric::class.java)
    val mLyric=LyricUtil.getLyric(lyric)
    println(mLyric)

}


data class Lyric(
    @SerializedName("code")
    val code: Int,
    @SerializedName("klyric")
    val klyric: Klyric,
    @SerializedName("lrc")
    val lrc: Lrc,
    @SerializedName("qfy")
    val qfy: Boolean,
    @SerializedName("romalrc")
    val romalrc: Romalrc,
    @SerializedName("sfy")
    val sfy: Boolean,
    @SerializedName("sgc")
    val sgc: Boolean,
    @SerializedName("tlyric")
    val tlyric: Tlyric,
    @SerializedName("pureMusic")
    val pureMusic: Boolean?
) {
    data class Klyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Lrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Romalrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Tlyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )
}

object LyricUtil {
    fun getLyric(lyric: Lyric): String {
        val _lyric= lyric.lrc.lyric
        if(lyric.pureMusic==null || lyric.pureMusic==false){
            if(lyric.tlyric==null){
                return _lyric
            }
            return mergedLyric(_lyric, lyric.tlyric.lyric)
        }

        return _lyric
    }

    private fun mergedLyric(lyric: String, tlyric: String): String {
        // 去除末尾的空白字符
        val lyricT = lyric.trim()
        val tlyricT = tlyric.trim()

        // 使用 HashMap 存储翻译歌词，键为时间，值为对应的歌词
        val tlyricMap = mutableMapOf<String, String>()

        // 处理翻译歌词，每行按 "]" 分割，存储到 HashMap
        tlyricT.lines().forEach { line ->
            val parts = line.split("]", limit = 2)
            if (parts.size == 2) {
                val time = parts[0].removePrefix("[") // 提取时间
                val text = parts[1] // 提取歌词
                tlyricMap[time] = text
            }
        }

        val merged = ArrayList<String>()

        // 处理主歌词
        lyricT.lines().forEach { line ->
            val parts = line.split("]", limit = 2)
            if (parts.size == 2) {
                val time = parts[0].removePrefix("[") // 提取时间
                val text = parts[1] // 提取歌词

                // 如果翻译歌词没有对应时间戳，直接添加原歌词
                if (time !in tlyricMap) {
                    merged.add("[$time]$text")
                } else {
                    // 否则，合并两行歌词
                    merged.add("[$time]$text")
                    merged.add("[$time]${tlyricMap[time]}")
                }
            }
        }
        println(merged.size)
        return merged.joinToString("\n")
    }

}