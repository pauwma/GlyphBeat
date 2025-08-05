package com.pauwma.glyphbeat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pauwma.glyphbeat.core.GlyphMatrixRenderer

/**
 * Composable that renders a preview of Glyph Matrix pixels.
 * 
 * @param pixels IntArray of 625 pixels representing the 25x25 matrix
 * @param modifier Modifier for the canvas
 */
@Composable
fun GlyphPreview(
    pixels: IntArray,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawGlyphMatrix(pixels)
    }
}

/**
 * Extension function to draw the Glyph Matrix on a Canvas.
 */
private fun DrawScope.drawGlyphMatrix(pixels: IntArray) {
    if (pixels.size != 625) return
    
    val matrixSize = 25
    val cellWidth = size.width / matrixSize
    val cellHeight = size.height / matrixSize
    
    // Convert flat array to shaped grid for accurate rendering
    val shapedGrid = GlyphMatrixRenderer.flatArrayToShapedGrid(pixels)
    
    for (row in 0 until matrixSize) {
        val pixelsInRow = GlyphMatrixRenderer.getPixelsInRow(row)
        val startCol = GlyphMatrixRenderer.getStartColumnForRow(row)
        
        for (col in 0 until pixelsInRow) {
            val brightness = shapedGrid[row][col]
            if (brightness > 0) {
                val alpha = brightness / 255f
                val actualCol = startCol + col
                
                drawRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(
                        x = actualCol * cellWidth,
                        y = row * cellHeight
                    ),
                    size = Size(cellWidth, cellHeight)
                )
            }
        }
    }
}