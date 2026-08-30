package org.bigblackowl.debttracker.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import org.bigblackowl.debttracker.preview.FakeCreditorRepository
import org.bigblackowl.debttracker.preview.FakeDebtorRepository

class ObserveContactSuggestionsUseCaseTest {

    private val now = Clock.System.now()

    private fun debtor(name: String, phone: String? = null, email: String? = null, avatar: String? = null) = Debtor(
        id = "d-$name", fullName = name, phone = phone, email = email, avatarUrl = avatar, comment = null,
        createdAt = now, updatedAt = now, status = DebtStatus.ACTIVE, syncStatus = SyncStatus.SYNCED,
    )

    private fun creditor(name: String, phone: String? = null, email: String? = null, avatar: String? = null) = Creditor(
        id = "c-$name", fullName = name, phone = phone, email = email, avatarUrl = avatar, comment = null,
        createdAt = now, updatedAt = now, status = DebtStatus.ACTIVE, syncStatus = SyncStatus.SYNCED,
    )

    private fun useCase(debtors: List<Debtor>, creditors: List<Creditor>) = ObserveContactSuggestionsUseCase(
        ObserveDebtorsUseCase(FakeDebtorRepository(seedDebtors = debtors, seedTransactions = emptyList())),
        ObserveCreditorsUseCase(FakeCreditorRepository(seedCreditors = creditors, seedTransactions = emptyList())),
    )

    @Test
    fun `merges debtors and creditors`() = runTest {
        val result = useCase(listOf(debtor("Alice")), listOf(creditor("Bob"))).invoke().first()
        assertEquals(setOf("Alice", "Bob"), result.map { it.fullName }.toSet())
    }

    @Test
    fun `same person on both sides is de-duplicated by name+phone+email`() = runTest {
        val result = useCase(
            listOf(debtor("Alice", phone = "0671112233")),
            listOf(creditor("Alice", phone = "0671112233")),
        ).invoke().first()
        assertEquals(1, result.count { it.fullName == "Alice" })
    }

    @Test
    fun `carries the avatar url through`() = runTest {
        val result = useCase(listOf(debtor("Alice", avatar = "https://x/a.png")), emptyList()).invoke().first()
        assertEquals("https://x/a.png", result.single().avatarUrl)
    }
}
