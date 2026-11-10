package app.lockon.ui.theme

import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import app.lockon.model.CardColor
import app.lockon.resources.Res
import app.lockon.resources.quicksand
import app.lockon.ui.components.PressWash
import org.jetbrains.compose.resources.Font

/**
 * Sky and Twilight (UX rule V3): a bright palette after Nintendo's recent menus. Light mode is a pale, quiet
 * sky under calm, solid white panels; dark mode is a deep twilight, never black. Primary is teal, for actions and the selected
 * state. Secondary is sky blue, for information and charts. Tertiary is soft gold, for celebration:
 * a legal deck, a full gauge, the Lock On moment.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF087581),
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
    tertiary = Color(0xFFFFC34D),
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
    /** The one gold, for celebration: a full gauge, a finished sync, the Lock On moment. Text on a day panel uses the darker tertiary. */
    val vFinYellow = Color(0xFFFFC34D)
    val cautionBlack = Color(0xFF1C2742)
    val teal = Color(0xFF2CC9D0)
    val sky = Color(0xFF8ED3F5)
    val blush = Color(0xFFF7B8D2)
}

/**
 * Rounded rectangles everywhere (UX rule V1), as in Nintendo's recent menus: panels, tiles and
 * controls. Only the floating navigation keeps pill ends ([pill]).
 */
object LockOnShapes {
    val panel = RoundedCornerShape(22.dp)
    val tile = RoundedCornerShape(16.dp)
    /** Corner radius of [tile], for parts of one that round only some corners. */
    val tileRadius = 16.dp
    val thumb = RoundedCornerShape(8.dp)
    /** Corner radius of [button], for controls that draw their own fill. Desktop's controls are shorter, so a little tighter. */
    val buttonRadius = if (app.lockon.platform.isMobile) 14.dp else 12.dp
    /** Buttons, chips, tabs, search fields, icon buttons and stamps. */
    val button = RoundedCornerShape(buttonRadius)
    /** The floating nav pill and its items, and switches. */
    val pill = RoundedCornerShape(percent = 50)
}

/** Charts are one slate ramp (UX rule V3), so a bar never reads as a card colour or an action. */
val ChartSlate = Color(0xFF3F5585)
val ChartSlateTwilight = Color(0xFF97A5CF)

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
 * licenses/Quicksand-OFL.txt) sets headings, labels and numbers at one weight, 600; body text stays the
 * platform sans at 400 for long-form readability. Size and colour make the hierarchy, not weight.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun lockOnTypography(): Typography {
    val base = Typography()
    val rounded = FontFamily(
        Font(Res.font.quicksand, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    )
    fun TextStyle.game(grow: Float = 0f, tracking: Double = 0.0) =
        copy(fontFamily = rounded, fontWeight = FontWeight.SemiBold, fontSize = (fontSize.value + grow).sp, letterSpacing = tracking.sp)
    // Quicksand's capitals and numbers sit below the middle of its line, which shows in small labels
    // centred in a chip, bar or button. Each size is raised by what it needs to look centred.
    fun TextStyle.raised(by: Float) = copy(baselineShift = BaselineShift(by))
    return base.copy(
        displayLarge = base.displayLarge.game(),
        displayMedium = base.displayMedium.game(),
        displaySmall = base.displaySmall.game(),
        headlineLarge = base.headlineLarge.game(),
        headlineMedium = base.headlineMedium.game(-2f),
        headlineSmall = base.headlineSmall.game(-1f),
        titleLarge = base.titleLarge.game(-1f),
        titleMedium = base.titleMedium.game(),
        titleSmall = base.titleSmall.game(),
        labelLarge = base.labelLarge.game(tracking = 0.2).raised(0.02f),
        labelMedium = base.labelMedium.game(tracking = 0.2).raised(0.05f),
        labelSmall = base.labelSmall.game(tracking = 0.2).raised(0.05f),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    )
}

@Composable
fun LockOnTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, shapes = MaterialShapes, typography = lockOnTypography()) {
        // Every plain clickable answers with the flat wash (UX rule V5), and Material's own controls show no ripple.
        CompositionLocalProvider(
            LocalIndication provides PressWash(colors.onSurface, colors.primary),
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
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
