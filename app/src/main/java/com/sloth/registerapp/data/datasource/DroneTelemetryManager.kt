package com.sloth.registerapp.data.datasource // Verifique se o pacote está correto

import android.util.Log
import com.sloth.registerapp.data.sdk.DJIConnectionHelper
import com.sloth.registerapp.domain.model.DroneTelemetryData
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object DroneTelemetryManager {

    private val _telemetryData = MutableStateFlow(DroneTelemetryData())
    val telemetryData = _telemetryData.asStateFlow()

    // Callback para a bateria do Drone
    private val batteryCallback = dji.common.battery.BatteryState.Callback { batteryState ->
        _telemetryData.update { it.copy(droneBatteryPercentage = batteryState.chargeRemainingInPercent) }
    }

    // --- NOVO: Callback para a bateria do Controle Remoto ---
    private val rcBatteryCallback = dji.common.remotecontroller.BatteryState.Callback { rcBatteryState ->
        _telemetryData.update { it.copy(rcBatteryPercentage = rcBatteryState.remainingChargeInPercent) }
    }
    // O callback agora preenche todos os novos campos de dados.
    private val flightControllerCallback = FlightControllerState.Callback { flightControllerState ->
        _telemetryData.update {
            it.copy(
                // Dados de Voo
                attitude = flightControllerState.attitude,
                flightTimeInSeconds = flightControllerState.flightTimeInSeconds,
                isFlying = flightControllerState.isFlying,
                areMotorsOn = flightControllerState.areMotorsOn(),
                flightMode = flightControllerState.flightMode.name,
                velocityX = flightControllerState.velocityX,
                velocityY = flightControllerState.velocityY,
                velocityZ = flightControllerState.velocityZ,

                // Dados de Sensores
                satelliteCount = flightControllerState.satelliteCount,
                gpsSignalLevel = flightControllerState.gpsSignalLevel,
                ultrasonicHeightInMeters = flightControllerState.ultrasonicHeightInMeters,

                // Dados de Segurança
                isGoingHome = flightControllerState.isGoingHome,
                windWarning = flightControllerState.flightWindWarning
            )
        }
    }

    fun init(scope: CoroutineScope) {
        scope.launch {
            DJIConnectionHelper.product.collect { product ->
                if (product != null && product.isConnected) {
                    clearListeners()

                    if (product is Aircraft) {
                        product.flightController?.setStateCallback(flightControllerCallback)
                        product.battery?.setStateCallback(batteryCallback)
                    }

                    //product.remoteController?.setBatteryStateCallback(rcBatteryCallback)
                    Log.d( "Acitivity", "Listeners de telemetria registrados.")

                } else {
                    clearListeners()
                    _telemetryData.value = DroneTelemetryData()
                }
            }
        }
    }

    private fun clearListeners() {
        val product = DJIConnectionHelper.getProductInstance()
        if (product is Aircraft) {
            product.flightController?.setStateCallback(null)
            product.battery?.setStateCallback(null)
        }
        //product?.remoteController?.setBatteryStateCallback(null)
    }
}