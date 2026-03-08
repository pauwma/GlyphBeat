package com.pauwma.glyphbeat.themes.animation

import com.pauwma.glyphbeat.themes.base.AnimationTheme

/**
 * A custom theme imported from Glyph Museum.
 * Contains pre-converted 625-element frame data and per-frame durations.
 */
class CustomTheme(
    val postId: Long,
    private val title: String,
    val author: String,
    private val frames: List<IntArray>,
    private val durations: LongArray,
    val importDate: Long
) : AnimationTheme() {

    override fun getFrameCount(): Int = frames.size

    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        return frames[frameIndex]
    }

    override fun getThemeName(): String = title

    override fun getAnimationSpeed(): Long = durations.firstOrNull() ?: 150L

    override fun getDescription(): String = "@$author"

    /**
     * Per-frame duration support matching ThemeTemplate's interface.
     */
    fun getFrameDuration(frameIndex: Int): Long {
        return durations.getOrElse(frameIndex) { 150L }
    }

    fun hasIndividualFrameDurations(): Boolean = true
}
