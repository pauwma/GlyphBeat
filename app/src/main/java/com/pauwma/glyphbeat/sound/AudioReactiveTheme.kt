package com.pauwma.glyphbeat.sound

/**
 * Interface for themes that can respond to real-time audio data
 */
interface AudioReactiveTheme {
    
    /**
     * Generate a frame using current audio analysis data
     * 
     * @param frameIndex Current animation frame index
     * @param audioData Real-time audio analysis data
     * @return IntArray representing the 25x25 matrix frame
     */
    fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray
    
    /**
     * Whether this theme supports audio-reactive mode
     */
    fun supportsAudioReactive(): Boolean = true
}