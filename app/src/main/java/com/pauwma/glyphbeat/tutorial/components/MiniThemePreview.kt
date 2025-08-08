package com.pauwma.glyphbeat.tutorial.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Mini preview component for showing animated theme in tutorial.
 */
@Composable
fun MiniThemePreview(
    theme: AnimationTheme?,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    var currentFrame by remember { mutableStateOf(0) }
    val frameCount = remember(theme) { 
        try {
            theme?.getFrameCount() ?: 1
        } catch (e: Exception) {
            1
        }
    }
    
    // Animation loop
    LaunchedEffect(theme, isAnimating) {
        if (isAnimating && theme != null) {
            while (isActive) {
                try {
                    delay(theme.getAnimationSpeed())
                    currentFrame = (currentFrame + 1) % frameCount
                } catch (e: Exception) {
                    delay(100) // Default delay on error
                    currentFrame = 0
                }
            }
        } else {
            currentFrame = 0
        }
    }
    
    // Generate frame
    val pixels = remember(theme, currentFrame) {
        try {
            theme?.generateFrame(currentFrame) ?: IntArray(625) { 0 }
        } catch (e: Exception) {
            IntArray(625) { 0 }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        drawMiniGlyphMatrix(pixels)
    }
}

/**
 * Draw a miniature version of the Glyph Matrix.
 */
private fun DrawScope.drawMiniGlyphMatrix(pixels: IntArray) {
    if (pixels.size != 625) return
    
    val matrixSize = 25
    val cellSize = minOf(size.width, size.height) / matrixSize
    val offsetX = (size.width - cellSize * matrixSize) / 2
    val offsetY = (size.height - cellSize * matrixSize) / 2
    
    // Draw simplified version - just show active pixels
    val shapedGrid = GlyphMatrixRenderer.flatArrayToShapedGrid(pixels)
    
    for (row in 0 until matrixSize) {
        val pixelsInRow = GlyphMatrixRenderer.getPixelsInRow(row)
        val startCol = GlyphMatrixRenderer.getStartColumnForRow(row)
        
        for (col in 0 until pixelsInRow) {
            val brightness = shapedGrid[row][col]
            if (brightness > 0) {
                val alpha = brightness / 255f
                val x = offsetX + (startCol + col) * cellSize + cellSize / 2
                val y = offsetY + row * cellSize + cellSize / 2
                
                drawCircle(
                    color = Color.Red.copy(alpha = alpha),
                    radius = cellSize * 0.3f,
                    center = Offset(x, y)
                )
            }
        }
    }
}