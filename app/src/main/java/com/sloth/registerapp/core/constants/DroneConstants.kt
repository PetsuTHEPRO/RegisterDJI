package com.sloth.registerapp.core.constants

object DroneConstants {
    // Limites de bateria
    const val MIN_BATTERY_LEVEL = 20
    const val LOW_BATTERY_LEVEL = 30
    const val CRITICAL_BATTERY_LEVEL = 15

    // Limites de voo
    const val MAX_ALTITUDE_METERS = 120f
    const val MIN_ALTITUDE_METERS = 0.5f
    const val MAX_SPEED_MPS = 15f // metros por segundo
    const val MAX_DISTANCE_METERS = 500f

    // Timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L // 30 segundos
    const val COMMAND_TIMEOUT_MS = 5000L     // 5 segundos
    const val TELEMETRY_UPDATE_INTERVAL_MS = 100L // 100ms

    // Valores padrão
    const val DEFAULT_FLIGHT_ALTITUDE = 10f
    const val DEFAULT_FLIGHT_SPEED = 5f
    const val EMERGENCY_LAND_ALTITUDE = 2f
}