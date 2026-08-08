package org.bigblackowl.debttracker.data.sync

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.data.local.entity.CreditorEntity
import org.bigblackowl.debttracker.data.local.entity.CreditorTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtorEntity
import org.bigblackowl.debttracker.data.remote.dto.CreditorDto
import org.bigblackowl.debttracker.data.remote.dto.CreditorTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtorDto
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.TransactionType
import org.bigblackowl.debttracker.domain.model.toCreditorTransactionType
import org.bigblackowl.debttracker.domain.model.toDebtTransactionType

/**
 * Room [entity][org.bigblackowl.debttracker.data.local.entity] ↔ Supabase
 * [DTO][org.bigblackowl.debttracker.data.remote.dto] mapping, used by [SyncCoordinator]. Local
 * rows pulled from the server are always tagged [SyncStatus.SYNCED] — sync direction is
 * one-way per call, never round-tripped through this mapper.
 */
fun DebtorEntity.toDto(userId: String) = DebtorDto(
    id = id,
    userId = userId,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    status = status.name,
    currency = currency.name,
    isDeleted = isDeleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun DebtorDto.toEntity() = DebtorEntity(
    id = id,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    createdAt = kotlin.time.Instant.parse(createdAt),
    updatedAt = kotlin.time.Instant.parse(updatedAt),
    status = DebtStatus.valueOf(status),
    syncStatus = SyncStatus.SYNCED,
    currency = Currency.valueOf(currency),
    isDeleted = isDeleted,
)

fun DebtTransactionEntity.toDto(userId: String) = DebtTransactionDto(
    id = id,
    debtorId = debtorId,
    userId = userId,
    amount = amount.toStringExpanded().toDouble(),
    method = method.name,
    cardLastDigits = cardLastDigits,
    transactionDate = date.toString(),
    comment = comment,
    isDeleted = isDeleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun DebtTransactionDto.toEntity(): DebtTransactionEntity {
    val parsedAmount = BigDecimal.parseString(amount.toString())
    return DebtTransactionEntity(
        id = id,
        debtorId = debtorId,
        amount = parsedAmount,
        type = parsedAmount.toDebtTransactionType(),
        method = PaymentMethod.valueOf(method),
        cardLastDigits = cardLastDigits,
        date = kotlin.time.Instant.parse(transactionDate),
        comment = comment,
        createdAt = kotlin.time.Instant.parse(createdAt),
        updatedAt = kotlin.time.Instant.parse(updatedAt),
        syncStatus = SyncStatus.SYNCED,
        isDeleted = isDeleted,
    )
}

fun CreditorEntity.toDto(userId: String) = CreditorDto(
    id = id,
    userId = userId,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    status = status.name,
    currency = currency.name,
    isDeleted = isDeleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CreditorDto.toEntity() = CreditorEntity(
    id = id,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    createdAt = kotlin.time.Instant.parse(createdAt),
    updatedAt = kotlin.time.Instant.parse(updatedAt),
    status = DebtStatus.valueOf(status),
    syncStatus = SyncStatus.SYNCED,
    currency = Currency.valueOf(currency),
    isDeleted = isDeleted,
)

fun CreditorTransactionEntity.toDto(userId: String) = CreditorTransactionDto(
    id = id,
    creditorId = creditorId,
    userId = userId,
    amount = amount.toStringExpanded().toDouble(),
    method = method.name,
    cardLastDigits = cardLastDigits,
    transactionDate = date.toString(),
    comment = comment,
    isDeleted = isDeleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CreditorTransactionDto.toEntity(): CreditorTransactionEntity {
    val parsedAmount = BigDecimal.parseString(amount.toString())
    return CreditorTransactionEntity(
        id = id,
        creditorId = creditorId,
        amount = parsedAmount,
        type = parsedAmount.toCreditorTransactionType(),
        method = PaymentMethod.valueOf(method),
        cardLastDigits = cardLastDigits,
        date = kotlin.time.Instant.parse(transactionDate),
        comment = comment,
        createdAt = kotlin.time.Instant.parse(createdAt),
        updatedAt = kotlin.time.Instant.parse(updatedAt),
        syncStatus = SyncStatus.SYNCED,
        isDeleted = isDeleted,
    )
}
