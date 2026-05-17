package com.autobox.monitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import com.autobox.monitor.R
import com.autobox.monitor.model.ThemeMode

// Amber/warning — no M3 semantic role. Provided via CompositionLocal so it
// can differ between light and dark schemes while staying OEM-overridable via XML.
val LocalWarningColor = staticCompositionLocalOf { Color(0xFFFFB020) }

val MaterialTheme.warningColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWarningColor.current

@Composable
fun MonitorTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) buildDarkScheme() else buildLightScheme()
    val warningColor = colorResource(
        if (useDark) R.color.status_warning_dark else R.color.status_warning_light
    )

    CompositionLocalProvider(LocalWarningColor provides warningColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography.copy(
                bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            ),
            content = content,
        )
    }
}

@Composable
private fun buildDarkScheme(): ColorScheme = darkColorScheme(
    primary = colorResource(R.color.m3_primary),
    onPrimary = colorResource(R.color.m3_on_primary),
    primaryContainer = colorResource(R.color.m3_primary_container),
    onPrimaryContainer = colorResource(R.color.m3_on_primary_container),
    inversePrimary = colorResource(R.color.m3_inverse_primary),

    secondary = colorResource(R.color.m3_secondary),
    onSecondary = colorResource(R.color.m3_on_secondary),
    secondaryContainer = colorResource(R.color.m3_secondary_container),
    onSecondaryContainer = colorResource(R.color.m3_on_secondary_container),

    tertiary = colorResource(R.color.m3_tertiary),
    onTertiary = colorResource(R.color.m3_on_tertiary),
    tertiaryContainer = colorResource(R.color.m3_tertiary_container),
    onTertiaryContainer = colorResource(R.color.m3_on_tertiary_container),

    error = colorResource(R.color.m3_error),
    onError = colorResource(R.color.m3_on_error),
    errorContainer = colorResource(R.color.m3_error_container),
    onErrorContainer = colorResource(R.color.m3_on_error_container),

    background = colorResource(R.color.m3_background),
    onBackground = colorResource(R.color.m3_on_background),
    surface = colorResource(R.color.m3_surface),
    onSurface = colorResource(R.color.m3_on_surface),
    surfaceVariant = colorResource(R.color.m3_surface_variant),
    onSurfaceVariant = colorResource(R.color.m3_on_surface_variant),
    inverseSurface = colorResource(R.color.m3_inverse_surface),
    inverseOnSurface = colorResource(R.color.m3_inverse_on_surface),

    outline = colorResource(R.color.m3_outline),
    outlineVariant = colorResource(R.color.m3_outline_variant),
    scrim = colorResource(R.color.m3_scrim),
)

@Composable
private fun buildLightScheme(): ColorScheme = lightColorScheme(
    primary = colorResource(R.color.m3_light_primary),
    onPrimary = colorResource(R.color.m3_light_on_primary),
    primaryContainer = colorResource(R.color.m3_light_primary_container),
    onPrimaryContainer = colorResource(R.color.m3_light_on_primary_container),
    inversePrimary = colorResource(R.color.m3_light_inverse_primary),

    secondary = colorResource(R.color.m3_light_secondary),
    onSecondary = colorResource(R.color.m3_light_on_secondary),
    secondaryContainer = colorResource(R.color.m3_light_secondary_container),
    onSecondaryContainer = colorResource(R.color.m3_light_on_secondary_container),

    tertiary = colorResource(R.color.m3_light_tertiary),
    onTertiary = colorResource(R.color.m3_light_on_tertiary),
    tertiaryContainer = colorResource(R.color.m3_light_tertiary_container),
    onTertiaryContainer = colorResource(R.color.m3_light_on_tertiary_container),

    error = colorResource(R.color.m3_light_error),
    onError = colorResource(R.color.m3_light_on_error),
    errorContainer = colorResource(R.color.m3_light_error_container),
    onErrorContainer = colorResource(R.color.m3_light_on_error_container),

    background = colorResource(R.color.m3_light_background),
    onBackground = colorResource(R.color.m3_light_on_background),
    surface = colorResource(R.color.m3_light_surface),
    onSurface = colorResource(R.color.m3_light_on_surface),
    surfaceVariant = colorResource(R.color.m3_light_surface_variant),
    onSurfaceVariant = colorResource(R.color.m3_light_on_surface_variant),
    inverseSurface = colorResource(R.color.m3_light_inverse_surface),
    inverseOnSurface = colorResource(R.color.m3_light_inverse_on_surface),

    outline = colorResource(R.color.m3_light_outline),
    outlineVariant = colorResource(R.color.m3_light_outline_variant),
    scrim = colorResource(R.color.m3_light_scrim),
)
