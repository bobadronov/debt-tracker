package org.bigblackowl.debttracker.domain.validation

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
