package org.bigblackowl.debttracker.domain.model

enum class PaymentMethod { CASH, CARD }

/** Debt currency — fixed on the Creditor/Debtor at creation time, all of its transactions use the same one (no rate conversion). */
enum class Currency(val symbol: String, val numericCode: Int) {
    UAH("₴", 980), USD("$", 840), PLN("zł", 985), EUR("€", 978);

    /** ISO-4217 code — matches the constant's name, exposed separately for call-site readability. */
    val code: String get() = name

    /** "UAH ₴" — code + symbol, for pickers/labels where the symbol alone is ambiguous (₴/¥, zł/kr, etc.). */
    val label: String get() = "$code $symbol"
}

/** LEND = I lent, REPAY = I was repaid (Debtor transactions). */
enum class TransactionType { LEND, REPAY }

/** BORROW = I borrowed, RETURN = I paid back (Creditor transactions). */
enum class MyDebtTransactionType { BORROW, RETURN }

enum class DebtStatus { ACTIVE, CLOSED }

enum class SyncStatus { LOCAL_ONLY, SYNCED, PENDING, CONFLICT }
