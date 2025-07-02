package com.sloth.registerapp.DJI

import dji.common.battery.BatteryState
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object DroneTelemetryManager {

    // O StateFlow privado que irá armazenar os dados de telemetria.
    private val _telemetryData = MutableStateFlow(DroneTelemetryData())
    // A versão pública e somente leitura do StateFlow, que a UI irá observar.
    val telemetryData = _telemetryData.asStateFlow()

    private val batteryCallback = BatteryState.Callback { batteryState ->
        // Atualiza o nosso StateFlow com o novo percentual de bateria.
        _telemetryData.update { it.copy(batteryPercentage = batteryState.chargeRemainingInPercent) }
    }

    private val flightControllerCallback = FlightControllerState.Callback { flightControllerState ->
        // Atualiza o StateFlow com os novos dados de voo.
        _telemetryData.update {
            it.copy(
                satelliteCount = flightControllerState.satelliteCount,
                flightMode = flightControllerState.flightMode.name,
                velocityX = flightControllerState.velocityX,
                velocityY = flightControllerState.velocityY,
                velocityZ = flightControllerState.velocityZ
            )
        }
    }

    /**
     * Inicia o gerenciador. Deve ser chamado uma única vez, a partir da MainActivity.
     * Ele usa um CoroutineScope para observar as mudanças de conexão do drone.
     */
    fun init(scope: CoroutineScope) {
        scope.launch {
            DJIConnectionHelper.product.collect { product ->
                if (product != null && product.isConnected && product is Aircraft) {
                    // Drone conectou, configura os listeners.
                    product.flightController?.setStateCallback(flightControllerCallback)
                    product.battery?.setStateCallback(batteryCallback)
                } else {
                    // Drone desconectou, limpa os listeners e reseta os dados.
                    clearListeners()
                    _telemetryData.value = DroneTelemetryData() // Reseta para os valores padrão
                }
            }
        }
    }

    private fun clearListeners() {
        (DJIConnectionHelper.getProductInstance() as? Aircraft)?.let {
            it.flightController?.setStateCallback(null)
            it.battery?.setStateCallback(null)
        }
    }
}