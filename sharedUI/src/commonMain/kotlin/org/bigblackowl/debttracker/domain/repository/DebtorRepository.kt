package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance

/**
 * Offline-first: усі write-операції спершу йдуть у локальне сховище
 * (спек §5). Android/iOS/Desktop — Room-backed реалізація ([roomMain]),
 * Web (Фаза 10) — online-only реалізація напряму через Supabase.
 */
interface DebtorRepository {
    fun observeDebtors(): Flow<List<DebtorWithBalance>>
    fun observeDebtor(id: String): Flow<Debtor?>
    fun observeTransactions(debtorId: String): Flow<List<DebtTransaction>>
    suspend fun getDebtor(id: String): Debtor?
    suspend fun upsertDebtor(debtor: Debtor)
    suspend fun softDeleteDebtor(id: String)
    suspend fun addTransaction(transaction: DebtTransaction)
    suspend fun deleteAllData()
    /** Wipes this device's local cache only, leaving Supabase data untouched — Room: clears Room; Web: no-op (no local cache). */
    suspend fun clearLocalCache()
}
