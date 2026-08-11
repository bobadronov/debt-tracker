package org.bigblackowl.debttracker.domain.validation

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Залишає в полі вводу суми лише цифри та один десятковий роздільник, ігноруючи літери й інші символи. */
fun sanitizeAmountInput(raw: String): String {
    val sb = StringBuilder()
    var hasSeparator = false
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            (ch == '.' || ch == ',') && !hasSeparator -> {
                sb.append('.')
                hasSeparator = true
            }
        }
    }
    return sb.toString()
}

/** True if pasted/typed [text] sanitizes to a positive amount — used by the paste-hint's relevance check. */
fun isValidAmountText(text: String): Boolean {
    val sanitized = sanitizeAmountInput(text)
    return sanitized.isNotBlank() &&
        runCatching { BigDecimal.parseString(sanitized) }.getOrNull()?.let { it > BigDecimal.ZERO } == true
}
