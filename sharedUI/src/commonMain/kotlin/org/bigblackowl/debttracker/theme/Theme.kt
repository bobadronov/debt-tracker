package org.bigblackowl.debttracker.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import org.bigblackowl.debttracker.core.i18n.ProvideAppStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.koin.compose.koinInject

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

/**
 * Resolves the [AppSettings.theme] preference (`"dark"` / `"light"` / anything else = follow the OS)
 * to a concrete dark/light choice. Pure so it can be unit-tested without a composition.
 */
internal fun resolveIsDark(themePreference: String, systemIsDark: Boolean): Boolean =
    when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> systemIsDark
    }

/** The app's Material 3 [ColorScheme] for the current [AppSettings.theme] preference. */
internal fun appColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) DarkColorScheme else LightColorScheme

/**
 * The app's Material 3 [ColorScheme], for chrome that lives *outside* [AppTheme]'s own
 * `MaterialTheme` — the desktop window title bar and the notification toast windows, which are
 * separate compositions from [org.bigblackowl.debttracker.App].
 */
@Composable
fun rememberAppColorScheme(): ColorScheme {
    val settings = koinInject<AppSettings>()
    return appColorScheme(resolveIsDark(settings.theme, isSystemInDarkTheme()))
}

/**
 * Applies the Material 3 color scheme (light/dark, resolved from
 * [AppSettings.theme] with a `"system"` fallback to [isSystemInDarkTheme]),
 * provides [LocalDebtAccentColors] for the debt/repay accent colors, and provides
 * [org.bigblackowl.debttracker.core.i18n.LocalStrings] resolved from [AppSettings.locale].
 */
@Composable
internal fun AppTheme(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val settings = koinInject<AppSettings>()
    val isDark = resolveIsDark(settings.theme, isSystemInDarkTheme())
    onThemeChanged(!isDark)
    ProvideAppStrings(settings.locale) {
        CompositionLocalProvider(
            LocalDebtAccentColors provides if (isDark) DarkDebtAccentColors else LightDebtAccentColors
        ) {
            MaterialTheme(
                colorScheme = appColorScheme(isDark),
                content = { Surface(content = content) }
            )
        }
    }
}
