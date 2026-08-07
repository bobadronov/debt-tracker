package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class CreditorBalanceTest {

    private val now = Clock.System.now()

    private fun tx(amount: String, isDeleted: Boolean = false, id: String = "tx") = CreditorTransaction(
        id = id,
        creditorId = "creditor-1",
        amount = BigDecimal.parseString(amount),
        type = BigDecimal.parseString(amount).toCreditorTransactionType(),
        method = PaymentMethod.CASH,
        cardLastDigits = null,
        date = now,
        comment = null,
        createdAt = now,
        updatedAt = now,
        syncStatus = SyncStatus.SYNCED,
        isDeleted = isDeleted,
    )

    @Test
    fun `balance is negated sum of signed amounts`() {
        // -3000 (borrowed) + 1000 (returned) => sum = -2000, balance = 2000 still owed by me
        val transactions = listOf(tx("-3000"), tx("1000"))
        assertEquals(0, BigDecimal.parseString("2000").compareTo(transactions.creditorBalance()))
    }

    @Test
    fun `fully returned debt has zero balance`() {
        val transactions = listOf(tx("-3000"), tx("3000"))
        assertEquals(0, BigDecimal.ZERO.compareTo(transactions.creditorBalance()))
    }

    @Test
    fun `deleted transactions are excluded from balance`() {
        val transactions = listOf(tx("-3000"), tx("-500", isDeleted = true, id = "deleted-tx"))
        assertEquals(0, BigDecimal.parseString("3000").compareTo(transactions.creditorBalance()))
    }

    @Test
    fun `positive amount is a RETURN transaction`() {
        assertEquals(MyDebtTransactionType.RETURN, BigDecimal.parseString("1000").toCreditorTransactionType())
    }

    @Test
    fun `negative amount is a BORROW transaction`() {
        assertEquals(MyDebtTransactionType.BORROW, BigDecimal.parseString("-1000").toCreditorTransactionType())
    }

    @Test
    fun `zero amount is classified as BORROW not RETURN`() {
        assertEquals(MyDebtTransactionType.BORROW, BigDecimal.ZERO.toCreditorTransactionType())
    }
}
