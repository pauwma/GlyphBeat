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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.ui.ThemeRepository
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider

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
    val themeRepository = remember { ThemeRepository.getInstance(context) }
    
    // Check if theme supports settings and has custom settings
    var hasCustomSettings by remember(theme) { mutableStateOf(false) }
    val supportsSettings = theme is ThemeSettingsProvider
    
    LaunchedEffect(theme) {
        if (supportsSettings) {
            try {
                // Move to background thread to prevent ANR
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val themeSettings = themeRepository.getThemeSettings((theme as ThemeSettingsProvider).getSettingsId())
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        hasCustomSettings = themeSettings?.userValues?.isNotEmpty() == true
                    }
                }
            } catch (e: Exception) {
                hasCustomSettings = false
            }
        } else {
            hasCustomSettings = false
        }
    }
    Card(
        modifier = modifier
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
                        MaterialTheme.colorScheme.primary 
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
            // Glyph Matrix Preview
            GlyphMatrixPreview(
                theme = theme,
                isSelected = isSelected,
                previewSize = 120
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Theme Name
            Text(
                text = theme.getThemeName(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
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
            .padding(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Column(
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
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
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
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}