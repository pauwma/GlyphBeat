package com.pauwma.glyphbeat.core

/**
 * Defines the Glyph Matrix resolution for supported Nothing Phone devices.
 *
 * Phone 3: 25x25 grid, 489 active pixels in diamond shape
 * Phone 4a Pro: 13x13 grid, 137 active pixels in diamond shape
 */
enum class GlyphResolution(
    val gridSize: Int,
    val pixelCount: Int,
    val flatSize: Int,
    val shape: IntArray,
    val center: Int,
    val maxRadius: Float,
    val jsonVersion: Int,
    val dbValue: String
) {
    PHONE_3(
        gridSize = 25,
        pixelCount = 489,
        flatSize = 625,
        shape = intArrayOf(7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25, 25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7),
        center = 12,
        maxRadius = 12.5f,
        jsonVersion = 1,
        dbValue = "3"
    ),
    PHONE_4A(
        gridSize = 13,
        pixelCount = 137,
        flatSize = 169,
        shape = intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5),
        center = 6,
        maxRadius = 6.5f,
        jsonVersion = 4,
        dbValue = "4a"
    );

    companion object {
        fun fromPixelCount(count: Int): GlyphResolution = when (count) {
            489, 625 -> PHONE_3
            137, 169 -> PHONE_4A
            else -> PHONE_3
        }

        fun fromJsonVersion(version: Int): GlyphResolution = when (version) {
            4 -> PHONE_4A
            else -> PHONE_3
        }

        fun fromDbValue(value: String): GlyphResolution = when (value) {
            "4a" -> PHONE_4A
            else -> PHONE_3
        }
    }
}
