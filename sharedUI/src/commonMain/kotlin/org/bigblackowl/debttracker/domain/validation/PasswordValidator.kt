package org.bigblackowl.debttracker.domain.validation

/**
 * Client-side sign-up nudge only, not the real enforcement boundary — Supabase Auth's own
 * server-side minimum (6 characters by default) is what actually gates account creation.
 */
private const val MIN_PASSWORD_LENGTH = 8

fun isStrongEnoughPassword(value: String): Boolean = value.length >= MIN_PASSWORD_LENGTH
