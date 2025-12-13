package com.sloth.registerapp.core.constants

object DroneConstants {
    // Limites de bateria
    const val MIN_BATTERY_LEVEL = 20
    const val LOW_BATTERY_LEVEL = 30
    const val CRITICAL_BATTERY_LEVEL = 15

    // Timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L // 30 segundos
    const val COMMAND_TIMEOUT_MS = 5000L     // 5 segundos
    const val TELEMETRY_UPDATE_INTERVAL_MS = 100L // 100ms

    // Valores padrão
    const val DEFAULT_FLIGHT_ALTITUDE = 10f
    const val DEFAULT_FLIGHT_SPEED = 5f
    const val EMERGENCY_LAND_ALTITUDE = 2f
}