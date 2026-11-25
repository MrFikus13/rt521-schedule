package app.rt521.schedule.ui.theme

import androidx.core.graphics.ColorUtils
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import app.rt521.schedule.data.ThemeStyle
import kotlin.math.cos
import kotlin.math.sin

enum class PaletteFamily { MATERIAL, MIUIX }

data class PaletteOption(
    val id: String,
    val name: String,
    val color: Color,
    val family: PaletteFamily,
    val system: Boolean = false,
)

object Palettes {

    val material = listOf(
        PaletteOption("violet", "Фиолетовый", Color(0xFF6750A4), PaletteFamily.MATERIAL),
        PaletteOption("blue", "Синий", Color(0xFF445AF3), PaletteFamily.MATERIAL),
        PaletteOption("green", "Зелёный", Color(0xFF386A20), PaletteFamily.MATERIAL),
        PaletteOption("pink", "Розовый", Color(0xFF984061), PaletteFamily.MATERIAL),
        PaletteOption("orange", "Оранжевый", Color(0xFF8D5000), PaletteFamily.MATERIAL),
        PaletteOption("teal", "Бирюзовый", Color(0xFF00696D), PaletteFamily.MATERIAL),
        PaletteOption(
            "system", "Система", Color(0xFF7C6EB0),
            PaletteFamily.MATERIAL, system = true,
        ),
    )

    val miuix = listOf(
        PaletteOption(
            "system", "Material You", Color(0xFF7C6EB0),
            PaletteFamily.MIUIX, system = true,
        ),
        PaletteOption("miui", "MIUIX Blue", Color(0xFF3482FF), PaletteFamily.MIUIX),
        PaletteOption("mint", "Мята", Color(0xFF00BFA5), PaletteFamily.MIUIX),
        PaletteOption("coral", "Коралл", Color(0xFFFF6B5B), PaletteFamily.MIUIX),
        PaletteOption("lavender", "Лаванда", Color(0xFF8E7CFF), PaletteFamily.MIUIX),
        PaletteOption("gold", "Золото", Color(0xFFF5A623), PaletteFamily.MIUIX),
    )

    fun forStyle(style: ThemeStyle): List<PaletteOption> = when (style) {
        ThemeStyle.MIUIX -> miuix
        else -> material
    }

    fun optionFor(style: ThemeStyle, id: String): PaletteOption {
        val list = forStyle(style)
        return list.firstOrNull { it.id == id } ?: list.first()
    }

    fun resolvedId(style: ThemeStyle, id: String): String {
        val list = forStyle(style)
        return if (list.any { it.id == id }) id else list.first().id
    }
}

/* ============================================================
 *  Tonal palette generation (Material-like, from any seed)
 * ============================================================ */

private fun tonal(seed: Int, tone: Int, chroma: Double = 1.0, hueShift: Double = 0.0): Color {
    val hct = FloatArray(3)
    ColorUtils.colorToM3HCT(seed, hct)
    val hue = ((hct[0] + hueShift.toFloat()) % 360f + 360f) % 360f
    val chromaValue = (hct[1] * chroma.toFloat()).coerceAtLeast(0f)
    return Color(ColorUtils.M3HCTToColor(hue, chromaValue, tone.toFloat()))
}

