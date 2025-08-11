package com.pauwma.glyphbeat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.animation.CoverArtTheme
import com.pauwma.glyphbeat.themes.base.FrameTransitionSequence
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.ui.components.EnhancedCoverArtPreview
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.sound.MediaControlHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Miniature Glyph Matrix preview component that displays a scaled-down version of the 25x25 matrix.
 * Shows static preview for unselected themes and animated preview for the selected theme.
 * Uses enhanced preview for CoverArtTheme to show actual album art.
 */
@Composable
fun GlyphMatrixPreview(
    theme: AnimationTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    previewSize: Int = 120, // Size in dp for the preview
    settings: ThemeSettings? = null // Optional settings for enhanced preview
) {
    // Use enhanced preview for CoverArtTheme
    if (theme is CoverArtTheme && theme.useEnhancedPreview()) {
        EnhancedCoverArtPreview(
            theme = theme,
            isSelected = isSelected,
            settings = settings,
            modifier = modifier,
            previewSize = previewSize
        )
        return
    }
    
    // Regular preview for other themes
    val context = LocalContext.current
    var currentFrame by remember { mutableIntStateOf(0) }
    var transitionSequence by remember { mutableStateOf<FrameTransitionSequence?>(null) }
    var mediaTrigger by remember { mutableIntStateOf(0) }
    
    // Initialize transition sequence if theme uses frame transitions
    // Recreate when theme or settings change to support live updates
    LaunchedEffect(theme, settings) {
        transitionSequence = if (theme is ThemeTemplate && theme.hasFrameTransitions()) {
            // Apply settings first to ensure transitions reflect current configuration
            if (theme is ThemeSettingsProvider && settings != null) {
                theme.applySettings(settings)
            }
            theme.createTransitionSequence()
        } else {
            null
        }
        // Reset frame to beginning when transitions change
        currentFrame = 0
    }
    
    // Animation logic - OPTIMIZED to prevent ANR
    LaunchedEffect(isSelected, theme, transitionSequence) {
        if (isSelected) {
            // Only animate selected theme to reduce resource usage
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    val frameCount = try {
                        theme.getFrameCount()
                    } catch (e: Exception) {
                        1 // Default to 1 frame if error
                    }
                    
                    if (frameCount <= 1) break // No animation needed
                    
                    // Calculate frame timing based on theme (all on Default dispatcher)
                    val frameDuration = try {
                        val transitions = transitionSequence
                        when {
                            transitions != null -> transitions.getCurrentDuration()
                            else -> theme.getAnimationSpeed().coerceAtLeast(50L)
                        }
                    } catch (e: Exception) {
                        100L // Default delay if error
                    }
                    
                    delay(frameDuration)
                    
                    // Calculate next frame on Default dispatcher
                    val nextFrame = try {
                        val transitions = transitionSequence
                        if (transitions != null) {
                            transitions.advance()
                            transitions.getCurrentFrameIndex()
                        } else {
                            (currentFrame + 1) % frameCount
                        }
                    } catch (e: Exception) {
                        0 // Reset to first frame on error
                    }
                    
                    // Only update UI state on Main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        currentFrame = nextFrame
                    }
                }
            }
        } else {
            // Unselected themes show static preview frame
            // Check if theme has a custom preview frame index
            currentFrame = try {
                // Try to get previewFrameIndex property if it exists
                val previewIndexField = theme.javaClass.getDeclaredField("previewFrameIndex")
                previewIndexField.isAccessible = true
                val index = previewIndexField.get(theme) as? Int
                index?.coerceIn(0, theme.getFrameCount() - 1) ?: 0
            } catch (e: Exception) {
                // Default to first frame if property doesn't exist
                0
            }
        }
    }
    
    // Frame generation state - computed asynchronously to prevent blocking
    var frameData by remember { mutableStateOf(IntArray(625) { 0 }) }
    
    // Generate frame data on background thread
    LaunchedEffect(theme, currentFrame, mediaTrigger) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val generatedFrame = try {
                // For VinylTheme, ensure consistent circular shape regardless of size setting
                if (theme is com.pauwma.glyphbeat.themes.animation.VinylTheme) {
                    val currentVinylSizeField = theme.javaClass.getDeclaredField("currentVinylSize").apply { isAccessible = true }
                    val currentVinylSize = currentVinylSizeField.get(theme) as String
                    
                    if (currentVinylSize == "small") {
                        // Get the small frame data but apply the same circular mapping as large
                        val smallFramesField = theme.javaClass.getDeclaredField("smallFrames").apply { isAccessible = true }
                        val smallFrames = smallFramesField.get(theme) as Array<IntArray>
                        val shapedData = smallFrames[currentFrame]
                        
                        // Apply the same circular mapping logic as used for large size
                        val flatArray = IntArray(625) { 0 }
                        var shapedIndex = 0
                        
                        for (row in 0 until 25) {
                            for (col in 0 until 25) {
                                val flatIndex = row * 25 + col
                                
                                // Check if this pixel is within the circular matrix shape
                                val centerX = 12.0
                                val centerY = 12.0
                                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))
                                
                                if (distance <= 12.5 && shapedIndex < shapedData.size) {
                                    flatArray[flatIndex] = shapedData[shapedIndex]
                                    shapedIndex++
                                }
                            }
                        }
                        flatArray
                    } else {
                        // Use normal generation for large size
                        theme.generateFrame(currentFrame)
                    }
                } else {
                    theme.generateFrame(currentFrame)
                }
            } catch (e: Exception) {
                // Fallback to empty frame if generation fails
                IntArray(625) { 0 }
            }
            
            // Update frame data on main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                frameData = generatedFrame
            }
        }
    }
    
    // Get the theme's current brightness setting as state for smooth updates
    var themeBrightness by remember { mutableIntStateOf(255) }
    
    // Update brightness whenever theme changes or on regular intervals for smooth updates
    LaunchedEffect(theme, isSelected) {
        while (currentCoroutineContext().isActive) {
            try {
                val newBrightness = theme.getBrightness()
                if (themeBrightness != newBrightness) {
                    themeBrightness = newBrightness
                }
            } catch (e: Exception) {
                // Keep current brightness if unable to get
            }
            // Check for brightness changes every 100ms for smooth updates
            delay(100)
        }
    }
    
    Box(
        modifier = modifier
            .size(previewSize.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val containerSize = min(size.width, size.height)
            
            // Consistent grid sizing for all themes - always use the full container
            val gridSize = containerSize
            
            // Simple consistent sizing for all preview sizes
            val dotSize = gridSize / 25f // 25x25 grid
            val actualDotSize = dotSize * 0.8f // 80% of grid cell for dot, 20% for spacing
            
            // Calculate starting position to center the grid
            val startX = (size.width - gridSize) / 2f
            val startY = (size.height - gridSize) / 2f
            
            // Glyph Matrix shape definition - number of active pixels per row
            val glyphShape = intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
                25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
                19, 17, 15, 11, 7
            )
            
            // Draw each pixel as a small circle/dot, but only for active matrix positions
            for (row in 0 until 25) {
                val pixelsInRow = glyphShape[row]
                val startColForRow = (25 - pixelsInRow) / 2
                
                for (colInRow in 0 until pixelsInRow) {
                    val col = startColForRow + colInRow
                    val pixelIndex = row * 25 + col
                    val pixelValue = if (pixelIndex < frameData.size) frameData[pixelIndex] else 0
                    
                    // Use the unified brightness model for pixel calculations
                    val finalBrightness = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        pixelValue,
                        themeBrightness
                    )
                    
                    // Convert brightness to color using the unified model's preview alpha calculation
                    val color = if (finalBrightness == 0) {
                        Color.Transparent
                    } else {
                        // Use the unified model's preview alpha calculation for consistent appearance
                        val alpha = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculatePreviewAlpha(
                            pixelValue,
                            themeBrightness
                        )
                        Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
                    }
                    
                    // Always draw the dot to show the matrix shape, but use different opacity for inactive pixels
                    val finalColor = if (pixelValue > 0) {
                        color
                    } else {
                        // Show inactive matrix positions with very low opacity
                        Color.Gray.copy(alpha = 0.05f)
                    }
                    
                    // Simple consistent positioning
                    val x = startX + col * dotSize + dotSize / 2f
                    val y = startY + row * dotSize + dotSize / 2f
                    
                    drawCircle(
                        color = finalColor,
                        radius = actualDotSize / 2f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * Static preview version that shows only the first frame
 */
@Composable
fun GlyphMatrixStaticPreview(
    theme: AnimationTheme,
    modifier: Modifier = Modifier,
    previewSize: Int = 120
) {
    GlyphMatrixPreview(
        theme = theme,
        isSelected = false,
        modifier = modifier,
        previewSize = previewSize
    )
}