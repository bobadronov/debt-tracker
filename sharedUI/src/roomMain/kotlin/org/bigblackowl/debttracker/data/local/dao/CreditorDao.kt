package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.data.local.entity.CreditorEntity

/** Local (Room) source of truth for creditors — mirrors [org.bigblackowl.debttracker.data.local.dao.DebtorDao]. */
@Dao
interface CreditorDao {
    @Query("SELECT * FROM creditors WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CreditorEntity>>

    @Query("SELECT * FROM creditors WHERE id = :id")
    fun observeById(id: String): Flow<CreditorEntity?>

    @Query("SELECT * FROM creditors WHERE id = :id")
    suspend fun getById(id: String): CreditorEntity?

    /**
     * `INSERT OR REPLACE` on an existing PK deletes-then-reinserts the row in SQLite, which would
     * cascade-delete this creditor's transactions via their `ON DELETE CASCADE` FK. Only safe for
     * genuinely new rows — updates to an existing creditor must go through [update] instead.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(creditor: CreditorEntity)

    @Update
    suspend fun update(creditor: CreditorEntity)

    @Query("DELETE FROM creditors")
    suspend fun deleteAll()

    @Query("SELECT * FROM creditors WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<CreditorEntity>
}
