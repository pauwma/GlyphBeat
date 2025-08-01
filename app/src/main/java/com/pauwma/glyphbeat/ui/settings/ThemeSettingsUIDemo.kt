package com.pauwma.glyphbeat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R

/**
 * Demo screen showcasing all the theme settings UI components.
 * Useful for testing and development of the settings interface.
 */
@Preview(showBackground = true)
@Composable
fun ThemeSettingsUIDemo() {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Theme Settings UI Components",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = customFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Slider Setting Demo
                item {
                    DemoSection(title = "Slider Settings") {
                        var animationSpeed by remember { mutableStateOf(100) }
                        var brightness by remember { mutableStateOf(255) }
                        
                        SettingsSlider(
                            setting = SliderSetting(
                                id = "animation_speed",
                                displayName = "Animation Speed",
                                description = "Controls how fast the animation plays",
                                defaultValue = 100,
                                minValue = 50,
                                maxValue = 500,
                                stepSize = 10,
                                unit = "ms",
                                category = "Animation"
                            ),
                            currentValue = animationSpeed,
                            onValueChange = { animationSpeed = it.toInt() }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SettingsSlider(
                            setting = SliderSetting(
                                id = "brightness",
                                displayName = "Brightness",
                                description = "Adjust the brightness of the glyph display",
                                defaultValue = 255,
                                minValue = 50,
                                maxValue = 255,
                                stepSize = 5,
                                unit = null,
                                category = "Visual"
                            ),
                            currentValue = brightness,
                            onValueChange = { brightness = it.toInt() }
                        )
                    }
                }
                
                // Toggle Setting Demo
                item {
                    DemoSection(title = "Toggle Settings") {
                        var enableEffects by remember { mutableStateOf(true) }
                        var smoothTransitions by remember { mutableStateOf(false) }
                        
                        SettingsToggle(
                            setting = ToggleSetting(
                                id = "enable_effects",
                                displayName = "Enable Effects",
                                description = "Enable special visual effects for this theme",
                                defaultValue = true,
                                category = "Effects",
                                enabledLabel = "On",
                                disabledLabel = "Off"
                            ),
                            currentValue = enableEffects,
                            onValueChange = { enableEffects = it }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SettingsToggle(
                            setting = ToggleSetting(
                                id = "smooth_transitions",
                                displayName = "Smooth Transitions",
                                description = "Enable smooth transitions between animation frames",
                                defaultValue = false,
                                category = "Animation"
                            ),
                            currentValue = smoothTransitions,
                            onValueChange = { smoothTransitions = it }
                        )
                    }
                }
                
                // Dropdown Setting Demo
                item {
                    DemoSection(title = "Dropdown Settings") {
                        var pattern by remember { mutableStateOf("spiral") }
                        var colorScheme by remember { mutableStateOf("monochrome") }
                        
                        SettingsDropdown(
                            setting = DropdownSetting(
                                id = "pattern",
                                displayName = "Animation Pattern",
                                description = "Choose the base pattern for the animation",
                                defaultValue = "spiral",
                                category = "Animation",
                                options = listOf(
                                    DropdownOption("spiral", "Spiral", "Classic spiral pattern"),
                                    DropdownOption("wave", "Wave", "Flowing wave pattern"),
                                    DropdownOption("pulse", "Pulse", "Pulsing circle pattern"),
                                    DropdownOption("random", "Random", "Random pixel pattern")
                                )
                            ),
                            currentValue = pattern,
                            onValueChange = { pattern = it }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SettingsDropdown(
                            setting = DropdownSetting(
                                id = "color_scheme",
                                displayName = "Color Scheme",
                                description = "Color scheme for the animation",
                                defaultValue = "monochrome",
                                category = "Visual",
                                options = listOf(
                                    DropdownOption("monochrome", "Monochrome", "Classic white-on-black"),
                                    DropdownOption("warm", "Warm", "Warm color tones"),
                                    DropdownOption("cool", "Cool", "Cool color tones"),
                                    DropdownOption("rainbow", "Rainbow", "Full spectrum colors")
                                )
                            ),
                            currentValue = colorScheme,
                            onValueChange = { colorScheme = it }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Demo section wrapper component.
 */
@Composable
private fun DemoSection(
    title: String,
    content: @Composable () -> Unit
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            content()
        }
    }
}