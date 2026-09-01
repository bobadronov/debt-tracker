package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

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
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val status: DebtStatus,       // той самий enum ACTIVE/CLOSED, що й у Debtor
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false,
    val linkedUserId: String? = null,    // auth.uid() зареєстрованого користувача, знайденого за phone/email
    val mirrorDebtorId: String? = null,  // id дзеркального рядка в акаунті linkedUserId
    /** Очікувана дата+час виплати боргу; `null` — не задано. Джерело нагадувань (core/notifications/DueReminderCoordinator). */
    val dueDate: kotlin.time.Instant? = null,
    /** Які додаткові нагадування-«за N днів» увімкнено (значення 1/2). Нагадування «того дня» — завжди, поки задано [dueDate]. */
    val reminderLeadDays: Set<Int> = emptySet(),
)

data class CreditorTransaction(
    val id: String,
    val creditorId: String,
    val amount: BigDecimal,       // ЗІ ЗНАКОМ: додатне (+) = я повернув (RETURN), від'ємне (−) = я взяв у борг (BORROW).
    val type: MyDebtTransactionType, // type = if (amount.isPositive) RETURN else BORROW
    val method: PaymentMethod,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false,
    val mirrorTransactionId: String? = null, // заповнено лише якщо цей рядок сам є авто-дзеркалом транзакції з іншого акаунту
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
