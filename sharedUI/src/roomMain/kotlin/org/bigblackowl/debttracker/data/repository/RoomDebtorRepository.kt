package org.bigblackowl.debttracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.bigblackowl.debttracker.data.local.dao.DebtTransactionDao
import org.bigblackowl.debttracker.data.local.dao.DebtorDao
import org.bigblackowl.debttracker.data.local.mapper.toDomain
import org.bigblackowl.debttracker.data.local.mapper.toEntity
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.debtorBalance
import org.bigblackowl.debttracker.domain.model.toDebtStatus
import org.bigblackowl.debttracker.domain.model.toDebtTransactionType
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/**
 * Offline-first (спек §5): усі write-операції йдуть у Room із `syncStatus = PENDING`.
 * Фактичний push у Supabase — Фаза 6 (background sync worker читає PENDING-записи).
 */
class RoomDebtorRepository(
    private val debtorDao: DebtorDao,
    private val transactionDao: DebtTransactionDao,
) : DebtorRepository {

    override fun observeDebtors(): Flow<List<DebtorWithBalance>> =
        combine(debtorDao.observeAll(), transactionDao.observeAll()) { debtors, transactions ->
            val transactionsByDebtor = transactions.groupBy { it.debtorId }
            debtors.map { entity ->
                val balance = transactionsByDebtor[entity.id]
                    .orEmpty()
                    .map { it.toDomain() }
                    .debtorBalance()
                DebtorWithBalance(entity.toDomain(), balance)
            }
        }

    override fun observeDebtor(id: String): Flow<Debtor?> =
        debtorDao.observeById(id).map { it?.toDomain() }

    override fun observeTransactions(debtorId: String): Flow<List<DebtTransaction>> =
        transactionDao.observeByDebtor(debtorId).map { list -> list.map { it.toDomain() } }

    override suspend fun getDebtor(id: String): Debtor? = debtorDao.getById(id)?.toDomain()

    override suspend fun upsertDebtor(debtor: Debtor) {
        val entity = debtor.copy(syncStatus = SyncStatus.PENDING).toEntity()
        // A plain upsert() is "INSERT OR REPLACE", which on an existing PK deletes-then-reinserts
        // the row in SQLite — that would cascade-delete this debtor's transactions via their
        // ON DELETE CASCADE FK. Route existing debtors through a genuine UPDATE instead.
        if (debtorDao.getById(entity.id) != null) debtorDao.update(entity) else debtorDao.upsert(entity)
    }

    override suspend fun softDeleteDebtor(id: String) {
        val entity = debtorDao.getById(id) ?: return
        debtorDao.update(
            entity.copy(isDeleted = true, syncStatus = SyncStatus.PENDING, updatedAt = kotlin.time.Clock.System.now())
        )
    }

    override suspend fun addTransaction(transaction: DebtTransaction) {
        val normalized = transaction.copy(
            type = transaction.amount.toDebtTransactionType(),
            syncStatus = SyncStatus.PENDING,
        )
        transactionDao.upsert(normalized.toEntity())
        recalcDebtorStatus(normalized.debtorId)
    }

    override suspend fun deleteAllData() {
        transactionDao.deleteAll()
        debtorDao.deleteAll()
    }

    /** Мірор Postgres-тригера з Фази 0 (recalc_debtor_status): status/updatedAt рахуються з транзакцій. */
    private suspend fun recalcDebtorStatus(debtorId: String) {
        val entity = debtorDao.getById(debtorId) ?: return
        val balance = transactionDao.getAllForDebtor(debtorId).map { it.toDomain() }.debtorBalance()
        debtorDao.update(
            entity.copy(
                status = balance.toDebtStatus(),
                syncStatus = SyncStatus.PENDING,
                updatedAt = kotlin.time.Clock.System.now(),
            )
        )
    }
}
