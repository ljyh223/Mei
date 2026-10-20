package com.ljyh.mei.utils.image

/** Tunable cutoffs for the flat-background artwork experiment. */
internal data class CoverBackgroundConfig(
    val colorTolerance: Float = 0.075f,
    val minimumBackgroundConfidence: Float = 0.65f,
    val solidEdgeMatch: Float = 0.90f,
    val softEdgeMatch: Float = 0.70f,
    val minimumSingleEdgeMatch: Float = 0.55f,
)
