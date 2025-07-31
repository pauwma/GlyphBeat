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
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.animation.styles.CoverArtTheme
import com.pauwma.glyphbeat.animation.styles.FrameTransitionSequence
import com.pauwma.glyphbeat.animation.styles.ThemeTemplate
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
 */
@Composable
fun GlyphMatrixPreview(
    theme: AnimationTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    previewSize: Int = 120 // Size in dp for the preview
) {
    val context = LocalContext.current
    var currentFrame by remember { mutableIntStateOf(0) }
    var transitionSequence by remember { mutableStateOf<FrameTransitionSequence?>(null) }
    var mediaTrigger by remember { mutableIntStateOf(0) }
    
    // Initialize transition sequence if theme uses frame transitions
    LaunchedEffect(theme) {
        transitionSequence = if (theme is ThemeTemplate && theme.hasFrameTransitions()) {
            theme.createTransitionSequence()
        } else {
            null
        }
    }
    
    // Monitor media changes for CoverArtTheme - FIXED: Move to background thread and add proper cancellation
    LaunchedEffect(theme, isSelected) {
        if (theme is CoverArtTheme && isSelected) { // Only monitor when selected to reduce load
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val mediaHelper = MediaControlHelper(context)
                var lastTrackTitle: String? = null
                
                while (kotlinx.coroutines.currentCoroutineContext().isActive) { // Proper cancellation check
                    try {
                        val trackInfo = mediaHelper.getTrackInfo()
                        val currentTrackTitle = trackInfo?.title
                        
                        if (currentTrackTitle != lastTrackTitle) {
                            // Media changed, clear cache and trigger recomposition
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                theme.clearCache()
                                mediaTrigger++
                            }
                            lastTrackTitle = currentTrackTitle
                        }
                        
                        delay(5000) // Reduced frequency: Check every 5 seconds
                    } catch (e: Exception) {
                        // Break the loop on any error to prevent infinite loops
                        break
                    }
                }
            }
        }
    }
    
    // Animation logic - OPTIMIZED to prevent ANR
    LaunchedEffect(isSelected, theme, transitionSequence) {
        if (isSelected) {
            // Only animate selected theme to reduce resource usage
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    val frameCount = theme.getFrameCount()
                    if (frameCount <= 1) break // No animation needed
                    
                    // Calculate frame timing based on theme
                    val transitions = transitionSequence
                    val frameDuration = when {
                        transitions != null -> transitions.getCurrentDuration()
                        else -> theme.getAnimationSpeed().coerceAtLeast(50L) // Minimum 50ms to match VinylTheme's minimum
                    }
                    
                    delay(frameDuration)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (transitions != null) {
                            transitions.advance()
                            currentFrame = transitions.getCurrentFrameIndex()
                        } else {
                            currentFrame = (currentFrame + 1) % frameCount
                        }
                    }
                }
            }
        } else {
            // Unselected themes show static first frame
            currentFrame = 0
        }
    }
    
    val frameData = remember(theme, currentFrame, mediaTrigger) {
        try {
            // For VinylTheme, ensure consistent circular shape regardless of size setting
            if (theme is com.pauwma.glyphbeat.animation.styles.VinylTheme) {
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
                    val brightness = if (pixelIndex < frameData.size) frameData[pixelIndex] else 0
                    
                    // Convert brightness to color - use exact matrix-like mapping for CoverArtTheme
                    val color = if (theme is CoverArtTheme) {
                        // Direct linear brightness mapping to match matrix exactly
                        if (brightness == 0) {
                            Color.Transparent
                        } else {
                            // Linear mapping: brightness 0-255 -> alpha 0.0-1.0
                            val alpha = (brightness / 255f).coerceIn(0f, 1f)
                            Color.White.copy(alpha = alpha)
                        }
                    } else {
                        // Original mapping for other themes
                        when {
                            brightness == 0 -> Color.Transparent
                            brightness < 100 -> Color.Gray.copy(alpha = 0.3f)
                            brightness < 200 -> Color.Gray.copy(alpha = 0.7f)
                            else -> Color.White
                        }
                    }
                    
                    // Always draw the dot to show the matrix shape, but use different opacity for inactive pixels
                    val finalColor = if (brightness > 0) {
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