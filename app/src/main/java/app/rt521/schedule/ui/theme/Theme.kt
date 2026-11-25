package app.rt521.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.AppSettings
import app.rt521.schedule.data.ThemeMode
import app.rt521.schedule.data.ThemeStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Immutable
data class DesignColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val scrim: Color,
)

@Immutable
data class DesignType(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val countdown: TextStyle,
)

@Immutable
data class DesignShapes(
    val material: Shapes,
    val card: Shape,
    val hero: Shape,
    val chip: Shape,
    val button: Shape,
    val field: Shape,
    val sheet: Shape,
    val smallCard: Shape,
)

@Immutable
data class Design(
    val style: ThemeStyle,
    val dark: Boolean,
    val colors: DesignColors,
    val miuix: MiuixColors?,
    val type: DesignType,
    val shapes: DesignShapes,
    val scheme: ColorScheme,
    val heroGradient: List<Color>,
    val heroContent: Color,
    val backgroundGradient: List<Color>,
    val paletteName: String,
    val paletteColor: Color,
) {
    val isMiuix: Boolean get() = style == ThemeStyle.MIUIX
    val isExpressive: Boolean get() = style != ThemeStyle.MIUIX
}

val LocalDesign = staticCompositionLocalOf<Design> {
    error("Design not provided")
}

private fun ColorScheme.toDesignColors(): DesignColors = DesignColors(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    outline = outline,
    outlineVariant = outlineVariant,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    scrim = scrim,
)

private fun miuixDesignColors(c: MiuixColors): DesignColors = DesignColors(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.accent,
    secondary = c.accent,
    onSecondary = c.onAccent,
    secondaryContainer = c.accentSoft,
    onSecondaryContainer = c.accent,
    tertiary = c.warning,
    onTertiary = Color.White,
    tertiaryContainer = c.warning.copy(alpha = 0.14f),
    onTertiaryContainer = c.warning,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceSecondary,
    onSurfaceVariant = c.textSecondary,
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surfaceSecondary,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surfaceElevated,
    outline = c.textTertiary,
    outlineVariant = c.divider,
    error = c.danger,
    onError = Color.White,
    errorContainer = if (c.background.isLight()) Color(0xFFFFE5E3) else Color(0xFF561E1A),
    onErrorContainer = if (c.background.isLight()) Color(0xFF8B0000) else Color(0xFFFFDAD6),
    inverseSurface = if (c.background.isLight()) Color(0xFF1C1C1E) else Color.White,
    inverseOnSurface = if (c.background.isLight()) Color.White else Color.Black,
    scrim = Color.Black,
)

private fun Color.isLight(): Boolean = luminance() > 0.5f

private fun expressiveType(): DesignType {
    val base = Typography()
    return DesignType(
        display = base.displayLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 50.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1.8).sp,
        ),
        headline = base.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.8).sp,
        ),
        title = base.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.3).sp,
        ),
        cardTitle = base.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.1).sp,
        ),
        body = base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        label = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
        ),
        countdown = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 46.sp,
            letterSpacing = (-1.5).sp,
        ),
    )
}

private fun miuixType(): DesignType {
    val sans = FontFamily.Default
    return DesignType(
        display = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Black,
            fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.0).sp,
        ),
        headline = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Black,
            fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp,
        ),
        title = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Bold,
            fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp,
        ),
        cardTitle = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Bold,
            fontSize = 16.5.sp, lineHeight = 21.sp, letterSpacing = (-0.15).sp,
        ),
        body = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 21.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 16.sp,
        ),
        label = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, lineHeight = 17.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp,
        ),
        countdown = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Black,
            fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.2).sp,
        ),
    )
}

private fun hybridType(): DesignType {
    val e = expressiveType()
    return e.copy(
        display = e.display.copy(fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.Black),
        headline = e.headline.copy(
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
        ),
        title = e.title.copy(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold),
        cardTitle = e.cardTitle.copy(fontSize = 17.sp),
        labelSmall = e.labelSmall.copy(letterSpacing = 0.9.sp),
    )
}

private fun shapesFor(style: ThemeStyle): DesignShapes = when (style) {
    ThemeStyle.MATERIAL -> DesignShapes(
        material = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        card = RoundedCornerShape(28.dp),
        hero = RoundedCornerShape(36.dp),
        chip = CircleShape,
        button = RoundedCornerShape(22.dp),
        field = RoundedCornerShape(16.dp),
        sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        smallCard = RoundedCornerShape(18.dp),
    )

    ThemeStyle.MIUIX -> DesignShapes(
        material = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp),
        ),
        card = RoundedCornerShape(20.dp),
        hero = RoundedCornerShape(24.dp),
        chip = RoundedCornerShape(10.dp),
        button = RoundedCornerShape(16.dp),
        field = RoundedCornerShape(14.dp),
        sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        smallCard = RoundedCornerShape(14.dp),
    )

    ThemeStyle.HYBRID -> DesignShapes(
        material = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(30.dp),
            extraLarge = RoundedCornerShape(38.dp),
        ),
        card = RoundedCornerShape(topStart = 30.dp, topEnd = 18.dp, bottomEnd = 30.dp, bottomStart = 12.dp),
        hero = RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomEnd = 42.dp, bottomStart = 14.dp),
        chip = RoundedCornerShape(14.dp),
        button = RoundedCornerShape(20.dp),
        field = RoundedCornerShape(18.dp),
        sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        smallCard = RoundedCornerShape(20.dp),
    )
}

