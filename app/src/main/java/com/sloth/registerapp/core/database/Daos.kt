package com.sloth.registerapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'SYNCING'")
    suspend fun resetSyncingToPending()
    
    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastAttemptAt = :lastAttempt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, retryCount: Int, lastAttempt: Long?, errorMessage: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MissionCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mission: MissionCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<MissionCacheEntity>)

    @Query("SELECT * FROM mission_cache WHERE missionId = :missionId")
    suspend fun getById(missionId: String): MissionCacheEntity?

    @Query("SELECT * FROM mission_cache ORDER BY cachedAt DESC")
    suspend fun getAll(): List<MissionCacheEntity>

    @Query("DELETE FROM mission_cache WHERE missionId = :missionId")
    suspend fun deleteById(missionId: String)
}

@Dao
interface FlightReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: FlightReportEntity)

    @Query("SELECT * FROM flight_reports ORDER BY startedAtMs DESC")
    suspend fun getAll(): List<FlightReportEntity>
}
