package com.pauwma.glyphbeat.tutorial.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.core.AppConfig
import com.pauwma.glyphbeat.theme.NothingRed
import kotlinx.coroutines.delay

/**
 * Welcome page showing app overview and key features.
 */
@Composable
fun WelcomePage(
    isVisible: Boolean = true,
    onGetStarted: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App Logo
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(1000)) +
                    scaleIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.media_player_track_toy_icon),
                    contentDescription = "Glyph Beat Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(0.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Title
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 300))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.tutorial_welcome_title),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Version ${AppConfig.APP_VERSION}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 600))
        ) {
            Text(
                text = context.getString(R.string.tutorial_welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Features
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 900))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(
                    icon = Icons.Outlined.MusicNote,
                    title = context.getString(R.string.tutorial_welcome_feature_music_title),
                    description = context.getString(R.string.tutorial_welcome_feature_music_desc),
                    customFont = customFont
                )

                FeatureCard(
                    icon = Icons.Outlined.Palette,
                    title = context.getString(R.string.tutorial_welcome_feature_themes_title),
                    description = context.getString(R.string.tutorial_welcome_feature_themes_desc),
                    customFont = customFont
                )

                FeatureCard(
                    icon = Icons.Outlined.TouchApp,
                    title = context.getString(R.string.tutorial_welcome_feature_control_title),
                    description = context.getString(R.string.tutorial_welcome_feature_control_desc),
                    customFont = customFont
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Get Started Button
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 1200))
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingRed
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on right
                Text(
                    text = context.getString(R.string.tutorial_welcome_get_started),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    customFont: FontFamily,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NothingRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NothingRed,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = customFont
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}