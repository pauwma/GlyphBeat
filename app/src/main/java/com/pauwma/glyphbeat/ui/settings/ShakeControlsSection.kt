package com.pauwma.glyphbeat.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pauwma.glyphbeat.data.BehaviorSettings
import com.pauwma.glyphbeat.data.ShakeBehavior
import com.pauwma.glyphbeat.data.ShakeControlSettings
import com.pauwma.glyphbeat.data.getSkipSettings
import com.pauwma.glyphbeat.data.getPlayPauseSettings
import com.pauwma.glyphbeat.data.getAutoStartSettings
import com.pauwma.glyphbeat.services.shake.ShakeDetector
import kotlin.math.roundToInt

@Composable
fun ShakeControlsSection(
    settings: ShakeControlSettings,
    onSettingsChange: (ShakeControlSettings) -> Unit,
    autoStartEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    var mainExpanded by remember { mutableStateOf(false) }
    var generalExpanded by rememberSaveable { mutableStateOf(true) }
    var behaviorExpanded by rememberSaveable { mutableStateOf(true) }
    
    // Store settings for each behavior type to preserve user customizations
    var skipSettings by remember { mutableStateOf(BehaviorSettings.SkipSettings()) }
    var playPauseSettings by remember { mutableStateOf(BehaviorSettings.PlayPauseSettings()) }
    var autoStartSettings by remember { mutableStateOf(BehaviorSettings.AutoStartSettings()) }
    
    // Update behavior settings when they change to preserve user customizations
    LaunchedEffect(settings.behaviorSettings) {
        when (settings.behaviorSettings) {
            is BehaviorSettings.SkipSettings -> skipSettings = settings.behaviorSettings
            is BehaviorSettings.PlayPauseSettings -> playPauseSettings = settings.behaviorSettings
            is BehaviorSettings.AutoStartSettings -> autoStartSettings = settings.behaviorSettings
        }
    }
    
    
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Main header with master toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { mainExpanded = !mainExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Shake Controls",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = customFont
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Configure different shake gesture controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                // Animated expand/collapse icon
                val rotationAngle by animateFloatAsState(
                    targetValue = if (mainExpanded) 180f else 0f,
                    label = "expandIcon"
                )
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (mainExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Master enable/disable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shake Controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Custom toggle switch
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (settings.enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { 
                            val newEnabled = !settings.enabled
                            onSettingsChange(settings.copy(enabled = newEnabled))
                            // Auto expand/collapse when user manually toggles the switch
                            if (newEnabled) {
                                mainExpanded = true
                            } else {
                                mainExpanded = false
                            }
                        }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .align(if (settings.enabled) Alignment.CenterEnd else Alignment.CenterStart)
                    )
                }
            }
            
            // Detailed settings (visible when expanded, regardless of enabled state)
            AnimatedVisibility(
                visible = mainExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // General settings section
                    SettingsSection(
                        title = "General Settings",
                        description = "Universal settings for all shake behaviors",
                        expanded = generalExpanded,
                        onExpandedChange = { generalExpanded = it }
                    ) {
                        GeneralShakeSettings(
                            sensitivity = settings.sensitivity,
                            onSensitivityChange = { newSensitivity ->
                                onSettingsChange(settings.copy(sensitivity = newSensitivity))
                            },
                            hapticFeedback = settings.hapticFeedback,
                            onHapticFeedbackChange = { newHaptic ->
                                onSettingsChange(settings.copy(hapticFeedback = newHaptic))
                            },
                            enabled = settings.enabled
                        )
                    }

                    // Behavior selection dropdown
                    BehaviorSelectionDropdown(
                        selectedBehavior = settings.behavior,
                        onBehaviorChange = { newBehavior ->
                            val behaviorSettings = when (newBehavior) {
                                ShakeBehavior.SKIP -> skipSettings
                                ShakeBehavior.PLAY_PAUSE -> playPauseSettings
                                ShakeBehavior.AUTO_START -> autoStartSettings
                            }
                            
                            val newSettings = settings.copy(
                                behavior = newBehavior,
                                behaviorSettings = behaviorSettings
                            )
                            onSettingsChange(newSettings)
                        },
                        enabled = settings.enabled,
                        autoStartEnabled = autoStartEnabled
                    )
                    
                    // Behavior-specific settings section
                    SettingsSection(
                        title = "${settings.behavior.displayName} Settings",
                        description = settings.behavior.description,
                        expanded = behaviorExpanded,
                        onExpandedChange = { behaviorExpanded = it }
                    ) {
                        when (settings.behavior) {
                            ShakeBehavior.SKIP -> {
                                val skipSettings = settings.getSkipSettings() ?: BehaviorSettings.SkipSettings()
                                SkipBehaviorSettings(
                                    settings = skipSettings,
                                    onSettingsChange = { newSkipSettings ->
                                        onSettingsChange(settings.copy(behaviorSettings = newSkipSettings))
                                    },
                                    enabled = settings.enabled
                                )
                            }
                            ShakeBehavior.PLAY_PAUSE -> {
                                val playPauseSettings = settings.getPlayPauseSettings() ?: BehaviorSettings.PlayPauseSettings()
                                PlayPauseBehaviorSettings(
                                    settings = playPauseSettings,
                                    onSettingsChange = { newPlayPauseSettings ->
                                        onSettingsChange(settings.copy(behaviorSettings = newPlayPauseSettings))
                                    },
                                    enabled = settings.enabled
                                )
                            }
                            ShakeBehavior.AUTO_START -> {
                                val autoStartSettings = settings.getAutoStartSettings() ?: BehaviorSettings.AutoStartSettings()
                                AutoStartBehaviorSettings(
                                    settings = autoStartSettings,
                                    onSettingsChange = { newAutoStartSettings ->
                                        onSettingsChange(settings.copy(behaviorSettings = newAutoStartSettings))
                                    },
                                    enabled = settings.enabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BehaviorSelectionDropdown(
    selectedBehavior: ShakeBehavior,
    onBehaviorChange: (ShakeBehavior) -> Unit,
    enabled: Boolean = true,
    autoStartEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val behaviors = ShakeBehavior.entries.toTypedArray()
    
    val dropdownOptions = behaviors.mapNotNull { behavior ->
        // Filter out AUTO_START when auto-start is disabled, unless it's currently selected
        if (behavior == ShakeBehavior.AUTO_START && !autoStartEnabled && selectedBehavior != ShakeBehavior.AUTO_START) {
            null
        } else {
            val label = if (behavior == ShakeBehavior.AUTO_START && !autoStartEnabled) {
                "${behavior.displayName} (Auto-Start disabled)"
            } else {
                behavior.displayName
            }
            
            DropdownOption(
                value = behavior.id,
                label = label,
                description = behavior.description
            )
        }
    }
    
    val dropdownSetting = DropdownSetting(
        id = "shake_behavior",
        displayName = "Shake Behavior",
        description = "Choose what action to perform when device is shaken",
        defaultValue = selectedBehavior.id,
        options = dropdownOptions
    )
    
    SettingsDropdown(
        setting = dropdownSetting,
        currentValue = selectedBehavior.id,
        onValueChange = { newValue ->
            behaviors.find { it.id == newValue }?.let { newBehavior ->
                // Prevent selection of AUTO_START when auto-start is disabled
                if (newBehavior == ShakeBehavior.AUTO_START && !autoStartEnabled) {
                    // Do nothing - ignore the selection
                    return@let
                }
                onBehaviorChange(newBehavior)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun GeneralShakeSettings(
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    hapticFeedback: Boolean,
    onHapticFeedbackChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sensitivity slider
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shake Sensitivity",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = customFont,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when (sensitivity) {
                    ShakeDetector.SENSITIVITY_HIGH -> "High - Gentle shake required"
                    ShakeDetector.SENSITIVITY_MEDIUM -> "Medium - Moderate shake required"
                    else -> "Low - Strong shake required"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Low",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Start
                )
                
                Slider(
                    value = when (sensitivity) {
                        ShakeDetector.SENSITIVITY_HIGH -> 2f
                        ShakeDetector.SENSITIVITY_MEDIUM -> 1f
                        else -> 0f
                    },
                    onValueChange = { value ->
                        val newSensitivity = when {
                            value < 0.5f -> ShakeDetector.SENSITIVITY_LOW
                            value < 1.5f -> ShakeDetector.SENSITIVITY_MEDIUM
                            else -> ShakeDetector.SENSITIVITY_HIGH
                        }
                        onSensitivityChange(newSensitivity)
                    },
                    valueRange = 0f..2f,
                    steps = 1,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0xFF1A1A1A),
                        activeTickColor = Color(0xFF1A1A1A),
                        inactiveTickColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Text(
                    text = "High",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
        }
        
        // Haptic feedback toggle
        EnhancedToggleSetting(
            title = "Haptic Feedback",
            description = "Vibrate when shake gesture is detected",
            checked = hapticFeedback,
            onCheckedChange = onHapticFeedbackChange,
            enabledLabel = "Enabled",
            disabledLabel = "Disabled",
            useAlternateColors = true
        )
    }
}

@Composable
private fun SkipBehaviorSettings(
    settings: BehaviorSettings.SkipSettings,
    onSettingsChange: (BehaviorSettings.SkipSettings) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skip delay slider
        ShortTimeoutSlider(
            label = "Skip Delay",
            description = "Delay between shake detections to prevent accidental skips",
            currentValue = settings.skipDelay,
            onValueChange = { newDelay ->
                onSettingsChange(settings.copy(skipDelay = newDelay))
            },
            minLabel = "Off",
            useAlternateColors = true
        )
        
        // Skip when paused toggle
        EnhancedToggleSetting(
            title = "Skip When Paused",
            description = "Allow skipping to next track even when media is paused",
            checked = settings.skipWhenPaused,
            onCheckedChange = { newValue ->
                onSettingsChange(settings.copy(skipWhenPaused = newValue))
            },
            useAlternateColors = true
        )
        
        // Skip when unlocked toggle
        EnhancedToggleSetting(
            title = "Skip When Unlocked",
            description = "Allow skipping when device screen is unlocked and active",
            checked = settings.skipWhenUnlocked,
            onCheckedChange = { newValue ->
                onSettingsChange(settings.copy(skipWhenUnlocked = newValue))
            },
            useAlternateColors = true
        )
    }
}

@Composable
private fun PlayPauseBehaviorSettings(
    settings: BehaviorSettings.PlayPauseSettings,
    onSettingsChange: (BehaviorSettings.PlayPauseSettings) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lock screen behavior toggle
        EnhancedToggleSetting(
            title = "Lock Screen Behavior",
            description = "Allow play/pause when device is locked",
            checked = settings.lockScreenBehavior,
            onCheckedChange = { newValue ->
                onSettingsChange(settings.copy(lockScreenBehavior = newValue))
            },
            enabledLabel = "Enabled",
            disabledLabel = "Disabled",
            useAlternateColors = true
        )
        
        // Auto-resume delay slider
        TimeoutSlider(
            label = "Auto-Resume Delay",
            description = "Automatically resume playback after pausing via shake",
            currentValue = settings.autoResumeDelay,
            onValueChange = { newDelay ->
                onSettingsChange(settings.copy(autoResumeDelay = newDelay))
            },
            minLabel = "Off",
            useAlternateColors = true
        )
    }
}

@Composable
private fun AutoStartBehaviorSettings(
    settings: BehaviorSettings.AutoStartSettings,
    onSettingsChange: (BehaviorSettings.AutoStartSettings) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeout slider
        TimeoutSlider(
            label = "Re-enable Delay",
            description = "Pause duration before re-enabling auto-start service",
            currentValue = settings.timeout,
            onValueChange = { newTimeout ->
                onSettingsChange(settings.copy(timeout = newTimeout))
            },
            minLabel = "Off",
            useAlternateColors = true
        )
    }
}