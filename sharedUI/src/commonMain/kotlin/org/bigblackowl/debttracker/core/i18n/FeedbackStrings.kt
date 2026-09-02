package org.bigblackowl.debttracker.core.i18n

import androidx.compose.ui.text.intl.Locale

/**
 * Label + subtitle for the Settings → About → "Send feedback" row.
 *
 * Deliberately standalone from [Strings]: that data class's flat constructor already sits at the
 * JVM 255-parameter method limit (see the note there — its generated `copy$default` is the ceiling),
 * so it can take no more fields. Resolved the same way as [resolveStrings]:
 * [AppSettings.locale][org.bigblackowl.debttracker.core.settings.AppSettings.locale] key first,
 * then the platform language, then Ukrainian.
 */
data class FeedbackStrings(val title: String, val subtitle: String)

private val feedbackStringsByLanguage = mapOf(
    "uk" to FeedbackStrings("Надіслати відгук", "Пропозиції та повідомлення про помилки"),
    "en" to FeedbackStrings("Send feedback", "Suggestions and bug reports"),
    "de" to FeedbackStrings("Feedback senden", "Vorschläge und Fehlerberichte"),
    "es" to FeedbackStrings("Enviar comentarios", "Sugerencias e informes de errores"),
    "fr" to FeedbackStrings("Envoyer un commentaire", "Suggestions et rapports de bugs"),
    "it" to FeedbackStrings("Invia un feedback", "Suggerimenti e segnalazioni di bug"),
    "nl" to FeedbackStrings("Feedback sturen", "Suggesties en bugmeldingen"),
    "pl" to FeedbackStrings("Wyślij opinię", "Propozycje i zgłoszenia błędów"),
    "pt" to FeedbackStrings("Enviar feedback", "Sugestões e relatórios de erros"),
    "cs" to FeedbackStrings("Odeslat zpětnou vazbu", "Návrhy a hlášení chyb"),
)

fun resolveFeedbackStrings(localeSetting: String): FeedbackStrings =
    feedbackStringsByLanguage[localeSetting]
        ?: feedbackStringsByLanguage[Locale.current.language]
        ?: feedbackStringsByLanguage.getValue("uk")
