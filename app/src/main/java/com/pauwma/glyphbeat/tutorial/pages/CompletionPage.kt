package com.pauwma.glyphbeat.tutorial.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.theme.NothingRed
import kotlinx.coroutines.delay

/**
 * Completion page showing success state and launch button.
 */
@Composable
fun CompletionPage(
    isVisible: Boolean = true,
    onStartApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var animationStarted by remember { mutableStateOf(false) }
    val customFont = FontFamily(Font(R.font.ntype82regular))

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

    // Success checkmark animation
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp), // Add extra top padding for safety
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Icon
        AnimatedVisibility(
            visible = animationStarted,
            enter = scaleIn(animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(NothingRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NothingRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Success Message
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 300))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.tutorial_completion_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = context.getString(R.string.tutorial_completion_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Feature Summary
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = context.getString(R.string.tutorial_completion_next_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = customFont
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FeatureItem(
                        icon = Icons.Default.Settings,
                        text = context.getString(R.string.tutorial_completion_step_glyph)
                    )

                    FeatureItem(
                        icon = Icons.Default.PlayArrow,
                        text = context.getString(R.string.tutorial_completion_step_music)
                    )

                    FeatureItem(
                        icon = Icons.Default.TouchApp,
                        text = context.getString(R.string.tutorial_completion_step_control)
                    )

                    FeatureItem(
                        icon = Icons.Default.Tune,
                        text = context.getString(R.string.tutorial_completion_step_customize)
                    )

                    FeatureItem(
                        icon = Icons.Default.Explore,
                        text = context.getString(R.string.tutorial_completion_step_explore)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Launch Button
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 900))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.tutorial_completion_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onStartApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingRed
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.tutorial_completion_start),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on left
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(NothingRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NothingRed,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}