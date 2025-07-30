package com.pauwma.glyphbeat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.AnimationTheme
import com.pauwma.glyphbeat.GlyphMatrixRenderer
import com.pauwma.glyphbeat.animation.styles.FrameTransitionSequence
import com.pauwma.glyphbeat.animation.styles.ThemeTemplate
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Miniature Glyph Matrix preview component that displays a scaled-down version of the 25x25 matrix.
 * Shows static preview for unselected themes and animated preview for the selected theme.
 */
@Composable
fun GlyphMatrixPreview(
    theme: AnimationTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    previewSize: Int = 120 // Size in dp for the preview
) {
    var currentFrame by remember { mutableIntStateOf(0) }
    var transitionSequence by remember { mutableStateOf<FrameTransitionSequence?>(null) }
    
    // Initialize transition sequence if theme uses frame transitions
    LaunchedEffect(theme) {
        transitionSequence = if (theme is ThemeTemplate && theme.hasFrameTransitions()) {
            theme.createTransitionSequence()
        } else {
            null
        }
    }
    
    // Animation logic - only animate if selected
    LaunchedEffect(isSelected, theme, transitionSequence) {
        if (isSelected) {
            val sequence = transitionSequence
            if (sequence != null) {
                // Use frame transitions animation
                sequence.reset()
                while (true) {
                    currentFrame = sequence.getCurrentFrameIndex()
                    delay(sequence.getCurrentDuration())
                    sequence.advance()
                }
            } else {
                // Use standard frame-by-frame animation
                while (true) {
                    delay(theme.getAnimationSpeed())
                    currentFrame = (currentFrame + 1) % theme.getFrameCount()
                }
            }
        } else {
            currentFrame = 0 // Show first frame for unselected themes
        }
    }
    
    val frameData = remember(theme, currentFrame) {
        theme.generateFrame(currentFrame)
    }
    
    Box(
        modifier = modifier
            .size(previewSize.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val dotSize = min(size.width, size.height) / 25f // 25x25 grid
            val spacing = dotSize * 0.2f // Small spacing between dots
            val actualDotSize = dotSize * 0.8f
            
            // Calculate starting position to center the grid
            val startX = (size.width - (25 * dotSize)) / 2f
            val startY = (size.height - (25 * dotSize)) / 2f
            
            // Draw each pixel as a small circle/dot
            for (row in 0 until 25) {
                for (col in 0 until 25) {
                    val pixelIndex = row * 25 + col
                    val brightness = if (pixelIndex < frameData.size) frameData[pixelIndex] else 0
                    
                    // Convert brightness to color
                    val color = when {
                        brightness == 0 -> Color.Transparent
                        brightness < 100 -> Color.Gray.copy(alpha = 0.3f)
                        brightness < 200 -> Color.Gray.copy(alpha = 0.7f)
                        else -> Color.White
                    }
                    
                    if (brightness > 0) {
                        val x = startX + col * dotSize + actualDotSize / 2f
                        val y = startY + row * dotSize + actualDotSize / 2f
                        
                        drawCircle(
                            color = color,
                            radius = actualDotSize / 2f,
                            center = Offset(x, y)
                        )
                    }
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