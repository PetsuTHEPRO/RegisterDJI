package com.sloth.registerapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String,        // CREATE_MISSION, UPDATE_MISSION, DELETE_MISSION
    val entityId: String,             // ID da entidade
    val entityType: String,           // MISSION, FLIGHT, etc
    val payload: String,              // JSON da operação
    val status: String = "PENDING",   // PENDING, SYNCING, COMPLETED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)

@Entity(tableName = "mission_cache")
data class MissionCacheEntity(
    @PrimaryKey
    val missionId: String,
    val name: String,
    val description: String? = null,
    val missionData: String,           // JSON completo da missão
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val cachedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

@Entity(tableName = "flight_reports")
data class FlightReportEntity(
    @PrimaryKey
    val id: String,
    val missionName: String,
    val aircraftName: String,
    val createdAtMs: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationMs: Long,
    val finalObservation: String?,
    val extraDataJson: String = "{}"
)
