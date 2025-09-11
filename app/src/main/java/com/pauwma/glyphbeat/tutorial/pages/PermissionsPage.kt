package com.pauwma.glyphbeat.tutorial.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.theme.NothingRed
import com.pauwma.glyphbeat.tutorial.TutorialViewModel
import com.pauwma.glyphbeat.tutorial.components.PermissionCard
import kotlinx.coroutines.delay

/**
 * Permissions page for setting up required app permissions.
 */
@Composable
fun PermissionsPage(
    isVisible: Boolean = true,
    permissionsGranted: Map<String, Boolean>,
    deviceManufacturer: String = "",
    deviceModel: String = "",
    isNothingDevice: Boolean = false,
    onRequestPermission: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var animationStarted by remember { mutableStateOf(false) }
    val customFont = FontFamily(Font(R.font.ntype82regular))

    val notificationGranted = permissionsGranted[TutorialViewModel.PERMISSION_NOTIFICATION] ?: false
    val glyphGranted = permissionsGranted[TutorialViewModel.PERMISSION_GLYPH] ?: false
    val allGranted = notificationGranted && (glyphGranted || !isNothingDevice)

    // Trigger animation when page becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            animationStarted = false
            delay(100)
            animationStarted = true
        } else {
            animationStarted = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Title
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (allGranted) Color.Green else NothingRed,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.getString(R.string.tutorial_permissions_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = context.getString(R.string.tutorial_permissions_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Status Summary
        if (allGranted) {
            AnimatedVisibility(
                visible = animationStarted,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(R.string.tutorial_permissions_all_granted),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Permission Cards
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notification Access Permission
                PermissionCard(
                    title = context.getString(R.string.tutorial_permissions_notification_title),
                    description = context.getString(R.string.tutorial_permissions_notification_desc),
                    icon = Icons.Default.Notifications,
                    isGranted = notificationGranted,
                    onRequestPermission = {
                        onRequestPermission(TutorialViewModel.PERMISSION_NOTIFICATION)
                    },
                    customFont = customFont
                )

                // Glyph Matrix Permission (only for specific Nothing phones)
                PermissionCard(
                    title = context.getString(R.string.tutorial_permissions_glyph_title),
                    description = when {
                        isNothingDevice -> context.getString(R.string.tutorial_permissions_glyph_desc_granted)
                        deviceManufacturer.equals("Nothing", ignoreCase = true) -> context.getString(R.string.tutorial_permissions_glyph_desc_unsupported, deviceModel)
                        else -> context.getString(R.string.tutorial_permissions_glyph_desc_unavailable)
                    },
                    icon = Icons.Default.GridOn,
                    isGranted = glyphGranted,
                    onRequestPermission = {
                        // This permission is device-specific
                    },
                    customFont = customFont,
                    isAvailable = isNothingDevice
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Section
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 900))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.tutorial_permissions_why_title),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = customFont
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = context.getString(R.string.tutorial_permissions_why_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation Buttons
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 1200))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.tutorial_permissions_back))
                    Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on left
                }

                Button(
                    onClick = if (allGranted) onContinue else onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allGranted) NothingRed else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (allGranted) {
                        Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on right
                        Text(context.getString(R.string.tutorial_permissions_continue))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on right
                        Text(
                            text = context.getString(R.string.tutorial_permissions_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}