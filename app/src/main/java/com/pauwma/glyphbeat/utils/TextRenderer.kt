package com.pauwma.glyphbeat.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

/**
 * Text rendering utilities for converting strings to pixel arrays for the Glyph Matrix.
 * Handles text-to-bitmap conversion, scrolling buffers, and brightness adjustments.
 * Respects the circular shape of the Glyph Matrix for proper rendering.
 */
class TextRenderer {
    
    companion object {
        private const val LOG_TAG = "TextRenderer"
        private const val MATRIX_WIDTH = 25
        private const val MATRIX_HEIGHT = 25
        
        // Glyph Matrix shape definition - number of pixels per row
        private val GLYPH_SHAPE = intArrayOf(
            7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
            25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
            19, 17, 15, 11, 7
        )
        
        /**
         * Render text to a bitmap buffer for scrolling.
         * @param text The text to render
         * @param spacing Character spacing in pixels
         * @param brightness Overall brightness (0.0-1.0)
         * @return Bitmap containing the rendered text
         */
        fun renderTextToBitmap(
            text: String, 
            spacing: Int = 1,
            brightness: Float = 1.0f
        ): Bitmap? {
            if (text.isEmpty()) return null
            
            // Calculate total width needed
            val textWidth = PixelFont.calculateTextWidth(text, spacing)
            val textHeight = PixelFont.CHAR_HEIGHT
            
            // Create bitmap with some padding
            val bitmap = Bitmap.createBitmap(
                textWidth,
                textHeight,
                Bitmap.Config.ARGB_8888
            )
            
            // Clear bitmap to transparent
            bitmap.eraseColor(Color.TRANSPARENT)
            
            // Draw each character
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            
            var xOffset = 0
            for (char in text) {
                drawCharacter(canvas, paint, char, xOffset, 0, brightness)
                xOffset += PixelFont.CHAR_WIDTH + spacing
            }
            
            return bitmap
        }
        
        /**
         * Render text directly to a pixel array that fits the matrix.
         * @param text The text to render
         * @param scrollOffset Horizontal scroll offset
         * @param spacing Character spacing in pixels
         * @param brightness Overall brightness (0.0-1.0)
         * @param verticalOffset Vertical position offset (for centering)
         * @return IntArray of 625 pixels for the Glyph Matrix
         */
        fun renderTextToMatrix(
            text: String,
            scrollOffset: Int = 0,
            spacing: Int = 1,
            brightness: Float = 1.0f,
            verticalOffset: Int = 9
        ): IntArray {
            val pixelArray = IntArray(625) { 0 }
            
            if (text.isEmpty()) return pixelArray
            
            // Calculate text dimensions
            val textWidth = PixelFont.calculateTextWidth(text, spacing)
            
            // For seamless scrolling, we need to handle wraparound
            val effectiveOffset = if (textWidth > 0) {
                scrollOffset % (textWidth + MATRIX_WIDTH)
            } else {
                0
            }
            
            // Draw each character
            var xPos = -effectiveOffset
            
            // Draw the text twice for seamless scrolling
            for (pass in 0..1) {
                for (char in text) {
                    drawCharacterToArray(
                        pixelArray,
                        char,
                        xPos,
                        verticalOffset,
                        brightness
                    )
                    xPos += PixelFont.CHAR_WIDTH + spacing
                }
                
                // Add gap between repeats
                xPos += MATRIX_WIDTH
            }
            
            return pixelArray
        }
        
        /**
         * Create a scrolling text buffer with wraparound.
         * @param text The text to render
         * @param bufferMultiplier How many times wider than the matrix the buffer should be
         * @param spacing Character spacing in pixels
         * @param brightness Overall brightness (0.0-1.0)
         * @return IntArray representing the extended text buffer
         */
        fun createScrollBuffer(
            text: String,
            bufferMultiplier: Int = 3,
            spacing: Int = 1,
            brightness: Float = 1.0f
        ): IntArray {
            if (text.isEmpty()) {
                return IntArray(MATRIX_WIDTH * bufferMultiplier * PixelFont.CHAR_HEIGHT) { 0 }
            }
            
            val textWidth = PixelFont.calculateTextWidth(text, spacing)
            val bufferWidth = maxOf(textWidth + MATRIX_WIDTH * 2, MATRIX_WIDTH * bufferMultiplier)
            val buffer = IntArray(bufferWidth * PixelFont.CHAR_HEIGHT) { 0 }
            
            // Render text multiple times for seamless scrolling
            var xOffset = 0
            while (xOffset < bufferWidth) {
                var charPos = 0
                for (char in text) {
                    drawCharacterToBuffer(
                        buffer,
                        bufferWidth,
                        char,
                        xOffset + charPos,
                        0,
                        brightness
                    )
                    charPos += PixelFont.CHAR_WIDTH + spacing
                }
                xOffset += textWidth + MATRIX_WIDTH // Add gap between repeats
            }
            
            return buffer
        }
        
        /**
         * Extract a window from the scroll buffer to display on the matrix.
         * Respects the circular shape of the Glyph Matrix.
         * @param buffer The scroll buffer
         * @param bufferWidth Width of the buffer
         * @param scrollOffset Current scroll position
         * @param verticalOffset Vertical position on the matrix
         * @return IntArray of 625 pixels for the Glyph Matrix
         */
        fun extractMatrixWindow(
            buffer: IntArray,
            bufferWidth: Int,
            scrollOffset: Int,
            verticalOffset: Int = 9
        ): IntArray {
            val pixelArray = IntArray(625) { 0 }
            val bufferHeight = PixelFont.CHAR_HEIGHT
            
            // Calculate wrapped scroll offset
            val wrappedOffset = if (bufferWidth > 0) {
                scrollOffset % bufferWidth
            } else {
                0
            }
            
            // Copy pixels from buffer to matrix, respecting circular shape
            for (y in 0 until bufferHeight) {
                val matrixY = verticalOffset + y
                if (matrixY !in 0 until MATRIX_HEIGHT) continue
                
                // Get the valid pixel range for this row
                val pixelsInRow = GLYPH_SHAPE[matrixY]
                val startCol = (MATRIX_WIDTH - pixelsInRow) / 2
                
                // Extract from buffer linearly and place in matrix with circular shape
                for (col in 0 until pixelsInRow) {
                    // Buffer position is linear - extract from continuous horizontal strip
                    val bufferX = (wrappedOffset + col) % bufferWidth
                    val bufferIndex = y * bufferWidth + bufferX
                    
                    // Matrix position includes circular shape offset
                    val matrixX = startCol + col
                    
                    if (bufferIndex < buffer.size) {
                        val matrixIndex = matrixY * MATRIX_WIDTH + matrixX
                        pixelArray[matrixIndex] = buffer[bufferIndex]
                    }
                }
            }
            
            return pixelArray
        }
        
        /**
         * Check if a pixel position is within the circular shape of the Glyph Matrix.
         * @param x X coordinate (0-24)
         * @param y Y coordinate (0-24)
         * @return True if the pixel is within the valid circular area
         */
        private fun isPixelInCircle(x: Int, y: Int): Boolean {
            if (y !in 0 until MATRIX_HEIGHT) return false
            
            val pixelsInRow = GLYPH_SHAPE[y]
            val startCol = (MATRIX_WIDTH - pixelsInRow) / 2
            val endCol = startCol + pixelsInRow
            
            return x in startCol until endCol
        }
        
        /**
         * Draw a character to a canvas (for bitmap rendering).
         */
        private fun drawCharacter(
            canvas: Canvas,
            paint: Paint,
            char: Char,
            x: Int,
            y: Int,
            brightness: Float
        ) {
            val charData = PixelFont.getCharacter(char)
            val alpha = (255 * brightness).toInt().coerceIn(0, 255)
            paint.alpha = alpha
            
            for (row in charData.indices) {
                for (col in charData[row].indices) {
                    if (charData[row][col]) {
                        canvas.drawPoint(
                            (x + col).toFloat(),
                            (y + row).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
        
        /**
         * Draw a character directly to a pixel array (for matrix rendering).
         * Respects the circular shape of the Glyph Matrix.
         */
        private fun drawCharacterToArray(
            pixelArray: IntArray,
            char: Char,
            x: Int,
            y: Int,
            brightness: Float
        ) {
            val charData = PixelFont.getCharacter(char)
            val pixelValue = (255 * brightness).toInt().coerceIn(0, 255)
            
            for (row in charData.indices) {
                for (col in charData[row].indices) {
                    if (charData[row][col]) {
                        val pixelX = x + col
                        val pixelY = y + row
                        
                        // Check if pixel is within the circular shape
                        if (isPixelInCircle(pixelX, pixelY)) {
                            val index = pixelY * MATRIX_WIDTH + pixelX
                            pixelArray[index] = pixelValue
                        }
                    }
                }
            }
        }
        
        /**
         * Draw a character to a buffer array.
         */
        private fun drawCharacterToBuffer(
            buffer: IntArray,
            bufferWidth: Int,
            char: Char,
            x: Int,
            y: Int,
            brightness: Float
        ) {
            val charData = PixelFont.getCharacter(char)
            val pixelValue = (255 * brightness).toInt().coerceIn(0, 255)
            val bufferHeight = buffer.size / bufferWidth
            
            for (row in charData.indices) {
                for (col in charData[row].indices) {
                    if (charData[row][col]) {
                        val pixelX = x + col
                        val pixelY = y + row
                        
                        // Check bounds
                        if (pixelX in 0 until bufferWidth && pixelY in 0 until bufferHeight) {
                            val index = pixelY * bufferWidth + pixelX
                            if (index < buffer.size) {
                                buffer[index] = pixelValue
                            }
                        }
                    }
                }
            }
        }
        
        /**
         * Apply opacity/brightness adjustment to a pixel array.
         * @param pixelArray The pixel array to modify
         * @param opacity Opacity multiplier (0.0-1.0)
         * @return Modified pixel array
         */
        fun applyOpacity(pixelArray: IntArray, opacity: Float): IntArray {
            val adjustedOpacity = opacity.coerceIn(0.0f, 1.0f)
            return pixelArray.map { pixel ->
                (pixel * adjustedOpacity).toInt().coerceIn(0, 255)
            }.toIntArray()
        }
        
        /**
         * Format media metadata for display.
         * @param title Song title
         * @param artist Artist name (optional)
         * @param album Album name (optional)
         * @param showArtist Whether to include artist
         * @param showAlbum Whether to include album
         * @param separator Separator string between fields
         * @return Formatted display string
         */
        fun formatMediaText(
            title: String?,
            artist: String?,
            album: String?,
            showArtist: Boolean = true,
            showAlbum: Boolean = true,
            separator: String = " - "
        ): String {
            val parts = mutableListOf<String>()
            
            // Always add title if available
            if (!title.isNullOrBlank()) {
                parts.add(title)
            }
            
            // Add artist if enabled and available
            if (showArtist && !artist.isNullOrBlank() && artist != "Unknown") {
                parts.add(artist)
            }
            
            // Add album if enabled and available
            if (showAlbum && !album.isNullOrBlank() && album != "Unknown") {
                parts.add(album)
            }
            
            // Fallback if no parts
            if (parts.isEmpty()) {
                return "No Media"
            }
            
            return parts.joinToString(separator)
        }
        
        /**
         * Estimate scroll speed based on text length.
         * @param textLength Length of the text
         * @param baseSpeed Base scroll speed setting (1-10)
         * @return Pixels to scroll per frame
         */
        fun calculateScrollSpeed(textLength: Int, baseSpeed: Int): Int {
            // Base speed: 1-10 maps to 1-4 pixels per frame
            val speedMapping = mapOf(
                1 to 1, 2 to 1, 3 to 2, 4 to 2, 5 to 2,
                6 to 3, 7 to 3, 8 to 3, 9 to 4, 10 to 4
            )
            
            return speedMapping[baseSpeed.coerceIn(1, 10)] ?: 2
        }
    }
}