@Composable
fun RT521Theme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> systemDark
    }

    if (settings.themeStyle == ThemeStyle.MIUIX) {
        MiuixDesignHost(settings = settings, dark = dark, content = content)
        return
    }

    val style = settings.themeStyle
    val paletteId = Palettes.resolvedId(style, settings.palette)
    val option = Palettes.optionFor(style, paletteId)

    val scheme: ColorScheme = when {
        option.system && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = systemDynamicScheme(dark)
            dynamic ?: materialScheme(Color(0xFF6750A4), dark)
        }
        else -> materialScheme(option.color, dark)
    }

    val designType = when (style) {
        ThemeStyle.MATERIAL -> expressiveType()
        ThemeStyle.HYBRID -> hybridType()
        else -> expressiveType()
    }
    val designShapes = shapesFor(style)
    val colors = scheme.toDesignColors()

    val heroGradient = when (style) {
        ThemeStyle.HYBRID -> listOf(
            scheme.primary,
            androidx.compose.ui.graphics.lerp(scheme.primary, scheme.tertiary, 0.62f),
        )
        else -> listOf(scheme.primaryContainer, scheme.tertiaryContainer)
    }
    val heroContent = if (style == ThemeStyle.HYBRID) {
        colors.onPrimary
    } else {
        colors.onPrimaryContainer
    }

    val backgroundGradient = when (style) {
        ThemeStyle.MATERIAL -> listOf(
            colors.background,
            androidx.compose.ui.graphics.lerp(colors.background, colors.primaryContainer, 0.20f),
        )
        ThemeStyle.HYBRID -> listOf(
            androidx.compose.ui.graphics.lerp(colors.background, colors.primaryContainer, 0.35f),
            colors.background,
            androidx.compose.ui.graphics.lerp(colors.background, colors.tertiaryContainer, 0.28f),
        )
        else -> listOf(colors.background, colors.background)
    }

    val design = Design(
        style = style,
        dark = dark,
        colors = colors,
        miuix = null,
        type = designType,
        shapes = designShapes,
        scheme = scheme,
        heroGradient = heroGradient,
        heroContent = heroContent,
        backgroundGradient = backgroundGradient,
        paletteName = option.name,
        paletteColor = option.color,
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = designShapes.material,
    ) {
        CompositionLocalProvider(LocalDesign provides design, content = content)
    }
}

/* ============================================================
 *  Miuix / HyperOS host with Material You (Monet) colors
 * ============================================================ */

@Composable
private fun MiuixDesignHost(
    settings: AppSettings,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val paletteId = Palettes.resolvedId(ThemeStyle.MIUIX, settings.palette)
    val option = Palettes.optionFor(ThemeStyle.MIUIX, paletteId)
    val useMonet = paletteId != "miui"

    val controller = remember(settings.themeMode, paletteId, dark) {
        val mode = when (settings.themeMode) {
            ThemeMode.LIGHT -> if (useMonet) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
            ThemeMode.DARK -> if (useMonet) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
            ThemeMode.AUTO -> if (useMonet) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        }
        ThemeController(
            colorSchemeMode = mode,
            keyColor = when {
                !useMonet -> null
                paletteId == "system" -> null
                else -> option.color
            },
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = ThemePaletteStyle.Expressive,
            isDark = dark,
        )
    }

    MiuixTheme(controller = controller) {
        val official = MiuixTheme.colorScheme
        val miuix = official.toMiuixColors(dark)
        val colors = official.toDesignColors()
        val design = Design(
            style = ThemeStyle.MIUIX,
            dark = dark,
            colors = colors,
            miuix = miuix,
            type = miuixType(),
            shapes = shapesFor(ThemeStyle.MIUIX),
            scheme = materialScheme(option.color, dark),
            heroGradient = listOf(official.tertiaryContainer, official.tertiaryContainer),
            heroContent = official.onTertiaryContainer,
            backgroundGradient = listOf(official.background, official.background),
            paletteName = option.name,
            paletteColor = option.color,
        )
        CompositionLocalProvider(LocalDesign provides design, content = content)
    }
}

private fun top.yukonga.miuix.kmp.theme.Colors.toDesignColors(): DesignColors = DesignColors(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariantSummary,
    surfaceContainerLowest = surface,
    surfaceContainerLow = surface,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    outline = outline,
    outlineVariant = dividerLine,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    inverseSurface = onBackground,
    inverseOnSurface = background,
    scrim = windowDimming,
)

private fun top.yukonga.miuix.kmp.theme.Colors.toMiuixColors(dark: Boolean): MiuixColors = MiuixColors(
    accent = primary,
    accentPressed = primaryVariant,
    accentSoft = tertiaryContainer,
    onAccent = onPrimary,
    background = background,
    surface = surface,
    surfaceSecondary = secondaryContainer,
    surfaceElevated = surfaceContainerHigh,
    textPrimary = onBackground,
    textSecondary = onSurfaceVariantSummary,
    textTertiary = onBackgroundVariant,
    divider = dividerLine,
    danger = error,
    warning = onTertiaryContainer,
    success = primary,
)

/* ============================================================
 *  Extra shape helpers for the expressive layouts
 * ============================================================ */

val CookieShape: Shape = RoundedCornerShape(
    topStart = 34.dp, topEnd = 14.dp, bottomEnd = 34.dp, bottomStart = 14.dp,
)

val PillShape: Shape = CircleShape
