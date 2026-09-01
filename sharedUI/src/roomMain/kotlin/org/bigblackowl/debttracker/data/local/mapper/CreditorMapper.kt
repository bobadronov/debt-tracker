package org.bigblackowl.debttracker.data.local.mapper

import org.bigblackowl.debttracker.data.local.entity.CreditorEntity
import org.bigblackowl.debttracker.data.local.entity.CreditorTransactionEntity
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.encodeReminderLeadDays
import org.bigblackowl.debttracker.domain.model.parseReminderLeadDays

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
    linkedUserId = linkedUserId,
    mirrorDebtorId = mirrorDebtorId,
    dueDate = dueDate,
    reminderLeadDays = parseReminderLeadDays(reminderLeadDays),
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
    linkedUserId = linkedUserId,
    mirrorDebtorId = mirrorDebtorId,
    dueDate = dueDate,
    reminderLeadDays = reminderLeadDays.encodeReminderLeadDays(),
)

fun CreditorTransactionEntity.toDomain() = CreditorTransaction(
    id = id,
    creditorId = creditorId,
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

fun CreditorTransaction.toEntity() = CreditorTransactionEntity(
    id = id,
    creditorId = creditorId,
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
