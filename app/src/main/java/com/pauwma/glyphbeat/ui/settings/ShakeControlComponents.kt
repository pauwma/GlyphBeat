package com.pauwma.glyphbeat.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.data.ShakeControlSettings
import kotlin.math.roundToInt

/**
 * Reusable settings section with expand/collapse functionality
 */
@Composable
fun SettingsSection(
    title: String,
    description: String? = null,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and expand icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = customFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    description?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Animated expand/collapse icon
                val rotationAngle by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "expandIcon"
                )
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Animated content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Timeout slider with special "Disabled" handling
 */
@Composable
fun TimeoutSlider(
    label: String,
    description: String,
    currentValue: Long,
    onValueChange: (Long) -> Unit,
    minLabel: String? = null,
    offLabel: String = "Disabled",
    useAlternateColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val timeoutOptions = ShakeControlSettings.TIMEOUT_OPTIONS
    val timeoutOptionsShort = ShakeControlSettings.TIMEOUT_OPTIONS_SHORT
    val currentIndex = timeoutOptions.indexOfFirst { it.first == currentValue }.let {
        if (it == -1) 0 else it
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header with label and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (currentValue == 0L) offLabel else timeoutOptions[currentIndex].second,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont,
                    fontSize = 15.sp
                ),
                color = if (currentValue == 0L) 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else 
                    MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider with min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = minLabel ?: timeoutOptions.first().second,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Start
            )
            
            Slider(
                value = currentIndex.toFloat(),
                onValueChange = { value ->
                    val newIndex = value.roundToInt().coerceIn(0, timeoutOptions.size - 1)
                    onValueChange(timeoutOptions[newIndex].first)
                },
                valueRange = 0f..(timeoutOptions.size - 1).toFloat(),
                steps = timeoutOptions.size - 2,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = if (currentValue == 0L) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (currentValue == 0L) 
                        if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant
                    else 
                        MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    inactiveTickColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Text(
                text = timeoutOptions.last().second,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Short timeout slider for Skip Delay with shorter time options
 */
@Composable
fun ShortTimeoutSlider(
    label: String,
    description: String,
    currentValue: Long,
    onValueChange: (Long) -> Unit,
    minLabel: String? = null,
    offLabel: String = "Disabled",
    useAlternateColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val timeoutOptions = ShakeControlSettings.TIMEOUT_OPTIONS_SHORT
    val currentIndex = timeoutOptions.indexOfFirst { it.first == currentValue }.let {
        if (it == -1) 0 else it
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header with label and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (currentValue == 0L) offLabel else timeoutOptions[currentIndex].second,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont,
                    fontSize = 15.sp
                ),
                color = if (currentValue == 0L) 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else 
                    MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider with min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = minLabel ?: timeoutOptions.first().second,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Start
            )
            
            Slider(
                value = currentIndex.toFloat(),
                onValueChange = { value ->
                    val newIndex = value.roundToInt().coerceIn(0, timeoutOptions.size - 1)
                    onValueChange(timeoutOptions[newIndex].first)
                },
                valueRange = 0f..(timeoutOptions.size - 1).toFloat(),
                steps = timeoutOptions.size - 2,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = if (currentValue == 0L) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (currentValue == 0L) 
                        if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant
                    else 
                        MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    inactiveTickColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Text(
                text = timeoutOptions.last().second,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Battery awareness settings with toggle and threshold slider
 */
@Composable
fun BatteryAwarenessSettings(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    threshold: Int,
    onThresholdChange: (Int) -> Unit,
    useAlternateColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Toggle switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Battery Awareness",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = customFont,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Disable auto-start below battery threshold",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
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
                        if (enabled) MaterialTheme.colorScheme.primary
                        else if (useAlternateColors) Color(0xFF1A1A1A)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onEnabledChange(!enabled) }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }
        }
        
        // Threshold slider (only visible when enabled)
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Battery Threshold",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = customFont,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "${threshold}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = customFont,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.width(24.dp)
                    )
                    
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { value ->
                            onThresholdChange(value.roundToInt())
                        },
                        valueRange = 5f..25f,
                        steps = 3, // 5%, 10%, 15%, 20%, 25%
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant,
                            activeTickColor = if (useAlternateColors) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            inactiveTickColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Text(
                        text = "25%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Enhanced toggle setting with custom styling
 */
@Composable
fun EnhancedToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabledLabel: String = "Enabled",
    disabledLabel: String = "Disabled",
    useAlternateColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = customFont,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
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
                        if (checked) MaterialTheme.colorScheme.primary
                        else if (useAlternateColors) Color(0xFF1A1A1A)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onCheckedChange(!checked) }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }
        }
        
        // State label
        Text(
            text = if (checked) enabledLabel else disabledLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = customFont,
                fontSize = 11.sp
            ),
            color = if (checked) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        )
    }
}