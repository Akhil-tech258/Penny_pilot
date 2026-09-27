package com.privacyexpense.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TextBlack,
    onPrimary = BgWarmWhite,
    primaryContainer = AvatarPeach,
    onPrimaryContainer = TextBlack,
    secondary = AccentGold,
    onSecondary = TextBlack,
    background = BgWarmWhite,
    onBackground = TextBlack,
    surface = BgWarmWhite,
    onSurface = TextBlack,
    surfaceVariant = Color(0xFFF9FAFB),
    onSurfaceVariant = TextSecondary,
    outline = TableBorderBlack
)

@Composable
fun PrivacyExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
