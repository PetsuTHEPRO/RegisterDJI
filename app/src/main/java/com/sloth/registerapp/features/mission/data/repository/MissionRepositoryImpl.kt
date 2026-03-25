package com.sloth.registerapp.features.mission.data.repository

import android.content.Context
import android.util.Log
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
import com.sloth.registerapp.features.mission.data.preview.MissionPreviewStorage
import com.sloth.registerapp.features.mission.data.remote.dto.MissionCreateRequestDto
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.remote.dto.WaypointActionCreateRequestDto
import com.sloth.registerapp.features.mission.data.remote.dto.WaypointCreateRequestDto
import retrofit2.HttpException
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
            val missions = apiService.getMissions().map { mission ->
                logWaypointOrder("getMissions:raw", mission)
                preserveWaypointOrder(mission, ownerUserId)
                    .also { logWaypointOrder("getMissions:ordered", it) }
            }
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
            val mission = apiService.getMissionById(id).let {
                logWaypointOrder("getMission:raw", it)
                preserveWaypointOrder(it, ownerUserId)
                    .also { ordered -> logWaypointOrder("getMission:ordered", ordered) }
            }
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
            val response = apiService.createMission(mission.toCreateRequestDto())
            val created = mission.copy(id = response.mission_id)
            MissionPreviewStorage.savePreview(appContext, created)
            cacheMission(created, syncStatus = STATUS_SYNCED, ownerUserId = ownerUserId)
            sessionManager.updateLastSyncTime()
            created
        }.recoverCatching {
            enqueueOfflineCreate(mission, ownerUserId).getOrThrow()
        }
    }

    override suspend fun deleteMission(id: Int): Result<Unit> {
        val ownerUserId = resolveOwnerUserId()
        return runCatching {
            if (id > 0) {
                if (!canSyncWithServer(ownerUserId)) {
                    throw IllegalStateException("Sem conexao ou autenticacao para excluir a missao no servidor.")
                }
                deleteMissionOnServer(id)
            }

            val cachedMissions = missionCacheDao.getAll(ownerUserId)
            cachedMissions.forEach { entity ->
                val cachedMission = entity.missionData.toServerMissionOrNull() ?: return@forEach
                if (cachedMission.id == id) {
                    missionCacheDao.deleteById(entity.missionId, ownerUserId)
                }
            }

            val queuedCreates = syncQueueDao.getByStatus(status = STATUS_PENDING, ownerUserId = ownerUserId)
                .filter { it.entityType == ENTITY_TYPE_MISSION && it.operationType == OP_CREATE_MISSION } +
                syncQueueDao.getByStatus(status = STATUS_FAILED, ownerUserId = ownerUserId)
                    .filter { it.entityType == ENTITY_TYPE_MISSION && it.operationType == OP_CREATE_MISSION }

            queuedCreates.forEach { item ->
                val queuedMission = item.payload.toServerMissionOrNull() ?: return@forEach
                if (queuedMission.id == id) {
                    syncQueueDao.deleteById(item.id)
                }
            }

            MissionPreviewStorage.deletePreview(appContext, id)
        }
    }

    private suspend fun deleteMissionOnServer(id: Int) {
        val primary = apiService.deleteMission(id)
        if (primary.isSuccessful) return

        if (primary.code() == 404) return

        if (primary.code() != 405) {
            throw HttpException(primary)
        }

        val fallback = apiService.deleteMissionPlural(id)
        if (!fallback.isSuccessful) {
            throw HttpException(fallback)
        }
    }

    private suspend fun enqueueOfflineCreate(mission: ServerMissionDto, ownerUserId: String): Result<ServerMissionDto> {
        val localId = "local:${UUID.randomUUID()}"
        val localMission = mission.copy(id = generateLocalMissionId())

        return runCatching {
            MissionPreviewStorage.savePreview(appContext, localMission)
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
                apiService.createMission(payload.copy(id = 0).toCreateRequestDto())
            }.onSuccess { response ->
                MissionPreviewStorage.renamePreview(appContext, payload.id, response.mission_id)
                val createdMission = payload.copy(id = response.mission_id)
                MissionPreviewStorage.savePreview(appContext, createdMission)
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

    private suspend fun preserveWaypointOrder(
        mission: ServerMissionDto,
        ownerUserId: String
    ): ServerMissionDto {
        if (mission.waypoints.any { it.sequence != null }) return mission

        val cachedMission = missionCacheDao.getById(mission.id.toString(), ownerUserId)
            ?.missionData
            ?.toServerMissionOrNull()
            ?: return mission

        val sequenceQueues = cachedMission.waypoints
            .mapIndexed { index, waypoint ->
                waypointKey(waypoint) to (waypoint.sequence ?: index)
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> ArrayDeque(values) }

        val mergedWaypoints = mission.waypoints.map { waypoint ->
            val key = waypointKey(waypoint)
            waypoint.copy(sequence = sequenceQueues[key]?.removeFirstOrNull())
        }

        return mission.copy(waypoints = mergedWaypoints)
    }

    private fun waypointKey(waypoint: com.sloth.registerapp.features.mission.data.remote.dto.WaypointDto): String {
        return buildString {
            append(waypoint.latitude)
            append('|')
            append(waypoint.longitude)
            append('|')
            append(waypoint.altitude)
            append('|')
            append(waypoint.turn_mode)
            append('|')
            append(gson.toJson(waypoint.actions))
        }
    }

    private fun logWaypointOrder(stage: String, mission: ServerMissionDto) {
        val summary = mission.waypoints.mapIndexed { index, waypoint ->
            "#$index(id=${waypoint.waypointId}, seq=${waypoint.sequence}, lat=${waypoint.latitude}, lon=${waypoint.longitude})"
        }.joinToString()
        Log.d("MissionRepository", "$stage mission=${mission.id} name=${mission.name} waypoints=$summary")
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
        private const val TAG = "MissionRepository"
        private const val ENTITY_TYPE_MISSION = "MISSION"
        private const val OP_CREATE_MISSION = "CREATE_MISSION"

        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_SYNCING = "SYNCING"
        private const val STATUS_SYNCED = "SYNCED"
        private const val STATUS_FAILED = "FAILED"
        private const val GUEST_OWNER_ID = "__guest__"
    }
}

private val finishedActionCodes = mapOf(
    "NO_ACTION" to 0,
    "GO_HOME" to 1,
    "AUTO_LAND" to 2,
    "GO_FIRST_WAYPOINT" to 3,
    "CONTINUE_UNTIL_END" to 4
)

private val flightPathModeCodes = mapOf(
    "NORMAL" to 0,
    "CURVED" to 1
)

private val gotoFirstWaypointModeCodes = mapOf(
    "SAFELY" to 0,
    "POINT_TO_POINT" to 1
)

private val headingModeCodes = mapOf(
    "AUTO" to 0,
    "USING_INITIAL_DIRECTION" to 1,
    "CONTROL_BY_REMOTE_CONTROLLER" to 2,
    "USING_WAYPOINT_HEADING" to 3,
    "TOWARD_POINT_OF_INTEREST" to 4
)

private val turnModeCodes = mapOf(
    "CLOCKWISE" to 0,
    "COUNTER_CLOCKWISE" to 1
)

private val waypointActionTypeCodes = mapOf(
    "STAY" to 0,
    "START_TAKE_PHOTO" to 1,
    "TAKE_PHOTO" to 1,
    "PHOTO" to 1,
    "START_RECORD" to 2,
    "STOP_RECORD" to 3,
    "RESET_GIMBAL_YAW" to 4,
    "GIMBAL_PITCH" to 5,
    "CAMERA_ZOOM" to 6,
    "CAMERA_FOCUS" to 7,
    "PHOTO_GROUPING" to 8,
    "FINE_TUNE_GIMBAL_PITCH" to 9
)

private fun ServerMissionDto.toCreateRequestDto(): MissionCreateRequestDto {
    return MissionCreateRequestDto(
        auto_flight_speed = auto_flight_speed,
        exit_on_signal_lost = exit_on_signal_lost,
        finished_action = finishedActionCodes[finished_action] ?: 0,
        flight_path_mode = flightPathModeCodes[flight_path_mode] ?: 0,
        gimbal_pitch_rotation_enabled = gimbal_pitch_rotation_enabled,
        goto_first_waypoint_mode = gotoFirstWaypointModeCodes[goto_first_waypoint_mode] ?: 0,
        heading_mode = headingModeCodes[heading_mode] ?: 0,
        id = id,
        max_flight_speed = max_flight_speed,
        name = name,
        poi_latitude = poi_latitude,
        poi_longitude = poi_longitude,
        repeat_times = repeat_times,
        waypoints = waypoints.map { waypoint ->
            WaypointCreateRequestDto(
                actions = waypoint.actions.mapNotNull { action ->
                    action.toWaypointActionCreateRequestDto()
                },
                altitude = waypoint.altitude,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                turn_mode = turnModeCodes[waypoint.turn_mode] ?: 0
            )
        }
    )
}

private fun Any.toWaypointActionCreateRequestDto(): WaypointActionCreateRequestDto? {
    return when (this) {
        is Map<*, *> -> {
            val rawType = (this["action_type"] ?: this["type"] ?: this["action"] ?: this["name"])?.toString()
            val rawParam = this["action_param"] ?: this["param"] ?: this["value"] ?: this["angle"] ?: this["duration"]
            val actionCode = rawType?.uppercase()?.let { waypointActionTypeCodes[it] } ?: return null
            WaypointActionCreateRequestDto(
                action_type = actionCode,
                action_param = (rawParam as? Number)?.toInt() ?: rawParam?.toString()?.toIntOrNull() ?: 0
            )
        }
        is String -> {
            val actionCode = waypointActionTypeCodes[this.uppercase()] ?: return null
            WaypointActionCreateRequestDto(
                action_type = actionCode,
                action_param = 0
            )
        }
        else -> null
    }
}
