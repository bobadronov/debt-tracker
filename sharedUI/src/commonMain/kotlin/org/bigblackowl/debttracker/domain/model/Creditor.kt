package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant

/**
 * Кредитор: людина, якій я винен (дзеркало [Debtor], спек §4.1).
 * Окрема сутність навмисно — контакт живе рівно в одному з двох списків
 * одночасно, автоматичного взаємозаліку з [Debtor] немає.
 */
data class Creditor(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,           // якщо збігається з профілем зареєстрованого користувача — джерело автозаповнення (§ProfileLookup)
    val avatarUrl: String?,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: DebtStatus,       // той самий enum ACTIVE/CLOSED, що й у Debtor
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false
)

data class CreditorTransaction(
    val id: String,
    val creditorId: String,
    val amount: BigDecimal,       // ЗІ ЗНАКОМ: додатне (+) = я повернув (RETURN), від'ємне (−) = я взяв у борг (BORROW).
    val type: MyDebtTransactionType, // type = if (amount.isPositive) RETURN else BORROW
    val method: PaymentMethod,
    val cardLastDigits: String?,
    val date: Instant,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false
)

/** balance = -Σ(amount): скільки я ще винен цій людині. Та сама формула, що й у [debtorBalance]. */
fun List<CreditorTransaction>.creditorBalance(): BigDecimal =
    this.filterNot { it.isDeleted }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.negate()

/** type виводиться зі знаку суми: signum() > 0 → RETURN (я повернув), інакше BORROW (я взяв у борг). */
fun BigDecimal.toCreditorTransactionType(): MyDebtTransactionType =
    if (this.signum() > 0) MyDebtTransactionType.RETURN else MyDebtTransactionType.BORROW

/** Проєкція для CreditorListScreen: кредитор + обчислений баланс (спек §4.1, §6, п.3). */
data class CreditorWithBalance(
    val creditor: Creditor,
    val balance: BigDecimal
)
