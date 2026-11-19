package com.ljyh.mei.demo.album

/** One reversible timeline: open the lid, lift clear of the rim, then turn and move aside. */
internal const val CassettePackageOpenProgress = .45f
internal const val CassetteBodyScale = .93f
internal const val CassetteCaseRimZ = .10f

internal data class CassettePackagePose(
    val lidAngle: Float,
    val tapeX: Float,
    val tapeZ: Float,
    val tapeAngle: Float,
)

internal fun cassettePackagePose(progress: Float): CassettePackagePose {
    fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    val progress = progress.coerceIn(0f, 1f)
    val lid = smooth(progress / CassettePackageOpenProgress)
    val extraction = smooth((progress - CassettePackageOpenProgress) / (1f - CassettePackageOpenProgress))
    val lift = smooth(extraction / .30f)
    val slide = smooth((extraction - .34f) / .66f)
    return CassettePackagePose(
        lidAngle = 145f * lid,
        tapeX = 1.34f * slide,
        tapeZ = .015f + .38f * lift,
        tapeAngle = 90f * (1f - slide),
    )
}
