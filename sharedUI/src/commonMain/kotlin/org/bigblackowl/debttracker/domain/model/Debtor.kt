package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Боржник: людина, якій я дав у борг (мені винні). */
data class Debtor(
    val id: String,               // UUID, генерується локально (для offline-first)
    val fullName: String,
    val phone: String?,
    val email: String?,           // якщо збігається з профілем зареєстрованого користувача — джерело автозаповнення (§ProfileLookup)
    val avatarUrl: String?,       // null → ініціали на кольоровому фоні (детермінований колір з hash(id))
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val status: DebtStatus,
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false, // soft delete для синхронізації
    val linkedUserId: String? = null,      // auth.uid() зареєстрованого користувача, знайденого за phone/email
    val mirrorCreditorId: String? = null,  // id дзеркального рядка в акаунті linkedUserId
    /** Очікувана дата+час повернення боргу; `null` — не задано. Джерело нагадувань (core/notifications/DueReminderCoordinator). */
    val dueDate: kotlin.time.Instant? = null,
    /** Які додаткові нагадування-«за N днів» увімкнено (значення 1/2). Нагадування «того дня» — завжди, поки задано [dueDate]. */
    val reminderLeadDays: Set<Int> = emptySet(),
)

data class DebtTransaction(
    val id: String,
    val debtorId: String,
    val amount: BigDecimal,       // ЗІ ЗНАКОМ: додатне (+) = мені повернули (REPAY), від'ємне (−) = я дав у борг (LEND).
    val type: TransactionType,    // денормалізовано для зручності запитів/індексів; type = if (amount.isPositive) REPAY else LEND
    val method: PaymentMethod,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false,
    val mirrorTransactionId: String? = null, // заповнено лише якщо цей рядок сам є авто-дзеркалом транзакції з іншого акаунту
)

/** balance = -Σ(amount): скільки боржник ще винен мені. */
fun List<DebtTransaction>.debtorBalance(): BigDecimal =
    this.filterNot { it.isDeleted }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.negate()

fun BigDecimal.toDebtStatus(): DebtStatus =
    if (this <= BigDecimal.ZERO) DebtStatus.CLOSED else DebtStatus.ACTIVE

/** type виводиться зі знаку суми: signum() > 0 → REPAY (мені повернули), інакше LEND (я дав). */
fun BigDecimal.toDebtTransactionType(): TransactionType =
    if (this.signum() > 0) TransactionType.REPAY else TransactionType.LEND

/** Проєкція для DebtorListScreen: боржник + обчислений баланс (спек §6, п.3). */
data class DebtorWithBalance(
    val debtor: Debtor,
    val balance: BigDecimal
)
