package app.lockon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import app.lockon.model.CardColor
import app.lockon.resources.Res
import app.lockon.resources.quicksand
import org.jetbrains.compose.resources.Font

/**
 * Sky glass (UX rule V3): a bright anime-game palette. Light mode is a clear sky with frosted white
 * glass; dark mode is a soft twilight, never black. Primary is teal, for actions and the selected
 * state. Secondary is sky blue, for information and charts. Tertiary is soft gold, for celebration:
 * a legal deck, a full gauge, the Lock On moment.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B8794),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F1F3),
    onPrimaryContainer = Color(0xFF00373D),
    secondary = Color(0xFF2F6FD6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF0B2A5C),
    tertiary = Color(0xFF9A6200),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6B0),
    onTertiaryContainer = Color(0xFF3A2500),
    background = Color(0xFFE6F2FA),
    onBackground = Color(0xFF1C2742),
    surface = Color(0xFFF7FBFF),
    onSurface = Color(0xFF1C2742),
    surfaceVariant = Color(0xFFE2EBF5),
    onSurfaceVariant = Color(0xFF4F5B74),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F9FE),
    surfaceContainer = Color(0xFFEDF4FB),
    surfaceContainerHigh = Color(0xFFE6EEF8),
    surfaceContainerHighest = Color(0xFFDDE7F3),
    outline = Color(0xFF7D8BA5),
    outlineVariant = Color(0xFFC9D6E6),
    error = Color(0xFFC62E3D),
    errorContainer = Color(0xFFFFDDE0),
    onErrorContainer = Color(0xFF5A0011),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EE0E6),
    onPrimary = Color(0xFF00323A),
    primaryContainer = Color(0xFF0F4E57),
    onPrimaryContainer = Color(0xFFC0F6F8),
    secondary = Color(0xFF93B9FF),
    onSecondary = Color(0xFF0B2A5C),
    secondaryContainer = Color(0xFF29467F),
    onSecondaryContainer = Color(0xFFDCE8FF),
    tertiary = Color(0xFFFFCA70),
    onTertiary = Color(0xFF402A00),
    tertiaryContainer = Color(0xFF5C4100),
    onTertiaryContainer = Color(0xFFFFE6B0),
    background = Color(0xFF151C36),
    onBackground = Color(0xFFEEF3FF),
    surface = Color(0xFF1B2342),
    onSurface = Color(0xFFEEF3FF),
    surfaceVariant = Color(0xFF2C365C),
    onSurfaceVariant = Color(0xFFB4BFD8),
    surfaceContainerLowest = Color(0xFF10162C),
    surfaceContainerLow = Color(0xFF19213E),
    surfaceContainer = Color(0xFF1F2849),
    surfaceContainerHigh = Color(0xFF263056),
    surfaceContainerHighest = Color(0xFF2E3963),
    outline = Color(0xFF8290B0),
    outlineVariant = Color(0xFF3A4672),
    error = Color(0xFFFF8E97),
    errorContainer = Color(0xFF6E1222),
    onErrorContainer = Color(0xFFFFDDE0),
)

/** Fixed colours for decoration that reads the same in both themes. */
object SuitColors {
    val armorBlue = Color(0xFF3F7FE6)
    val armorWhite = Color(0xFFF3F7FC)
    val chestRed = Color(0xFFE5484D)
    /** Soft gold for celebration: a full gauge, a legal deck, the Lock On moment. */
    val vFinYellow = Color(0xFFFFC34D)
    val cautionBlack = Color(0xFF1C2742)
    val teal = Color(0xFF2CC9D0)
    val sky = Color(0xFF8ED3F5)
    val blush = Color(0xFFF7B8D2)
}

/** Soft, rounded shapes everywhere (UX rule V1): panels, tiles and pills. */
object LockOnShapes {
    val panel = RoundedCornerShape(22.dp)
    val tile = RoundedCornerShape(16.dp)
    val thumb = RoundedCornerShape(8.dp)
    /** Buttons, chips, tabs, search fields and stamps. */
    val button = RoundedCornerShape(percent = 50)
}

private val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = LockOnShapes.tile,
    large = LockOnShapes.panel,
    extraLarge = RoundedCornerShape(28.dp),
)

/** Counts use tabular figures so totals don't jitter while editing (UX rule I1). */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/**
 * Friendly rounded type in mixed case, like a game's menus (UX rule V2a). Quicksand (SIL OFL,
 * licenses/Quicksand-OFL.txt) sets headings, labels and numbers in bold; body text stays the
 * platform sans for long-form readability.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun lockOnTypography(): Typography {
    val base = Typography()
    val rounded = FontFamily(
        Font(Res.font.quicksand, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(Res.font.quicksand, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        Font(Res.font.quicksand, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
    fun TextStyle.game(weight: FontWeight, grow: Float = 0f, tracking: Double = 0.0) =
        copy(fontFamily = rounded, fontWeight = weight, fontSize = (fontSize.value + grow).sp, letterSpacing = tracking.sp)
    return base.copy(
        displayLarge = base.displayLarge.game(FontWeight.ExtraBold),
        displayMedium = base.displayMedium.game(FontWeight.ExtraBold),
        displaySmall = base.displaySmall.game(FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.game(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.game(FontWeight.ExtraBold, -2f),
        headlineSmall = base.headlineSmall.game(FontWeight.ExtraBold, -1f),
        titleLarge = base.titleLarge.game(FontWeight.ExtraBold, -1f),
        titleMedium = base.titleMedium.game(FontWeight.Bold),
        titleSmall = base.titleSmall.game(FontWeight.Bold),
        labelLarge = base.labelLarge.game(FontWeight.Bold, tracking = 0.2),
        labelMedium = base.labelMedium.game(FontWeight.Bold, tracking = 0.2),
        labelSmall = base.labelSmall.game(FontWeight.Bold, tracking = 0.2),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    )
}

@Composable
fun LockOnTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, shapes = MaterialShapes, typography = lockOnTypography(), content = content)
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
