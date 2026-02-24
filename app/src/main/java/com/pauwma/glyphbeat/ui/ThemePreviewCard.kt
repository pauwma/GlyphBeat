package com.pauwma.glyphbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.theme.NothingAndroidSDKDemoTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider

private val NothingTypeFont = FontFamily(
    Font(R.font.ntype82regular, FontWeight.Normal)
)

/**
 * Theme preview card component with Nothing brand styling.
 * Shows glyph matrix preview, theme name, description, and settings access.
 */
@Composable
fun ThemePreviewCard(
    theme: AnimationTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localeContext = remember(configuration) { context }
    val themeRepository = remember(configuration) { ThemeRepository.refreshForLocaleChange(localeContext) }

    // Check if theme supports settings and has custom settings
    var hasCustomSettings by remember(theme, configuration) { mutableStateOf(false) }
    var currentSettings by remember(theme, configuration) { mutableStateOf<com.pauwma.glyphbeat.ui.settings.ThemeSettings?>(null) }
    val supportsSettings = theme is ThemeSettingsProvider

    // Initial settings load - fully on IO thread to prevent ANR
    LaunchedEffect(theme) {
        if (supportsSettings) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val themeSettings = themeRepository.getThemeSettings((theme as ThemeSettingsProvider).getSettingsId())
                    val hasSettings = themeSettings?.userValues?.isNotEmpty() == true

                    // Apply settings if available (safe to do on IO thread)
                    if (themeSettings != null) {
                        (theme as ThemeSettingsProvider).applySettings(themeSettings)
                    }

                    // Only update UI state on Main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        hasCustomSettings = hasSettings
                        currentSettings = themeSettings
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        hasCustomSettings = false
                        currentSettings = null
                    }
                }
            }
        } else {
            hasCustomSettings = false
            currentSettings = null
        }
    }

    // Monitor settings changes for real-time updates
    LaunchedEffect(theme, supportsSettings) {
        if (supportsSettings) {
            themeRepository.settingsChangedFlow.collect { (themeId, settings) ->
                // Check if this settings change is for our theme
                if (themeId == (theme as ThemeSettingsProvider).getSettingsId()) {
                    // Update the settings state which will trigger preview recomposition
                    currentSettings = settings
                    hasCustomSettings = settings.userValues.isNotEmpty()
                    // Apply settings to the theme instance
                    theme.applySettings(settings)
                }
            }
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(4.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glyph Matrix Preview with settings
                GlyphMatrixPreview(
                    theme = theme,
                    isSelected = isSelected,
                    previewSize = 120,
                    settings = currentSettings
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Name - use key() to force recomposition on locale changes
                key(configuration) {
                    Text(
                        text = theme.getThemeName(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = NothingTypeFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    )

                    // Theme Description
                    com.pauwma.glyphbeat.ui.components.AutoScalingText(
                        text = theme.getDescription(),
                        maxFontSize = 12.sp,
                        minFontSize = 10.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        maxLines = 2
                    )
                }
            }

            // Settings button and custom indicator - only show for selected themes that support settings
            if (supportsSettings && isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    onOpenSettings?.let { openSettings ->
                        IconButton(
                            onClick = openSettings,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Theme Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Custom settings indicator dot
                    if (hasCustomSettings) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(20.dp.roundToPx(), 4.dp.roundToPx()) }
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact version of theme preview card for smaller spaces
 */
@Composable
fun CompactThemePreviewCard(
    theme: AnimationTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onSelect() }
            .padding(2.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Smaller Glyph Matrix Preview
            GlyphMatrixPreview(
                theme = theme,
                isSelected = isSelected,
                previewSize = 80
            )

            // Theme Name Only
            Text(
                text = theme.getThemeName(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                fontFamily = NothingTypeFont,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Previews ---

private class PreviewTheme(
    private val name: String = "Vinyl Record",
    private val description: String = "Spinning vinyl animation"
) : AnimationTheme() {
    override fun getFrameCount(): Int = 8
    override fun generateFrame(frameIndex: Int): IntArray = IntArray(625) { if (it % 3 == frameIndex % 3) 200 else 0 }
    override fun getThemeName(): String = name
    override fun getDescription(): String = description
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Selected")
@Composable
private fun ThemePreviewCardSelectedPreview() {
    NothingAndroidSDKDemoTheme {
        ThemePreviewCard(
            theme = PreviewTheme(),
            isSelected = true,
            onSelect = {},
            onOpenSettings = {},
            modifier = Modifier.width(180.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Unselected")
@Composable
private fun ThemePreviewCardUnselectedPreview() {
    NothingAndroidSDKDemoTheme {
        ThemePreviewCard(
            theme = PreviewTheme("Dancing Duck", "Fun duck animation"),
            isSelected = false,
            onSelect = {},
            modifier = Modifier.width(180.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Compact - Selected")
@Composable
private fun CompactThemePreviewCardSelectedPreview() {
    NothingAndroidSDKDemoTheme {
        CompactThemePreviewCard(
            theme = PreviewTheme(),
            isSelected = true,
            onSelect = {},
            modifier = Modifier.width(140.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Compact - Unselected")
@Composable
private fun CompactThemePreviewCardUnselectedPreview() {
    NothingAndroidSDKDemoTheme {
        CompactThemePreviewCard(
            theme = PreviewTheme("Waveform", "Audio waveform display"),
            isSelected = false,
            onSelect = {},
            modifier = Modifier.width(140.dp)
        )
    }
}
