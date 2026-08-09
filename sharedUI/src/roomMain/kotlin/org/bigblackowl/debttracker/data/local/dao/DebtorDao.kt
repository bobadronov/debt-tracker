package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    /**
     * `INSERT OR REPLACE` on an existing PK deletes-then-reinserts the row in SQLite, which would
     * cascade-delete this debtor's transactions via their `ON DELETE CASCADE` FK. Only safe for
     * genuinely new rows — updates to an existing debtor must go through [update] instead.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debtor: DebtorEntity)

    @Update
    suspend fun update(debtor: DebtorEntity)

    @Query("DELETE FROM debtors")
    suspend fun deleteAll()

    /** Фаза 6: рядки, ще не відправлені у Supabase (спек §5, offline-first). */
    @Query("SELECT * FROM debtors WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<DebtorEntity>
}