fun materialScheme(seed: Color, dark: Boolean): ColorScheme {
    val key = seed.toArgb()
    val primary40 = tonal(key, 40)
    val primary80 = tonal(key, 80)
    val primary90 = tonal(key, 90)
    val primary30 = tonal(key, 30)
    val primary20 = tonal(key, 20)
    val primary10 = tonal(key, 10)
    val secondary40 = tonal(key, 40, 0.35)
    val secondary80 = tonal(key, 80, 0.35)
    val secondary90 = tonal(key, 90, 0.30)
    val secondary30 = tonal(key, 30, 0.35)
    val secondary20 = tonal(key, 20, 0.35)
    val secondary10 = tonal(key, 10, 0.35)
    val tertiary40 = tonal(key, 40, 0.85, 65.0)
    val tertiary80 = tonal(key, 80, 0.85, 65.0)
    val tertiary90 = tonal(key, 90, 0.80, 65.0)
    val tertiary30 = tonal(key, 30, 0.85, 65.0)
    val tertiary20 = tonal(key, 20, 0.85, 65.0)
    val tertiary10 = tonal(key, 10, 0.85, 65.0)
    val neutral6 = tonal(key, 6, 0.045)
    val neutral10 = tonal(key, 10, 0.045)
    val neutral12 = tonal(key, 12, 0.045)
    val neutral17 = tonal(key, 17, 0.045)
    val neutral20 = tonal(key, 20, 0.05)
    val neutral22 = tonal(key, 22, 0.05)
    val neutral24 = tonal(key, 24, 0.05)
    val neutral87 = tonal(key, 87, 0.04)
    val neutral90 = tonal(key, 90, 0.05)
    val neutral92 = tonal(key, 92, 0.05)
    val neutral94 = tonal(key, 94, 0.045)
    val neutral95 = tonal(key, 95, 0.04)
    val neutral96 = tonal(key, 96, 0.04)
    val neutral98 = tonal(key, 98, 0.035)
    val neutral99 = tonal(key, 99, 0.03)
    val variant30 = tonal(key, 30, 0.12)
    val variant50 = tonal(key, 50, 0.10)
    val variant60 = tonal(key, 60, 0.10)
    val variant80 = tonal(key, 80, 0.10)
    val variant90 = tonal(key, 90, 0.10)

    val error40 = Color(0xFFB3261E)
    val error80 = Color(0xFFF2B8B5)

    return if (!dark) {
        lightColorScheme(
            primary = primary40,
            onPrimary = Color.White,
            primaryContainer = primary90,
            onPrimaryContainer = primary10,
            inversePrimary = primary80,
            secondary = secondary40,
            onSecondary = Color.White,
            secondaryContainer = secondary90,
            onSecondaryContainer = secondary10,
            tertiary = tertiary40,
            onTertiary = Color.White,
            tertiaryContainer = tertiary90,
            onTertiaryContainer = tertiary10,
            background = neutral99,
            onBackground = neutral10,
            surface = neutral99,
            onSurface = neutral10,
            surfaceVariant = variant90,
            onSurfaceVariant = variant30,
            surfaceTint = primary40,
            inverseSurface = neutral20,
            inverseOnSurface = neutral95,
            error = error40,
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = variant50,
            outlineVariant = variant80,
            scrim = Color.Black,
            surfaceBright = neutral98,
            surfaceDim = neutral87,
            surfaceContainer = neutral94,
            surfaceContainerHigh = neutral92,
            surfaceContainerHighest = neutral90,
            surfaceContainerLow = neutral96,
            surfaceContainerLowest = Color.White,
            primaryFixed = primary90,
            primaryFixedDim = primary80,
            onPrimaryFixed = primary10,
            onPrimaryFixedVariant = primary30,
            secondaryFixed = secondary90,
            secondaryFixedDim = secondary80,
            onSecondaryFixed = secondary10,
            onSecondaryFixedVariant = secondary30,
            tertiaryFixed = tertiary90,
            tertiaryFixedDim = tertiary80,
            onTertiaryFixed = tertiary10,
            onTertiaryFixedVariant = tertiary30,
        )
    } else {
        darkColorScheme(
            primary = primary80,
            onPrimary = primary20,
            primaryContainer = primary30,
            onPrimaryContainer = primary90,
            inversePrimary = primary40,
            secondary = secondary80,
            onSecondary = secondary20,
            secondaryContainer = secondary30,
            onSecondaryContainer = secondary90,
            tertiary = tertiary80,
            onTertiary = tertiary20,
            tertiaryContainer = tertiary30,
            onTertiaryContainer = tertiary90,
            background = neutral6,
            onBackground = neutral90,
            surface = neutral6,
            onSurface = neutral90,
            surfaceVariant = variant30,
            onSurfaceVariant = variant80,
            surfaceTint = primary80,
            inverseSurface = neutral90,
            inverseOnSurface = neutral20,
            error = error80,
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = variant60,
            outlineVariant = variant30,
            scrim = Color.Black,
            surfaceBright = neutral24,
            surfaceDim = neutral6,
            surfaceContainer = neutral12,
            surfaceContainerHigh = neutral17,
            surfaceContainerHighest = neutral22,
            surfaceContainerLow = neutral10,
            surfaceContainerLowest = Color(0xFF0F0D13),
            primaryFixed = primary90,
            primaryFixedDim = primary80,
            onPrimaryFixed = primary10,
            onPrimaryFixedVariant = primary30,
            secondaryFixed = secondary90,
            secondaryFixedDim = secondary80,
            onSecondaryFixed = secondary10,
            onSecondaryFixedVariant = secondary30,
            tertiaryFixed = tertiary90,
            tertiaryFixedDim = tertiary80,
            onTertiaryFixed = tertiary10,
            onTertiaryFixedVariant = tertiary30,
        )
    }
}

