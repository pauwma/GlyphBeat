package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.ui.screens.ThemeSelectionScreen

/**
 * Screen for Media Player toy theme selection.
 * Wraps the existing ThemeSelectionScreen with context for media player themes.
 */
@Composable
fun MediaPlayerScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simply pass through to ThemeSelectionScreen without extra wrapper
    ThemeSelectionScreen(
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
    )
}