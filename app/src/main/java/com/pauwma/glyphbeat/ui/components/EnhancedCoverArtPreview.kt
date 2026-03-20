package com.pauwma.glyphbeat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.themes.animation.CoverArtTheme
import com.pauwma.glyphbeat.themes.animation.preview.CoverArtPreviewManager
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueFloat
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueLong
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getToggleValue
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
/**
 * EnhancedCoverArtPreview - Specialized preview component for CoverArtTheme.
 * 
 * This composable provides an enhanced preview specifically designed for the
 * CoverArtTheme, showing actual album art with all theme settings applied in
 * real-time. It offers a more accurate representation than the generic preview.
 * 
 * Features:
 * - Real album art display (when available)
 * - Live theme settings application
 * - Smooth rotation animation
 * - Responsive to media changes
 * - Optimized for performance
 * 
 * The preview automatically adapts to:
 * - Brightness settings
 * - Contrast enhancement
 * - Rotation animation
 * - Paused opacity
 * - Media state changes
 * 
 * @param theme The CoverArtTheme instance to preview
 * @param isSelected Whether this theme is currently selected
 * @param settings Optional theme settings to apply
 * @param modifier Modifier for the preview container
 * @param previewSize Size of the preview in dp
 */
@Composable
fun EnhancedCoverArtPreview(
    theme: CoverArtTheme,
    isSelected: Boolean,
    settings: ThemeSettings? = null,
    modifier: Modifier = Modifier,
    previewSize: Int = 120
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Create and remember the preview manager
    val previewManager = remember(theme) {
        CoverArtPreviewManager(context)
    }
    
    // Collect preview state
    val previewState by previewManager.previewState.collectAsState()
    
    // Extract settings values that affect preview (for triggering updates)
    val rotationEnabled = settings?.getToggleValue("enable_rotation", false) ?: false
    val rotationSpeed = settings?.getSliderValueLong("rotation_speed", 150L) ?: 150L
    val brightness = settings?.getSliderValueFloat("cover_brightness", 1.0f) ?: 1.0f
    val contrast = settings?.getToggleValue("enhance_contrast", true) ?: true
    val pausedOpacity = settings?.getSliderValueFloat("paused_opacity", 0.4f) ?: 0.4f
    
    // Start/stop monitoring based on selection
    DisposableEffect(isSelected) {
        if (isSelected) {
            previewManager.startMonitoring()
        }
        
        onDispose {
            if (isSelected) {
                previewManager.stopMonitoring()
            }
        }
    }
    
    // Track previous settings to avoid unnecessary updates
    var previousSettings by remember { mutableStateOf(settings) }
    var previousIsSelected by remember { mutableStateOf(isSelected) }
    
    // Apply settings when they change - only when actually different (on IO thread)
    LaunchedEffect(settings, isSelected) {
        // Only apply if settings actually changed or selection state changed
        if (settings != previousSettings || isSelected != previousIsSelected) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                settings?.let { 
                    previewManager.applySettings(it, isSelected)
                }
            }
            previousSettings = settings
            previousIsSelected = isSelected
        }
    }
    
    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            previewManager.cleanup()
        }
    }
    
    // Main preview UI
    Box(
        modifier = modifier
            .size(previewSize.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Direct canvas without rotation (rotation is already in the pixel data)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            drawCoverArtPreview(
                frameData = previewState.frameData,
                isPlaying = previewState.isPlaying,
                hasMedia = previewState.hasMedia,
                isSelected = isSelected
            )
        }
        
        // Overlay indicators
        if (!previewState.hasMedia) {
            // No media indicator
            Canvas(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            ) {
                drawCircle(
                    color = Color.Gray,
                    radius = size.minDimension / 2
                )
            }
        } else if (!previewState.isPlaying && isSelected) {
            // Paused indicator
            Canvas(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            ) {
                drawCircle(
                    color = Color.Yellow,
                    radius = size.minDimension / 2
                )
            }
        }
    }
}

/**
 * Draw the cover art preview on the canvas.
 */
private fun DrawScope.drawCoverArtPreview(
    frameData: IntArray,
    isPlaying: Boolean,
    hasMedia: Boolean,
    isSelected: Boolean
) {
    val inner = size.minDimension
    val gapRatio = 0.18f
    val cornerRatio = 0.18f

    val startX = (size.width - inner) / 2f
    val startY = (size.height - inner) / 2f

    val res = com.pauwma.glyphbeat.core.DeviceManager.resolution
    val gs = res.gridSize
    val glyphShape = res.shape

    val cell = inner / gs.toFloat()
    val gap = cell * gapRatio
    val side = cell - gap
    val cr = side * cornerRatio

    // Draw each pixel
    for (row in 0 until gs) {
        val pixelsInRow = glyphShape[row]
        val startCol = (gs - pixelsInRow) / 2

        for (colInRow in 0 until pixelsInRow) {
            val col = startCol + colInRow
            val index = row * gs + col

            if (index < frameData.size) {
                val value = frameData[index]

                // Apply state-based dimming then map to grayscale like GlyphMuseum
                val adjustedBrightness = when {
                    value == 0 -> 0f
                    !isSelected -> value / 255f * 0.7f // Dimmer when not selected
                    !isPlaying && hasMedia -> value / 255f * 0.5f // Paused state
                    else -> value / 255f // Normal state
                }

                val color = Color(adjustedBrightness, adjustedBrightness, adjustedBrightness, 1f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        x = startX + col * cell + gap / 2f,
                        y = startY + row * cell + gap / 2f
                    ),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(cr, cr)
                )
            }
        }
    }
}

/**
 * Simplified version for static preview.
 */
@Composable
fun StaticCoverArtPreview(
    theme: CoverArtTheme,
    modifier: Modifier = Modifier,
    previewSize: Int = 120
) {
    EnhancedCoverArtPreview(
        theme = theme,
        isSelected = false,
        settings = null,
        modifier = modifier,
        previewSize = previewSize
    )
}