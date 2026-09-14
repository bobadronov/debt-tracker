package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** "2026-08-28 18:13" in the device's current time zone — shared format for session/notification lists. */
fun kotlin.time.Instant.formatDateTime(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).let { dt ->
        "${dt.date} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }

/** "01.09.2026" in the device's current time zone — the debt due-date format (as in ContactDetailComponents). */
fun kotlin.time.Instant.formatDueDate(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
        "${it.day.toString().padStart(2, '0')}.${it.month.number.toString().padStart(2, '0')}.${it.year}"
    }

/** "14:30" in the device's current time zone. */
fun kotlin.time.Instant.formatDueTime(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).let { dt ->
        "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }

/** "01.09.2026, 14:30" — debt due date+time for the reminder text. */
fun kotlin.time.Instant.formatDueDateTime(): String = "${formatDueDate()}, ${formatDueTime()}"

/** "1234.56 ₴" — amount + currency symbol, without locale-specific digit grouping. */
fun BigDecimal.formatMoney(currency: Currency): String = "${toStringExpanded()} ${currency.symbol}"

/** Groups amounts by currency — there are no exchange rates, so cross-currency totals are never added together. */
fun <T> List<T>.sumByCurrency(currencyOf: (T) -> Currency, amountOf: (T) -> BigDecimal): Map<Currency, BigDecimal> =
    groupingBy(currencyOf).fold(BigDecimal.ZERO) { acc, item -> acc + amountOf(item) }

/** Renders each non-zero bucket on its own line (a vertical list); zero currencies are skipped, an empty map yields zero in the default currency. */
fun Map<Currency, BigDecimal>.formatTotals(): String {
    val nonZero = filterValues { it.compareTo(BigDecimal.ZERO) != 0 }
    return if (nonZero.isEmpty()) BigDecimal.ZERO.formatMoney(Currency.UAH)
    else nonZero.entries.joinToString("\n") { (currency, amount) -> amount.formatMoney(currency) }
}
