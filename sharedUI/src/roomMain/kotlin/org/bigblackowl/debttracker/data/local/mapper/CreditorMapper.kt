package org.bigblackowl.debttracker.data.local.mapper

import org.bigblackowl.debttracker.data.local.entity.CreditorEntity
import org.bigblackowl.debttracker.data.local.entity.CreditorTransactionEntity
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction

/** [CreditorEntity]/[CreditorTransactionEntity] ↔ domain-model mapping — mirrors [org.bigblackowl.debttracker.data.local.mapper.DebtorMapper]. */
fun CreditorEntity.toDomain() = Creditor(
    id = id,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    syncStatus = syncStatus,
    currency = currency,
    isDeleted = isDeleted,
)

fun Creditor.toEntity() = CreditorEntity(
    id = id,
    fullName = fullName,
    phone = phone,
    email = email,
    avatarUrl = avatarUrl,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    syncStatus = syncStatus,
    currency = currency,
    isDeleted = isDeleted,
)

fun CreditorTransactionEntity.toDomain() = CreditorTransaction(
    id = id,
    creditorId = creditorId,
    amount = amount,
    type = type,
    method = method,
    cardLastDigits = cardLastDigits,
    date = date,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    isDeleted = isDeleted,
)

fun CreditorTransaction.toEntity() = CreditorTransactionEntity(
    id = id,
    creditorId = creditorId,
    amount = amount,
    type = type,
    method = method,
    cardLastDigits = cardLastDigits,
    date = date,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    isDeleted = isDeleted,
)
