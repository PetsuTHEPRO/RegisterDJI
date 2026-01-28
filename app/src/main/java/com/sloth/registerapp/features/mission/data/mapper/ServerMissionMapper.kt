package com.sloth.registerapp.features.mission.data.mapper

import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.Mission

/**
 * Mapper para converter ServerMissionDto (modelo de servidor) para Mission (modelo de UI).
 * Separa responsabilidades e facilita testes unitários.
 */
object ServerMissionMapper {
    fun toDomain(serverMission: ServerMissionDto): Mission {
        return Mission(
            id = serverMission.id,
            name = serverMission.name,
            latitude = serverMission.poi_latitude,
            longitude = serverMission.poi_longitude,
            waypointCount = serverMission.waypoints.size,
            autoSpeed = serverMission.auto_flight_speed.toFloat(),
            maxSpeed = serverMission.max_flight_speed.toFloat()
        )
    }

    fun toDomainList(serverMissions: List<ServerMissionDto>): List<Mission> {
        return serverMissions.map { toDomain(it) }
    }
}
