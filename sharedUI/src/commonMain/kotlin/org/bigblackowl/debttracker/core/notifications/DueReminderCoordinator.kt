package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.formatDueDateTime
import org.bigblackowl.debttracker.domain.model.formatDueTime
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Keeps local due-date reminders in sync with the debtor/creditor lists (спек — нагадування про
 * повернення боргу). Whenever the lists change it recomputes the full set of pending reminders —
 * one "on the day" reminder for every active contact with a [Debtor.dueDate], plus one for each
 * user-chosen lead day (1 / 2 days before) — and hands it to the platform [ReminderScheduler].
 *
 * Runs regardless of sign-in (due dates are plain local data), started once per process next to
 * [NotificationsPoller.start].
 */
class DueReminderCoordinator(
    private val scope: CoroutineScope,
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
    private val scheduler: ReminderScheduler,
    private val localNotifier: LocalNotifier,
    private val appSettings: AppSettings,
) {
    fun start() {
        scope.launch {
            if (appSettings.notificationsEnabled) runCatching { localNotifier.requestPermission() }
            combine(
                debtorRepository.observeDebtors(),
                creditorRepository.observeCreditors(),
            ) { debtors, creditors -> debtors to creditors }
                .collectLatest { (debtors, creditors) ->
                    val reminders = if (appSettings.notificationsEnabled) {
                        buildReminders(
                            debtors = debtors.map { it.debtor },
                            creditors = creditors.map { it.creditor },
                            strings = resolveStrings(appSettings.locale),
                        )
                    } else {
                        emptyList()
                    }
                    runCatching { scheduler.sync(reminders) }
                }
        }
    }

    companion object {
        /** Pure so it can be unit-tested without a scheduler or a composition. */
        internal fun buildReminders(
            debtors: List<Debtor>,
            creditors: List<Creditor>,
            strings: Strings,
            now: Instant = Clock.System.now(),
        ): List<ScheduledReminder> = buildList {
            debtors.forEach { d ->
                if (d.isDeleted || d.status != DebtStatus.ACTIVE) return@forEach
                val due = d.dueDate ?: return@forEach
                addAll(
                    remindersFor(
                        kind = "debtor", id = d.id, due = due, leadDays = d.reminderLeadDays,
                        now = now, strings = strings,
                        body = { whenText -> strings.dueReminder.debtorBody(d.fullName, whenText) },
                        deepLink = NotificationDeepLinks.reminderLink(debtorId = d.id),
                    ),
                )
            }
            creditors.forEach { c ->
                if (c.isDeleted || c.status != DebtStatus.ACTIVE) return@forEach
                val due = c.dueDate ?: return@forEach
                addAll(
                    remindersFor(
                        kind = "creditor", id = c.id, due = due, leadDays = c.reminderLeadDays,
                        now = now, strings = strings,
                        body = { whenText -> strings.dueReminder.creditorBody(c.fullName, whenText) },
                        deepLink = NotificationDeepLinks.reminderLink(creditorId = c.id),
                    ),
                )
            }
        }

        private fun remindersFor(
            kind: String,
            id: String,
            due: Instant,
            leadDays: Set<Int>,
            now: Instant,
            strings: Strings,
            body: (whenText: String) -> String,
            deepLink: String,
        ): List<ScheduledReminder> {
            // On-the-day (0) is always scheduled; lead days are opt-in ("сьогодні обовязково").
            val offsets = (listOf(0) + leadDays.filter { it > 0 }).distinct()
            return offsets.mapNotNull { daysBefore ->
                val at = due - daysBefore.days
                if (at <= now) return@mapNotNull null
                val whenText = if (daysBefore == 0) {
                    strings.dueReminder.whenToday(due.formatDueTime())
                } else {
                    due.formatDueDateTime()
                }
                ScheduledReminder(
                    key = "$kind:$id:$daysBefore",
                    atEpochMillis = at.toEpochMilliseconds(),
                    title = strings.dueReminder.label,
                    body = body(whenText),
                    deepLink = deepLink,
                )
            }
        }
    }
}
