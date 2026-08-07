package org.bigblackowl.debttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(creditor: CreditorEntity)

    @Query("DELETE FROM creditors")
    suspend fun deleteAll()

    @Query("SELECT * FROM creditors WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<CreditorEntity>
}
