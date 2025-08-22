package com.pauwma.glyphbeat.utils

/**
 * Pixel font definitions for rendering text on the Glyph Matrix.
 * Uses a 5x7 pixel font for good readability on the 25x25 matrix.
 * 
 * Each character is defined as a 2D array where:
 * - true = pixel on
 * - false = pixel off
 */
object PixelFont {
    
    const val CHAR_WIDTH = 5
    const val CHAR_HEIGHT = 7
    const val CHAR_SPACING = 1 // Space between characters
    
    /**
     * Font data for ASCII characters 32-126
     * Each character is represented as a 7x5 boolean array
     */
    private val fontData = mapOf(
        ' ' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        '!' to arrayOf(
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, true, false, false)
        ),
        '"' to arrayOf(
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        '\'' to arrayOf(
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        '(' to arrayOf(
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, true, false)
        ),
        ')' to arrayOf(
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false)
        ),
        ',' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false)
        ),
        '-' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        '.' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, true, true, false, false)
        ),
        '/' to arrayOf(
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, false, false, false, false)
        ),
        '0' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, true, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, true, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        '1' to arrayOf(
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        '2' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, true, true, true, true)
        ),
        '3' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, true, true, false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        '4' to arrayOf(
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, true, false),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false)
        ),
        '5' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        '6' to arrayOf(
            booleanArrayOf(false, false, true, true, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        '7' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false)
        ),
        '8' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        '9' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, true, true, false, false)
        ),
        ':' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        'A' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'B' to arrayOf(
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false)
        ),
        'C' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'D' to arrayOf(
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false)
        ),
        'E' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, true)
        ),
        'F' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false)
        ),
        'G' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'H' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'I' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        'J' to arrayOf(
            booleanArrayOf(false, false, true, true, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(false, true, true, false, false)
        ),
        'K' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(true, false, true, false, false),
            booleanArrayOf(true, true, false, false, false),
            booleanArrayOf(true, false, true, false, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(true, false, false, false, true)
        ),
        'L' to arrayOf(
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, true)
        ),
        'M' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, false, true, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'N' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, false, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, false, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'O' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'P' to arrayOf(
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false)
        ),
        'Q' to arrayOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(false, true, true, false, true)
        ),
        'R' to arrayOf(
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, true, false, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(true, false, false, false, true)
        ),
        'S' to arrayOf(
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true, true, true, true, false)
        ),
        'T' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false)
        ),
        'U' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'V' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, true, false, false)
        ),
        'W' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, true, false, true, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'X' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'Y' to arrayOf(
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false)
        ),
        'Z' to arrayOf(
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, true)
        ),
        // Lowercase letters (same as uppercase for simplicity)
        'a' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true)
        ),
        'b' to arrayOf(
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false)
        ),
        'c' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        'd' to arrayOf(
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true)
        ),
        'e' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        'f' to arrayOf(
            booleanArrayOf(false, false, true, true, false),
            booleanArrayOf(false, true, false, false, true),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false)
        ),
        'g' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'h' to arrayOf(
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'i' to arrayOf(
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        'j' to arrayOf(
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, true, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(false, true, true, false, false)
        ),
        'k' to arrayOf(
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, true, false),
            booleanArrayOf(true, false, true, false, false),
            booleanArrayOf(true, true, false, false, false),
            booleanArrayOf(true, false, true, false, false),
            booleanArrayOf(true, false, false, true, false)
        ),
        'l' to arrayOf(
            booleanArrayOf(false, true, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, true, true, false)
        ),
        'm' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, true, false, true, false),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'n' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true)
        ),
        'o' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'p' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(true, false, false, false, false)
        ),
        'q' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true)
        ),
        'r' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, true, true, false),
            booleanArrayOf(true, true, false, false, true),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(true, false, false, false, false)
        ),
        's' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true, true, true, true, false)
        ),
        't' to arrayOf(
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, true, true, true, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, true, false, false, true),
            booleanArrayOf(false, false, true, true, false)
        ),
        'u' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true)
        ),
        'v' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, true, false, false)
        ),
        'w' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(true, false, true, false, true),
            booleanArrayOf(false, true, false, true, false)
        ),
        'x' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, true, false),
            booleanArrayOf(true, false, false, false, true)
        ),
        'y' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(true, false, false, false, true),
            booleanArrayOf(false, true, true, true, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, true, true, true, false)
        ),
        'z' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(true, true, true, true, true),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(true, true, true, true, true)
        )
    )
    
    /**
     * Get the pixel data for a character.
     * Returns a default character (space) if the character is not defined.
     */
    fun getCharacter(char: Char): Array<BooleanArray> {
        return fontData[char] ?: fontData[' ']!!
    }
    
    /**
     * Check if a character is defined in the font.
     */
    fun hasCharacter(char: Char): Boolean {
        return fontData.containsKey(char)
    }
    
    /**
     * Calculate the width needed to render a string.
     * @param text The text to measure
     * @param spacing Additional spacing between characters (default is CHAR_SPACING)
     * @return Total width in pixels
     */
    fun calculateTextWidth(text: String, spacing: Int = CHAR_SPACING): Int {
        if (text.isEmpty()) return 0
        return text.length * CHAR_WIDTH + (text.length - 1) * spacing
    }
}