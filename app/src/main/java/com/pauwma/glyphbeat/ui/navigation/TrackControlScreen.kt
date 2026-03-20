package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.services.trackcontrol.TrackControlThemeManager
import com.pauwma.glyphbeat.ui.components.GlyphPreview
import com.pauwma.glyphbeat.ui.GlyphMatrixPreview
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsSheet
import com.pauwma.glyphbeat.theme.NothingRed
import kotlinx.coroutines.launch

/**
 * Screen for Track Control toy theme selection.
 * Shows available themes for next/previous track controls with preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackControlScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeManager = remember { TrackControlThemeManager.getInstance(context) }
    val selectedThemeIndex by themeManager.selectedThemeIndexFlow.collectAsState()
    val settingsChanged by themeManager.settingsChangedFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    // Settings sheet state
    var selectedThemeForSettings by remember { mutableStateOf<TrackControlTheme?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Trigger to refresh theme cards when settings change
    var settingsChangeTrigger by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
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
                    text = "Track Control Themes",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        // Theme grid - with bottom padding for the apply button
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
            itemsIndexed(themeManager.availableThemes) { index, theme ->
                TrackControlThemeCard(
                    theme = theme,
                    themeIndex = index,
                    isSelected = index == selectedThemeIndex,
                    settingsVersion = settingsChanged?.hashCode() ?: 0, // Force recomposition on settings change
                    onSelect = {
                        scope.launch {
                            themeManager.selectTheme(index)
                        }
                    },
                    onSettings = {
                        selectedThemeForSettings = theme
                        showSettingsSheet = true
                    }
                )
            }
        }
    }
    
    // Theme Settings Sheet - show as popup when requested
    if (showSettingsSheet) {
        selectedThemeForSettings?.let { theme ->
            if (theme is com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider) {
                ThemeSettingsSheet(
                    theme = theme,
                    isVisible = true,
                    onDismiss = {
                        showSettingsSheet = false
                        selectedThemeForSettings = null
                    },
                    onSettingsChanged = {
                        settingsChangeTrigger++
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackControlThemeCard(
    theme: TrackControlTheme,
    themeIndex: Int,
    isSelected: Boolean,
    settingsVersion: Int = 0, // Force recomposition when settings change
    onSelect: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { TrackControlThemeManager.getInstance(context) }
    
    Card(
        modifier = Modifier
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Combined preview showing both directions
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    // Show NEXT direction preview as primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        // Get fresh preview frame to reflect current settings
                        val pixels = if (isSelected && settingsVersion != 0) {
                            // For selected theme, get fresh frame from current instance
                            themeManager.currentTheme.getPreviewFrame(TrackControlTheme.Direction.NEXT)
                        } else {
                            themeManager.getThemePreview(
                                themeIndex,
                                TrackControlTheme.Direction.NEXT
                            )
                        }
                        val res = com.pauwma.glyphbeat.core.DeviceManager.resolution
                        val gs = res.gridSize
                        val glyphShape = res.shape
                        val containerSize = kotlin.math.min(size.width, size.height)
                        val dotSize = containerSize / gs.toFloat()
                        val actualDotSize = dotSize * 0.8f
                        val startX = (size.width - containerSize) / 2f
                        val startY = (size.height - containerSize) / 2f

                        for (row in 0 until gs) {
                            val pixelsInRow = glyphShape[row]
                            val startColForRow = (gs - pixelsInRow) / 2

                            for (colInRow in 0 until pixelsInRow) {
                                val col = startColForRow + colInRow
                                val index = row * gs + col
                                val value = if (index < pixels.size) pixels[index].coerceIn(0, 255) else 0
                                val brightness = value / 255f

                                drawCircle(
                                    color = Color(brightness, brightness, brightness, 1f),
                                    radius = actualDotSize / 2f,
                                    center = androidx.compose.ui.geometry.Offset(
                                        x = startX + col * dotSize + dotSize / 2f,
                                        y = startY + row * dotSize + dotSize / 2f
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Theme Name
                Text(
                    text = theme.getThemeName(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    fontFamily = FontFamily(Font(R.font.ntype82regular, FontWeight.Normal)),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Theme Description
                Text(
                    text = theme.getDescription(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    maxLines = 3
                )
            }
            
            // Settings button - only show for selected themes that support settings
            if (theme is com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider && isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = onSettings,
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
            }
        }
    }
}