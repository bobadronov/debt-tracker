package org.bigblackowl.debttracker.data.local.mapper

import org.bigblackowl.debttracker.data.local.entity.DebtTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtorEntity
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtTransaction

/** [DebtorEntity]/[DebtTransactionEntity] ↔ domain-model mapping. */
fun DebtorEntity.toDomain() = Debtor(
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

fun Debtor.toEntity() = DebtorEntity(
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

fun DebtTransactionEntity.toDomain() = DebtTransaction(
    id = id,
    debtorId = debtorId,
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

fun DebtTransaction.toEntity() = DebtTransactionEntity(
    id = id,
    debtorId = debtorId,
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
