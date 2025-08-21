package com.pauwma.glyphbeat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import kotlin.math.roundToInt

/**
 * Slider control for numeric settings.
 * Displays current value, min/max labels, and provides smooth interaction.
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
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Header with name and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (setting.showValue) {
                val displayValue = when {
                    setting.unit == "x" -> {
                        // Special formatting for brightness multipliers
                        val multiplier = currentValue.toFloat()
                        String.format(java.util.Locale.ROOT, "%.1f", multiplier)
                    }
                    setting.stepSize.toFloat() % 1 == 0f -> currentValue.toInt().toString()
                    else -> String.format(java.util.Locale.ROOT, "%.1f", currentValue.toFloat())
                }
                val valueText = if (setting.unit != null) "$displayValue${setting.unit}" else displayValue
                
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = customFont,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Description
        Text(
            text = setting.description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = customFont,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Slider with min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val minLabel = when {
                setting.unit == "x" -> {
                    val multiplier = setting.minValue.toFloat()
                    String.format(java.util.Locale.ROOT, "%.1fx", multiplier)
                }
                else -> setting.minValue.toString()
            }
            
            Text(
                text = minLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = customFont,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Start
            )
            
            Slider(
                value = floatValue,
                onValueChange = { newValue ->
                    // Snap to step size
                    val steps = ((newValue - setting.minValue.toFloat()) / setting.stepSize.toFloat()).roundToInt()
                    val snappedValue = setting.minValue.toFloat() + (steps * setting.stepSize.toFloat())
                    
                    // Convert back to appropriate number type
                    val finalValue = when {
                        setting.stepSize.toFloat() % 1 == 0f -> snappedValue.toInt()
                        else -> snappedValue
                    }
                    onValueChange(finalValue)
                },
                valueRange = setting.minValue.toFloat()..setting.maxValue.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            val maxLabel = when {
                setting.unit == "x" -> {
                    val multiplier = setting.maxValue.toFloat()
                    String.format(java.util.Locale.ROOT, "%.1fx", multiplier)
                }
                else -> setting.maxValue.toString()
            }
            
            Text(
                text = maxLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = customFont,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Toggle/switch control for boolean settings.
 * Displays clear on/off states with descriptive labels.
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = setting.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = customFont,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = customFont,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
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
        
        // State label
        Text(
            text = if (currentValue) setting.enabledLabel else setting.disabledLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = customFont,
                fontSize = 11.sp
            ),
            color = if (currentValue) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
    }
}

/**
 * Dropdown control for multiple choice settings.
 * Displays expandable menu with current selection.
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
            .padding(vertical = 4.dp)
    ) {
        // Setting name
        Text(
            text = setting.displayName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = customFont,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Description
        Text(
            text = setting.description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = customFont,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
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
                    .padding(horizontal = 8.dp),
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
                            fontFamily = customFont,
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
                            Column {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = customFont,
                                        fontSize = 14.sp
                                    ),
                                    color = if (option.value == currentValue)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                
                                option.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = customFont,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
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