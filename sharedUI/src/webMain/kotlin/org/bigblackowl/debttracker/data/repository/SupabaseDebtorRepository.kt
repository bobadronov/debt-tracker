package org.bigblackowl.debttracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.data.remote.dto.DebtTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtorDto
import org.bigblackowl.debttracker.data.remote.mapper.toDomain
import org.bigblackowl.debttracker.data.remote.mapper.toDto
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.debtorBalance
import org.bigblackowl.debttracker.domain.model.toDebtStatus
import org.bigblackowl.debttracker.domain.model.toDebtTransactionType
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

@Serializable
private data class LinkDebtorParams(@SerialName("p_debtor_id") val debtorId: String)

/**
 * Web: online-only [DebtorRepository] — no local cache (Room has no wasmJs/js target, спек §1),
 * reads/writes go straight through Supabase Postgrest, list screens stay live via Realtime
 * ([selectAsFlow], мірор [org.bigblackowl.debttracker.data.sync.SyncCoordinator]'s pull side).
 */
@OptIn(SupabaseExperimental::class, ExperimentalCoroutinesApi::class)
class SupabaseDebtorRepository(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
) : DebtorRepository {

    private fun requireUserId(): String =
        authRepository.currentUserId ?: error("SupabaseDebtorRepository used while signed out")

    override fun observeDebtors(): Flow<List<DebtorWithBalance>> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(emptyList())
            } else {
                combine(
                    client.from("debtors")
                        .selectAsFlow(DebtorDto::id, filter = { eq("user_id", userId) }),
                    client.from("debt_transactions")
                        .selectAsFlow(DebtTransactionDto::id, filter = { eq("user_id", userId) }),
                ) { debtorDtos, transactionDtos ->
                    val transactionsByDebtor = transactionDtos
                        .filterNot { it.isDeleted }
                        .map { it.toDomain() }
                        .groupBy { it.debtorId }
                    debtorDtos
                        .filterNot { it.isDeleted }
                        .map { dto ->
                            val debtor = dto.toDomain()
                            DebtorWithBalance(debtor, transactionsByDebtor[debtor.id].orEmpty().debtorBalance())
                        }
                }
            }
        }

    override fun observeDebtor(id: String): Flow<Debtor?> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(null)
            } else {
                client.from("debtors")
                    .selectAsFlow(DebtorDto::id, filter = { eq("user_id", userId) })
                    .map { rows -> rows.firstOrNull { it.id == id && !it.isDeleted }?.toDomain() }
            }
        }

    override fun observeTransactions(debtorId: String): Flow<List<DebtTransaction>> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(emptyList())
            } else {
                client.from("debt_transactions")
                    .selectAsFlow(DebtTransactionDto::id, filter = { eq("user_id", userId) })
                    .map { rows ->
                        rows.filter { it.debtorId == debtorId && !it.isDeleted }.map { it.toDomain() }
                    }
            }
        }

    override suspend fun getDebtor(id: String): Debtor? {
        val userId = requireUserId()
        return client.from("debtors")
            .select { filter { eq("id", id); eq("user_id", userId) } }
            .decodeSingleOrNull<DebtorDto>()
            ?.toDomain()
    }

    override suspend fun upsertDebtor(debtor: Debtor) {
        client.from("debtors").upsert(debtor.toDto(requireUserId()))
    }

    override suspend fun softDeleteDebtor(id: String) {
        val userId = requireUserId()
        val existing = getDebtor(id) ?: return
        client.from("debtors").upsert(
            existing.copy(isDeleted = true, updatedAt = kotlin.time.Clock.System.now()).toDto(userId)
        )
    }

    override suspend fun addTransaction(transaction: DebtTransaction) {
        val userId = requireUserId()
        val normalized = transaction.copy(type = transaction.amount.toDebtTransactionType())
        client.from("debt_transactions").upsert(normalized.toDto(userId))
        recalcDebtorStatus(normalized.debtorId, userId)
    }

    // upsert is keyed by id — editing an existing transaction is the same round trip as adding one.
    override suspend fun updateTransaction(transaction: DebtTransaction) = addTransaction(transaction)

    override suspend fun softDeleteTransaction(id: String) {
        val userId = requireUserId()
        val existing = client.from("debt_transactions")
            .select { filter { eq("id", id); eq("user_id", userId) } }
            .decodeSingleOrNull<DebtTransactionDto>()
            ?.toDomain() ?: return
        client.from("debt_transactions").upsert(
            existing.copy(isDeleted = true, updatedAt = kotlin.time.Clock.System.now()).toDto(userId)
        )
        recalcDebtorStatus(existing.debtorId, userId)
    }

    override suspend fun deleteAllData() {
        val userId = requireUserId()
        client.from("debt_transactions").delete { filter { eq("user_id", userId) } }
        client.from("debtors").delete { filter { eq("user_id", userId) } }
    }

    // No local cache on web (see class doc) — nothing to clear on sign-out.
    override suspend fun clearLocalCache() {}

    override suspend fun linkToRegisteredUser(debtorId: String): String? {
        if (!authRepository.isAuthenticated.value) return null
        return runCatching {
            client.postgrest.rpc("link_debtor_to_registered_user", LinkDebtorParams(debtorId)).decodeAs<String?>()
        }.getOrNull()
    }

    /** Мірор Room-репозиторію: status рахується з транзакцій, а не приходить з Postgres-тригера напряму сюди. */
    private suspend fun recalcDebtorStatus(debtorId: String, userId: String) {
        val debtor = client.from("debtors")
            .select { filter { eq("id", debtorId); eq("user_id", userId) } }
            .decodeSingleOrNull<DebtorDto>()
            ?.toDomain() ?: return
        val balance = client.from("debt_transactions")
            .select { filter { eq("debtor_id", debtorId); eq("user_id", userId) } }
            .decodeList<DebtTransactionDto>()
            .map { it.toDomain() }
            .debtorBalance()
        client.from("debtors").upsert(
            debtor.copy(status = balance.toDebtStatus(), updatedAt = kotlin.time.Clock.System.now()).toDto(userId)
        )
    }
}
