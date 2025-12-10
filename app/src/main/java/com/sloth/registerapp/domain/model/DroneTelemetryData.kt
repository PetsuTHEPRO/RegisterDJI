package com.sloth.registerapp.domain.model

import dji.common.flightcontroller.Attitude
import dji.common.flightcontroller.FlightWindWarning
import dji.common.flightcontroller.GPSSignalLevel

/**
 * Um data class que agrupa todas as informações de telemetria do drone.
 * Usamos valores padrão para quando o drone estiver desconectado.
 */
data class DroneTelemetryData(
    // Baterias
    val droneBatteryPercentage: Int = 0,
    val rcBatteryPercentage: Int = 0,

    // Voo
    val attitude: Attitude = Attitude(0.0, 0.0, 0.0),
    val flightTimeInSeconds: Int = 0,
    val isFlying: Boolean = false,
    val areMotorsOn: Boolean = false,
    val flightMode: String = "Desconectado",
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val velocityZ: Float = 0f,

    // Sensores e Sinais
    val satelliteCount: Int = 0,
    val gpsSignalLevel: GPSSignalLevel = GPSSignalLevel.LEVEL_0,
    val ultrasonicHeightInMeters: Float = 0f,

    // Segurança
    val isGoingHome: Boolean = false,
    val windWarning: FlightWindWarning = FlightWindWarning.UNKNOWN
)