/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.eInkColorScheme
import com.mudita.mmd.eInkTypography

val DefaultThemeColor = Color(0xFFED5564)

/**
 * eInk Music is monochrome-only (MMD design language) — dark/pureBlack/themeColor
 * params are kept for call-site compatibility (theme preview screens, saved prefs)
 * but no longer change the rendered palette.
 */
val EInkMusicColorScheme: ColorScheme = eInkColorScheme.copy(
    surfaceBright = eInkColorScheme.background,
    surfaceDim = eInkColorScheme.background,
    surfaceContainer = eInkColorScheme.background,
    surfaceContainerHigh = eInkColorScheme.background,
    surfaceContainerHighest = eInkColorScheme.background,
    surfaceContainerLowest = eInkColorScheme.background,
)

/**
 * MMD's eInkTypography leaves `lineHeight` Unspecified on every style it customizes.
 * Several stock screens do `.lineHeight.toDp()` arithmetic (e.g. grid-height calculations),
 * which throws "Only Sp can convert to Px" on an Unspecified TextUnit. Patched once here,
 * same fix shape as the Unspecified color-role crash in the audiobook player.
 */
val EInkMusicTypography: Typography = eInkTypography.copy(
    headlineLarge = eInkTypography.headlineLarge.copy(lineHeight = 34.sp),
    titleLarge = eInkTypography.titleLarge.copy(lineHeight = 30.sp),
    titleMedium = eInkTypography.titleMedium.copy(lineHeight = 26.sp),
    titleSmall = eInkTypography.titleSmall.copy(lineHeight = 22.sp),
    bodyLarge = eInkTypography.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = eInkTypography.bodyMedium.copy(lineHeight = 24.sp),
    bodySmall = eInkTypography.bodySmall.copy(lineHeight = 20.sp),
    labelLarge = eInkTypography.labelLarge.copy(lineHeight = 24.sp),
    labelMedium = eInkTypography.labelMedium.copy(lineHeight = 20.sp),
    labelSmall = eInkTypography.labelSmall.copy(lineHeight = 18.sp),
)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    ThemeMMD(
        colorScheme = EInkMusicColorScheme,
        typography = EInkMusicTypography,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
