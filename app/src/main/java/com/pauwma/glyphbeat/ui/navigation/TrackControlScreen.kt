package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val scope = rememberCoroutineScope()
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    // Settings sheet state
    var selectedThemeForSettings by remember { mutableStateOf<TrackControlTheme?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Trigger to refresh theme cards when settings change
    var settingsChangeTrigger by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = modifier
            .fillMaxSize(),
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
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(themeManager.availableThemes) { index, theme ->
                TrackControlThemeCard(
                    theme = theme,
                    themeIndex = index,
                    isSelected = index == selectedThemeIndex,
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
    onSelect: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { TrackControlThemeManager.getInstance(context) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.surfaceVariant 
                    else 
                        MaterialTheme.colorScheme.surface
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) 
                        NothingRed 
                    else 
                        MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
                            color = if (isSelected) NothingRed else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                ) {
                    // Show NEXT direction preview as primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        val pixels = themeManager.getThemePreview(
                            themeIndex,
                            TrackControlTheme.Direction.NEXT
                        )
                        // Simple preview rendering
                        val cellSize = size.width / 25f
                        for (row in 0 until 25) {
                            for (col in 0 until 25) {
                                val index = row * 25 + col
                                if (index < pixels.size && pixels[index] > 0) {
                                    val alpha = pixels[index] / 255f
                                    drawCircle(
                                        color = Color.White.copy(alpha = alpha),
                                        radius = cellSize * 0.3f,
                                        center = androidx.compose.ui.geometry.Offset(
                                            x = col * cellSize + cellSize / 2,
                                            y = row * cellSize + cellSize / 2
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Theme Name
                Text(
                    text = theme.getThemeName(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = if (isSelected) 
                        NothingRed 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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