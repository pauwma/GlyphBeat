package com.pauwma.glyphbeat.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import com.pauwma.glyphbeat.tutorial.components.TutorialProgressIndicator
import com.pauwma.glyphbeat.tutorial.pages.*
import kotlinx.coroutines.launch

/**
 * Main tutorial screen that contains the pager and navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    state: TutorialState,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onSkipTutorial: () -> Unit,
    onCompleteTutorial: () -> Unit,
    onRequestPermission: (Context, String) -> Unit,
    onUpdatePermissionStatus: (String, Boolean) -> Unit,
    onSkipPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.totalPages }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pager with state
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    
    // Update state when pager changes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            when {
                pagerState.currentPage > state.currentPage -> onNextPage()
                pagerState.currentPage < state.currentPage -> onPreviousPage()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                    0 -> WelcomePage(
                        onGetStarted = onNextPage,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> ThemeGuidePage(
                        onContinue = onNextPage,
                        onBack = onPreviousPage,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> PermissionsPage(
                        permissionsGranted = state.permissionsGranted,
                        deviceManufacturer = state.deviceManufacturer,
                        deviceModel = state.deviceModel,
                        isNothingDevice = state.isNothingDevice,
                        onRequestPermission = { permission ->
                            onRequestPermission(context, permission)
                        },
                        onContinue = onNextPage,
                        onBack = onPreviousPage,
                        onSkip = {
                            // Skip permissions but continue to completion page
                            onSkipPermissions()
                            onNextPage()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    3 -> CompletionPage(
                        onStartApp = onCompleteTutorial,
                        modifier = Modifier.fillMaxSize()
                    )
                    }
                }
            }
            
            // Progress indicator at bottom
            TutorialProgressIndicator(
                currentPage = pagerState.currentPage,
                totalPages = state.totalPages,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 24.dp)
            )
        }
        
        // Skip button (show on first 3 pages)
        if (state.currentPage < state.totalPages - 1) {
            TextButton(
                onClick = onSkipTutorial,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}