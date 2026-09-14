package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Debtor: a person I lent money to (they owe me). */
data class Debtor(
    val id: String,               // UUID, generated locally (for offline-first)
    val fullName: String,
    val phone: String?,
    val email: String?,           // if it matches a registered user's profile — source of autofill (§ProfileLookup)
    val avatarUrl: String?,       // null → initials on a colored background (deterministic color from hash(id))
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val status: DebtStatus,
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false, // soft delete for sync
    val linkedUserId: String? = null,      // auth.uid() of the registered user found by phone/email
    val mirrorCreditorId: String? = null,  // id of the mirrored row in the linkedUserId account
    /** Expected debt repayment date+time; `null` — not set. Source for reminders (core/notifications/DueReminderCoordinator). */
    val dueDate: kotlin.time.Instant? = null,
    /** Which extra "N days before" reminders are enabled (values 1/2). The "on the day" reminder is always on while [dueDate] is set. */
    val reminderLeadDays: Set<Int> = emptySet(),
)

data class DebtTransaction(
    val id: String,
    val debtorId: String,
    val amount: BigDecimal,       // SIGNED: positive (+) = repaid to me (REPAY), negative (−) = I lent (LEND).
    val type: TransactionType,    // denormalized for query/index convenience; type = if (amount.isPositive) REPAY else LEND
    val method: PaymentMethod,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false,
    val mirrorTransactionId: String? = null, // filled in only if this row is itself an auto-mirror of a transaction from another account
)

/** balance = -Σ(amount): how much the debtor still owes me. */
fun List<DebtTransaction>.debtorBalance(): BigDecimal =
    this.filterNot { it.isDeleted }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.negate()

fun BigDecimal.toDebtStatus(): DebtStatus =
    if (this <= BigDecimal.ZERO) DebtStatus.CLOSED else DebtStatus.ACTIVE

/** type is derived from the amount's sign: signum() > 0 → REPAY (repaid to me), otherwise LEND (I lent). */
fun BigDecimal.toDebtTransactionType(): TransactionType =
    if (this.signum() > 0) TransactionType.REPAY else TransactionType.LEND

/** Projection for DebtorListScreen: debtor + computed balance (spec §6, item 3). */
data class DebtorWithBalance(
    val debtor: Debtor,
    val balance: BigDecimal
)
