package org.bigblackowl.debttracker.domain.model

enum class PaymentMethod { CASH, CARD }

/** Валюта заборгованості — фіксується на Creditor/Debtor при створенні, всі його транзакції в ній же (без конвертації курсів). */
enum class Currency(val symbol: String, val numericCode: Int) {
    UAH("₴", 980), USD("$", 840), PLN("zł", 985), EUR("€", 978);

    /** ISO-4217 код — збігається з іменем константи, винесений окремо для читабельності call-site'ів. */
    val code: String get() = name

    /** "UAH ₴" — код + символ, для пікерів/лейблів, де сам лише символ неоднозначний (₴/¥, zł/kr тощо). */
    val label: String get() = "$code $symbol"

    companion object {
        /** ISO-4217 числовий код → [Currency], або `null` для валюти поза списком застосунку. Використовує курс валют. */
        fun fromNumericCode(code: Int): Currency? = entries.firstOrNull { it.numericCode == code }

        /** ISO-4217 літерний код ("USD") → [Currency], або `null` поза списком застосунку. */
        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code == code }
    }
}

/** LEND = я дав, REPAY = мені повернули (Debtor-транзакції). */
enum class TransactionType { LEND, REPAY }

/** BORROW = я взяв у борг, RETURN = я повернув (Creditor-транзакції). */
enum class MyDebtTransactionType { BORROW, RETURN }

enum class DebtStatus { ACTIVE, CLOSED }

enum class SyncStatus { LOCAL_ONLY, SYNCED, PENDING, CONFLICT }
