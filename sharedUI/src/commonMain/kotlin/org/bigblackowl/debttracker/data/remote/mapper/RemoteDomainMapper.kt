package org.bigblackowl.debttracker.data.remote.mapper

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.data.remote.dto.CreditorDto
import org.bigblackowl.debttracker.data.remote.dto.CreditorTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtorDto
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.toCreditorTransactionType
import org.bigblackowl.debttracker.domain.model.toDebtTransactionType

/**
 * Domain ↔ Supabase [DTO][org.bigblackowl.debttracker.data.remote.dto] mapping used by the
 * online-only Web repositories (no Room entity in between, unlike
 * [org.bigblackowl.debttracker.data.sync.RemoteMapper] which mirrors Room's offline-first path).
 * Rows read back from Supabase are always tagged [SyncStatus.SYNCED].
 */
fun Debtor.toDto(userId: String) = DebtorDto(
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
    linkedUserId = linkedUserId,
    mirrorCreditorId = mirrorCreditorId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun DebtorDto.toDomain() = Debtor(
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
    linkedUserId = linkedUserId,
    mirrorCreditorId = mirrorCreditorId,
)

fun DebtTransaction.toDto(userId: String) = DebtTransactionDto(
    id = id,
    debtorId = debtorId,
    userId = userId,
    amount = amount.toStringExpanded().toDouble(),
    method = method.name,
    transactionDate = date.toString(),
    comment = comment,
    isDeleted = isDeleted,
    mirrorTransactionId = mirrorTransactionId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun DebtTransactionDto.toDomain(): DebtTransaction {
    val parsedAmount = BigDecimal.parseString(amount.toString())
    return DebtTransaction(
        id = id,
        debtorId = debtorId,
        amount = parsedAmount,
        type = parsedAmount.toDebtTransactionType(),
        method = PaymentMethod.valueOf(method),
        date = kotlin.time.Instant.parse(transactionDate),
        comment = comment,
        createdAt = kotlin.time.Instant.parse(createdAt),
        updatedAt = kotlin.time.Instant.parse(updatedAt),
        syncStatus = SyncStatus.SYNCED,
        isDeleted = isDeleted,
        mirrorTransactionId = mirrorTransactionId,
    )
}

fun Creditor.toDto(userId: String) = CreditorDto(
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
    linkedUserId = linkedUserId,
    mirrorDebtorId = mirrorDebtorId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CreditorDto.toDomain() = Creditor(
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
    linkedUserId = linkedUserId,
    mirrorDebtorId = mirrorDebtorId,
)

fun CreditorTransaction.toDto(userId: String) = CreditorTransactionDto(
    id = id,
    creditorId = creditorId,
    userId = userId,
    amount = amount.toStringExpanded().toDouble(),
    method = method.name,
    transactionDate = date.toString(),
    comment = comment,
    isDeleted = isDeleted,
    mirrorTransactionId = mirrorTransactionId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CreditorTransactionDto.toDomain(): CreditorTransaction {
    val parsedAmount = BigDecimal.parseString(amount.toString())
    return CreditorTransaction(
        id = id,
        creditorId = creditorId,
        amount = parsedAmount,
        type = parsedAmount.toCreditorTransactionType(),
        method = PaymentMethod.valueOf(method),
        date = kotlin.time.Instant.parse(transactionDate),
        comment = comment,
        createdAt = kotlin.time.Instant.parse(createdAt),
        updatedAt = kotlin.time.Instant.parse(updatedAt),
        syncStatus = SyncStatus.SYNCED,
        isDeleted = isDeleted,
        mirrorTransactionId = mirrorTransactionId,
    )
}
