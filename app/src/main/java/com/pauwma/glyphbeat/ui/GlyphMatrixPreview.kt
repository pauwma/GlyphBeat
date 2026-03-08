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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.animation.CoverArtTheme
import com.pauwma.glyphbeat.themes.base.AudioReactiveTheme
import com.pauwma.glyphbeat.themes.base.FrameTransitionSequence
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.ui.components.EnhancedCoverArtPreview
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
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
 * Uses enhanced preview for CoverArtTheme to show actual album art.
 */
@Composable
fun GlyphMatrixPreview(
    theme: AnimationTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    previewSize: Int = 120, // Size in dp for the preview
    settings: ThemeSettings? = null // Optional settings for enhanced preview
) {
    // Use enhanced preview for CoverArtTheme
    if (theme is CoverArtTheme && theme.useEnhancedPreview()) {
        EnhancedCoverArtPreview(
            theme = theme,
            isSelected = isSelected,
            settings = settings,
            modifier = modifier,
            previewSize = previewSize
        )
        return
    }
    
    // Regular preview for other themes
    val context = LocalContext.current
    var currentFrame by remember { mutableIntStateOf(0) }
    var transitionSequence by remember { mutableStateOf<FrameTransitionSequence?>(null) }
    var mediaTrigger by remember { mutableIntStateOf(0) }

    val isAudioReactive = theme is AudioReactiveTheme
    
    // Initialize transition sequence if theme uses frame transitions
    // Recreate when theme or settings change to support live updates
    LaunchedEffect(theme, settings) {
        transitionSequence = if (theme is ThemeTemplate && theme.hasFrameTransitions()) {
            // Apply settings first to ensure transitions reflect current configuration
            if (theme is ThemeSettingsProvider && settings != null) {
                theme.applySettings(settings)
            }
            theme.createTransitionSequence()
        } else {
            null
        }
        // Reset frame to beginning when transitions change
        currentFrame = 0
    }
    
    // Animation logic - OPTIMIZED to prevent ANR
    LaunchedEffect(isSelected, theme, transitionSequence) {
        if (isSelected) {
            // Only animate selected theme to reduce resource usage
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    val frameCount = try {
                        theme.getFrameCount()
                    } catch (e: Exception) {
                        1 // Default to 1 frame if error
                    }
                    
                    if (frameCount <= 1) break // No animation needed
                    
                    // Calculate frame timing based on theme (all on Default dispatcher)
                    val frameDuration = try {
                        val transitions = transitionSequence
                        when {
                            transitions != null -> transitions.getCurrentDuration()
                            theme is ThemeTemplate && theme.hasIndividualFrameDurations() ->
                                theme.getFrameDuration(currentFrame).coerceAtLeast(50L)
                            theme is com.pauwma.glyphbeat.themes.animation.CustomTheme ->
                                theme.getFrameDuration(currentFrame).coerceAtLeast(50L)
                            else -> theme.getAnimationSpeed().coerceAtLeast(50L)
                        }
                    } catch (e: Exception) {
                        100L // Default delay if error
                    }
                    
                    delay(frameDuration)
                    
                    // Calculate next frame on Default dispatcher
                    val nextFrame = try {
                        val transitions = transitionSequence
                        if (transitions != null) {
                            transitions.advance()
                            transitions.getCurrentFrameIndex()
                        } else {
                            (currentFrame + 1) % frameCount
                        }
                    } catch (e: Exception) {
                        0 // Reset to first frame on error
                    }
                    
                    // Only update UI state on Main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        currentFrame = nextFrame
                    }
                }
            }
        } else {
            // Unselected themes show static preview frame
            // Check if theme has a custom preview frame index
            currentFrame = try {
                // Try to get previewFrameIndex property if it exists
                val previewIndexField = theme.javaClass.getDeclaredField("previewFrameIndex")
                previewIndexField.isAccessible = true
                val index = previewIndexField.get(theme) as? Int
                index?.coerceIn(0, theme.getFrameCount() - 1) ?: 0
            } catch (e: Exception) {
                // Default to first frame if property doesn't exist
                0
            }
        }
    }
    
    // Frame generation state - computed asynchronously to prevent blocking
    var frameData by remember { mutableStateOf(IntArray(625) { 0 }) }
    
    // Audio-reactive preview: run generateAudioReactiveFrame with simulated spectrum
    // Uses the theme's real smoothing/rendering pipeline for authentic look
    LaunchedEffect(isSelected, isAudioReactive) {
        if (isSelected && isAudioReactive) {
            var time = 0.0
            withContext(Dispatchers.Default) {
                while (currentCoroutineContext().isActive) {
                    time += 0.05
                    // Base heights matching the static preview shape
                    val baseHeights = floatArrayOf(
                        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.2f,
                        0.3f, 0.5f, 0.7f, 0.9f, 0.7f, 0.5f, 0.6f, 0.8f,
                        0.6f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f
                    )
                    val spectrum = FloatArray(25) { band ->
                        val base = baseHeights[band]
                        val v1 = kotlin.math.sin(time * 2.3 + band * 0.7).toFloat() * 0.15f
                        val v2 = kotlin.math.sin(time * 3.7 + band * 0.4).toFloat() * 0.1f
                        val v3 = kotlin.math.sin(time * 1.1 + band * 1.2).toFloat() * 0.08f
                        (base + v1 + v2 + v3).coerceIn(0f, 1f)
                    }
                    val beat = (kotlin.math.sin(time * 4.0) * 0.5 + 0.5).coerceIn(0.0, 1.0)
                    val audioData = AudioData(
                        beatIntensity = beat,
                        bassLevel = spectrum.take(8).average(),
                        midLevel = spectrum.slice(8..17).average(),
                        trebleLevel = spectrum.drop(18).average(),
                        isPlaying = true,
                        spectrumBands = spectrum
                    )
                    val frame = try {
                        (theme as AudioReactiveTheme).generateAudioReactiveFrame(currentFrame, audioData)
                    } catch (e: Exception) {
                        IntArray(625) { 0 }
                    }
                    withContext(Dispatchers.Main) { frameData = frame }
                    delay(theme.getAnimationSpeed().coerceAtLeast(33L))
                }
            }
        }
    }

    // Generate frame data on background thread (fallback / non-audio-reactive)
    LaunchedEffect(theme, currentFrame, mediaTrigger) {
        // Skip if audio-reactive preview is handling it
        if (isSelected && isAudioReactive) return@LaunchedEffect

        withContext(Dispatchers.Default) {
            val generatedFrame = try {
                // For VinylTheme, ensure consistent circular shape regardless of size setting
                if (theme is com.pauwma.glyphbeat.themes.animation.VinylTheme) {
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

            // Update frame data on main thread
            withContext(Dispatchers.Main) {
                frameData = generatedFrame
            }
        }
    }
    
    // Get the theme's current brightness setting as state for smooth updates
    var themeBrightness by remember { mutableIntStateOf(255) }
    
    // Update brightness whenever theme changes or on regular intervals for smooth updates
    LaunchedEffect(theme, isSelected) {
        while (currentCoroutineContext().isActive) {
            try {
                val newBrightness = theme.getBrightness()
                if (themeBrightness != newBrightness) {
                    themeBrightness = newBrightness
                }
            } catch (e: Exception) {
                // Keep current brightness if unable to get
            }
            // Check for brightness changes every 100ms for smooth updates
            delay(100)
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
            val inner = min(size.width, size.height)
            val gapRatio = 0.18f
            val cornerRatio = 0.18f

            val startX = (size.width - inner) / 2f
            val startY = (size.height - inner) / 2f

            val cell = inner / 25f
            val gap = cell * gapRatio
            val side = cell - gap
            val cr = side * cornerRatio

            // Glyph Matrix shape definition - number of active pixels per row
            val glyphShape = intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
                25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
                19, 17, 15, 11, 7
            )

            for (row in 0 until 25) {
                val pixelsInRow = glyphShape[row]
                val colStart = (25 - pixelsInRow) / 2

                for (c in 0 until pixelsInRow) {
                    val col = colStart + c
                    val pixelIndex = row * 25 + col
                    val pixelValue = if (pixelIndex < frameData.size) frameData[pixelIndex] else 0

                    val finalBrightness = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        pixelValue,
                        themeBrightness
                    )

                    val brightness = (finalBrightness / 255f).coerceIn(0f, 1f)
                    val color = Color(brightness, brightness, brightness, 1f)

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(
                            x = startX + col * cell + gap / 2f,
                            y = startY + row * cell + gap / 2f
                        ),
                        size = Size(side, side),
                        cornerRadius = CornerRadius(cr, cr)
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