package com.sloth.registerapp.features.mission.data.repository

import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.SdiaApiService
import com.sloth.registerapp.features.mission.data.mapper.ServerMissionMapper
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Implementação concreta do repositório de missões.
 * Acessa dados através da API e WebSocket do servidor.
 */
class MissionRepositoryImpl(
    private val apiService: SdiaApiService,
    private val tokenRepository: TokenRepository
) : MissionRepository {

    override suspend fun getMissions(): Result<List<Mission>> {
        return try {
            val missions = apiService.getMissions()
            Result.success(ServerMissionMapper.toDomainList(missions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMission(id: Int): Result<ServerMissionDto?> {
        return try {
            val missions = apiService.getMissions()
            val mission = missions.find { it.id == id }
            Result.success(mission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun listenMissionUpdates(): Flow<Mission> {
        // TODO: Implementar WebSocket listener quando houver
        return emptyFlow()
    }

    override suspend fun uploadMission(mission: ServerMissionDto): Result<ServerMissionDto> {
        // TODO: Implementar upload de missão
        return Result.failure(NotImplementedError("Upload de missão não implementado"))
    }
}
