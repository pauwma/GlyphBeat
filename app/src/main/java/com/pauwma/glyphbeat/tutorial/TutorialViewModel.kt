package com.pauwma.glyphbeat.tutorial

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauwma.glyphbeat.isNotificationAccessGranted
import com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.os.Build

/**
 * ViewModel for managing tutorial state and navigation.
 */
class TutorialViewModel : ViewModel() {
    
    private val _tutorialState = MutableStateFlow(TutorialState())
    val tutorialState: StateFlow<TutorialState> = _tutorialState.asStateFlow()
    
    fun nextPage() {
        val currentState = _tutorialState.value
        if (currentState.currentPage < currentState.totalPages - 1) {
            _tutorialState.value = currentState.copy(
                currentPage = currentState.currentPage + 1,
                completedPages = currentState.completedPages + currentState.currentPage
            )
        }
    }
    
    fun previousPage() {
        val currentState = _tutorialState.value
        if (currentState.currentPage > 0) {
            _tutorialState.value = currentState.copy(
                currentPage = currentState.currentPage - 1
            )
        }
    }
    
    fun canGoBack(): Boolean = _tutorialState.value.currentPage > 0
    
    fun checkAllPermissions(context: Context) {
        viewModelScope.launch {
            val permissions = mutableMapOf<String, Boolean>()
            
            // Get device information
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val isNothing = isNothingPhone()
            val hasGlyph = hasGlyphMatrix()
            
            // Check notification access
            permissions[PERMISSION_NOTIFICATION] = isNotificationAccessGranted(context)
            
            // Check Glyph Matrix permission (only granted for Nothing phones with Glyph Matrix)
            permissions[PERMISSION_GLYPH] = hasGlyph
            
            _tutorialState.value = _tutorialState.value.copy(
                permissionsGranted = permissions,
                deviceManufacturer = manufacturer,
                deviceModel = model,
                isNothingDevice = hasGlyph  // Only true if it has Glyph Matrix
            )
        }
    }
    
    fun requestPermission(context: Context, permission: String) {
        when (permission) {
            PERMISSION_NOTIFICATION -> {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
            // Add other permission requests as needed
        }
    }
    
    fun updatePermissionStatus(permission: String, granted: Boolean) {
        val currentPermissions = _tutorialState.value.permissionsGranted.toMutableMap()
        currentPermissions[permission] = granted
        
        _tutorialState.value = _tutorialState.value.copy(
            permissionsGranted = currentPermissions
        )
    }
    
    fun markTutorialCompleted(context: Context) {
        TutorialPreferences.setTutorialCompleted(context, true)
    }
    
    fun skipPermissions(context: Context) {
        TutorialPreferences.setSkippedPermissions(context, true)
        markTutorialCompleted(context)
    }
    
    private fun isNothingPhone(): Boolean {
        return Build.MANUFACTURER.equals("Nothing", ignoreCase = true)
    }
    
    private fun hasGlyphMatrix(): Boolean {
        // Only Nothing Phone (1), Phone (2), and Phone (3) have the full Glyph Matrix
        // Phone (2a), (2a) Plus, CMF Phone 1 only have basic LED strips
        if (!isNothingPhone()) return false
        
        val model = Build.MODEL.uppercase()
        return when {
            model.contains("A063") -> false  // Nothing Phone (1)
            model.contains("A065") -> false  // Nothing Phone (2)
            model.contains("A024") -> true  // Nothing Phone (3)
            model.contains("A142") -> false // Nothing Phone (2a)
            model.contains("A059") -> false // Nothing Phone (2a) Plus
            else -> false // Unknown model or CMF phones
        }
    }
    
    private fun getDeviceInfo(): Pair<String, String> {
        return Build.MANUFACTURER to Build.MODEL
    }
    
    companion object {
        const val PERMISSION_NOTIFICATION = "notification_access"
        const val PERMISSION_GLYPH = "glyph_matrix"
    }
}

/**
 * Data class representing the tutorial state.
 */
data class TutorialState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val isFirstLaunch: Boolean = true,
    val completedPages: Set<Int> = emptySet(),
    val permissionsGranted: Map<String, Boolean> = emptyMap(),
    val deviceManufacturer: String = "",
    val deviceModel: String = "",
    val isNothingDevice: Boolean = false
)