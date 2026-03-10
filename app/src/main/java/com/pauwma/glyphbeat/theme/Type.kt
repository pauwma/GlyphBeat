package com.pauwma.glyphbeat.theme

import androidx.compose.material3.Typography
import com.pauwma.glyphbeat.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GeistMonoFont = FontFamily(
    Font(R.font.geistmonoregular, FontWeight.Normal),
    Font(R.font.geistmonomedium, FontWeight.Medium),
    Font(R.font.geistmonosemibold, FontWeight.SemiBold),
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)