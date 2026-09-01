package org.bigblackowl.debttracker.domain.model

/**
 * `reminder_lead_days` is stored (Room column, Supabase column) as a comma-joined string of
 * "remind me N days before the due date" values — `""` / `"1"` / `"2"` / `"1,2"`. The domain
 * models carry it as a [Set]<[Int]> instead; these two helpers are the only conversion point.
 * Values are clamped to the supported set {1, 2}; the on-the-day reminder is always implied
 * whenever a due date is set and is never listed here.
 */
val SUPPORTED_REMINDER_LEAD_DAYS: List<Int> = listOf(2, 1)

fun parseReminderLeadDays(raw: String?): Set<Int> =
    raw?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it in SUPPORTED_REMINDER_LEAD_DAYS }
        ?.toSet()
        .orEmpty()

fun Set<Int>.encodeReminderLeadDays(): String =
    this.filter { it in SUPPORTED_REMINDER_LEAD_DAYS }.distinct().sorted().joinToString(",")
