package org.bigblackowl.debttracker.ui.screens.notifications

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason

/** MVI contract for [NotificationsScreen] — історія сповіщень про дзеркальні борги (спек §7). */
data class NotificationsState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    /** Ненульове — відкрито діалог «відхилити операцію» для цього сповіщення (0014). */
    val correctionDialogFor: AppNotification? = null,
)

sealed interface NotificationsIntent {
    data class Open(val notification: AppNotification) : NotificationsIntent
    data class Delete(val id: String) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
    /** [org.bigblackowl.debttracker.domain.model.NotificationType.LINK_REQUEST] row actions (0013, B3-фікс). */
    data class ApproveLinkRequest(val notificationId: String, val requestId: String) : NotificationsIntent
    data class RejectLinkRequest(val notificationId: String, val requestId: String) : NotificationsIntent

    /** *_TRANSACTION_ADDED row: пропозиція правки суми / скасування операції авторові (0014). */
    data class OpenCorrectionDialog(val notification: AppNotification) : NotificationsIntent
    data object DismissCorrectionDialog : NotificationsIntent
    data class SubmitCorrection(
        val notificationId: String,
        val reason: CorrectionReason,
        /** Модуль нової суми — лише для [CorrectionReason.WRONG_AMOUNT]. */
        val amount: BigDecimal?,
    ) : NotificationsIntent

    /** [org.bigblackowl.debttracker.domain.model.NotificationType.TRANSACTION_CORRECTION] row actions (автор) (0014). */
    data class ApproveCorrection(val notificationId: String, val correctionId: String) : NotificationsIntent
    data class RejectCorrection(val notificationId: String, val correctionId: String) : NotificationsIntent
}

sealed interface NotificationsEffect {
    data class NavigateToDebtor(val debtorId: String) : NotificationsEffect
    data class NavigateToCreditor(val creditorId: String) : NotificationsEffect
}
