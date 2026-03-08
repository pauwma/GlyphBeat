package com.pauwma.glyphbeat.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.animation.CustomTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsSheet
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.ui.ThemePreviewCard
import com.pauwma.glyphbeat.ui.CompactThemePreviewCard

/**
 * Theme selection screen with Nothing brand styling.
 * Features a grid of animated previews with a fixed apply button at the bottom.
 */
@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localeContext = remember(configuration) { context }
    val themeRepository = remember(configuration) { ThemeRepository.refreshForLocaleChange(localeContext) }
    val selectedThemeIndex by themeRepository.selectedThemeIndex
    val customFont = FontFamily(Font(R.font.ntype82regular))

    // Settings sheet state
    var selectedThemeForSettings by remember { mutableStateOf<AnimationTheme?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Custom theme delete confirmation
    var themeToDelete by remember { mutableStateOf<CustomTheme?>(null) }

    // Read custom themes at composable level so state changes trigger recomposition
    val importedThemes = themeRepository.customThemes
    val allThemes = themeRepository.availableThemes
    
    // Apply settings to newly selected theme - fully on IO thread
    LaunchedEffect(selectedThemeIndex) {
        if (selectedThemeIndex in themeRepository.availableThemes.indices) {
            val selectedTheme = themeRepository.availableThemes[selectedThemeIndex]
            if (selectedTheme is ThemeSettingsProvider) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val existingSettings = themeRepository.getThemeSettings(selectedTheme.getSettingsId())
                        if (existingSettings != null) {
                            // Apply settings on IO thread (safe for theme objects)
                            selectedTheme.applySettings(existingSettings)
                        }
                    } catch (e: Exception) {
                        // Ignore errors
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = localeContext.getString(R.string.screen_animation_themes),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = customFont
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Theme Grid - with bottom padding for the apply button
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Built-in themes
                items(allThemes.filter { it !is CustomTheme }) { theme ->
                    val themeIndex = allThemes.indexOf(theme)
                    val isSelected = themeRepository.isThemeSelected(themeIndex)

                    ThemePreviewCard(
                        theme = theme,
                        isSelected = isSelected,
                        onSelect = {
                            themeRepository.selectTheme(themeIndex)
                        },
                        onOpenSettings = {
                            selectedThemeForSettings = theme
                            showSettingsSheet = true
                        }
                    )
                }

                // Custom themes section — always visible
                // Section header
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = localeContext.getString(R.string.imported_themes_header),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = customFont
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                if (importedThemes.isNotEmpty()) {
                    items(importedThemes) { theme ->
                        val themeIndex = allThemes.indexOf(theme)
                        val isSelected = themeRepository.isThemeSelected(themeIndex)

                        ThemePreviewCard(
                            theme = theme,
                            isSelected = isSelected,
                            onSelect = {
                                themeRepository.selectTheme(themeIndex)
                            },
                            onOpenSettings = null,
                            onDelete = {
                                themeToDelete = theme
                            }
                        )
                    }
                } else {
                    // Empty state — promote Glyph Museum
                    item(span = { GridItemSpan(2) }) {
                        val cardContext = LocalContext.current
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Museum icon
                                Image(
                                    painter = painterResource(R.drawable.ic_glyph_museum),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = localeContext.getString(R.string.imported_empty_title),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = customFont
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = localeContext.getString(R.string.imported_empty_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                // CTA button
                                Button(
                                    onClick = {
                                        try {
                                            cardContext.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.pauwma.glyphmuseum"))
                                            )
                                        } catch (e: Exception) {
                                            cardContext.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.pauwma.glyphmuseum"))
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = localeContext.getString(R.string.imported_empty_cta),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = customFont
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Delete confirmation dialog
            themeToDelete?.let { theme ->
                com.pauwma.glyphbeat.ui.dialogs.CustomConfirmationDialog(
                    isVisible = true,
                    onDismiss = { themeToDelete = null },
                    onConfirm = {
                        themeRepository.deleteCustomTheme(theme.postId)
                    },
                    title = localeContext.getString(R.string.delete_theme_title),
                    description = localeContext.getString(R.string.delete_theme_message, theme.getThemeName()),
                    icon = Icons.Outlined.DeleteOutline,
                    confirmButtonText = localeContext.getString(R.string.delete),
                    dismissButtonText = localeContext.getString(R.string.cancel)
                )
            }
        }

        // Theme Settings Sheet
        selectedThemeForSettings?.let { theme ->
            ThemeSettingsSheet(
                theme = theme,
                isVisible = showSettingsSheet,
                onDismiss = {
                    showSettingsSheet = false
                    selectedThemeForSettings = null
                },
                onSettingsChanged = {
                    // Settings are applied to themes directly via the flow notifications
                }
            )
        }
    }
}

/**
 * Compact version of the theme selection screen for smaller displays
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactThemeSelectionScreen(
    modifier: Modifier = Modifier,
    onThemeApplied: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeRepository = remember { ThemeRepository.getInstance(context) }
    val selectedThemeIndex by themeRepository.selectedThemeIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Compact Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Compact Theme Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(themeRepository.availableThemes) { theme ->
                    val themeIndex = themeRepository.availableThemes.indexOf(theme)
                    val isSelected = themeRepository.isThemeSelected(themeIndex)

                    CompactThemePreviewCard(
                        theme = theme,
                        isSelected = isSelected,
                        onSelect = {
                            themeRepository.selectTheme(themeIndex)
                        }
                    )
                }
            }

            // Bottom Apply Button
            Button(
                onClick = onThemeApplied,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "APPLY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}