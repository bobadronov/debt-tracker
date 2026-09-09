package org.bigblackowl.debttracker.domain.repository

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason

/**
 * Онлайн-only доступ до таблиці `notifications` (без Room — сповіщення мають сенс лише в
 * Account+Sync режимі, як і саме дзеркалювання боргів, спек §7). Жоден метод не кидає
 * виняток — мережеві помилки ковтаються, повертаючи порожній результат/false.
 */
interface NotificationRepository {
    /** Сповіщення, створені пізніше за [after] (або всі, якщо null), від найновіших до найстаріших. */
    suspend fun fetchSince(after: kotlin.time.Instant?): List<AppNotification>
    /** Повна історія для екрана "Сповіщення", від найновіших до найстаріших. */
    suspend fun fetchAll(): List<AppNotification>
    suspend fun unreadCount(): Int
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun delete(id: String)
    /** RPC `approve_link_request` (0013) — виконує дзеркалювання, яке чекало на згоду цілі. */
    suspend fun approveLinkRequest(requestId: String): Boolean
    /** RPC `reject_link_request` (0013) — відхиляє pending-запит без дзеркалювання. */
    suspend fun rejectLinkRequest(requestId: String): Boolean

    /**
     * RPC `propose_transaction_correction` (0014) — отримувач *_TRANSACTION_ADDED пропонує авторові
     * правку. [amount] — модуль нової суми, потрібен лише для [CorrectionReason.WRONG_AMOUNT].
     */
    suspend fun proposeTransactionCorrection(
        notificationId: String,
        reason: CorrectionReason,
        amount: BigDecimal?,
    ): Boolean
    /** RPC `approve_transaction_correction` (0014) — автор приймає правку, зміна дзеркалиться назад. */
    suspend fun approveTransactionCorrection(correctionId: String): Boolean
    /** RPC `reject_transaction_correction` (0014) — автор відхиляє правку, пропонувача сповіщено. */
    suspend fun rejectTransactionCorrection(correctionId: String): Boolean
}
