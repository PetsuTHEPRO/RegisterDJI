package com.sloth.registerapp.features.mission.data.drone.manager

import android.util.Log
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.FlightCommandError
import com.sloth.registerapp.features.mission.domain.model.FlightCommandRejectionReason
import com.sloth.registerapp.features.mission.domain.model.FlightCommandResult
import com.sloth.registerapp.features.mission.domain.model.FlightCommandType
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import dji.common.util.CommonCallbacks
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class DroneCommandManager {

    private val _droneState = MutableStateFlow(DroneState.ON_GROUND)
    val droneState: StateFlow<DroneState> = _droneState

    private val scope = CoroutineScope(Dispatchers.IO)

    private val telemetryManager = DroneTelemetryManager(scope)
    val telemetry: StateFlow<DroneTelemetry> = telemetryManager.telemetry
    
    // Job para controlar o envio contínuo de comandos
    private var virtualStickJob: Job? = null
    private var safetyStopJob: Job? = null
    private var disableVirtualStickJob: Job? = null
    private var isVirtualStickEnabled = false

    init {
        scope.launch {
            telemetryManager.droneState.collect { state ->
                _droneState.value = state
            }
        }
    }

    private fun getFlightController(): FlightController? {
        val product = DJIConnectionHelper.getProductInstance()
        return if (product is Aircraft) {
            product.flightController
        } else {
            Log.e(TAG, "❌ Produto não é uma aeronave")
            null
        }
    }

    private fun getCamera(): Camera? {
        val product = DJIConnectionHelper.getProductInstance()
        return if (product is Aircraft) {
            product.camera
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
        if (enable) {
            // Evita que um disable atrasado desligue o stick no meio de um novo comando
            disableVirtualStickJob?.cancel()
            disableVirtualStickJob = null
        }

        if (isVirtualStickEnabled == enable) {
            onResult(true)
            return
        }

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

    private fun scheduleSafetyStop() {
        safetyStopJob?.cancel()
        safetyStopJob = scope.launch {
            // Fail-safe: impede movimento indefinido por toque único/acidental.
            delay(MOVE_SAFETY_TIMEOUT_MS)
            Log.w(TAG, "⚠️ Safety timeout atingido. Parando movimento automaticamente.")
            stopMovement()
        }
    }

    // ========== DECOLAGEM E POUSO ==========

    fun takeOff(onResult: (FlightCommandResult) -> Unit = {}) {
        scope.launch {
            onResult(executeFlightCommand(
                command = FlightCommandType.TAKE_OFF,
                requiredState = DroneState.ON_GROUND,
                pendingState = DroneState.TAKING_OFF,
                rollbackState = DroneState.ON_GROUND
            ) { callback ->
                startTakeoff(callback)
            })
        }
    }

    fun land(onResult: (FlightCommandResult) -> Unit = {}) {
        scope.launch {
            stopMovement()
            onResult(executeFlightCommand(
                command = FlightCommandType.LAND,
                requiredState = DroneState.IN_AIR,
                pendingState = DroneState.LANDING,
                rollbackState = DroneState.IN_AIR
            ) { callback ->
                startLanding(callback)
            })
        }
    }

    fun returnToHome(onResult: (FlightCommandResult) -> Unit = {}) {
        scope.launch {
            onResult(executeFlightCommand(
                command = FlightCommandType.RETURN_TO_HOME,
                requiredState = DroneState.IN_AIR,
                pendingState = DroneState.GOING_HOME,
                rollbackState = DroneState.IN_AIR
            ) { callback ->
                startGoHome(callback)
            })
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
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
                scheduleSafetyStop()
            }
        }
    }

    // ========== PARAR MOVIMENTO ==========

    fun stopMovement() {
        Log.d(TAG, "⏹️ Parando movimento")

        // Cancela o job de envio contínuo
        virtualStickJob?.cancel()
        virtualStickJob = null
        safetyStopJob?.cancel()
        safetyStopJob = null

        // Envia comando de parada
        getFlightController()?.sendVirtualStickFlightControlData(
            FlightControlData(0f, 0f, 0f, 0f),
            null
        )

        // Desabilita Virtual Stick após 500ms
        disableVirtualStickJob?.cancel()
        disableVirtualStickJob = scope.launch {
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
            safetyStopJob?.cancel()
            safetyStopJob = null

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
            telemetryManager.updateTelemetry(speed = 0f)
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
        telemetryManager.updateTelemetry(
            altitude = altitude,
            speed = speed,
            distance = distance,
            gps = gps,
            battery = battery
        )
    }

    fun stop() {
        stopMovement()
        disableVirtualStickJob?.cancel()
        disableVirtualStickJob = null
        telemetryManager.stop()
    }

    fun isConnected(): Boolean = getFlightController() != null

    private suspend fun executeFlightCommand(
        command: FlightCommandType,
        requiredState: DroneState,
        pendingState: DroneState,
        rollbackState: DroneState,
        action: FlightController.(CommonCallbacks.CompletionCallback<DJIError>) -> Unit
    ): FlightCommandResult {
        val currentState = _droneState.value
        if (currentState != requiredState) {
            Log.w(TAG, "⚠️ Comando $command ignorado. Estado atual: $currentState")
            return FlightCommandResult.Rejected(
                command = command,
                reason = FlightCommandRejectionReason.INVALID_STATE
            )
        }

        val flightController = getFlightController()
        if (flightController == null) {
            Log.e(TAG, "❌ FlightController não disponível para $command")
            return FlightCommandResult.Rejected(
                command = command,
                reason = FlightCommandRejectionReason.NOT_CONNECTED
            )
        }

        Log.d(TAG, "🚁 Executando comando $command")
        _droneState.value = pendingState

        val result = flightController.awaitCompletion(command, action)
        if (result is FlightCommandResult.Failed) {
            _droneState.value = rollbackState
        }

        return result
    }

    private suspend fun FlightController.awaitCompletion(
        command: FlightCommandType,
        action: FlightController.(CommonCallbacks.CompletionCallback<DJIError>) -> Unit
    ): FlightCommandResult {
        return try {
            withTimeout(COMMAND_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    action(object : CommonCallbacks.CompletionCallback<DJIError> {
                        override fun onResult(error: DJIError?) {
                            if (!continuation.isActive) return

                            val result = if (error == null) {
                                Log.d(TAG, "✅ Comando $command aceito pelo SDK")
                                FlightCommandResult.Accepted(command)
                            } else {
                                Log.e(TAG, "❌ Erro em $command: ${error.description}")
                                FlightCommandResult.Failed(
                                    command = command,
                                    error = FlightCommandError.Sdk(error.description)
                                )
                            }
                            continuation.resume(result)
                        }
                    })
                }
            }
        } catch (_: TimeoutCancellationException) {
            Log.e(TAG, "❌ Timeout aguardando callback para $command")
            FlightCommandResult.Failed(
                command = command,
                error = FlightCommandError.Timeout
            )
        }
    }

    fun takePhoto(onResult: (Boolean, String?) -> Unit) {
        val camera = getCamera()
        if (camera == null) {
            onResult(false, "Câmera indisponível")
            return
        }
        camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE) { modeError ->
            if (modeError != null) {
                onResult(false, modeError.description)
                return@setShootPhotoMode
            }
            camera.startShootPhoto { shootError ->
                if (shootError == null) {
                    onResult(true, null)
                } else {
                    onResult(false, shootError.description)
                }
            }
        }
    }

    fun startRecording(onResult: (Boolean, String?) -> Unit) {
        val camera = getCamera()
        if (camera == null) {
            onResult(false, "Câmera indisponível")
            return
        }
        camera.startRecordVideo { error ->
            if (error == null) {
                onResult(true, null)
            } else {
                onResult(false, error.description)
            }
        }
    }

    fun stopRecording(onResult: (Boolean, String?) -> Unit) {
        val camera = getCamera()
        if (camera == null) {
            onResult(false, "Câmera indisponível")
            return
        }
        camera.stopRecordVideo { error ->
            if (error == null) {
                onResult(true, null)
            } else {
                onResult(false, error.description)
            }
        }
    }

    companion object {
        private const val TAG = "DroneCommand"
        private const val MOVE_SAFETY_TIMEOUT_MS = 1200L
        private const val COMMAND_TIMEOUT_MS = 8_000L
    }
}
