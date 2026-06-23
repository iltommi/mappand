package io.github.tommaso.mappand.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3b82f6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1d4ed8),
    background = Color(0xFF16213e),
    surface = Color(0xFF1e293b),
    onBackground = Color(0xFFe2e8f0),
    onSurface = Color(0xFFe2e8f0),
    secondary = Color(0xFF64748b),
    error = Color(0xFFef4444),
)

@Composable
fun MappandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
