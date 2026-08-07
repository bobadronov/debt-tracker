package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.data.local.entity.CreditorTransactionEntity

/** Local (Room) source of truth for creditor transactions — mirrors [org.bigblackowl.debttracker.data.local.dao.DebtTransactionDao]. */
@Dao
interface CreditorTransactionDao {
    @Query("SELECT * FROM creditor_transactions WHERE isDeleted = 0")
    fun observeAll(): Flow<List<CreditorTransactionEntity>>

    @Query("SELECT * FROM creditor_transactions WHERE creditorId = :creditorId AND isDeleted = 0 ORDER BY date DESC")
    fun observeByCreditor(creditorId: String): Flow<List<CreditorTransactionEntity>>

    @Query("SELECT * FROM creditor_transactions WHERE creditorId = :creditorId AND isDeleted = 0")
    suspend fun getAllForCreditor(creditorId: String): List<CreditorTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: CreditorTransactionEntity)

    @Query("DELETE FROM creditor_transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM creditor_transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<CreditorTransactionEntity>
}
