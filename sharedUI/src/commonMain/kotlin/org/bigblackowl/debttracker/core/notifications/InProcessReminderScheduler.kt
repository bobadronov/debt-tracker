package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Desktop / Web [ReminderScheduler]: a coroutine per pending reminder that [delay]s until its
 * fire time and then posts via [LocalNotifier]. There is no OS-level persistence — reminders
 * only fire while the app / page is running (documented limitation; the debt itself is still in
 * the list, so a missed reminder just means the user opens the app and sees it there).
 *
 * [sync] is only ever called from [DueReminderCoordinator]'s single collector coroutine, so the
 * `jobs` map is mutated serially and needs no extra locking.
 */
class InProcessReminderScheduler(
    private val scope: CoroutineScope,
    private val notifier: LocalNotifier,
) : ReminderScheduler {

    private var jobs: Map<String, Job> = emptyMap()

    override fun sync(reminders: List<ScheduledReminder>) {
        val now = Clock.System.now().toEpochMilliseconds()
        val wanted = reminders.filter { it.atEpochMillis > now }.associateBy { it.key }

        val next = HashMap<String, Job>()
        // Keep timers that are still wanted and haven't fired; cancel the rest.
        jobs.forEach { (key, job) ->
            if (key in wanted && job.isActive) next[key] = job else job.cancel()
        }
        wanted.forEach { (key, reminder) ->
            if (key in next) return@forEach
            next[key] = scope.launch {
                delay((reminder.atEpochMillis - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0))
                runCatching { notifier.notify(reminder.title, reminder.body, reminder.deepLink) }
            }
        }
        jobs = next
    }
}
