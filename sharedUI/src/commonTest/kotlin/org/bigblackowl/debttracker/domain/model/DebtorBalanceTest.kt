package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class DebtorBalanceTest {

    private val now = Clock.System.now()

    private fun tx(amount: String, isDeleted: Boolean = false, id: String = "tx") = DebtTransaction(
        id = id,
        debtorId = "debtor-1",
        amount = BigDecimal.parseString(amount),
        type = BigDecimal.parseString(amount).toDebtTransactionType(),
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
        // -1500 (lent) + 500 (repaid) => sum = -1000, balance = -sum = 1000 still owed to me
        val transactions = listOf(tx("-1500"), tx("500"))
        assertEquals(0, BigDecimal.parseString("1000").compareTo(transactions.debtorBalance()))
    }

    @Test
    fun `fully repaid debtor has zero balance`() {
        val transactions = listOf(tx("-1500"), tx("1500"))
        assertEquals(0, BigDecimal.ZERO.compareTo(transactions.debtorBalance()))
    }

    @Test
    fun `empty transaction list yields zero balance`() {
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyList<DebtTransaction>().debtorBalance()))
    }

    @Test
    fun `deleted transactions are excluded from balance`() {
        val transactions = listOf(tx("-1500"), tx("-800", isDeleted = true, id = "deleted-tx"))
        assertEquals(0, BigDecimal.parseString("1500").compareTo(transactions.debtorBalance()))
    }

    @Test
    fun `positive balance maps to ACTIVE status`() {
        assertEquals(DebtStatus.ACTIVE, BigDecimal.parseString("100").toDebtStatus())
    }

    @Test
    fun `zero balance maps to CLOSED status`() {
        assertEquals(DebtStatus.CLOSED, BigDecimal.ZERO.toDebtStatus())
    }

    @Test
    fun `negative balance maps to CLOSED status`() {
        assertEquals(DebtStatus.CLOSED, BigDecimal.parseString("-50").toDebtStatus())
    }

    @Test
    fun `positive amount is a REPAY transaction`() {
        assertEquals(TransactionType.REPAY, BigDecimal.parseString("500").toDebtTransactionType())
    }

    @Test
    fun `negative amount is a LEND transaction`() {
        assertEquals(TransactionType.LEND, BigDecimal.parseString("-500").toDebtTransactionType())
    }

    @Test
    fun `zero amount is classified as LEND not REPAY`() {
        // signum() > 0 is required for REPAY; zero does not satisfy that, so it falls through to LEND.
        assertEquals(TransactionType.LEND, BigDecimal.ZERO.toDebtTransactionType())
    }
}
