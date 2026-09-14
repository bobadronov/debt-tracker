package org.bigblackowl.debttracker.domain.repository

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason

/**
 * Online-only access to the `notifications` table (no Room — notifications only make sense in
 * Account+Sync mode, same as debt mirroring itself, spec §7). No method throws an
 * exception — network errors are swallowed, returning an empty result/false.
 */
interface NotificationRepository {
    /** Notifications created after [after] (or all, if null), newest to oldest. */
    suspend fun fetchSince(after: kotlin.time.Instant?): List<AppNotification>
    /** Full history for the "Notifications" screen, newest to oldest. */
    suspend fun fetchAll(): List<AppNotification>
    suspend fun unreadCount(): Int
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun delete(id: String)
    /** RPC `approve_link_request` (0013) — performs the mirroring that was waiting on the target's consent. */
    suspend fun approveLinkRequest(requestId: String): Boolean
    /** RPC `reject_link_request` (0013) — rejects the pending request without mirroring. */
    suspend fun rejectLinkRequest(requestId: String): Boolean

    /**
     * RPC `propose_transaction_correction` (0014) — the recipient of a *_TRANSACTION_ADDED proposes
     * a fix to the author. [amount] — magnitude of the new amount, only needed for [CorrectionReason.WRONG_AMOUNT].
     */
    suspend fun proposeTransactionCorrection(
        notificationId: String,
        reason: CorrectionReason,
        amount: BigDecimal?,
    ): Boolean
    /** RPC `approve_transaction_correction` (0014) — the author accepts the fix, the change is mirrored back. */
    suspend fun approveTransactionCorrection(correctionId: String): Boolean
    /** RPC `reject_transaction_correction` (0014) — the author rejects the fix, the proposer is notified. */
    suspend fun rejectTransactionCorrection(correctionId: String): Boolean
}
