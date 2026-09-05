package org.bigblackowl.debttracker.core.notifications

import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.NotificationType

/**
 * Локалізований текст сповіщення — спільний для [NotificationsPoller] (показ системного
 * сповіщення) і [org.bigblackowl.debttracker.ui.screens.notifications.NotificationsScreen]
 * (історія), щоб формулювання не розходилось між ними.
 *
 * @param redactAmount When true (Settings → "hide amounts in notifications"), returns a generic
 * body instead of the name/amount detail — used only for the OS notification
 * ([NotificationsPoller]), never for the in-app history screen.
 */
fun AppNotification.formatBody(strings: Strings, redactAmount: Boolean = false): String {
    if (redactAmount) return strings.notificationBody.genericBody
    val name = actorDisplayName ?: "—"
    val amountText = amount?.toStringExpanded() ?: "?"
    val currencyText = currency?.name.orEmpty()
    return when (type) {
        NotificationType.DEBTOR_LINKED -> strings.notificationBody.debtorLinked(name, amountText, currencyText)
        NotificationType.CREDITOR_LINKED -> strings.notificationBody.creditorLinked(name, amountText, currencyText)
        NotificationType.DEBT_TRANSACTION_ADDED -> strings.notificationBody.debtTransactionAdded(name, amountText, currencyText)
        NotificationType.CREDIT_TRANSACTION_ADDED -> strings.notificationBody.creditTransactionAdded(name, amountText, currencyText)
        NotificationType.LINK_REQUEST -> strings.notificationBody.linkRequestReceived(name)
        NotificationType.LINK_REQUEST_APPROVED -> strings.notificationBody.linkRequestApproved(name)
    }
}
