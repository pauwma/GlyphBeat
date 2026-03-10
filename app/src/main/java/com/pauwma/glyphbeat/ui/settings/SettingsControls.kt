package com.pauwma.glyphbeat.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.theme.GeistMonoFont
import kotlin.math.roundToInt

/**
 * Slider control for numeric settings.
 * Compact design with value under title and descriptive end labels.
 */
@Composable
fun SettingsSlider(
    setting: SliderSetting,
    currentValue: Number,
    onValueChange: (Number) -> Unit,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val floatValue = currentValue.toFloat()

    // Determine icon based on setting ID
    val icon: ImageVector? = when (setting.id) {
        CommonSettingIds.ANIMATION_SPEED -> Icons.Default.Speed
        CommonSettingIds.BRIGHTNESS -> Icons.Rounded.LightMode
        else -> null
    }

    // Determine descriptive end labels based on setting ID
    val (startLabel, endLabel) = when (setting.id) {
        CommonSettingIds.ANIMATION_SPEED -> "Slow" to "Fast"
        CommonSettingIds.BRIGHTNESS -> "Dim" to "Bright"
        else -> {
            val minLabel = when {
                setting.unit == "x" -> String.format(java.util.Locale.ROOT, "%.1fx", setting.minValue.toFloat())
                else -> setting.minValue.toString()
            }
            val maxLabel = when {
                setting.unit == "x" -> String.format(java.util.Locale.ROOT, "%.1fx", setting.maxValue.toFloat())
                else -> setting.maxValue.toString()
            }
            minLabel to maxLabel
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Row 1: Title + optional icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Row 2: Current value in GeistMono
        if (setting.showValue) {
            val displayValue = when {
                setting.unit == "x" -> {
                    String.format(java.util.Locale.ROOT, "%.1f", currentValue.toFloat())
                }
                setting.stepSize.toFloat() % 1 == 0f -> currentValue.toInt().toString()
                else -> String.format(java.util.Locale.ROOT, "%.1f", currentValue.toFloat())
            }
            val valueText = if (setting.unit != null) "$displayValue${setting.unit}" else displayValue

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = GeistMonoFont
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact slider
        Slider(
            value = floatValue,
            onValueChange = { newValue ->
                val steps = ((newValue - setting.minValue.toFloat()) / setting.stepSize.toFloat()).roundToInt()
                val snappedValue = setting.minValue.toFloat() + (steps * setting.stepSize.toFloat())
                val finalValue = when {
                    setting.stepSize.toFloat() % 1 == 0f -> snappedValue.toInt()
                    else -> snappedValue
                }
                onValueChange(finalValue)
            },
            valueRange = setting.minValue.toFloat()..setting.maxValue.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .graphicsLayer(scaleY = 0.95f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                activeTickColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Descriptive end labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = GeistMonoFont
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = GeistMonoFont
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Toggle/switch control for boolean settings.
 */
@Composable
fun SettingsToggle(
    setting: ToggleSetting,
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Custom toggle switch
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (currentValue) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onValueChange(!currentValue) }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .align(if (currentValue) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }
        }
    }
}

/**
 * Toggle buttons for dropdown settings with <=4 options.
 */
@Composable
fun SettingsToggleButtons(
    setting: DropdownSetting,
    currentValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = setting.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = customFont,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            setting.options.forEach { option ->
                val isSelected = option.value == currentValue

                if (isSelected) {
                    Button(
                        onClick = { onValueChange(option.value) },
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                            .defaultMinSize(minHeight = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        AutoScalingText(
                            text = option.label,
                            maxFontSize = 12.sp,
                            minFontSize = 8.sp,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            fontFamily = GeistMonoFont,
                            maxLines = 1
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onValueChange(option.value) },
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                            .defaultMinSize(minHeight = 18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        AutoScalingText(
                            text = option.label,
                            maxFontSize = 12.sp,
                            minFontSize = 8.sp,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontFamily = GeistMonoFont,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Auto-scaling text that fits within its container.
 */
@Composable
fun AutoScalingText(
    text: String,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
) {
    Text(
        text = text,
        style = style.copy(
            fontSize = maxFontSize,
            fontWeight = fontWeight ?: style.fontWeight
        ),
        fontFamily = fontFamily,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = modifier
    )
}

/**
 * Dropdown control for multiple choice settings (5+ options).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    setting: DropdownSetting,
    currentValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    var expanded by remember { mutableStateOf(false) }
    val currentOption = setting.options.find { it.value == currentValue }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Setting name
        if (setting.displayName.isNotEmpty()) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Dropdown trigger
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = if (expanded)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .menuAnchor()
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentOption?.label ?: currentValue,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = GeistMonoFont,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dropdown menu
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                setting.options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GeistMonoFont,
                                    fontSize = 14.sp
                                ),
                                color = if (option.value == currentValue)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onValueChange(option.value)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}