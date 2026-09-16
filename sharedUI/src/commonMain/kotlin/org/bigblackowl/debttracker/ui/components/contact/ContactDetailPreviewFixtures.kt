package org.bigblackowl.debttracker.ui.components.contact

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import kotlin.time.Clock
import kotlin.time.Instant

/** Sample data shared by [ContactDetailScaffold]'s and [ContactDetailContent]'s own previews. */
internal data class SampleTransaction(
    val id: String,
    val amount: BigDecimal,
    val method: PaymentMethod,
    val comment: String?,
    val createdAt: Instant,
)

internal val sampleTransactions = listOf(
    SampleTransaction("1", BigDecimal.parseString("500"), PaymentMethod.CASH, "Repaid half", Clock.System.now()),
    SampleTransaction("2", BigDecimal.parseString("-1200"), PaymentMethod.CARD, null, Clock.System.now()),
)
