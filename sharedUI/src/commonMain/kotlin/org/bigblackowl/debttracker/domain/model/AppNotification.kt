package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Тип події з таблиці `notifications` (спек §7, міграції 0007/0013) — мірор `type` check-constraint. */
enum class NotificationType {
    DEBTOR_LINKED, CREDITOR_LINKED, DEBT_TRANSACTION_ADDED, CREDIT_TRANSACTION_ADDED,
    /** Телефон-матч (0013, B3-фікс) чекає на approve_link_request/reject_link_request цілі. */
    LINK_REQUEST,
    /** Ціль підтвердила [LINK_REQUEST] — надсилається назад вимагачу. */
    LINK_REQUEST_APPROVED,
}

/**
 * Сповіщення про прив'язку/нову транзакцію в дзеркальному борзі — рядок з таблиці
 * `notifications`, опитується [org.bigblackowl.debttracker.core.notifications.NotificationsPoller]
 * кожні 15с. Онлайн-only сутність (без Room), як і саме дзеркалювання.
 */
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorDisplayName: String?,
    val relatedDebtorId: String?,
    val relatedCreditorId: String?,
    /** Ненульове для [NotificationType.LINK_REQUEST]/[NotificationType.LINK_REQUEST_APPROVED] — id рядка в pending_link_requests. */
    val relatedLinkRequestId: String?,
    val amount: BigDecimal?,
    val currency: Currency?,
    val isRead: Boolean,
    val createdAt: kotlin.time.Instant,
)
