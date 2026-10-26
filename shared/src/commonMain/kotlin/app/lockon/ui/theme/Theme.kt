package app.lockon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.lockon.resources.Res
import app.lockon.resources.rajdhani_bold
import app.lockon.resources.rajdhani_semibold
import org.jetbrains.compose.resources.Font
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.lockon.model.CardColor

/**
 * Material 3 colour roles built from a mobile-suit tricolour (UX rule V3):
 * primary is the V-fin yellow used for actions, secondary is armour blue for good states,
 * tertiary is chest red for emphasis. Dark mode is the cockpit; light mode is white armour.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC94D),
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF584400),
    onPrimaryContainer = Color(0xFFFFE08F),
    secondary = Color(0xFFAFC6FF),
    onSecondary = Color(0xFF0F2F66),
    secondaryContainer = Color(0xFF26437F),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFFFB3AC),
    onTertiary = Color(0xFF680006),
    tertiaryContainer = Color(0xFF8C1D18),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1320),
    onBackground = Color(0xFFE2E6F0),
    surface = Color(0xFF0D1320),
    onSurface = Color(0xFFE2E6F0),
    surfaceVariant = Color(0xFF2A3446),
    onSurfaceVariant = Color(0xFFBCC5D6),
    surfaceContainerLowest = Color(0xFF080D17),
    surfaceContainerLow = Color(0xFF121A28),
    surfaceContainer = Color(0xFF162030),
    surfaceContainerHigh = Color(0xFF1C2738),
    surfaceContainerHighest = Color(0xFF233044),
    outline = Color(0xFF8690A3),
    outlineVariant = Color(0xFF3A4558),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF745B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08F),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF2A5BB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E2FF),
    onSecondaryContainer = Color(0xFF001A43),
    tertiary = Color(0xFFB3261E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFF7F8FC),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE0E3EC),
    onSurfaceVariant = Color(0xFF444955),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F3F9),
    surfaceContainer = Color(0xFFEBEEF5),
    surfaceContainerHigh = Color(0xFFE5E8F0),
    surfaceContainerHighest = Color(0xFFDFE3EB),
    outline = Color(0xFF757A86),
    outlineVariant = Color(0xFFC4C8D2),
)

/** Fixed mobile-suit colours for decoration (tricolour stripe, V-fin, caution stripes). */
object SuitColors {
    val armorBlue = Color(0xFF2A5BB8)
    val armorWhite = Color(0xFFE9ECF2)
    val chestRed = Color(0xFFD3352B)
    val vFinYellow = Color(0xFFFFC94D)
    val cautionBlack = Color(0xFF1A1C22)
}

/**
 * Armour-plate cut corners for large surfaces only: cards, panels and card faces (UX rule V1).
 * Small controls keep Material's rounded shapes.
 */
object LockOnShapes {
    val panel = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    val tile = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    val thumb = CutCornerShape(topStart = 6.dp)
}

private val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    // Material cards and similar containers become armour plates.
    medium = LockOnShapes.tile,
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Counts use tabular figures so totals don't jitter while editing (UX rule I1). */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/**
 * Rajdhani (SIL OFL, licenses/Rajdhani-OFL.txt) gives headings, labels and numbers a cockpit-display
 * feel. Body text stays the platform font for long-form readability. Rajdhani runs small, so its
 * sizes are nudged up.
 */
@Composable
private fun lockOnTypography(): Typography {
    val hud = FontFamily(Font(Res.font.rajdhani_semibold, FontWeight.SemiBold), Font(Res.font.rajdhani_bold, FontWeight.Bold))
    val base = Typography()
    fun TextStyle.hud(weight: FontWeight, grow: Int) =
        copy(fontFamily = hud, fontWeight = weight, fontSize = (fontSize.value + grow).sp, letterSpacing = 0.3.sp)
    return base.copy(
        displaySmall = base.displaySmall.hud(FontWeight.Bold, 2),
        headlineLarge = base.headlineLarge.hud(FontWeight.Bold, 2),
        headlineMedium = base.headlineMedium.hud(FontWeight.Bold, 2),
        headlineSmall = base.headlineSmall.hud(FontWeight.Bold, 2),
        titleLarge = base.titleLarge.hud(FontWeight.Bold, 2),
        titleMedium = base.titleMedium.hud(FontWeight.Bold, 2),
        titleSmall = base.titleSmall.hud(FontWeight.SemiBold, 2),
        labelLarge = base.labelLarge.hud(FontWeight.SemiBold, 2),
        labelMedium = base.labelMedium.hud(FontWeight.SemiBold, 1),
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
