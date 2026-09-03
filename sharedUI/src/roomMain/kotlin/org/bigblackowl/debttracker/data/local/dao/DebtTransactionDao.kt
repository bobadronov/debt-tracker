package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.data.local.entity.DebtTransactionEntity

@Dao
interface DebtTransactionDao {
    /** Усі транзакції по всіх боржниках — для агрегації балансу в списку (DebtorRepository.observeDebtors). */
    @Query("SELECT * FROM debt_transactions WHERE isDeleted = 0")
    fun observeAll(): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE debtorId = :debtorId AND isDeleted = 0 ORDER BY date DESC")
    fun observeByDebtor(debtorId: String): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE debtorId = :debtorId AND isDeleted = 0")
    suspend fun getAllForDebtor(debtorId: String): List<DebtTransactionEntity>

    @Query("SELECT * FROM debt_transactions WHERE id = :id")
    suspend fun getById(id: String): DebtTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: DebtTransactionEntity)

    @Query("DELETE FROM debt_transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM debt_transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<DebtTransactionEntity>
}
