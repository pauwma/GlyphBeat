package com.pauwma.glyphbeat.utils

import android.util.Base64

/**
 * Simple UID obfuscation for deep links between Glyph Museum and Glyph Beat.
 * XOR with a fixed key + Base64 encoding to deter casual inspection.
 * Not cryptographically secure — just keeps UIDs out of plain sight.
 */
object UidObfuscation {

    private const val XOR_KEY = "GlyphBeatMuseum2024"

    fun encode(uid: String): String {
        val keyBytes = XOR_KEY.toByteArray(Charsets.UTF_8)
        val xored = uid.toByteArray(Charsets.UTF_8).mapIndexed { index, byte ->
            (byte.toInt() xor keyBytes[index % keyBytes.size].toInt()).toByte()
        }.toByteArray()
        return Base64.encodeToString(xored, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decode(token: String): String {
        val keyBytes = XOR_KEY.toByteArray(Charsets.UTF_8)
        val xored = Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP)
        val original = xored.mapIndexed { index, byte ->
            (byte.toInt() xor keyBytes[index % keyBytes.size].toInt()).toByte()
        }.toByteArray()
        return String(original, Charsets.UTF_8)
    }
}
