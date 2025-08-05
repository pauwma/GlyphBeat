package com.pauwma.glyphbeat.core

/**
 * Utility class for transforming pixel data between different formats for the Glyph Matrix.
 * 
 * The Glyph Matrix has a unique shape where each row has different number of pixels:
 * Row 0: 7 pixels, Row 1: 11 pixels, etc.
 * 
 * This class converts between:
 * 1. Shaped grid format: 2D array where each row contains only the actual pixels for that row
 * 2. Flat array format: 1D array of 625 elements (25x25) expected by the SDK
 */
class GlyphMatrixRenderer {
    
    companion object {
        // The shape definition for each row of the Glyph Matrix
        private val GLYPH_SHAPE = intArrayOf(
            7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
            25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
            19, 17, 15, 11, 7
        )
        
        const val TOTAL_ROWS = 25
        const val MAX_COLUMNS = 25
        const val FLAT_ARRAY_SIZE = TOTAL_ROWS * MAX_COLUMNS // 625
        
        /**
         * Converts a shaped grid to a flat array format expected by the Glyph Matrix SDK.
         * 
         * @param shapedGrid 2D array where shapedGrid[row] contains the pixel values for that row.
         *                   Each row should have exactly GLYPH_SHAPE[row] elements.
         * @return IntArray of size 625 with pixels positioned correctly for the matrix
         */
        fun shapedGridToFlatArray(shapedGrid: Array<IntArray>): IntArray {
            require(shapedGrid.size == TOTAL_ROWS) { 
                "Shaped grid must have exactly $TOTAL_ROWS rows, got ${shapedGrid.size}" 
            }
            
            val flatArray = IntArray(FLAT_ARRAY_SIZE) { 0 }
            
            for (row in 0 until TOTAL_ROWS) {
                val expectedPixels = GLYPH_SHAPE[row]
                val rowData = shapedGrid[row]
                
                require(rowData.size == expectedPixels) {
                    "Row $row should have $expectedPixels pixels, got ${rowData.size}"
                }
                
                // Calculate the starting column to center the pixels in this row
                val startCol = (MAX_COLUMNS - expectedPixels) / 2
                
                // Copy pixels to the flat array
                for (col in 0 until expectedPixels) {
                    val flatIndex = row * MAX_COLUMNS + (startCol + col)
                    flatArray[flatIndex] = rowData[col]
                }
            }
            
            return flatArray
        }
        
        /**
         * Converts a flat array back to shaped grid format.
         * 
         * @param flatArray IntArray of size 625 from the Glyph Matrix SDK
         * @return 2D array where each row contains only the actual pixels for that row
         */
        fun flatArrayToShapedGrid(flatArray: IntArray): Array<IntArray> {
            require(flatArray.size == FLAT_ARRAY_SIZE) {
                "Flat array must have exactly $FLAT_ARRAY_SIZE elements, got ${flatArray.size}"
            }
            
            val shapedGrid = Array(TOTAL_ROWS) { row ->
                IntArray(GLYPH_SHAPE[row])
            }
            
            for (row in 0 until TOTAL_ROWS) {
                val pixelsInRow = GLYPH_SHAPE[row]
                val startCol = (MAX_COLUMNS - pixelsInRow) / 2
                
                for (col in 0 until pixelsInRow) {
                    val flatIndex = row * MAX_COLUMNS + (startCol + col)
                    shapedGrid[row][col] = flatArray[flatIndex]
                }
            }
            
            return shapedGrid
        }
        
        /**
         * Creates an empty shaped grid with all pixels set to 0.
         * 
         * @return Empty 2D array with correct shape for the Glyph Matrix
         */
        fun createEmptyShapedGrid(): Array<IntArray> {
            return Array(TOTAL_ROWS) { row ->
                IntArray(GLYPH_SHAPE[row]) { 0 }
            }
        }
        
        /**
         * Creates an empty flat array with all pixels set to 0.
         * 
         * @return Empty IntArray of size 625
         */
        fun createEmptyFlatArray(): IntArray {
            return IntArray(FLAT_ARRAY_SIZE) { 0 }
        }
        
        /**
         * Parses a comma-separated string of pixel values into a flat array.
         * 
         * @param pixelString Comma-separated string of 625 pixel values
         * @return IntArray of parsed pixel values
         */
        fun parsePixelString(pixelString: String): IntArray {
            val values = pixelString.split(",").map { it.trim().toInt() }
            require(values.size == FLAT_ARRAY_SIZE) {
                "Pixel string must contain exactly $FLAT_ARRAY_SIZE values, got ${values.size}"
            }
            return values.toIntArray()
        }
        
        /**
         * Converts a flat array to a comma-separated string.
         * 
         * @param flatArray IntArray of pixel values
         * @return Comma-separated string representation
         */
        fun flatArrayToPixelString(flatArray: IntArray): String {
            require(flatArray.size == FLAT_ARRAY_SIZE) {
                "Flat array must have exactly $FLAT_ARRAY_SIZE elements, got ${flatArray.size}"
            }
            return flatArray.joinToString(",")
        }
        
        /**
         * Gets the number of actual pixels in a specific row.
         * 
         * @param row Row index (0-24)
         * @return Number of pixels in that row
         */
        fun getPixelsInRow(row: Int): Int {
            require(row in 0 until TOTAL_ROWS) {
                "Row must be between 0 and ${TOTAL_ROWS - 1}, got $row"
            }
            return GLYPH_SHAPE[row]
        }
        
        /**
         * Gets the starting column index for a specific row (for centering).
         * 
         * @param row Row index (0-24)
         * @return Starting column index for that row
         */
        fun getStartColumnForRow(row: Int): Int {
            require(row in 0 until TOTAL_ROWS) {
                "Row must be between 0 and ${TOTAL_ROWS - 1}, got $row"
            }
            return (MAX_COLUMNS - GLYPH_SHAPE[row]) / 2
        }
        
        /**
         * Validates that a brightness value is within the valid range.
         * 
         * @param brightness Brightness value to validate
         * @return Clamped brightness value between 0 and 255
         */
        fun clampBrightness(brightness: Int): Int {
            return brightness.coerceIn(0, 255)
        }
        
        // Animation and Drawing Utilities
        
        /**
         * Draws a line between two points on a flat array grid.
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param x1 Starting X coordinate
         * @param y1 Starting Y coordinate
         * @param x2 Ending X coordinate
         * @param y2 Ending Y coordinate
         * @param brightness Brightness value (0-255)
         */
        fun drawLine(grid: IntArray, x1: Int, y1: Int, x2: Int, y2: Int, brightness: Int) {
            val clampedBrightness = clampBrightness(brightness)
            val steps = maxOf(kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1), 1)
            
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = (x1 + (x2 - x1) * t).toInt()
                val y = (y1 + (y2 - y1) * t).toInt()
                
                if (x in 0 until MAX_COLUMNS && y in 0 until TOTAL_ROWS) {
                    grid[y * MAX_COLUMNS + x] = clampedBrightness
                }
            }
        }
        
        /**
         * Draws a circle outline on a flat array grid.
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param centerX Center X coordinate
         * @param centerY Center Y coordinate
         * @param radius Circle radius
         * @param brightness Brightness value (0-255)
         */
        fun drawCircle(grid: IntArray, centerX: Int, centerY: Int, radius: Int, brightness: Int) {
            val clampedBrightness = clampBrightness(brightness)
            val angleStep = 360.0 / (radius * 8) // More points for larger circles
            
            for (angle in 0 until 360 step angleStep.toInt()) {
                val radians = Math.toRadians(angle.toDouble())
                val x = centerX + (kotlin.math.cos(radians) * radius).toInt()
                val y = centerY + (kotlin.math.sin(radians) * radius).toInt()
                
                if (x in 0 until MAX_COLUMNS && y in 0 until TOTAL_ROWS) {
                    grid[y * MAX_COLUMNS + x] = clampedBrightness
                }
            }
        }
        
        /**
         * Draws a filled dot (small circle) on a flat array grid.
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param centerX Center X coordinate
         * @param centerY Center Y coordinate
         * @param radius Dot radius
         * @param brightness Brightness value (0-255)
         */
        fun drawDot(grid: IntArray, centerX: Int, centerY: Int, radius: Int, brightness: Int) {
            val clampedBrightness = clampBrightness(brightness)
            
            for (y in (centerY - radius)..(centerY + radius)) {
                for (x in (centerX - radius)..(centerX + radius)) {
                    val distance = kotlin.math.sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble())
                    if (distance <= radius && x in 0 until MAX_COLUMNS && y in 0 until TOTAL_ROWS) {
                        grid[y * MAX_COLUMNS + x] = clampedBrightness
                    }
                }
            }
        }
        
        /**
         * Fills the entire grid with a specific brightness value.
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param brightness Brightness value (0-255)
         */
        fun fillGrid(grid: IntArray, brightness: Int) {
            val clampedBrightness = clampBrightness(brightness)
            grid.fill(clampedBrightness)
        }
        
        /**
         * Creates a rotating line animation with the specified number of frames.
         * 
         * @param frameCount Number of animation frames to generate
         * @param lineLength Length of the rotating line
         * @param brightness Brightness of the line (0-255)
         * @return Array of IntArrays, each representing one animation frame
         */
        fun createRotatingLineFrames(frameCount: Int, lineLength: Int = 8, brightness: Int = 255): Array<IntArray> {
            return Array(frameCount) { frameIndex ->
                val grid = createEmptyFlatArray()
                val centerX = MAX_COLUMNS / 2
                val centerY = TOTAL_ROWS / 2
                val angle = frameIndex * 360.0 / frameCount
                val radians = Math.toRadians(angle)
                
                val endX = centerX + (kotlin.math.cos(radians) * lineLength).toInt()
                val endY = centerY + (kotlin.math.sin(radians) * lineLength).toInt()
                
                drawLine(grid, centerX, centerY, endX, endY, brightness)
                drawDot(grid, centerX, centerY, 1, brightness) // Center dot
                
                grid
            }
        }
        
        /**
         * Creates a pulsing circle animation with the specified number of frames.
         * 
         * @param frameCount Number of animation frames to generate
         * @param maxRadius Maximum radius of the pulse
         * @param brightness Brightness of the circle (0-255)
         * @return Array of IntArrays, each representing one animation frame
         */
        fun createPulseFrames(frameCount: Int, maxRadius: Int = 10, brightness: Int = 255): Array<IntArray> {
            return Array(frameCount) { frameIndex ->
                val grid = createEmptyFlatArray()
                val centerX = MAX_COLUMNS / 2
                val centerY = TOTAL_ROWS / 2
                
                // Create expanding and contracting pulse
                val progress = frameIndex.toFloat() / frameCount
                val radius = (kotlin.math.sin(progress * Math.PI) * maxRadius).toInt()
                
                if (radius > 0) {
                    drawCircle(grid, centerX, centerY, radius, brightness)
                }
                drawDot(grid, centerX, centerY, 1, brightness) // Center dot
                
                grid
            }
        }
        
        /**
         * Creates a wave animation with the specified number of frames.
         * 
         * @param frameCount Number of animation frames to generate
         * @param amplitude Wave amplitude
         * @param brightness Brightness of the wave (0-255)
         * @return Array of IntArrays, each representing one animation frame
         */
        fun createWaveFrames(frameCount: Int, amplitude: Int = 5, brightness: Int = 255): Array<IntArray> {
            return Array(frameCount) { frameIndex ->
                val grid = createEmptyFlatArray()
                val centerY = TOTAL_ROWS / 2
                val phaseOffset = frameIndex * 2 * Math.PI / frameCount
                
                for (x in 0 until MAX_COLUMNS) {
                    val y = centerY + (kotlin.math.sin(x * 0.5 + phaseOffset) * amplitude).toInt()
                    if (y in 0 until TOTAL_ROWS) {
                        grid[y * MAX_COLUMNS + x] = brightness
                    }
                }
                
                grid
            }
        }
        
        /**
         * Creates horizontal wave frames optimized for audio-reactive themes.
         * 
         * @param frameCount Number of animation frames to generate
         * @param amplitude Wave amplitude (1-8)
         * @param brightness Brightness of the wave (0-255)
         * @param wavelength Number of complete wave cycles across the display
         * @return Array of IntArrays, each representing one animation frame
         */
        fun createHorizontalWaveFrames(
            frameCount: Int, 
            amplitude: Int = 5, 
            brightness: Int = 255,
            wavelength: Float = 2.0f
        ): Array<IntArray> {
            return Array(frameCount) { frameIndex ->
                val grid = createEmptyFlatArray()
                drawHorizontalWave(grid, frameIndex, frameCount, amplitude, brightness, wavelength)
                grid
            }
        }
        
        /**
         * Draws a horizontal sine wave on the grid.
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param frameIndex Current frame index for animation phase
         * @param frameCount Total frame count for phase calculation
         * @param amplitude Wave amplitude (1-8)
         * @param brightness Brightness of the wave (0-255)
         * @param wavelength Number of complete wave cycles across the display
         * @param thickness Wave line thickness (1-3)
         */
        fun drawHorizontalWave(
            grid: IntArray, 
            frameIndex: Int = 0, 
            frameCount: Int = 1,
            amplitude: Int = 5, 
            brightness: Int = 255,
            wavelength: Float = 2.0f,
            thickness: Int = 1
        ) {
            val clampedBrightness = clampBrightness(brightness)
            val clampedAmplitude = amplitude.coerceIn(1, 8)
            val clampedThickness = thickness.coerceIn(1, 3)
            
            val centerY = TOTAL_ROWS / 2
            val phaseOffset = if (frameCount > 1) {
                frameIndex * 2 * Math.PI / frameCount
            } else {
                0.0
            }
            
            for (x in 0 until MAX_COLUMNS) {
                // Calculate wave Y position
                val waveY = centerY + (kotlin.math.sin(x * wavelength * Math.PI / MAX_COLUMNS + phaseOffset) * clampedAmplitude).toInt()
                
                // Draw wave with thickness
                for (thickness_offset in -(clampedThickness/2)..(clampedThickness/2)) {
                    val pixelY = waveY + thickness_offset
                    if (pixelY in 0 until TOTAL_ROWS) {
                        // Distance-based brightness for thickness gradient
                        val distanceFromCenter = kotlin.math.abs(thickness_offset).toFloat() / (clampedThickness/2).coerceAtLeast(1)
                        val adjustedBrightness = (clampedBrightness * (1.0 - distanceFromCenter * 0.4)).toInt()
                        
                        grid[pixelY * MAX_COLUMNS + x] = adjustedBrightness
                    }
                }
            }
        }
        
        /**
         * Draws multiple horizontal waves with different frequencies (audio spectrum simulation).
         * 
         * @param grid IntArray representing the 25x25 matrix
         * @param bassLevel Bass frequency level (0.0-1.0)
         * @param midLevel Mid frequency level (0.0-1.0)
         * @param trebleLevel Treble frequency level (0.0-1.0)
         * @param frameIndex Current frame for animation phase
         * @param frameCount Total frames for phase calculation
         * @param maxBrightness Maximum brightness (0-255)
         */
        fun drawAudioSpectrumWaves(
            grid: IntArray,
            bassLevel: Double,
            midLevel: Double,
            trebleLevel: Double,
            frameIndex: Int = 0,
            frameCount: Int = 1,
            maxBrightness: Int = 255
        ) {
            val phaseOffset = if (frameCount > 1) {
                frameIndex * 2 * Math.PI / frameCount
            } else {
                0.0
            }
            
            // Draw bass wave (low frequency, high amplitude)
            if (bassLevel > 0.05) {
                val bassAmplitude = (bassLevel * 6).toInt().coerceIn(1, 6)
                val bassBrightness = (maxBrightness * bassLevel * 0.9).toInt()
                val bassY = 18 // Lower position for bass
                
                for (x in 0 until MAX_COLUMNS) {
                    val waveY = bassY + (kotlin.math.sin(x * 0.8 * Math.PI / MAX_COLUMNS + phaseOffset) * bassAmplitude).toInt()
                    if (waveY in 0 until TOTAL_ROWS) {
                        grid[waveY * MAX_COLUMNS + x] = clampBrightness(bassBrightness)
                    }
                }
            }
            
            // Draw mid wave (medium frequency)
            if (midLevel > 0.05) {
                val midAmplitude = (midLevel * 4).toInt().coerceIn(1, 4)
                val midBrightness = (maxBrightness * midLevel * 0.8).toInt()
                val midY = 12 // Center position for mids
                
                for (x in 0 until MAX_COLUMNS) {
                    val waveY = midY + (kotlin.math.sin(x * 1.5 * Math.PI / MAX_COLUMNS + phaseOffset) * midAmplitude).toInt()
                    if (waveY in 0 until TOTAL_ROWS) {
                        grid[waveY * MAX_COLUMNS + x] = clampBrightness(midBrightness)
                    }
                }
            }
            
            // Draw treble wave (high frequency, low amplitude)
            if (trebleLevel > 0.05) {
                val trebleAmplitude = (trebleLevel * 3).toInt().coerceIn(1, 3)
                val trebleBrightness = (maxBrightness * trebleLevel * 0.7).toInt()
                val trebleY = 6 // Upper position for treble
                
                for (x in 0 until MAX_COLUMNS) {
                    val waveY = trebleY + (kotlin.math.sin(x * 2.5 * Math.PI / MAX_COLUMNS + phaseOffset) * trebleAmplitude).toInt()
                    if (waveY in 0 until TOTAL_ROWS) {
                        grid[waveY * MAX_COLUMNS + x] = clampBrightness(trebleBrightness)
                    }
                }
            }
        }
    }
}