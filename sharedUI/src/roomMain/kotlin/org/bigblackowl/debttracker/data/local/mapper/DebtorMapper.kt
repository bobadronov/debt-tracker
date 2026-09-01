package org.bigblackowl.debttracker.data.local.mapper

import org.bigblackowl.debttracker.data.local.entity.DebtTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtorEntity
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.encodeReminderLeadDays
import org.bigblackowl.debttracker.domain.model.parseReminderLeadDays

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
    linkedUserId = linkedUserId,
    mirrorCreditorId = mirrorCreditorId,
    dueDate = dueDate,
    reminderLeadDays = parseReminderLeadDays(reminderLeadDays),
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
    linkedUserId = linkedUserId,
    mirrorCreditorId = mirrorCreditorId,
    dueDate = dueDate,
    reminderLeadDays = reminderLeadDays.encodeReminderLeadDays(),
)

fun DebtTransactionEntity.toDomain() = DebtTransaction(
    id = id,
    debtorId = debtorId,
    amount = amount,
    type = type,
    method = method,
    date = date,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    isDeleted = isDeleted,
    mirrorTransactionId = mirrorTransactionId,
)

fun DebtTransaction.toEntity() = DebtTransactionEntity(
    id = id,
    debtorId = debtorId,
    amount = amount,
    type = type,
    method = method,
    date = date,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    isDeleted = isDeleted,
    mirrorTransactionId = mirrorTransactionId,
)
