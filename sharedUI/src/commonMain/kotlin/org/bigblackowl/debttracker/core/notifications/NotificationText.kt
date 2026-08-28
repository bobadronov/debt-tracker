package org.bigblackowl.debttracker.core.notifications

import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.NotificationType

/**
 * Локалізований текст сповіщення — спільний для [NotificationsPoller] (показ системного
 * сповіщення) і [org.bigblackowl.debttracker.ui.screens.notifications.NotificationsScreen]
 * (історія), щоб формулювання не розходилось між ними.
 */
fun AppNotification.formatBody(strings: Strings): String {
    val name = actorDisplayName ?: "—"
    val amountText = amount?.toStringExpanded() ?: "?"
    val currencyText = currency?.name.orEmpty()
    return when (type) {
        NotificationType.DEBTOR_LINKED -> strings.notificationBodyDebtorLinked(name, amountText, currencyText)
        NotificationType.CREDITOR_LINKED -> strings.notificationBodyCreditorLinked(name, amountText, currencyText)
        NotificationType.DEBT_TRANSACTION_ADDED -> strings.notificationBodyDebtTransactionAdded(name, amountText, currencyText)
        NotificationType.CREDIT_TRANSACTION_ADDED -> strings.notificationBodyCreditTransactionAdded(name, amountText, currencyText)
    }
}
