package org.bigblackowl.debttracker.domain.validation

private const val PHONE_DIGITS_MAX = 10
private const val PHONE_DIGITS_MIN_RELEVANT = 9

/**
 * Keeps only digits and caps at the Ukrainian national number length — e.g. "0501234567" — which
 * is the raw format [org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation]
 * expects (it always prepends its own "+38" and reconstructs the "(0XX) XXX XX XX" grouping from
 * this). A number that still carries the "380" country code (typed with a "+", pasted, or scanned
 * from another user's QR contact card — none of those go through the masked input field, so
 * nothing has stripped it yet) would otherwise just get truncated from the *end*, e.g.
 * "+380501234567" → digits "380501234567" → first 10 → "3805012345" (wrong number, missing its
 * last two digits) instead of the intended "0501234567".
 */
fun sanitizePhoneInput(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    val national = if (digits.length > PHONE_DIGITS_MAX && digits.startsWith("380")) {
        "0" + digits.removePrefix("380")
    } else {
        digits
    }
    return national.take(PHONE_DIGITS_MAX)
}

/** True if pasted/typed [text] has enough digits to look like a phone number — used by the paste-hint's relevance check. */
fun isPhonePasteRelevant(text: String): Boolean = sanitizePhoneInput(text).length >= PHONE_DIGITS_MIN_RELEVANT
