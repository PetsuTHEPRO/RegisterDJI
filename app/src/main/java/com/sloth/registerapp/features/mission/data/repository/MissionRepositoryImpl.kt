package com.sloth.registerapp.features.mission.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.model.ServerAuthState
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
import kotlinx.coroutines.flow.first
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
    private val localSessionManager by lazy { LocalSessionManager.getInstance(appContext) }
    private val sessionManager by lazy { SessionManager.getInstance(appContext) }
    private val gson = Gson()

    override suspend fun getMissions(): Result<List<Mission>> {
        val ownerUserId = resolveOwnerUserId()
        val canSyncWithServer = canSyncWithServer(ownerUserId)
        if (canSyncWithServer) {
            flushPendingMissionCreates(ownerUserId)
        }

        if (!canSyncWithServer) {
            return runCatching {
                missionCacheDao.getAll(ownerUserId).mapNotNull { entity ->
                    entity.missionData.toServerMissionOrNull()?.let(ServerMissionMapper::toDomain)
                }
            }
        }

        return runCatching {
            val missions = apiService.getMissions()
            cacheMissions(missions, syncStatus = STATUS_SYNCED, ownerUserId = ownerUserId)
            sessionManager.updateLastSyncTime()
            ServerMissionMapper.toDomainList(missions)
        }.recoverCatching { onlineError ->
            val cached = missionCacheDao.getAll(ownerUserId)
            if (cached.isEmpty()) {
                throw onlineError
            }
            cached.mapNotNull { entity ->
                entity.missionData.toServerMissionOrNull()?.let(ServerMissionMapper::toDomain)
            }
        }
    }

    override suspend fun getMission(id: Int): Result<ServerMissionDto?> {
        val ownerUserId = resolveOwnerUserId()
        if (!canSyncWithServer(ownerUserId)) {
            return runCatching {
                missionCacheDao.getById(id.toString(), ownerUserId)
                    ?.missionData
                    ?.toServerMissionOrNull()
            }
        }
        return runCatching {
            val missions = apiService.getMissions()
            val mission = missions.find { it.id == id }
            mission?.let { cacheMission(it, syncStatus = STATUS_SYNCED, ownerUserId = ownerUserId) }
            mission
        }.recoverCatching {
            missionCacheDao.getById(id.toString(), ownerUserId)
                ?.missionData
                ?.toServerMissionOrNull()
        }
    }

    override fun listenMissionUpdates(): Flow<Mission> {
        // TODO: Implementar WebSocket listener quando houver
        return emptyFlow()
    }

    override suspend fun uploadMission(mission: ServerMissionDto): Result<ServerMissionDto> {
        val ownerUserId = resolveOwnerUserId()
        if (ownerUserId == GUEST_OWNER_ID) {
            return Result.failure(
                IllegalStateException("Faça login para criar missão.")
            )
        }
        // Regra de segurança: alteração de missão existente não é permitida nesta camada.
        if (mission.id > 0) {
            return Result.failure(
                IllegalStateException("Alteração de missão existente está bloqueada. Crie uma nova missão pelo servidor.")
            )
        }

        if (!canSyncWithServer(ownerUserId)) {
            return enqueueOfflineCreate(mission, ownerUserId)
        }

        return runCatching {
            val created = apiService.createMission(mission)
            cacheMission(created, syncStatus = STATUS_SYNCED, ownerUserId = ownerUserId)
            sessionManager.updateLastSyncTime()
            created
        }.recoverCatching {
            enqueueOfflineCreate(mission, ownerUserId).getOrThrow()
        }
    }

    private suspend fun enqueueOfflineCreate(mission: ServerMissionDto, ownerUserId: String): Result<ServerMissionDto> {
        val localId = "local:${UUID.randomUUID()}"
        val localMission = mission.copy(id = generateLocalMissionId())

        return runCatching {
            syncQueueDao.insert(
                SyncQueueEntity(
                    ownerUserId = ownerUserId,
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
                missionIdOverride = localId,
                ownerUserId = ownerUserId
            )

            localMission
        }
    }

    private suspend fun flushPendingMissionCreates(ownerUserId: String) {
        val toSync = syncQueueDao.getByStatus(status = STATUS_PENDING, ownerUserId = ownerUserId)
            .filter { it.entityType == ENTITY_TYPE_MISSION && it.operationType == OP_CREATE_MISSION } +
            syncQueueDao.getByStatus(status = STATUS_FAILED, ownerUserId = ownerUserId)
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
                missionCacheDao.deleteById(item.entityId, ownerUserId)
                cacheMission(createdMission, syncStatus = STATUS_SYNCED, ownerUserId = ownerUserId)
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

    private suspend fun cacheMissions(missions: List<ServerMissionDto>, syncStatus: String, ownerUserId: String) {
        val now = System.currentTimeMillis()
        missionCacheDao.insertAll(
            missions.map { mission ->
                MissionCacheEntity(
                    missionId = mission.id.toString(),
                    ownerUserId = ownerUserId,
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
        missionIdOverride: String? = null,
        ownerUserId: String
    ) {
        val now = System.currentTimeMillis()
        missionCacheDao.insert(
            MissionCacheEntity(
                missionId = missionIdOverride ?: mission.id.toString(),
                ownerUserId = ownerUserId,
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

    private suspend fun resolveOwnerUserId(): String {
        val userId = localSessionManager.currentUserId.first()
        return if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
    }

    private suspend fun canSyncWithServer(ownerUserId: String): Boolean {
        if (ownerUserId == GUEST_OWNER_ID) return false
        if (!isConnected()) return false
        val authState = localSessionManager.serverAuthState.first()
        return authState != ServerAuthState.SERVER_AUTH_REQUIRED
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
        private const val GUEST_OWNER_ID = "__guest__"
    }
}
