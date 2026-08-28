package org.bigblackowl.debttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.SyncStatus

/** Дзеркало [DebtorEntity] для напрямку "Я винен" (спек §4.1). */
@Entity(tableName = "creditors")
data class CreditorEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val avatarUrl: String?,
    val comment: String?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
    val status: DebtStatus,
    val syncStatus: SyncStatus,
    val currency: Currency = Currency.UAH,
    val isDeleted: Boolean = false,
    val linkedUserId: String? = null,
    val mirrorDebtorId: String? = null,
)
