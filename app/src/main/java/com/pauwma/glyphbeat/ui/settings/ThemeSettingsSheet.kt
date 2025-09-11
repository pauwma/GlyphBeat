package com.pauwma.glyphbeat.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.services.trackcontrol.TrackControlThemeManager
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.data.ThemeRepository
import kotlinx.coroutines.launch

/**
 * Bottom sheet that displays theme settings for customization.
 * Includes grouped settings, reset functionality, and real-time updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    theme: AnimationTheme,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeRepository = remember { ThemeRepository.getInstance(context) }
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val scope = rememberCoroutineScope()

    // Get current theme settings
    var themeSettings by remember(theme) { mutableStateOf<ThemeSettings?>(null) }
    var isLoading by remember(theme) { mutableStateOf(true) }
    var error by remember(theme) { mutableStateOf<String?>(null) }

    // Load settings when theme changes - moved to background thread
    LaunchedEffect(theme) {
        isLoading = true
        error = null
        try {
            // Move to background thread to prevent ANR
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Check if theme supports settings first
                val settings = if (theme is ThemeSettingsProvider) {
                    themeRepository.getThemeSettings(theme.getSettingsId())
                } else {
                    null
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    themeSettings = settings
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            error = "Failed to load settings: ${e.message}"
            isLoading = false
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = theme.getThemeName(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = customFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = theme.getDescription(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = customFont,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Row {
                        // Reset button
                        IconButton(
                            onClick = {
                                themeSettings?.let { settings ->
                                    scope.launch {
                                        try {
                                            val resetSettings = settings.withAllValuesReset()
                                            val themeId = if (theme is ThemeSettingsProvider) theme.getSettingsId() else theme.getThemeName()
                                            themeRepository.saveThemeSettings(themeId, resetSettings)
                                            themeSettings = resetSettings
                                            onSettingsChanged()
                                        } catch (e: Exception) {
                                            error = "Failed to reset: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = themeSettings?.userValues?.isNotEmpty() == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset to defaults",
                                tint = if (themeSettings?.userValues?.isNotEmpty() == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    error != null -> {
                        ErrorMessage(
                            message = error!!,
                            onRetry = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    try {
                                        val themeId = if (theme is ThemeSettingsProvider) theme.getSettingsId() else theme.getThemeName()
                                        themeSettings = themeRepository.getThemeSettings(themeId)
                                        isLoading = false
                                    } catch (e: Exception) {
                                        error = "Failed to load settings: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }

                    themeSettings != null -> {
                        ThemeSettingsContent(
                            themeSettings = themeSettings!!,
                            onSettingChanged = { settingId, value ->
                                scope.launch {
                                    try {
                                        val updatedSettings = themeSettings!!.withUpdatedValue(settingId, value)
                                        val themeId = if (theme is ThemeSettingsProvider) theme.getSettingsId() else theme.getThemeName()
                                        themeRepository.saveThemeSettings(themeId, updatedSettings)
                                        themeSettings = updatedSettings
                                        onSettingsChanged()
                                    } catch (e: Exception) {
                                        error = "Failed to save: ${e.message}"
                                    }
                                }
                            }
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No settings available",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = customFont
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet that displays theme settings for track control themes.
 * Uses TrackControlThemeManager instead of ThemeRepository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    theme: TrackControlTheme,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeManager = remember { TrackControlThemeManager.getInstance(context) }
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val scope = rememberCoroutineScope()

    // Get current theme settings
    var themeSettings by remember(theme) { mutableStateOf<ThemeSettings?>(null) }
    var isLoading by remember(theme) { mutableStateOf(true) }
    var error by remember(theme) { mutableStateOf<String?>(null) }

    // Load settings when theme changes - moved to background thread
    LaunchedEffect(theme) {
        isLoading = true
        error = null
        try {
            // Move to background thread to prevent ANR
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Check if theme supports settings first
                val settings = if (theme is TrackControlThemeSettingsProvider) {
                    themeManager.getCurrentThemeSettings()
                } else {
                    null
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    themeSettings = settings
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            error = "Failed to load settings: ${e.message}"
            isLoading = false
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = theme.getThemeName(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = customFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = theme.getDescription(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = customFont,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Row {
                        // Reset button
                        IconButton(
                            onClick = {
                                themeSettings?.let { settings ->
                                    scope.launch {
                                        try {
                                            val resetSettings = settings.withAllValuesReset()
                                            themeManager.updateCurrentThemeSettings(resetSettings)
                                            themeSettings = resetSettings
                                            onSettingsChanged()
                                        } catch (e: Exception) {
                                            error = "Failed to reset: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = themeSettings?.userValues?.isNotEmpty() == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset to defaults",
                                tint = if (themeSettings?.userValues?.isNotEmpty() == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    error != null -> {
                        ErrorMessage(
                            message = error!!,
                            onRetry = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    try {
                                        themeSettings = themeManager.getCurrentThemeSettings()
                                        isLoading = false
                                    } catch (e: Exception) {
                                        error = "Failed to load settings: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }

                    themeSettings != null -> {
                        ThemeSettingsContent(
                            themeSettings = themeSettings!!,
                            onSettingChanged = { settingId, value ->
                                scope.launch {
                                    try {
                                        val updatedSettings = themeSettings!!.withUpdatedValue(settingId, value)
                                        themeManager.updateCurrentThemeSettings(updatedSettings)
                                        themeSettings = updatedSettings
                                        onSettingsChanged()
                                    } catch (e: Exception) {
                                        error = "Failed to save: ${e.message}"
                                    }
                                }
                            }
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No settings available",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = customFont
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Content section that displays the actual settings grouped by category.
 */
