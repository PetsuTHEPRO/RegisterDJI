package com.sloth.registerapp.features.mission.data.mapper

import com.sloth.registerapp.features.mission.data.model.ServerMission
import com.sloth.registerapp.features.mission.domain.model.Mission

/**
 * Mapper para converter ServerMission (modelo de servidor) para Mission (modelo de UI).
 * Separa responsabilidades e facilita testes unitários.
 */
object ServerMissionMapper {
    fun toDomain(serverMission: ServerMission): Mission {
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

    fun toDomainList(serverMissions: List<ServerMission>): List<Mission> {
        return serverMissions.map { toDomain(it) }
    }
}
