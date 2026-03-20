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
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val themeRepository = remember { ThemeRepository.getInstance(context) }
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
                items(
                    allThemes.filter { it !is CustomTheme },
                    key = { it.getThemeName() }
                ) { theme ->
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
                // Section header with flanking dividers
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                            thickness = 1.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Text(
                                text = localeContext.getString(R.string.imported_themes_header).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = customFont,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                            thickness = 1.dp
                        )
                    }
                }

                if (importedThemes.isNotEmpty()) {
                    items(
                        importedThemes,
                        key = { (it as CustomTheme).postId }
                    ) { theme ->
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
                            },
                            onDelete = {
                                themeToDelete = theme
                            }
                        )
                    }
                } else {
                    // Empty state — promote Glyph Museum
                    item(span = { GridItemSpan(2) }) {
                        val cardContext = LocalContext.current
                        val isMuseumInstalled = remember {
                            try {
                                cardContext.packageManager.getPackageInfo("com.pauwma.glyphmuseum", 0)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Museum icon — subtle, smaller
                                    Image(
                                        painter = painterResource(R.drawable.ic_glyph_museum),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        colorFilter = ColorFilter.tint(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = localeContext.getString(R.string.imported_empty_title),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = customFont
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = localeContext.getString(
                                            if (isMuseumInstalled) R.string.imported_empty_description_installed
                                            else R.string.imported_empty_description
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 18.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // CTA button — changes based on whether Glyph Museum is installed
                                    Button(
                                        onClick = {
                                            if (isMuseumInstalled) {
                                                val launchIntent = cardContext.packageManager.getLaunchIntentForPackage("com.pauwma.glyphmuseum")
                                                if (launchIntent != null) {
                                                    cardContext.startActivity(launchIntent)
                                                }
                                            } else {
                                                try {
                                                    cardContext.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.pauwma.glyphmuseum"))
                                                    )
                                                } catch (e: Exception) {
                                                    cardContext.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.pauwma.glyphmuseum"))
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = localeContext.getString(
                                                if (isMuseumInstalled) R.string.imported_empty_cta_installed
                                                else R.string.imported_empty_cta
                                            ),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
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
                },
                onDelete = if (theme is CustomTheme) {
                    {
                        themeToDelete = theme as CustomTheme
                        showSettingsSheet = false
                        selectedThemeForSettings = null
                    }
                } else null
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