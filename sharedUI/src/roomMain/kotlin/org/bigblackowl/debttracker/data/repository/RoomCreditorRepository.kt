package org.bigblackowl.debttracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

@Serializable
private data class LinkCreditorParams(@SerialName("p_creditor_id") val creditorId: String)

/** Дзеркало [RoomDebtorRepository] для напрямку "Я винен" (спек §4.1, §5). */
class RoomCreditorRepository(
    private val creditorDao: CreditorDao,
    private val transactionDao: CreditorTransactionDao,
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
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

    override suspend fun clearLocalCache() {
        transactionDao.deleteAll()
        creditorDao.deleteAll()
    }

    override suspend fun linkToRegisteredUser(creditorId: String): String? {
        if (!authRepository.isAuthenticated.value) return null
        return runCatching {
            client.postgrest.rpc("link_creditor_to_registered_user", LinkCreditorParams(creditorId)).decodeAs<String?>()
        }.getOrNull()
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
