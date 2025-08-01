package com.pauwma.glyphbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsSheet
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider

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
    val themeRepository = remember { ThemeRepository.getInstance(context) }
    val selectedThemeIndex by themeRepository.selectedThemeIndex
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    // Settings sheet state
    var selectedThemeForSettings by remember { mutableStateOf<AnimationTheme?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Trigger to refresh theme cards when settings change
    var settingsChangeTrigger by remember { mutableIntStateOf(0) }
    
    // Load and apply existing settings to all themes when screen opens
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            themeRepository.availableThemes.forEach { theme ->
                if (theme is ThemeSettingsProvider) {
                    try {
                        val existingSettings = themeRepository.getThemeSettings(theme.getSettingsId())
                        if (existingSettings != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                theme.applySettings(existingSettings)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore errors for individual themes
                    }
                }
            }
        }
    }
    
    // Apply settings to newly selected theme
    LaunchedEffect(selectedThemeIndex) {
        if (selectedThemeIndex in themeRepository.availableThemes.indices) {
            val selectedTheme = themeRepository.availableThemes[selectedThemeIndex]
            if (selectedTheme is ThemeSettingsProvider) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val existingSettings = themeRepository.getThemeSettings(selectedTheme.getSettingsId())
                        if (existingSettings != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                selectedTheme.applySettings(existingSettings)
                            }
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Animation Themes",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = customFont
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(themeRepository.availableThemes) { theme ->
                    val themeIndex = themeRepository.availableThemes.indexOf(theme)
                    val isSelected = themeRepository.isThemeSelected(themeIndex)
                    
                    key("${theme.getThemeName()}_$settingsChangeTrigger") {
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
                }
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
                    // Trigger refresh of theme cards to update custom settings indicators
                    settingsChangeTrigger++
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