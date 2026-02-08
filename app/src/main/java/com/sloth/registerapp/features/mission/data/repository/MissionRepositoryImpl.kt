package com.sloth.registerapp.features.mission.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.database.AppDatabase
import com.sloth.registerapp.core.database.MissionCacheEntity
import com.sloth.registerapp.core.database.SyncQueueEntity
import com.sloth.registerapp.core.network.ConnectivityMonitor
import com.sloth.registerapp.core.network.SdiaApiService
import com.sloth.registerapp.features.mission.data.mapper.ServerMissionMapper
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/**
 * Implementação concreta do repositório de missões com suporte offline-first:
 * - online: busca API e atualiza cache local
 * - offline/erro: retorna cache local
 * - criação offline permitida (fila local + cache)
 * - alteração bloqueada sem servidor
 */
class MissionRepositoryImpl(
    context: Context,
    private val apiService: SdiaApiService,
    @Suppress("UNUSED_PARAMETER")
    private val tokenRepository: TokenRepository
) : MissionRepository {

    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.getInstance(appContext) }
    private val missionCacheDao by lazy { database.missionCacheDao() }
    private val syncQueueDao by lazy { database.syncQueueDao() }
    private val gson = Gson()

    override suspend fun getMissions(): Result<List<Mission>> {
        if (isConnected()) {
            flushPendingMissionCreates()
        }

        return runCatching {
            val missions = apiService.getMissions()
            cacheMissions(missions, syncStatus = STATUS_SYNCED)
            ServerMissionMapper.toDomainList(missions)
        }.recoverCatching { onlineError ->
            val cached = missionCacheDao.getAll()
            if (cached.isEmpty()) {
                throw onlineError
            }
            cached.mapNotNull { entity ->
                entity.missionData.toServerMissionOrNull()?.let(ServerMissionMapper::toDomain)
            }
        }
    }

    override suspend fun getMission(id: Int): Result<ServerMissionDto?> {
        return runCatching {
            val missions = apiService.getMissions()
            val mission = missions.find { it.id == id }
            mission?.let { cacheMission(it, syncStatus = STATUS_SYNCED) }
            mission
        }.recoverCatching {
            missionCacheDao.getById(id.toString())
                ?.missionData
                ?.toServerMissionOrNull()
        }
    }

    override fun listenMissionUpdates(): Flow<Mission> {
        // TODO: Implementar WebSocket listener quando houver
        return emptyFlow()
    }

    override suspend fun uploadMission(mission: ServerMissionDto): Result<ServerMissionDto> {
        // Regra de segurança: alteração de missão existente não é permitida nesta camada.
        if (mission.id > 0) {
            return Result.failure(
                IllegalStateException("Alteração de missão existente está bloqueada. Crie uma nova missão pelo servidor.")
            )
        }

        if (!isConnected()) {
            return enqueueOfflineCreate(mission)
        }

        return runCatching {
            val created = apiService.createMission(mission)
            cacheMission(created, syncStatus = STATUS_SYNCED)
            created
        }.recoverCatching {
            enqueueOfflineCreate(mission).getOrThrow()
        }
    }

    private suspend fun enqueueOfflineCreate(mission: ServerMissionDto): Result<ServerMissionDto> {
        val localId = "local:${UUID.randomUUID()}"
        val localMission = mission.copy(id = generateLocalMissionId())

        return runCatching {
            syncQueueDao.insert(
                SyncQueueEntity(
                    operationType = OP_CREATE_MISSION,
                    entityId = localId,
                    entityType = ENTITY_TYPE_MISSION,
                    payload = gson.toJson(localMission),
                    status = STATUS_PENDING
                )
            )

            cacheMission(
                mission = localMission,
                syncStatus = STATUS_PENDING,
                missionIdOverride = localId
            )

            localMission
        }
    }

    private suspend fun flushPendingMissionCreates() {
        val toSync = syncQueueDao.getByStatus(status = STATUS_PENDING)
            .filter { it.entityType == ENTITY_TYPE_MISSION && it.operationType == OP_CREATE_MISSION } +
            syncQueueDao.getByStatus(status = STATUS_FAILED)
                .filter { it.entityType == ENTITY_TYPE_MISSION && it.operationType == OP_CREATE_MISSION }

        toSync.forEach { item ->
            syncQueueDao.updateStatus(
                id = item.id,
                status = STATUS_SYNCING,
                retryCount = item.retryCount,
                lastAttempt = System.currentTimeMillis(),
                errorMessage = null
            )

            val payload = item.payload.toServerMissionOrNull()
            if (payload == null) {
                syncQueueDao.updateStatus(
                    id = item.id,
                    status = STATUS_FAILED,
                    retryCount = item.retryCount + 1,
                    lastAttempt = System.currentTimeMillis(),
                    errorMessage = "Payload inválido"
                )
                return@forEach
            }

            runCatching {
                // Sanitiza para criação no servidor.
                apiService.createMission(payload.copy(id = 0))
            }.onSuccess { createdMission ->
                missionCacheDao.deleteById(item.entityId)
                cacheMission(createdMission, syncStatus = STATUS_SYNCED)
                syncQueueDao.deleteById(item.id)
            }.onFailure { error ->
                syncQueueDao.updateStatus(
                    id = item.id,
                    status = STATUS_FAILED,
                    retryCount = item.retryCount + 1,
                    lastAttempt = System.currentTimeMillis(),
                    errorMessage = error.message
                )
            }
        }
    }

    private suspend fun cacheMissions(missions: List<ServerMissionDto>, syncStatus: String) {
        val now = System.currentTimeMillis()
        missionCacheDao.insertAll(
            missions.map { mission ->
                MissionCacheEntity(
                    missionId = mission.id.toString(),
                    name = mission.name,
                    missionData = gson.toJson(mission),
                    syncStatus = syncStatus,
                    cachedAt = now,
                    syncedAt = if (syncStatus == STATUS_SYNCED) now else null
                )
            }
        )
    }

    private suspend fun cacheMission(
        mission: ServerMissionDto,
        syncStatus: String,
        missionIdOverride: String? = null
    ) {
        val now = System.currentTimeMillis()
        missionCacheDao.insert(
            MissionCacheEntity(
                missionId = missionIdOverride ?: mission.id.toString(),
                name = mission.name,
                missionData = gson.toJson(mission),
                syncStatus = syncStatus,
                cachedAt = now,
                syncedAt = if (syncStatus == STATUS_SYNCED) now else null
            )
        )
    }

    private fun isConnected(): Boolean {
        return ConnectivityMonitor.getInstance(appContext).isConnectedNow()
    }

    private fun String.toServerMissionOrNull(): ServerMissionDto? {
        return runCatching {
            gson.fromJson(this, ServerMissionDto::class.java)
        }.getOrNull()
    }

    private fun generateLocalMissionId(): Int {
        val value = (System.currentTimeMillis() and 0x7fffffff).toInt()
        return -value.coerceAtLeast(1)
    }

    companion object {
        private const val ENTITY_TYPE_MISSION = "MISSION"
        private const val OP_CREATE_MISSION = "CREATE_MISSION"

        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_SYNCING = "SYNCING"
        private const val STATUS_SYNCED = "SYNCED"
        private const val STATUS_FAILED = "FAILED"
    }
}
