package com.marishutka.chess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF2196F3),
    tertiary = Color(0xFFFF9800)
)
private val Light = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF2196F3),
    tertiary = Color(0xFFFF9800)
)

@Composable
fun MarishutkaChessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
