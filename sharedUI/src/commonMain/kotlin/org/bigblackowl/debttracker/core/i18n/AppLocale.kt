package org.bigblackowl.debttracker.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import org.bigblackowl.debttracker.core.i18n.translate.CsStrings
import org.bigblackowl.debttracker.core.i18n.translate.DeStrings
import org.bigblackowl.debttracker.core.i18n.translate.EnStrings
import org.bigblackowl.debttracker.core.i18n.translate.EsStrings
import org.bigblackowl.debttracker.core.i18n.translate.FrStrings
import org.bigblackowl.debttracker.core.i18n.translate.ItStrings
import org.bigblackowl.debttracker.core.i18n.translate.NlStrings
import org.bigblackowl.debttracker.core.i18n.translate.PlStrings
import org.bigblackowl.debttracker.core.i18n.translate.PtStrings
import org.bigblackowl.debttracker.core.i18n.translate.UkStrings

/** Language codes the app ships translations for; anything else falls back to [org.bigblackowl.debttracker.core.i18n.translate.UkStrings]. */
private val supportedLanguages = mapOf(
    "uk" to UkStrings,
    "en" to EnStrings,
    "de" to DeStrings,
    "es" to EsStrings,
    "fr" to FrStrings,
    "it" to ItStrings,
    "nl" to NlStrings,
    "pl" to PlStrings,
    "pt" to PtStrings,
    "cs" to CsStrings,
)

val LocalStrings = staticCompositionLocalOf { UkStrings }

/**
 * Resolves [AppSettings.locale][org.bigblackowl.debttracker.core.settings.AppSettings.locale]
 * ("system" or any key in [supportedLanguages]) to a concrete [Strings] instance. "system" reads the platform's
 * current locale via [Locale.current] — no dependency on Compose Multiplatform resource
 * environments, which have no public API for overriding the locale independent of the OS.
 */
fun resolveStrings(localeSetting: String): Strings =
    supportedLanguages[localeSetting] ?: supportedLanguages[Locale.current.language] ?: UkStrings

/** Provides [LocalStrings] for the whole app, re-resolved whenever [localeSetting] changes. */
@Composable
fun ProvideAppStrings(localeSetting: String, content: @Composable () -> Unit) {
    val strings = remember(localeSetting) { resolveStrings(localeSetting) }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}
