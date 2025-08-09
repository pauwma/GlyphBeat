package com.pauwma.glyphbeat.tutorial.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.theme.NothingRed
import kotlinx.coroutines.delay

/**
 * Theme guide page explaining the theme system and customization options.
 */
@Composable
fun ThemeGuidePage(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }
    val customFont = FontFamily(Font(R.font.ntype82regular))
    
    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Title
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Theme System",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Customize your Glyph Matrix animations",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Theme Categories - Horizontal Layout
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 300))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Theme Categories",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCardWithPreview(
                        title = "Media Player",
                        description = "Different animations",
                        previewResIds = listOf(
                            R.drawable.vinyl_preview,
                            R.drawable.duck_preview,
                            R.drawable.cover_art_preview,
                            R.drawable.minimal_preview
                        ),
                        modifier = Modifier.weight(1f),
                        customFont = customFont
                    )
                    
                    CategoryCardWithPreview(
                        title = "Track Control",
                        description = "Playback feedback",
                        previewResIds = listOf(
                            R.drawable.minimal_arrow_preview,
                            R.drawable.minimal_arrow_preview
                        ),
                        modifier = Modifier.weight(1f),
                        customFont = customFont
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Customization Options - Horizontal Layout
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Customization",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedCustomizationItem(
                        icon = Icons.Default.LightMode,
                        title = "Brightness",
                        description = "Adjust intensity",
                        customFont = customFont,
                        modifier = Modifier.weight(1f)
                    )
                    
                    EnhancedCustomizationItem(
                        icon = Icons.Default.Speed,
                        title = "Speed",
                        description = "Control tempo",
                        customFont = customFont,
                        modifier = Modifier.weight(1f)
                    )
                    
                    EnhancedCustomizationItem(
                        icon = Icons.Default.Tune,
                        title = "Settings",
                        description = "And much more",
                        customFont = customFont,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation Buttons
        AnimatedVisibility(
            visible = animationStarted,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 900))
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
                    Text("Back")
                    Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on left
                }
                
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingRed
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp)) // Balance padding for icon on right
                    Text("Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryCardWithPreview(
    title: String,
    description: String,
    previewResIds: List<Int>,
    modifier: Modifier = Modifier,
    customFont: FontFamily = FontFamily.Default
) {
    // State for carousel
    var currentImageIndex by remember { mutableStateOf(0) }
    
    // Cache the vector resources to avoid repeated loading
    val cachedImages = remember(previewResIds) {
        previewResIds.map { id ->
            // This will be loaded in the Crossfade composable
            id
        }
    }
    
    // Auto-advance carousel after initial delay to prevent lag
    LaunchedEffect(key1 = cachedImages) {
        if (cachedImages.size > 1) {
            delay(500) // Wait 0.5 seconds before starting carousel
            while (true) {
                delay(1750) // Increased interval to reduce CPU usage
                currentImageIndex = (currentImageIndex + 1) % cachedImages.size
            }
        }
    }
    
    // Animate the transition
    val imageAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing
        ),
        label = "imageAlpha"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Preview Image Section with Carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Crossfade between images
                Crossfade(
                    targetState = currentImageIndex,
                    animationSpec = tween(durationMillis = 500),
                    label = "imageCrossfade"
                ) { index ->
                    Image(
                        imageVector = ImageVector.vectorResource(id = cachedImages[index]),
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                    )
                }
            }
            
            // Text Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EnhancedCustomizationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    customFont: FontFamily = FontFamily.Default
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NothingRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = customFont,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}