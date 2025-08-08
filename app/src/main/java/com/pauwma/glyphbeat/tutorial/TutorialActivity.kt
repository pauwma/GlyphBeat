package com.pauwma.glyphbeat.tutorial

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pauwma.glyphbeat.MainActivity
import com.pauwma.glyphbeat.theme.NothingAndroidSDKDemoTheme
import com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences

/**
 * Tutorial activity that guides users through app setup on first launch.
 * Features interactive pages for app overview, theme explanation, and permission setup.
 */
class TutorialActivity : ComponentActivity() {
    
    private val viewModel: TutorialViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NothingAndroidSDKDemoTheme {
                val tutorialState by viewModel.tutorialState.collectAsState()
                
                TutorialScreen(
                    state = tutorialState,
                    onNextPage = viewModel::nextPage,
                    onPreviousPage = viewModel::previousPage,
                    onSkipTutorial = { finishTutorial(skipped = true) },
                    onCompleteTutorial = { finishTutorial(skipped = false) },
                    onRequestPermission = viewModel::requestPermission,
                    onUpdatePermissionStatus = viewModel::updatePermissionStatus,
                    onSkipPermissions = { 
                        TutorialPreferences.setSkippedPermissions(this, true)
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check permission status when returning from settings
        viewModel.checkAllPermissions(this)
    }
    
    private fun finishTutorial(skipped: Boolean) {
        // Mark tutorial as completed
        viewModel.markTutorialCompleted(this)
        
        // If user skipped, track that they skipped permissions
        if (skipped) {
            TutorialPreferences.setSkippedPermissions(this, true)
        }
        
        // Launch main activity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        when {
            viewModel.canGoBack() -> viewModel.previousPage()
            else -> {
                // Show confirmation dialog or just exit
                super.onBackPressed()
            }
        }
    }
}