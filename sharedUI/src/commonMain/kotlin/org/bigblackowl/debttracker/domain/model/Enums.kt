package org.bigblackowl.debttracker.domain.model

enum class PaymentMethod { CASH, CARD }

/** Валюта заборгованості — фіксується на Creditor/Debtor при створенні, всі його транзакції в ній же (без конвертації курсів). */
enum class Currency(val symbol: String) {
    UAH("₴"), USD("$"), PLN("zł"), EUR("€");

    /** ISO-4217 код — збігається з іменем константи, винесений окремо для читабельності call-site'ів. */
    val code: String get() = name

    /** "UAH ₴" — код + символ, для пікерів/лейблів, де сам лише символ неоднозначний (₴/¥, zł/kr тощо). */
    val label: String get() = "$code $symbol"
}

/** LEND = я дав, REPAY = мені повернули (Debtor-транзакції). */
enum class TransactionType { LEND, REPAY }

/** BORROW = я взяв у борг, RETURN = я повернув (Creditor-транзакції). */
enum class MyDebtTransactionType { BORROW, RETURN }

enum class DebtStatus { ACTIVE, CLOSED }

enum class SyncStatus { LOCAL_ONLY, SYNCED, PENDING, CONFLICT }
