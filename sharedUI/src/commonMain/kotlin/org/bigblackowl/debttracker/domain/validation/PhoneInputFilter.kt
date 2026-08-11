package org.bigblackowl.debttracker.domain.validation

private const val PHONE_DIGITS_MAX = 10
private const val PHONE_DIGITS_MIN_RELEVANT = 9

/** Keeps only digits and caps at the Ukrainian national number length (used by [org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation]). */
fun sanitizePhoneInput(raw: String): String = raw.filter(Char::isDigit).take(PHONE_DIGITS_MAX)

/** True if pasted/typed [text] has enough digits to look like a phone number — used by the paste-hint's relevance check. */
fun isPhonePasteRelevant(text: String): Boolean = sanitizePhoneInput(text).length >= PHONE_DIGITS_MIN_RELEVANT
