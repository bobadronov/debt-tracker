package org.bigblackowl.debttracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.bigblackowl.debttracker.data.local.dao.CreditorDao
import org.bigblackowl.debttracker.data.local.dao.CreditorTransactionDao
import org.bigblackowl.debttracker.data.local.mapper.toDomain
import org.bigblackowl.debttracker.data.local.mapper.toEntity
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.creditorBalance
import org.bigblackowl.debttracker.domain.model.toCreditorTransactionType
import org.bigblackowl.debttracker.domain.model.toDebtStatus
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Дзеркало [RoomDebtorRepository] для напрямку "Я винен" (спек §4.1, §5). */
class RoomCreditorRepository(
    private val creditorDao: CreditorDao,
    private val transactionDao: CreditorTransactionDao,
) : CreditorRepository {

    override fun observeCreditors(): Flow<List<CreditorWithBalance>> =
        combine(creditorDao.observeAll(), transactionDao.observeAll()) { creditors, transactions ->
            val transactionsByCreditor = transactions.groupBy { it.creditorId }
            creditors.map { entity ->
                val balance = transactionsByCreditor[entity.id]
                    .orEmpty()
                    .map { it.toDomain() }
                    .creditorBalance()
                CreditorWithBalance(entity.toDomain(), balance)
            }
        }

    override fun observeCreditor(id: String): Flow<Creditor?> =
        creditorDao.observeById(id).map { it?.toDomain() }

    override fun observeTransactions(creditorId: String): Flow<List<CreditorTransaction>> =
        transactionDao.observeByCreditor(creditorId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCreditor(id: String): Creditor? = creditorDao.getById(id)?.toDomain()

    override suspend fun upsertCreditor(creditor: Creditor) {
        val entity = creditor.copy(syncStatus = SyncStatus.PENDING).toEntity()
        // A plain upsert() is "INSERT OR REPLACE", which on an existing PK deletes-then-reinserts
        // the row in SQLite — that would cascade-delete this creditor's transactions via their
        // ON DELETE CASCADE FK. Route existing creditors through a genuine UPDATE instead.
        if (creditorDao.getById(entity.id) != null) creditorDao.update(entity) else creditorDao.upsert(entity)
    }

    override suspend fun softDeleteCreditor(id: String) {
        val entity = creditorDao.getById(id) ?: return
        creditorDao.update(
            entity.copy(isDeleted = true, syncStatus = SyncStatus.PENDING, updatedAt = kotlin.time.Clock.System.now())
        )
    }

    override suspend fun addTransaction(transaction: CreditorTransaction) {
        val normalized = transaction.copy(
            type = transaction.amount.toCreditorTransactionType(),
            syncStatus = SyncStatus.PENDING,
        )
        transactionDao.upsert(normalized.toEntity())
        recalcCreditorStatus(normalized.creditorId)
    }

    override suspend fun deleteAllData() {
        transactionDao.deleteAll()
        creditorDao.deleteAll()
    }

    private suspend fun recalcCreditorStatus(creditorId: String) {
        val entity = creditorDao.getById(creditorId) ?: return
        val balance = transactionDao.getAllForCreditor(creditorId).map { it.toDomain() }.creditorBalance()
        creditorDao.update(
            entity.copy(
                status = balance.toDebtStatus(),
                syncStatus = SyncStatus.PENDING,
                updatedAt = kotlin.time.Clock.System.now(),
            )
        )
    }
}
