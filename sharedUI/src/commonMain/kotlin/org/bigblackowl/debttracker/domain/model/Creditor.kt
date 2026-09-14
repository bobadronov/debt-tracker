package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * Creditor: a person I owe money to (mirror of [Debtor], spec §4.1).
 * A separate entity by design — a contact lives in exactly one of the two lists
 * at a time, there's no automatic netting against [Debtor].
 */
data class Creditor(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,           // if it matches a registered user's profile — source of autofill (§ProfileLookup)
    val avatarUrl: String?,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val status: DebtStatus,       // the same ACTIVE/CLOSED enum as in Debtor
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false,
    val linkedUserId: String? = null,    // auth.uid() of the registered user found by phone/email
    val mirrorDebtorId: String? = null,  // id of the mirrored row in the linkedUserId account
    /** Expected debt repayment date+time; `null` — not set. Source for reminders (core/notifications/DueReminderCoordinator). */
    val dueDate: kotlin.time.Instant? = null,
    /** Which extra "N days before" reminders are enabled (values 1/2). The "on the day" reminder is always on while [dueDate] is set. */
    val reminderLeadDays: Set<Int> = emptySet(),
)

data class CreditorTransaction(
    val id: String,
    val creditorId: String,
    val amount: BigDecimal,       // SIGNED: positive (+) = I paid back (RETURN), negative (−) = I borrowed (BORROW).
    val type: MyDebtTransactionType, // type = if (amount.isPositive) RETURN else BORROW
    val method: PaymentMethod,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false,
    val mirrorTransactionId: String? = null, // filled in only if this row is itself an auto-mirror of a transaction from another account
)

/** balance = -Σ(amount): how much I still owe this person. The same formula as in [debtorBalance]. */
fun List<CreditorTransaction>.creditorBalance(): BigDecimal =
    this.filterNot { it.isDeleted }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }.negate()

/** type is derived from the amount's sign: signum() > 0 → RETURN (I paid back), otherwise BORROW (I borrowed). */
fun BigDecimal.toCreditorTransactionType(): MyDebtTransactionType =
    if (this.signum() > 0) MyDebtTransactionType.RETURN else MyDebtTransactionType.BORROW

/** Projection for CreditorListScreen: creditor + computed balance (spec §4.1, §6, item 3). */
data class CreditorWithBalance(
    val creditor: Creditor,
    val balance: BigDecimal
)
