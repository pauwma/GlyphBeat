package com.pauwma.glyphbeat.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A Text composable that automatically scales down font size to fit within specified constraints.
 *
 * @param text The text to be displayed
 * @param modifier Modifier to be applied to the Text
 * @param color The color of the text
 * @param maxFontSize The maximum font size to start with (default: 18.sp)
 * @param minFontSize The minimum font size to scale down to (default: 10.sp)
 * @param maxLines Maximum number of lines allowed (default: 1)
 * @param fontSizeStep The step size for font size reduction (default: 1.sp)
 * @param style Base text style to apply
 * @param textAlign Text alignment
 * @param fontFamily Font family
 * @param fontWeight Font weight
 * @param fontStyle Font style
 * @param letterSpacing Letter spacing
 * @param textDecoration Text decoration
 */
@Composable
fun AutoScalingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = 18.sp,
    minFontSize: TextUnit = 10.sp,
    maxLines: Int = 1,
    fontSizeStep: TextUnit = 1.sp,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null
) {
    var scaledFontSize by remember(text, maxFontSize, minFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }
    val density = LocalDensity.current

    Text(
        text = text,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        style = style.copy(
            fontSize = scaledFontSize,
            fontFamily = fontFamily ?: style.fontFamily,
            fontWeight = fontWeight ?: style.fontWeight,
            fontStyle = fontStyle ?: style.fontStyle,
            letterSpacing = letterSpacing.takeIf { it != TextUnit.Unspecified } ?: style.letterSpacing,
            textDecoration = textDecoration ?: style.textDecoration
        ),
        textAlign = textAlign,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        onTextLayout = { textLayoutResult: TextLayoutResult ->
            val maxFontSizePx = with(density) { maxFontSize.toPx() }
            val minFontSizePx = with(density) { minFontSize.toPx() }
            val stepSizePx = with(density) { fontSizeStep.toPx() }

            if (textLayoutResult.didOverflowHeight || textLayoutResult.lineCount > maxLines) {
                val currentFontSizePx = with(density) { scaledFontSize.toPx() }
                val newFontSizePx = (currentFontSizePx - stepSizePx).coerceAtLeast(minFontSizePx)

                if (newFontSizePx != currentFontSizePx && newFontSizePx >= minFontSizePx) {
                    scaledFontSize = with(density) { newFontSizePx.toSp() }
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}