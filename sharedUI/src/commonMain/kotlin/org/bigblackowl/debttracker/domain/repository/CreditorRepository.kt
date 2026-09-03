package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance

/** Дзеркало [DebtorRepository] для напрямку "Я винен" (спек §4.1). */
interface CreditorRepository {
    fun observeCreditors(): Flow<List<CreditorWithBalance>>
    fun observeCreditor(id: String): Flow<Creditor?>
    fun observeTransactions(creditorId: String): Flow<List<CreditorTransaction>>
    suspend fun getCreditor(id: String): Creditor?
    suspend fun upsertCreditor(creditor: Creditor)
    suspend fun softDeleteCreditor(id: String)
    suspend fun addTransaction(transaction: CreditorTransaction)
    /** Rewrites an existing transaction (amount/method/comment/date) by id and re-derives the creditor's status. */
    suspend fun updateTransaction(transaction: CreditorTransaction)
    /** Soft-deletes one transaction (isDeleted = true, syncs) and re-derives the creditor's status. */
    suspend fun softDeleteTransaction(id: String)
    suspend fun deleteAllData()
    /** Wipes this device's local cache only, leaving Supabase data untouched — Room: clears Room; Web: no-op (no local cache). */
    suspend fun clearLocalCache()
    /** Дзеркало [DebtorRepository.linkToRegisteredUser] — RPC `link_creditor_to_registered_user`. */
    suspend fun linkToRegisteredUser(creditorId: String): String?
}
