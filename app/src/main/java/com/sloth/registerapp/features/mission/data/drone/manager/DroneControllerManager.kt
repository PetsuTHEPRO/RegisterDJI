package com.sloth.registerapp.features.mission.data.drone.manager

import android.util.Log
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.data.drone.DroneState
import com.sloth.registerapp.features.mission.data.drone.DroneTelemetry
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DroneControllerManager {

    private val _droneState = MutableStateFlow(DroneState.ON_GROUND)
    val droneState: StateFlow<DroneState> = _droneState

    private val _telemetry = MutableStateFlow(DroneTelemetry())
    val telemetry: StateFlow<DroneTelemetry> = _telemetry

    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Job para controlar o envio contínuo de comandos
    private var virtualStickJob: Job? = null
    private var isVirtualStickEnabled = false

    private fun getFlightController(): FlightController? {
        val product = DJIConnectionHelper.getProductInstance()
        return if (product is Aircraft) {
            product.flightController
        } else {
            Log.e(TAG, "❌ Produto não é uma aeronave")
            null
        }
    }

    // ========== CONFIGURAÇÃO DO VIRTUAL STICK ==========

    private fun setupVirtualStickMode(flightController: FlightController) {
        // Configura os modos de controle
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY)
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY)
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY)
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY)
        
        Log.d(TAG, "✅ Virtual Stick configurado: VELOCITY mode")
    }

    private fun enableVirtualStick(enable: Boolean, onResult: (Boolean) -> Unit) {
        val flightController = getFlightController()
        if (flightController == null) {
            Log.e(TAG, "❌ FlightController não disponível")
            onResult(false)
            return
        }

        if (enable && !isVirtualStickEnabled) {
            setupVirtualStickMode(flightController)
        }

        flightController.setVirtualStickModeEnabled(enable) { error ->
            if (error == null) {
                isVirtualStickEnabled = enable
                Log.d(TAG, "✅ Virtual Stick ${if (enable) "ativado" else "desativado"}")
                onResult(true)
            } else {
                Log.e(TAG, "❌ Erro Virtual Stick: ${error.description}")
                onResult(false)
            }
        }
    }

    // ========== ENVIO CONTÍNUO DE COMANDOS ==========

    private fun startSendingCommands(controlData: FlightControlData) {
        // Cancela job anterior se existir
        virtualStickJob?.cancel()

        virtualStickJob = scope.launch {
            val flightController = getFlightController() ?: return@launch

            // Envia comandos continuamente a cada 200ms
            while (isActive) {
                flightController.sendVirtualStickFlightControlData(controlData, null)
                delay(200) // DJI recomenda 200ms entre comandos
            }
        }
    }

    // ========== DECOLAGEM E POUSO ==========

    fun takeOff() {
        if (_droneState.value != DroneState.ON_GROUND) {
            Log.w(TAG, "⚠️ Decolagem ignorada. Estado: ${_droneState.value}")
            return
        }

        val flightController = getFlightController()
        if (flightController == null) {
            Log.e(TAG, "❌ FlightController não disponível")
            return
        }

        scope.launch {
            Log.d(TAG, "🚁 Iniciando decolagem...")
            _droneState.value = DroneState.TAKING_OFF

            flightController.startTakeoff { error ->
                if (error == null) {
                    Log.d(TAG, "✅ Comando de decolagem enviado")
                    _droneState.value = DroneState.IN_AIR
                } else {
                    Log.e(TAG, "❌ Erro na decolagem: ${error.description}")
                    _droneState.value = DroneState.ON_GROUND
                }
            }
        }
    }

    fun land() {
        if (_droneState.value != DroneState.IN_AIR) {
            Log.w(TAG, "⚠️ Pouso ignorado. Estado: ${_droneState.value}")
            return
        }

        // Para qualquer movimento em andamento
        stopMovement()

        val flightController = getFlightController()
        if (flightController == null) {
            Log.e(TAG, "❌ FlightController não disponível")
            return
        }

        scope.launch {
            Log.d(TAG, "🛬 Iniciando pouso...")
            _droneState.value = DroneState.LANDING

            flightController.startLanding { error ->
                if (error == null) {
                    Log.d(TAG, "✅ Comando de pouso enviado")
                } else {
                    Log.e(TAG, "❌ Erro no pouso: ${error.description}")
                    _droneState.value = DroneState.IN_AIR
                }
            }

            delay(4000)
            _droneState.value = DroneState.ON_GROUND
            _telemetry.value = _telemetry.value.copy(altitude = 0f, speed = 0f)
        }
    }

    fun returnToHome() {
        if (_droneState.value != DroneState.IN_AIR) {
            Log.w(TAG, "⚠️ Retorno ignorado. Estado: ${_droneState.value}")
            return
        }

        val flightController = getFlightController()
        if (flightController == null) {
            Log.e(TAG, "❌ FlightController não disponível")
            return
        }

        scope.launch {
            Log.d(TAG, "🏠 Iniciando retorno para casa...")
            flightController.startGoHome { error ->
                if (error == null) {
                    Log.d(TAG, "✅ Comando de retorno enviado")
                    _droneState.value = DroneState.GOING_HOME
                } else {
                    Log.e(TAG, "❌ Erro no retorno: ${error.description}")
                }
            }
        }
    }

    // ========== MOVIMENTAÇÃO HORIZONTAL ==========

    fun moveForward(speed: Float = 2f) {
        if (!canMove()) return

        Log.d(TAG, "⬆️ Movendo para frente: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(speed, 0f, 0f, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    fun moveBackward(speed: Float = 2f) {
        if (!canMove()) return

        Log.d(TAG, "⬇️ Movendo para trás: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(-speed, 0f, 0f, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    fun moveLeft(speed: Float = 2f) {
        if (!canMove()) return

        Log.d(TAG, "⬅️ Movendo para esquerda: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, -speed, 0f, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    fun moveRight(speed: Float = 2f) {
        if (!canMove()) return

        Log.d(TAG, "➡️ Movendo para direita: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, speed, 0f, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    // ========== MOVIMENTAÇÃO VERTICAL ==========

    fun moveUp(speed: Float = 1f) {
        if (!canMove()) return

        Log.d(TAG, "⬆️ Subindo: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, 0f, 0f, speed)
                startSendingCommands(controlData)
            }
        }
    }

    fun moveDown(speed: Float = 1f) {
        if (!canMove()) return

        Log.d(TAG, "⬇️ Descendo: ${speed}m/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, 0f, 0f, -speed)
                startSendingCommands(controlData)
            }
        }
    }

    // ========== ROTAÇÃO ==========

    fun rotateLeft(speed: Float = 30f) {
        if (!canMove()) return

        Log.d(TAG, "↪️ Rotacionando esquerda: ${speed}°/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, 0f, -speed, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    fun rotateRight(speed: Float = 30f) {
        if (!canMove()) return

        Log.d(TAG, "↩️ Rotacionando direita: ${speed}°/s")

        enableVirtualStick(true) { success ->
            if (success) {
                val controlData = FlightControlData(0f, 0f, speed, 0f)
                startSendingCommands(controlData)
            }
        }
    }

    // ========== PARAR MOVIMENTO ==========

    fun stopMovement() {
        Log.d(TAG, "⏹️ Parando movimento")

        // Cancela o job de envio contínuo
        virtualStickJob?.cancel()
        virtualStickJob = null

        // Envia comando de parada
        getFlightController()?.sendVirtualStickFlightControlData(
            FlightControlData(0f, 0f, 0f, 0f),
            null
        )

        // Desabilita Virtual Stick após 500ms
        scope.launch {
            delay(500)
            enableVirtualStick(false) {}
        }
    }

    // ========== PARADA DE EMERGÊNCIA ==========

    fun emergencyStop() {
        scope.launch {
            Log.e(TAG, "🚨 PARADA DE EMERGÊNCIA!")

            val flightController = getFlightController()

            // Para movimentos
            virtualStickJob?.cancel()
            virtualStickJob = null

            // Desativa virtual stick
            enableVirtualStick(false) {}

            // Cancela operações
            flightController?.cancelTakeoff { error ->
                if (error == null) Log.d(TAG, "✅ Decolagem cancelada")
            }

            flightController?.cancelLanding { error ->
                if (error == null) Log.d(TAG, "✅ Pouso cancelado")
            }

            _droneState.value = DroneState.EMERGENCY_STOP
            _telemetry.value = _telemetry.value.copy(speed = 0f)
        }
    }

    // ========== MOVIMENTAÇÃO PARA COORDENADAS ==========

    fun moveTo(latitude: Double, longitude: Double, altitude: Float) {
        if (!canMove()) return

        scope.launch {
            Log.d(TAG, "📍 Movendo para: $latitude, $longitude @ ${altitude}m")
            
            // TODO: Implementar WaypointMission
            delay(5000)
            Log.d(TAG, "✅ Chegou ao destino (simulado)")
        }
    }

    // ========== UTILITÁRIOS ==========

    private fun canMove(): Boolean {
        val canMove = _droneState.value == DroneState.IN_AIR
        if (!canMove) {
            Log.w(TAG, "⚠️ Movimento ignorado. Estado: ${_droneState.value}")
        }
        return canMove
    }

    fun updateTelemetry(
        altitude: Float? = null,
        speed: Float? = null,
        distance: Float? = null,
        gps: Int? = null,
        battery: Int? = null
    ) {
        _telemetry.value = _telemetry.value.copy(
            altitude = altitude ?: _telemetry.value.altitude,
            speed = speed ?: _telemetry.value.speed,
            distanceFromHome = distance ?: _telemetry.value.distanceFromHome,
            gpsSatellites = gps ?: _telemetry.value.gpsSatellites,
            batteryLevel = battery ?: _telemetry.value.batteryLevel
        )
    }

    fun isConnected(): Boolean = getFlightController() != null

    companion object {
        private const val TAG = "DroneController"
    }
}