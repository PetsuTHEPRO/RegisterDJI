package com.sloth.registerapp.DJI

import dji.common.flightcontroller.Attitude

/**
 * Um data class que agrupa todas as informações de telemetria do drone.
 * Usamos valores padrão para quando o drone estiver desconectado.
 */
data class DroneTelemetryData(
    val batteryPercentage: Int = 0,
    val satelliteCount: Int = 0,
    val flightMode: String = "Desconectado",
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val velocityZ: Float = 0f
)