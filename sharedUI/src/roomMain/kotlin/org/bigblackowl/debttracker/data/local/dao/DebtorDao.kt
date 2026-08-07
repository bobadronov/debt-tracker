package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.data.local.entity.DebtorEntity

/** Local (Room) source of truth for debtors, backing the offline-first [org.bigblackowl.debttracker.data.repository.RoomDebtorRepository]. */
@Dao
interface DebtorDao {
    @Query("SELECT * FROM debtors WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DebtorEntity>>

    @Query("SELECT * FROM debtors WHERE id = :id")
    fun observeById(id: String): Flow<DebtorEntity?>

    @Query("SELECT * FROM debtors WHERE id = :id")
    suspend fun getById(id: String): DebtorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debtor: DebtorEntity)

    @Query("DELETE FROM debtors")
    suspend fun deleteAll()

    /** Фаза 6: рядки, ще не відправлені у Supabase (спек §5, offline-first). */
    @Query("SELECT * FROM debtors WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<DebtorEntity>
}
