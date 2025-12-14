package com.sloth.registerapp.data.repository

import com.sloth.registerapp.data.network.SdiaApiService
import kotlinx.coroutines.flow.first
import com.sloth.registerapp.data.model.ServerMission as ServerMission
import com.sloth.registerapp.model.Mission as UiMission

class MissionRepository(
    private val apiService: SdiaApiService,
    private val tokenRepository: TokenRepository
) {

    suspend fun getMissions(): Result<List<UiMission>> {
        return try {
            val missions = apiService.getMissions()
            Result.success(missions.map { it.toUiMission() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMission(id: Int): Result<ServerMission?> {
        return try {
            val missions = apiService.getMissions()
            val mission = missions.find { it.id == id }
            Result.success(mission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ServerMission.toUiMission(): UiMission {
        return UiMission(
            id = this.id,
            name = this.name,
            latitude = this.poi_latitude,
            longitude = this.poi_longitude,
            waypointCount = this.waypoints.size,
            autoSpeed = this.auto_flight_speed.toFloat(),
            maxSpeed = this.max_flight_speed.toFloat()
        )
    }
}
