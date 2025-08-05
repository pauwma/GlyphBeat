package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pauwma.glyphbeat.ui.screens.SettingsScreen

/**
 * Settings screen wrapper for navigation.
 * Provides the settings screen within the bottom navigation context.
 */
@Composable
fun SettingsNavigationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Delegate to the existing SettingsScreen
    SettingsScreen(
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}