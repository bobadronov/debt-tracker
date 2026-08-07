package org.bigblackowl.debttracker.domain.validation

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Loose shape check — gates when it's worth firing a profile lookup, not a strict RFC validator. */
fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())