@Composable
private fun ThemeSettingsContent(
    themeSettings: ThemeSettings,
    onSettingChanged: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customFont = FontFamily(Font(R.font.ntype82regular))

    // Group settings by category
    val settingsByCategory = themeSettings.settings.values.groupBy { it.category }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        settingsByCategory.forEach { (category, settings) ->
            // Category header
            item {
                Text(
                    text = SettingCategories.getLocalizedCategory(context, category),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Settings in this category
            items(settings) { setting ->
                val isFirstInCategory = settings.indexOf(setting) == 0
                SettingItem(
                    setting = setting,
                    currentValue = themeSettings.getValue(setting.id),
                    hasCustomValue = themeSettings.hasCustomValue(setting.id),
                    onValueChanged = { value ->
                        onSettingChanged(setting.id, value)
                    },
                    isFirstInCategory = isFirstInCategory
                )
            }

            // Spacer between categories
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Individual setting item with appropriate control based on setting type.
 */
@Composable
private fun SettingItem(
    setting: ThemeSetting,
    currentValue: Any?,
    hasCustomValue: Boolean,
    onValueChanged: (Any) -> Unit,
    isFirstInCategory: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = null
    ) {
        Box(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = if (isFirstInCategory) 6.dp else 12.dp,
                bottom = 12.dp
            )
        ) {
            when (setting) {
                is SliderSetting -> {
                    SettingsSlider(
                        setting = setting,
                        currentValue = currentValue as? Number ?: setting.defaultValue,
                        onValueChange = onValueChanged
                    )
                }

                is ToggleSetting -> {
                    SettingsToggle(
                        setting = setting,
                        currentValue = currentValue as? Boolean ?: setting.defaultValue,
                        onValueChange = onValueChanged
                    )
                }

                is DropdownSetting -> {
                    SettingsDropdown(
                        setting = setting,
                        currentValue = currentValue as? String ?: setting.defaultValue,
                        onValueChange = onValueChanged
                    )
                }
            }
        }
    }
}

/**
 * Error message display with retry functionality.
 */
@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(Font(R.font.ntype82regular))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = customFont
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}