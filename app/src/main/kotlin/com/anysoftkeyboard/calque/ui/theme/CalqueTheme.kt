package com.anysoftkeyboard.calque.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** CompositionLocal providing semantic Calque design system color tokens. */
val LocalCalqueColors = staticCompositionLocalOf { CalqueColors.Light.toTokens() }

/** CompositionLocal providing typography sizing spec. */
val LocalCalqueTypography = staticCompositionLocalOf { CalqueTypography() }

val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(CalqueColors.Light.slateBlueAccent),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(CalqueColors.Light.cognateEmeraldFill),
        onPrimaryContainer = Color(CalqueColors.Light.slateBlueAccent),
        secondary = Color(CalqueColors.Light.cognateEmerald),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(CalqueColors.Light.background),
        onBackground = Color(CalqueColors.Light.textPrimary),
        surface = Color(CalqueColors.Light.surface),
        onSurface = Color(CalqueColors.Light.textPrimary),
        surfaceVariant = Color(CalqueColors.Light.backgroundSecondary),
        onSurfaceVariant = Color(CalqueColors.Light.textSecondary),
        outline = Color(CalqueColors.Light.border),
        error = Color(CalqueColors.Light.errorCrimson),
        errorContainer = Color(CalqueColors.Light.errorCrimsonFill),
        onError = Color(0xFFFFFFFF),
    )

val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(CalqueColors.Dark.slateBlueAccent),
        onPrimary = Color(CalqueColors.Dark.background),
        primaryContainer = Color(CalqueColors.Dark.cognateEmeraldFill),
        onPrimaryContainer = Color(CalqueColors.Dark.slateBlueAccent),
        secondary = Color(CalqueColors.Dark.cognateEmerald),
        onSecondary = Color(CalqueColors.Dark.background),
        background = Color(CalqueColors.Dark.background),
        onBackground = Color(CalqueColors.Dark.textPrimary),
        surface = Color(CalqueColors.Dark.surface),
        onSurface = Color(CalqueColors.Dark.textPrimary),
        surfaceVariant = Color(CalqueColors.Dark.backgroundSecondary),
        onSurfaceVariant = Color(CalqueColors.Dark.textSecondary),
        outline = Color(CalqueColors.Dark.border),
        error = Color(CalqueColors.Dark.errorCrimson),
        errorContainer = Color(CalqueColors.Dark.errorCrimsonFill),
        onError = Color(CalqueColors.Dark.background),
    )

/** Entry point to access current Calque theme design tokens. */
object CalqueTheme {
    val colors: CalqueColorTokens
        @Composable @ReadOnlyComposable get() = LocalCalqueColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}

/**
 * Primary theme wrapper for Calque applying custom tokens and Material 3 theme plumbing.
 *
 * @param darkTheme Whether dark mode (Late Night Study) is enabled. Defaults to system setting.
 * @param content Composable tree to render within the theme.
 */
@Composable
fun CalqueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) CalqueColors.Dark.toTokens() else CalqueColors.Light.toTokens()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalCalqueColors provides tokens,
        LocalCalqueTypography provides CalqueTypography(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalqueComposeTypography,
            shapes = CalqueShapes,
            content = content,
        )
    }
}
