package org.bigblackowl.debttracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.bigblackowl.debttracker.data.remote.dto.CreditorDto
import org.bigblackowl.debttracker.data.remote.dto.CreditorTransactionDto
import org.bigblackowl.debttracker.data.remote.mapper.toDomain
import org.bigblackowl.debttracker.data.remote.mapper.toDto
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.creditorBalance
import org.bigblackowl.debttracker.domain.model.toCreditorTransactionType
import org.bigblackowl.debttracker.domain.model.toDebtStatus
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Дзеркало [SupabaseDebtorRepository] для напрямку "Я винен" (спек §4.1). */
@OptIn(SupabaseExperimental::class, ExperimentalCoroutinesApi::class)
class SupabaseCreditorRepository(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
) : CreditorRepository {

    private fun requireUserId(): String =
        authRepository.currentUserId ?: error("SupabaseCreditorRepository used while signed out")

    override fun observeCreditors(): Flow<List<CreditorWithBalance>> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(emptyList())
            } else {
                combine(
                    client.from("creditors")
                        .selectAsFlow(CreditorDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId)),
                    client.from("creditor_transactions")
                        .selectAsFlow(CreditorTransactionDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId)),
                ) { creditorDtos, transactionDtos ->
                    val transactionsByCreditor = transactionDtos
                        .filterNot { it.isDeleted }
                        .map { it.toDomain() }
                        .groupBy { it.creditorId }
                    creditorDtos
                        .filterNot { it.isDeleted }
                        .map { dto ->
                            val creditor = dto.toDomain()
                            CreditorWithBalance(creditor, transactionsByCreditor[creditor.id].orEmpty().creditorBalance())
                        }
                }
            }
        }

    override fun observeCreditor(id: String): Flow<Creditor?> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(null)
            } else {
                client.from("creditors")
                    .selectAsFlow(CreditorDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
                    .map { rows -> rows.firstOrNull { it.id == id && !it.isDeleted }?.toDomain() }
            }
        }

    override fun observeTransactions(creditorId: String): Flow<List<CreditorTransaction>> =
        authRepository.isAuthenticated.flatMapLatest { authenticated ->
            val userId = authRepository.currentUserId
            if (!authenticated || userId == null) {
                flowOf(emptyList())
            } else {
                client.from("creditor_transactions")
                    .selectAsFlow(CreditorTransactionDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
                    .map { rows ->
                        rows.filter { it.creditorId == creditorId && !it.isDeleted }.map { it.toDomain() }
                    }
            }
        }

    override suspend fun getCreditor(id: String): Creditor? {
        val userId = requireUserId()
        return client.from("creditors")
            .select { filter { eq("id", id); eq("user_id", userId) } }
            .decodeSingleOrNull<CreditorDto>()
            ?.toDomain()
    }

    override suspend fun upsertCreditor(creditor: Creditor) {
        client.from("creditors").upsert(creditor.toDto(requireUserId()))
    }

    override suspend fun softDeleteCreditor(id: String) {
        val userId = requireUserId()
        val existing = getCreditor(id) ?: return
        client.from("creditors").upsert(
            existing.copy(isDeleted = true, updatedAt = kotlin.time.Clock.System.now()).toDto(userId)
        )
    }

    override suspend fun addTransaction(transaction: CreditorTransaction) {
        val userId = requireUserId()
        val normalized = transaction.copy(type = transaction.amount.toCreditorTransactionType())
        client.from("creditor_transactions").upsert(normalized.toDto(userId))
        recalcCreditorStatus(normalized.creditorId, userId)
    }

    override suspend fun deleteAllData() {
        val userId = requireUserId()
        client.from("creditor_transactions").delete { filter { eq("user_id", userId) } }
        client.from("creditors").delete { filter { eq("user_id", userId) } }
    }

    private suspend fun recalcCreditorStatus(creditorId: String, userId: String) {
        val creditor = client.from("creditors")
            .select { filter { eq("id", creditorId); eq("user_id", userId) } }
            .decodeSingleOrNull<CreditorDto>()
            ?.toDomain() ?: return
        val balance = client.from("creditor_transactions")
            .select { filter { eq("creditor_id", creditorId); eq("user_id", userId) } }
            .decodeList<CreditorTransactionDto>()
            .map { it.toDomain() }
            .creditorBalance()
        client.from("creditors").upsert(
            creditor.copy(status = balance.toDebtStatus(), updatedAt = kotlin.time.Clock.System.now()).toDto(userId)
        )
    }
}
