package org.bigblackowl.debttracker.core.notifications

import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DueReminderCoordinatorTest {

    private val strings = resolveStrings("en")
    private val now = Instant.parse("2026-09-01T10:00:00Z")

    private fun debtor(
        due: Instant?,
        leadDays: Set<Int> = emptySet(),
        status: DebtStatus = DebtStatus.ACTIVE,
        deleted: Boolean = false,
    ) = Debtor(
        id = "d1", fullName = "Sam Lee", phone = null, email = null, avatarUrl = null, comment = null,
        createdAt = now, updatedAt = now, status = status, syncStatus = SyncStatus.SYNCED,
        isDeleted = deleted, dueDate = due, reminderLeadDays = leadDays,
    )

    @Test
    fun `no due date - no reminders`() {
        val out = DueReminderCoordinator.buildReminders(listOf(debtor(due = null)), emptyList(), strings, now)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `on-the-day reminder is always scheduled`() {
        val due = now + 5.days
        val out = DueReminderCoordinator.buildReminders(listOf(debtor(due = due)), emptyList(), strings, now)
        assertEquals(listOf("debtor:d1:0"), out.map { it.key })
        assertEquals(due.toEpochMilliseconds(), out.single().atEpochMillis)
    }

    @Test
    fun `lead days add extra reminders fired N days earlier`() {
        val due = now + 10.days
        val out = DueReminderCoordinator.buildReminders(
            listOf(debtor(due = due, leadDays = setOf(1, 2))), emptyList(), strings, now,
        )
        assertEquals(setOf("debtor:d1:0", "debtor:d1:1", "debtor:d1:2"), out.map { it.key }.toSet())
        assertEquals((due - 2.days).toEpochMilliseconds(), out.first { it.key == "debtor:d1:2" }.atEpochMillis)
    }

    @Test
    fun `reminders already in the past are dropped`() {
        val due = now + 12.hours
        val out = DueReminderCoordinator.buildReminders(
            listOf(debtor(due = due, leadDays = setOf(1, 2))), emptyList(), strings, now,
        )
        // due is <2 days out, so the 1- and 2-day-before instants are already past — only on-the-day survives.
        assertEquals(listOf("debtor:d1:0"), out.map { it.key })
    }

    @Test
    fun `closed or deleted debtors are skipped`() {
        val due = now + 5.days
        val closed = DueReminderCoordinator.buildReminders(listOf(debtor(due = due, status = DebtStatus.CLOSED)), emptyList(), strings, now)
        val deleted = DueReminderCoordinator.buildReminders(listOf(debtor(due = due, deleted = true)), emptyList(), strings, now)
        assertTrue(closed.isEmpty())
        assertTrue(deleted.isEmpty())
    }
}
