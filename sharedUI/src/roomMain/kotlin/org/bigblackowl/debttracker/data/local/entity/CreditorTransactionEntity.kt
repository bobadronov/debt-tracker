package org.bigblackowl.debttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus

/** Дзеркало [DebtTransactionEntity] для напрямку "Я винен" (спек §4.1). */
@Entity(
    tableName = "creditor_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CreditorEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creditorId"), Index("creditorId", "isDeleted", "date")]
)
data class CreditorTransactionEntity(
    @PrimaryKey val id: String,
    val creditorId: String,
    val amount: BigDecimal,
    val type: MyDebtTransactionType,
    val method: PaymentMethod,
    val date: kotlin.time.Instant,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val syncStatus: SyncStatus,
    val isDeleted: Boolean = false,
    val mirrorTransactionId: String? = null,
)
