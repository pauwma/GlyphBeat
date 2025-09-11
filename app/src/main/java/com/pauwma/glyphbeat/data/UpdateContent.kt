package com.pauwma.glyphbeat.data

/**
 * Data class representing update content for a specific version.
 */
data class UpdateContent(
    val versionName: String,
    val versionCode: Int,
    val title: String,
    val subtitle: String? = null,
    val features: List<Feature>,
    val improvements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList()
) {
    data class Feature(
        val title: String,
        val description: String,
        val emoji: String? = null,
        val isHighlight: Boolean = false
    )
}

/**
 * Manager for update content across different versions.
 */
object UpdateManager {

    /**
     * Get update content for a specific version.
     * Returns null if no update content is defined for the version.
     */
    fun getUpdateContent(versionCode: Int): UpdateContent? {
        return updateContents[versionCode]
    }

    /**
     * Get the latest update content available.
     */
    fun getLatestUpdateContent(): UpdateContent? {
        return updateContents.values.maxByOrNull { it.versionCode }
    }

    /**
     * Check if update content exists for a version.
     */
    fun hasUpdateContent(versionCode: Int): Boolean {
        return updateContents.containsKey(versionCode)
    }

    // Define update content for each version
    private val updateContents = mutableMapOf(
        // Version 1.2.0 (Version Code 10)
        7 to UpdateContent(
            versionName = "1.2.1",
            versionCode = 11,
            title = "New Features Available!",
            subtitle = "Enhanced animations and controls",
            features = listOf(
                UpdateContent.Feature(
                    title = "Scroll‑Text Theme",
                    description = "New Theme! Title, artist, album all flexin’ on a nonstop scroll",
                    emoji = "\uD83D\uDD24",
                    isHighlight = true
                ),
                UpdateContent.Feature(
                    title = "Auto‑Start",
                    description = "Pops up on the Matrix instantly. Zero taps, full vibes",
                    emoji = "⚡",
                    isHighlight = true
                ),
                UpdateContent.Feature(
                    title = "Better Shake Controls",
                    description = "More settings to customize shake controls at your desire",
                    emoji = "\uD83D\uDCF3"
                ),
                UpdateContent.Feature(
                    title = "Universal Media Player",
                    description = "Enjoy Glyph Beat anywhere your music comes from",
                    emoji = "\uD83C\uDF10"
                )
            ),
            bugFixes = listOf(
                "Fixed notification listener issues",
                "Resolved theme switching delays"
            )
        )

        // Version 1.3.0 - Example for future :P
        /* 8 to UpdateContent(
             versionName = "1.3.0",
             versionCode = 11,
             title = "Performance Update",
             subtitle = "Faster, smoother, better",
             features = listOf(
                 UpdateContent.Feature(
                     title = "Audio Reactive Themes",
                     description = "Animations that react to your music",
                     emoji = "🎵",
                     isHighlight = true
                 ),
                 UpdateContent.Feature(
                     title = "Custom Theme Editor",
                     description = "Create your own unique animations",
                     emoji = "✏️",
                     isHighlight = true
                 )
             ),
             improvements = listOf(
                 "Reduced memory usage",
                 "Faster theme loading",
                 "Improved gesture recognition"
             )
         )*/
    )

    /**
     * Add or update content for a version dynamically.
     * Useful for A/B testing or server-side updates.
     */
    fun addUpdateContent(content: UpdateContent) {
        updateContents[content.versionCode] = content
    }
}