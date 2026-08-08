package org.bigblackowl.debttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.TransactionType

/** Room row for a lend/repay transaction against a [DebtorEntity]; `amount` sign encodes `type`. */
@Entity(
    tableName = "debt_transactions",
    foreignKeys = [
        ForeignKey(
            entity = DebtorEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtorId"), Index("debtorId", "isDeleted", "date")]
)
data class DebtTransactionEntity(
    @PrimaryKey val id: String,
    val debtorId: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val method: PaymentMethod,
    val cardLastDigits: String?,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false
)