/* ============================================================
 *  MIUIX color scheme — flat HyperOS surfaces + one strong accent
 * ============================================================ */

data class MiuixColors(
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
)

fun miuixColors(seed: Color, dark: Boolean): MiuixColors {
    return if (!dark) {
        MiuixColors(
            accent = seed,
            accentPressed = seed.copy(alpha = 0.82f),
            accentSoft = ColorUtils.blendARGB(seed.toArgb(), 0xFFFFFFFF.toInt(), 0.88f).let(::Color),
            onAccent = Color.White,
            background = Color(0xFFF2F2F7),
            surface = Color(0xFFFFFFFF),
            surfaceSecondary = Color(0xFFF7F7FA),
            surfaceElevated = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF000000),
            textSecondary = Color(0xFF7C7C82),
            textTertiary = Color(0xFFA9A9B0),
            divider = Color(0xFFE6E6EB),
            danger = Color(0xFFFF3B30),
            warning = Color(0xFFFF9500),
            success = Color(0xFF34C759),
        )
    } else {
        MiuixColors(
            accent = ColorUtils.blendARGB(seed.toArgb(), 0xFFFFFFFF.toInt(), 0.18f).let(::Color),
            accentPressed = ColorUtils.blendARGB(seed.toArgb(), 0xFFFFFFFF.toInt(), 0.08f).let(::Color),
            accentSoft = ColorUtils.blendARGB(seed.toArgb(), 0xFF000000.toInt(), 0.72f).let(::Color),
            onAccent = Color.White,
            background = Color(0xFF000000),
            surface = Color(0xFF1C1C1E),
            surfaceSecondary = Color(0xFF242426),
            surfaceElevated = Color(0xFF2C2C2E),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF98989F),
            textTertiary = Color(0xFF6E6E73),
            divider = Color(0xFF2E2E31),
            danger = Color(0xFFFF453A),
            warning = Color(0xFFFF9F0A),
            success = Color(0xFF30D158),
        )
    }
}

fun miuixColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val c = miuixColors(seed, dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = if (dark) c.accent else c.accent,
        inversePrimary = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = if (dark) c.accent else c.accent,
        tertiary = if (dark) Color(0xFFFFB59D) else Color(0xFFB3482F),
        onTertiary = Color.White,
        tertiaryContainer = if (dark) Color(0xFF5D2A1E) else Color(0xFFFFDBD1),
        onTertiaryContainer = if (dark) Color(0xFFFFDBD1) else Color(0xFF3B0900),
        background = c.background,
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceSecondary,
        onSurfaceVariant = c.textSecondary,
        surfaceTint = c.accent,
        inverseSurface = if (dark) Color.White else Color(0xFF1C1C1E),
        inverseOnSurface = if (dark) Color.Black else Color.White,
        error = c.danger,
        onError = Color.White,
        errorContainer = if (dark) Color(0xFF561E1A) else Color(0xFFFFE5E3),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF8B0000),
        outline = if (dark) Color(0xFF48484A) else Color(0xFFC7C7CC),
        outlineVariant = c.divider,
        scrim = Color.Black,
        surfaceBright = if (dark) Color(0xFF2C2C2E) else Color.White,
        surfaceDim = if (dark) Color(0xFF000000) else Color(0xFFE8E8ED),
        surfaceContainer = c.surface,
        surfaceContainerHigh = if (dark) Color(0xFF2C2C2E) else Color.White,
        surfaceContainerHighest = if (dark) Color(0xFF3A3A3C) else Color(0xFFF7F7FA),
        surfaceContainerLow = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        surfaceContainerLowest = if (dark) Color(0xFF141416) else Color.White,
        primaryFixed = if (dark) c.accentSoft else c.accentSoft,
        primaryFixedDim = c.accent,
        onPrimaryFixed = if (dark) c.accent else c.accent,
        onPrimaryFixedVariant = if (dark) c.accent else c.accent,
        secondaryFixed = c.accentSoft,
        secondaryFixedDim = c.accent,
        onSecondaryFixed = c.accent,
        onSecondaryFixedVariant = c.accent,
    )
}

/* ============================================================
 *  Dynamic color (Material You)
 * ============================================================ */

@Composable
fun systemDynamicScheme(dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
