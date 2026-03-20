package com.pauwma.glyphbeat.themes.animation

import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.ui.settings.CommonSettingIds
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues
import com.pauwma.glyphbeat.ui.settings.SettingCategories
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsBuilder
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getDropdownValue
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueFloat

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
) : AnimationTheme(), ThemeSettingsProvider {

    var speedMultiplier: Float = 1.0f
        private set
    var loopMode: String = CommonSettingValues.LoopModes.NORMAL
        private set

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

    // ThemeSettingsProvider implementation

    override fun getSettingsId(): String = "custom_$postId"

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = CommonSettingIds.ANIMATION_SPEED,
                displayName = "Animation Speed",
                description = "Playback speed multiplier",
                defaultValue = 1.0f,
                minValue = 0.5f,
                maxValue = 2.0f,
                stepSize = 0.1f,
                unit = "x",
                category = SettingCategories.ANIMATION
            )
            .addDropdownSetting(
                id = CommonSettingIds.LOOP_MODE,
                displayName = "Loop Mode",
                description = "How the animation loops",
                defaultValue = CommonSettingValues.LoopModes.NORMAL,
                optionsMap = mapOf(
                    CommonSettingValues.LoopModes.NORMAL to "Forward",
                    CommonSettingValues.LoopModes.REVERSE to "Reverse",
                    CommonSettingValues.LoopModes.PING_PONG to "Ping Pong"
                ),
                category = SettingCategories.ANIMATION
            )
            .build()
    }

    override fun applySettings(settings: ThemeSettings) {
        speedMultiplier = settings.getSliderValueFloat(
            CommonSettingIds.ANIMATION_SPEED, 1.0f
        ).coerceIn(0.5f, 2.0f)
        loopMode = settings.getDropdownValue(
            CommonSettingIds.LOOP_MODE, CommonSettingValues.LoopModes.NORMAL
        )
    }
}
