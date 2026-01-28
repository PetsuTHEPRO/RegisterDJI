package com.sloth.registerapp.features.mission.data.remote.dto

data class ServerMissionDto(
    val auto_flight_speed: Double,
    val exit_on_signal_lost: Boolean,
    val finished_action: String,
    val flight_path_mode: String,
    val gimbal_pitch_rotation_enabled: Boolean,
    val goto_first_waypoint_mode: String,
    val heading_mode: String,
    val id: Int,
    val max_flight_speed: Double,
    val name: String,
    val poi_latitude: Double,
    val poi_longitude: Double,
    val repeat_times: Int,
    val waypoints: List<WaypointDto>
)

data class WaypointDto(
    val actions: List<Any>,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val turn_mode: String
)
