package app.lockon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.lockon.model.CardColor

private val Amber = Color(0xFFFFB347)
private val Navy = Color(0xFF0B1220)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2A1700),
    primaryContainer = Color(0xFF4A3000),
    onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFF8FB8FF),
    onSecondary = Color(0xFF0A2350),
    secondaryContainer = Color(0xFF1C2D4D),
    onSecondaryContainer = Color(0xFFD8E2FF),
    background = Navy,
    onBackground = Color(0xFFE3E7F0),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE3E7F0),
    surfaceVariant = Color(0xFF1B2638),
    onSurfaceVariant = Color(0xFFB8C2D4),
    surfaceContainer = Color(0xFF152033),
    surfaceContainerHigh = Color(0xFF1B2638),
    outline = Color(0xFF3A4860),
    outlineVariant = Color(0xFF26324A),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB5),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF2F5BA8),
    secondaryContainer = Color(0xFFD8E2FF),
    onSecondaryContainer = Color(0xFF001A43),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6E9F2),
    onSurfaceVariant = Color(0xFF44495A),
    surfaceContainer = Color(0xFFEEF0F6),
    surfaceContainerHigh = Color(0xFFE6E9F2),
    outline = Color(0xFF8A90A2),
    outlineVariant = Color(0xFFD0D4E0),
)

@Composable
fun LockOnTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

/** Accent for a card color, readable on both themes as a badge background with [onCardColor] text. */
fun cardColor(color: CardColor?): Color = when (color) {
    CardColor.BLUE -> Color(0xFF3B7BE8)
    CardColor.GREEN -> Color(0xFF2E9E5B)
    CardColor.RED -> Color(0xFFD9443B)
    CardColor.WHITE -> Color(0xFFD9DDE6)
    CardColor.PURPLE -> Color(0xFF8E5BD6)
    null -> Color(0xFF7A8296)
}

fun onCardColor(color: CardColor?): Color = if (color == CardColor.WHITE) Color(0xFF1A1F2B) else Color.White
