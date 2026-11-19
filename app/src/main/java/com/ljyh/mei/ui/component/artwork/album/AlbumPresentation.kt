package com.ljyh.mei.ui.component.artwork.album


internal const val VinylPresentationDurationMillis = 2400
internal fun albumLinerCapacity(format: AlbumMediaStyle): Int =
    if (format == AlbumMediaStyle.CD || format == AlbumMediaStyle.Vinyl) 6 else 8
internal data class AlbumPackageTrack(val number: Int, val title: String, val durationMillis: Long) {
    val durationLabel: String get() {
        if (durationMillis <= 0) return ""
        val seconds = durationMillis / 1000
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}

internal fun albumLinerPages(
    tracks: List<AlbumPackageTrack>, format: AlbumMediaStyle = AlbumMediaStyle.Cassette,
): List<List<AlbumPackageTrack>> = tracks.chunked(albumLinerCapacity(format)).ifEmpty { listOf(emptyList()) }

internal data class AlbumPresentation(
    val opening: Float,
    val yawOffset: Float,
    val pitch: Float,
    val rotation: Float = 0f,
)

private fun easedSegment(progress: Float, start: Float = 0f, end: Float = 1f): Float {
    val t = ((progress - start) / (end - start)).coerceIn(0f, 1f)
    // Zero velocity and acceleration at each boundary, including an interrupted reversal.
    return t * t * t * (t * (t * 6f - 15f) + 10f)
}

/** A single reversible progress drives the mechanism and camera; no delayed follow-up jobs. */
internal fun albumPresentation(format: AlbumMediaStyle, progress: Float): AlbumPresentation {
    val p = progress.coerceIn(0f, 1f)
    val camera = easedSegment(p)
    return when (format) {
        AlbumMediaStyle.CD -> AlbumPresentation(135f * camera, -20f * camera, -8f + 5f * camera)
        AlbumMediaStyle.Cassette -> AlbumPresentation(p, 6f * camera, -8f + 4f * camera)
        // Extract before turning the sleeve over to its actual track listing. Closing reverses it.
        AlbumMediaStyle.Vinyl -> AlbumPresentation(easedSegment(p, 0f, .25f),
            180f * easedSegment(p, .32f, 1f), -8f + 4f * camera, 75f * easedSegment(p, .02f, .25f))
        AlbumMediaStyle.Plain -> AlbumPresentation(0f, 0f, 0f)
    }
}

internal data class AlbumReflectionGeometry(val top: Float, val height: Float)

/** Fade the projected mirror within the stage; never scale or move its geometry. */
internal fun albumReflectionGeometry(
    stageHeight: Float,
    modelBottom: Float,
    reflectionTop: Float,
    reflectionBottom: Float,
): AlbumReflectionGeometry? {
    if (!listOf(stageHeight, modelBottom, reflectionTop, reflectionBottom).all { it.isFinite() } || stageHeight <= 0f) return null
    val top = reflectionTop.coerceAtLeast(0f)
    // A far contact edge can project above the nearest edge. Start at its actual reflected
    // bounds, and let the fade extend below the nearest edge instead of cutting that edge off.
    val bottom = minOf(stageHeight, reflectionBottom, modelBottom + stageHeight * .14f)
    return if (bottom > top) AlbumReflectionGeometry(top, bottom - top) else null
}

/** User intent is separate from the progress at which a drag paused the mechanism. */
internal fun albumNextOpeningTarget(format: AlbumMediaStyle, target: Float): Float = when (format) {
    AlbumMediaStyle.Cassette -> when {
        target < .22f -> CassettePackageOpenProgress
        target < .73f -> 1f
        else -> 0f
    }
    else -> if (target > 0f) 0f else 1f
}

internal const val AlbumModelCenterY = .46f
internal const val AlbumModelHalfHeight = .38f

/** Share the model's empty upper edge with the toolbar fade, retaining a 1dp guard. */
internal fun albumModelTopClearanceDp(fadeDp: Float, stageHeightDp: Float, format: AlbumMediaStyle): Float {
    if (format == AlbumMediaStyle.Plain) return fadeDp
    val builtInInset = (stageHeightDp * (AlbumModelCenterY - AlbumModelHalfHeight) - 1f).coerceAtLeast(0f)
    return (fadeDp - builtInInset).coerceAtLeast(0f)
}
