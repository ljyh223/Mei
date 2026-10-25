package com.ljyh.mei.utils.image

/** Matching thresholds; sampling sizes and the OKLab scoring formula stay fixed. */
internal data class CoverBackgroundConfig(
    val colorTolerance: Float = 0.075f,
    val minimumBackgroundConfidence: Float = 0.65f,
    val solidEdgeMatch: Float = 0.90f,
    val softEdgeMatch: Float = 0.70f,
    val minimumSingleEdgeMatch: Float = 0.55f,
    val maximumBandTexture: Float = 0.008f,
    val maximumBandGradient: Float = 0.045f,
    val minimumBandShare: Float = 0.55f,
    val minimumWholeShare: Float = 0.30f,
    val maximumSolidGap: Float = 0.10f,
    val maximumSoftGap: Float = 0.35f,
    val solidSideSlack: Float = 0.12f,
    val singleEdgeStrongMatch: Float = 0.85f,
    val threeSideMinimumMatch: Float = 0.30f,
    val allowSingleOccupiedSide: Boolean = true,
    val allowThreeStrongSides: Boolean = true,
) {
    fun normalized(): CoverBackgroundConfig {
        val defaults = CoverBackgroundConfig()
        val solid = solidEdgeMatch.safe(defaults.solidEdgeMatch, 0f..1f)
        val softGap = maximumSoftGap.safe(defaults.maximumSoftGap, 0f..1f)
        return copy(
            colorTolerance = colorTolerance.safe(defaults.colorTolerance, 0.025f..0.15f),
            minimumBackgroundConfidence = minimumBackgroundConfidence.safe(defaults.minimumBackgroundConfidence, 0f..1f),
            solidEdgeMatch = solid,
            softEdgeMatch = softEdgeMatch.safe(defaults.softEdgeMatch, 0f..solid),
            minimumSingleEdgeMatch = minimumSingleEdgeMatch.safe(defaults.minimumSingleEdgeMatch, 0f..solid),
            maximumBandTexture = maximumBandTexture.safe(defaults.maximumBandTexture, 0f..0.1f),
            maximumBandGradient = maximumBandGradient.safe(defaults.maximumBandGradient, 0f..0.2f),
            minimumBandShare = minimumBandShare.safe(defaults.minimumBandShare, 0.1f..1f),
            minimumWholeShare = minimumWholeShare.safe(defaults.minimumWholeShare, 0f..1f),
            maximumSolidGap = maximumSolidGap.safe(defaults.maximumSolidGap, 0f..softGap),
            maximumSoftGap = softGap,
            solidSideSlack = solidSideSlack.safe(defaults.solidSideSlack, 0f..1f),
            singleEdgeStrongMatch = singleEdgeStrongMatch.safe(defaults.singleEdgeStrongMatch, 0f..1f),
            threeSideMinimumMatch = threeSideMinimumMatch.safe(defaults.threeSideMinimumMatch, 0f..1f),
        )
    }
}

private fun Float.safe(default: Float, range: ClosedFloatingPointRange<Float>) =
    (if (isFinite()) this else default).coerceIn(range.start, range.endInclusive)

internal enum class CoverBackgroundPreset(
    val label: String,
    val description: String,
    val config: CoverBackgroundConfig?,
) {
    Relaxed("宽松", "接受轻微纹理和更明显的边缘遮挡，让更多封面参与扩展", CoverBackgroundConfig(
        colorTolerance = 0.095f, minimumBackgroundConfidence = 0.56f,
        solidEdgeMatch = 0.86f, softEdgeMatch = 0.60f, minimumSingleEdgeMatch = 0.40f,
        maximumBandTexture = 0.012f, maximumBandGradient = 0.065f,
        minimumBandShare = 0.50f, minimumWholeShare = 0.25f,
        maximumSolidGap = 0.16f, maximumSoftGap = 0.45f, solidSideSlack = 0.16f,
        singleEdgeStrongMatch = 0.80f, threeSideMinimumMatch = 0.20f,
    )),
    Standard("标准", "沿用当前判断条件，在扩展范围与自然过渡之间保持平衡", CoverBackgroundConfig()),
    Strict("严格", "更偏向平整纯色和完整边缘，关闭单边遮挡等例外规则", CoverBackgroundConfig(
        colorTolerance = 0.055f, minimumBackgroundConfidence = 0.75f,
        solidEdgeMatch = 0.96f, softEdgeMatch = 0.84f, minimumSingleEdgeMatch = 0.75f,
        maximumBandTexture = 0.004f, maximumBandGradient = 0.025f,
        minimumBandShare = 0.70f, minimumWholeShare = 0.45f,
        maximumSolidGap = 0.04f, maximumSoftGap = 0.15f, solidSideSlack = 0.06f,
        singleEdgeStrongMatch = 0.96f, threeSideMinimumMatch = 0.75f,
        allowSingleOccupiedSide = false, allowThreeStrongSides = false,
    )),
    Custom("自定义", "逐项调整识别阈值与边缘例外规则", null),
}
