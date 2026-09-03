package org.bigblackowl.debttracker.domain.validation

/** Length of a Ukrainian national significant number: operator code (2) + subscriber (7). */
const val UA_PHONE_NATIONAL_LEN = 9

/**
 * Normalizes any Ukrainian phone input to the bare 9-digit national significant number —
 * e.g. `"671234567"`, which renders as `+380 67 123 45 67`. Accepts every form a number can
 * arrive in: the domestic `"0XX XXX XX XX"` notation, `"380…"` / `"+380…"` international forms
 * (typed, pasted, or scanned from a QR contact card), `"00380…"`, and any separators.
 *
 * The trunk `0` and the `380` country code are display concerns, not storage — a Ukrainian
 * national number is 9 digits and never starts with `0`, so any leading zero is the trunk prefix
 * and is dropped. [org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation]
 * and [formatUkrainianPhone] add the `+380` back for display.
 *
 * Legacy rows stored in the old 10-digit `"0XX…"` form pass through here (on edit-load and on
 * display) and come out in the new form, so data self-heals as contacts are opened.
 */
fun sanitizePhoneInput(raw: String): String {
    var digits = raw.filter(Char::isDigit)
    digits = digits.trimStart('0')            // 00 international prefix / domestic trunk 0
    if (digits.startsWith("380")) digits = digits.substring(3)
    digits = digits.trimStart('0')            // trunk 0 after a stripped country code
    return digits.take(UA_PHONE_NATIONAL_LEN)
}

/** True if pasted/typed [text] has enough digits to look like a phone number — used by the paste-hint's relevance check. */
fun isPhonePasteRelevant(text: String): Boolean = sanitizePhoneInput(text).length >= UA_PHONE_NATIONAL_LEN

/**
 * Renders a stored phone value for display as `+380 XX XXX XX XX`. Tolerant of any stored format
 * (new 9-digit, legacy `0XX…`, `+380…`); a value that isn't yet a full national number is shown
 * best-effort with the digits it has. Returns `null` for `null`/blank so callers can skip the row.
 */
fun formatUkrainianPhone(raw: String?): String? {
    val national = raw?.let(::sanitizePhoneInput)?.takeIf { it.isNotEmpty() } ?: return null
    return buildString {
        append("+380")
        var i = 0
        for (size in intArrayOf(2, 3, 2, 2)) {
            if (i >= national.length) break
            append(' ')
            append(national.substring(i, minOf(i + size, national.length)))
            i += size
        }
    }
}
