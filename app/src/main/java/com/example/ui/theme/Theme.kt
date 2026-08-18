package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GothicColorScheme = darkColorScheme(
    primary = InkWhite,
    secondary = InkGrayLight,
    background = InkBlack,
    surface = InkGrayDark,
    onPrimary = InkBlack,
    onSecondary = InkWhite,
    onBackground = InkWhite,
    onSurface = InkWhite,
    error = InkRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GothicColorScheme,
        typography = Typography,
        content = content
    )
}

