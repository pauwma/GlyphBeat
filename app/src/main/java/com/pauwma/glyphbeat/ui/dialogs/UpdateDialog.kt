package com.pauwma.glyphbeat.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.data.UpdateContent
import com.pauwma.glyphbeat.theme.NothingMediumGrey
import com.pauwma.glyphbeat.theme.NothingRed
import kotlinx.coroutines.delay

/**
 * A beautiful update dialog that shows new features and changes.
 * Displays once per version on first launch after update.
 */
@Composable
fun UpdateDialog(
    updateContent: UpdateContent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("UpdateDialog", "UpdateDialog composable called with content: ${updateContent.title}")
    val customFont = FontFamily(Font(R.font.ntype82regular))

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Trigger animation on launch
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A1A))
                .border(
                    width = 1.dp,
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Close button in top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFB0B0B0),
                    modifier = Modifier.size(18.dp)
                )

            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Version Badge
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) +
                            slideInVertically(animationSpec = tween(300, delayMillis = 200))
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = NothingMediumGrey,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "VERSION ${updateContent.versionName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingMediumGrey,
                                    letterSpacing = 1.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 300))
                ) {
                    Text(
                        text = updateContent.title,
                        fontFamily = customFont,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.2.sp
                    )
                }

                // Subtitle
                updateContent.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 400))
                    ) {
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = Color(0xFFB0B0B0),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Features Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 500))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Highlight Features
                        updateContent.features.filter { it.isHighlight }.forEach { feature ->
                            FeatureCard(
                                feature = feature,
                                customFont = customFont,
                                isHighlight = true
                            )
                        }

                        // Regular Features
                        updateContent.features.filter { !it.isHighlight }.forEach { feature ->
                            FeatureCard(
                                feature = feature,
                                customFont = customFont,
                                isHighlight = false
                            )
                        }
                    }
                }

                // Improvements Section
                if (updateContent.improvements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 600))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Other",
                                fontFamily = customFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(0.dp))
                            updateContent.improvements.forEach { improvement ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontFamily = customFont,
                                        fontSize = 12.sp,
                                        color = Color(0xFF606060),
                                        lineHeight = 14.sp,
                                        modifier = Modifier.offset(y = (1).dp)
                                    )
                                    Text(
                                        text = improvement,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB0B0B0),
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Dismiss Button
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 700))
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .width(180.dp)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingRed
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).rotate(180F)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Love it!",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: UpdateContent.Feature,
    customFont: FontFamily,
    isHighlight: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlight) Color(0xFF2A2A2A) else Color(0xFF1F1F1F)
    val emojiColor = if (isHighlight) NothingRed else Color(0xFFE0E0E0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isHighlight) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0xFF3A3A3A),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            feature.emoji?.let { emoji ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 14.sp,
                        color = emojiColor,
                        fontFamily = FontFamily(Font(R.font.notoemoji))
                    )
                }
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = feature.title,
                    fontFamily = customFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = feature.description,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0),
                    lineHeight = 14.sp
                )
            }
        }
    }
}