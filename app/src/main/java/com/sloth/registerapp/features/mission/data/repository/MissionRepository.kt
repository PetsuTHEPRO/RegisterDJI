package com.sloth.registerapp.features.mission.data.repository

import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.SdiaApiService
import com.sloth.registerapp.features.mission.data.mapper.ServerMissionMapper
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto as ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.Mission as UiMission

class MissionRepository(
    private val apiService: SdiaApiService,
    private val tokenRepository: TokenRepository
) {

    suspend fun getMissions(): Result<List<UiMission>> {
        return try {
            val missions = apiService.getMissions()
            Result.success(ServerMissionMapper.toDomainList(missions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMission(id: Int): Result<ServerMissionDto?> {
        return try {
            val missions = apiService.getMissions()
            val mission = missions.find { it.id == id }
            Result.success(mission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
