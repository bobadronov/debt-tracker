package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance

/**
 * Offline-first: all write operations go to local storage first
 * (spec §5). Android/iOS/Desktop — Room-backed implementation ([roomMain]),
 * Web (Phase 10) — online-only implementation directly through Supabase.
 */
interface DebtorRepository {
    fun observeDebtors(): Flow<List<DebtorWithBalance>>
    fun observeDebtor(id: String): Flow<Debtor?>
    fun observeTransactions(debtorId: String): Flow<List<DebtTransaction>>
    suspend fun getDebtor(id: String): Debtor?
    suspend fun upsertDebtor(debtor: Debtor)
    suspend fun softDeleteDebtor(id: String)
    suspend fun addTransaction(transaction: DebtTransaction)
    /** Rewrites an existing transaction (amount/method/comment/date) by id and re-derives the debtor's status. */
    suspend fun updateTransaction(transaction: DebtTransaction)
    /** Soft-deletes one transaction (isDeleted = true, syncs) and re-derives the debtor's status. */
    suspend fun softDeleteTransaction(id: String)
    suspend fun deleteAllData()
    /** Wipes this device's local cache only, leaving Supabase data untouched — Room: clears Room; Web: no-op (no local cache). */
    suspend fun clearLocalCache()
    /**
     * Links a debtor to a registered user by phone/email (RPC
     * `link_debtor_to_registered_user`, idempotent) — mirrors existing transactions and notifies.
     * Online-only operation (requires a session): returns `null` without a session/network/match.
     */
    suspend fun linkToRegisteredUser(debtorId: String): String?
}
