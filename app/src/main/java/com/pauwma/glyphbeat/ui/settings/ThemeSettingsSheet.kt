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
import com.pauwma.glyphbeat.themes.animation.CustomTheme
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.services.trackcontrol.TrackControlThemeManager
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.theme.NothingRed
import com.pauwma.glyphbeat.theme.GeistMonoFont
import androidx.compose.material.icons.outlined.DeleteOutline
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
    onDelete: (() -> Unit)? = null,
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
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .size(width = 32.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = theme.getDescription(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = GeistMonoFont,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Row {
                        // Delete button (imported themes only)
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Theme",
                                    tint = NothingRed
                                )
                            }
                        }

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

                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .size(width = 32.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = theme.getDescription(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = GeistMonoFont,
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

                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
 * Flat layout with dividers between categories instead of card wrapping.
 */
@Composable
private fun ThemeSettingsContent(
    themeSettings: ThemeSettings,
    onSettingChanged: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group settings by category
    val settingsByCategory = themeSettings.settings.values.groupBy { it.category }
    val categoryEntries = settingsByCategory.entries.toList()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        categoryEntries.forEachIndexed { categoryIndex, (_, settings) ->
            // Settings in this category
            items(settings, key = { it.id }) { setting ->
                SettingItem(
                    setting = setting,
                    currentValue = themeSettings.getValue(setting.id),
                    hasCustomValue = themeSettings.hasCustomValue(setting.id),
                    onValueChanged = { value ->
                        onSettingChanged(setting.id, value)
                    }
                )
            }

            // Divider between categories (not after the last one)
            if (categoryIndex < categoryEntries.size - 1) {
                item {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Individual setting item with appropriate control based on setting type.
 * Rendered directly without card wrapping.
 */
@Composable
private fun SettingItem(
    setting: ThemeSetting,
    currentValue: Any?,
    hasCustomValue: Boolean,
    onValueChanged: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use coerceValue() for type-safe conversion to prevent ClassCastException
    // when Gson deserializes userValues with different types (e.g., Int → Double)
    val safeValue = try {
        setting.coerceValue(currentValue)
    } catch (e: Exception) {
        setting.defaultValue
    }

    when (setting) {
        is SliderSetting -> {
            SettingsSlider(
                setting = setting,
                currentValue = safeValue as? Number ?: setting.defaultValue,
                onValueChange = onValueChanged,
                modifier = modifier.fillMaxWidth()
            )
        }

        is ToggleSetting -> {
            SettingsToggle(
                setting = setting,
                currentValue = safeValue as? Boolean ?: setting.defaultValue,
                onValueChange = onValueChanged,
                modifier = modifier.fillMaxWidth()
            )
        }

        is DropdownSetting -> {
            if (setting.options.size <= 4) {
                SettingsToggleButtons(
                    setting = setting,
                    currentValue = safeValue as? String ?: setting.defaultValue,
                    onValueChange = onValueChanged,
                    modifier = modifier.fillMaxWidth()
                )
            } else {
                SettingsDropdown(
                    setting = setting,
                    currentValue = safeValue as? String ?: setting.defaultValue,
                    onValueChange = onValueChanged,
                    modifier = modifier.fillMaxWidth()
                )
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