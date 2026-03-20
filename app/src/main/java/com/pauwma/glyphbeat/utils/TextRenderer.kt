package com.pauwma.glyphbeat.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.pauwma.glyphbeat.core.DeviceManager

/**
 * Text rendering utilities for converting strings to pixel arrays for the Glyph Matrix.
 * Handles text-to-bitmap conversion, scrolling buffers, and brightness adjustments.
 * Respects the circular shape of the Glyph Matrix for proper rendering.
 * Resolution-aware: adapts to both Phone 3 (25x25) and Phone 4a Pro (13x13).
 */
class TextRenderer {

    companion object {
        private const val LOG_TAG = "TextRenderer"

        // Resolution-aware dimensions
        private val matrixWidth: Int get() = DeviceManager.resolution.gridSize
        private val matrixHeight: Int get() = DeviceManager.resolution.gridSize
        private val glyphShape: IntArray get() = DeviceManager.resolution.shape

        /**
         * Render text directly to a pixel array that fits the matrix.
         * @param text The text to render
         * @param scrollOffset Horizontal scroll offset
         * @param spacing Character spacing in pixels
         * @param brightness Overall brightness (0.0-1.0)
         * @param verticalOffset Vertical position offset (for centering). Default scales with grid size.
         * @return IntArray sized for current device resolution
         */
        fun renderTextToMatrix(
            text: String,
            scrollOffset: Int = 0,
            spacing: Int = 1,
            brightness: Float = 1.0f,
            verticalOffset: Int = (DeviceManager.resolution.gridSize * 9 + 12) / 25
        ): IntArray {
            val width = matrixWidth
            val pixelArray = IntArray(width * width) { 0 }

            if (text.isEmpty()) return pixelArray

            // Calculate text dimensions
            val textWidth = PixelFont.calculateTextWidth(text, spacing)

            // For seamless scrolling, we need to handle wraparound
            val effectiveOffset = if (textWidth > 0) {
                scrollOffset % (textWidth + width)
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
                xPos += width
            }

            return pixelArray
        }

        /**
         * Check if a pixel position is within the circular shape of the Glyph Matrix.
         * Resolution-aware: uses current device's shape definition.
         */
        private fun isPixelInCircle(x: Int, y: Int): Boolean {
            val height = matrixHeight
            val width = matrixWidth
            val shape = glyphShape
            if (y !in 0 until height) return false

            val pixelsInRow = shape[y]
            val startCol = (width - pixelsInRow) / 2
            val endCol = startCol + pixelsInRow

            return x in startCol until endCol
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
            val width = matrixWidth
            // Apply minimum brightness threshold to match hardware behavior
            val minBrightness = 0.034f  // Minimum visible brightness
            val adjustedBrightness = if (brightness > 0) {
                    minBrightness + (1.0f - minBrightness) * brightness
                } else {
                    0f
                }
            val pixelValue = (255 * adjustedBrightness).toInt().coerceIn(0, 255)

            for (row in charData.indices) {
                for (col in charData[row].indices) {
                    if (charData[row][col]) {
                        val pixelX = x + col
                        val pixelY = y + row

                        // Check if pixel is within the circular shape
                        if (isPixelInCircle(pixelX, pixelY)) {
                            val index = pixelY * width + pixelX
                            if (index in pixelArray.indices) {
                                pixelArray[index] = pixelValue
                            }
                        }
                    }
                }
            }
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

    }
}