package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Event type from the `notifications` table (spec §7, migrations 0007/0013/0014) — mirrors the `type` check constraint. */
enum class NotificationType {
    DEBTOR_LINKED, CREDITOR_LINKED, DEBT_TRANSACTION_ADDED, CREDIT_TRANSACTION_ADDED,
    /** A phone match (0013, B3 fix) waiting on the target's approve_link_request/reject_link_request. */
    LINK_REQUEST,
    /** The target approved [LINK_REQUEST] — sent back to the requester. */
    LINK_REQUEST_APPROVED,
    /** The recipient of a *_TRANSACTION_ADDED proposes an amount fix / cancellation to the author (0014) — waits on approve/reject_transaction_correction. */
    TRANSACTION_CORRECTION,
    /** The author accepted [TRANSACTION_CORRECTION] — sent back to the proposer. */
    TRANSACTION_CORRECTION_APPROVED,
    /** The author rejected [TRANSACTION_CORRECTION] — sent back to the proposer. */
    TRANSACTION_CORRECTION_REJECTED,
}

/**
 * Notification about a link / a new transaction in a mirrored debt — a row from the
 * `notifications` table, polled by [org.bigblackowl.debttracker.core.notifications.NotificationsPoller]
 * every 15s. An online-only entity (no Room), same as the mirroring itself.
 */
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorDisplayName: String?,
    val relatedDebtorId: String?,
    val relatedCreditorId: String?,
    /** Non-null for [NotificationType.LINK_REQUEST]/[NotificationType.LINK_REQUEST_APPROVED] — id of the row in pending_link_requests. */
    val relatedLinkRequestId: String?,
    /** Non-null for [NotificationType.DEBT_TRANSACTION_ADDED]/[NotificationType.CREDIT_TRANSACTION_ADDED] — id of the author's outgoing operation that can be corrected (0014). */
    val relatedTransactionId: String?,
    /** Non-null for the TRANSACTION_CORRECTION* types — id of the row in transaction_corrections (0014). */
    val relatedCorrectionId: String?,
    val amount: BigDecimal?,
    val currency: Currency?,
    val isRead: Boolean,
    val createdAt: kotlin.time.Instant,
)

/** Reason for rejecting a mirrored operation (0014) — mirrors `transaction_corrections.reason`. */
enum class CorrectionReason {
    /** The amount is wrong — the proposer supplies the new one (magnitude). */
    WRONG_AMOUNT,
    /** The operation didn't happen — a proposal to cancel it (soft-delete). */
    NOT_HAPPENED,
    ;

    val wire: String get() = when (this) {
        WRONG_AMOUNT -> "wrong_amount"
        NOT_HAPPENED -> "not_happened"
    }
}
