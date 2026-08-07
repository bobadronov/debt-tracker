package org.bigblackowl.debttracker.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

/** Language codes the app ships translations for; anything else falls back to [UkStrings]. */
private val supportedLanguages = setOf("uk", "en")

val LocalStrings = staticCompositionLocalOf { UkStrings }

/**
 * Resolves [AppSettings.locale][org.bigblackowl.debttracker.core.settings.AppSettings.locale]
 * ("system", "uk", or "en") to a concrete [Strings] instance. "system" reads the platform's
 * current locale via [Locale.current] — no dependency on Compose Multiplatform resource
 * environments, which have no public API for overriding the locale independent of the OS.
 */
fun resolveStrings(localeSetting: String): Strings = when (localeSetting) {
    "uk" -> UkStrings
    "en" -> EnStrings
    else -> if (Locale.current.language in supportedLanguages && Locale.current.language == "en") EnStrings else UkStrings
}

/** Provides [LocalStrings] for the whole app, re-resolved whenever [localeSetting] changes. */
@Composable
fun ProvideAppStrings(localeSetting: String, content: @Composable () -> Unit) {
    val strings = remember(localeSetting) { resolveStrings(localeSetting) }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}
