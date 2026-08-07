package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** "1234.56 ₴" — сума + символ валюти, без locale-специфічного групування розрядів. */
fun BigDecimal.formatMoney(currency: Currency): String = "${toStringExpanded()} ${currency.symbol}"

/** Групує суми за валютою — курсів обміну немає, тож крос-валютні тотали не додаються одне до одного. */
fun <T> List<T>.sumByCurrency(currencyOf: (T) -> Currency, amountOf: (T) -> BigDecimal): Map<Currency, BigDecimal> =
    groupingBy(currencyOf).fold(BigDecimal.ZERO) { acc, item -> acc + amountOf(item) }

/** Рендерить кожен ненульовий бакет на окремому рядку (вертикальний список); нульові валюти пропускаються, порожній мапі — нуль у валюті за замовчуванням. */
fun Map<Currency, BigDecimal>.formatTotals(): String {
    val nonZero = filterValues { it.compareTo(BigDecimal.ZERO) != 0 }
    return if (nonZero.isEmpty()) BigDecimal.ZERO.formatMoney(Currency.UAH)
    else nonZero.entries.joinToString("\n") { (currency, amount) -> amount.formatMoney(currency) }
}
