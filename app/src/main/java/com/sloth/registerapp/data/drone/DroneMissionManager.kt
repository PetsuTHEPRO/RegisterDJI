package com.sloth.registerapp.data.drone

import android.util.Log
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.sdk.mission.MissionControl
import dji.sdk.mission.waypoint.WaypointMissionOperator
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// O enum de estado interno permanece o mesmo
enum class MissionState {
    IDLE,
    PREPARING,
    UPLOADING,
    UPLOAD_FINISH,
    READY_TO_EXECUTE,
    EXECUTING,
    EXECUTION_PAUSED,
    FINISHED,
    ERROR
}

class DroneMissionManager(
    private val djiConnectionHelper: com.sloth.registerapp.data.sdk.DJIConnectionHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val TAG = "DroneMissionManager"
    // API do SDK v4 para obter o operador de missão.
    // É uma boa prática verificar se MissionControl não é nulo.
    private val waypointMissionOperator: WaypointMissionOperator? = MissionControl.getInstance()?.waypointMissionOperator

    private val _missionState = MutableStateFlow(MissionState.IDLE)
    val missionState = _missionState.asStateFlow()

    init {
        scope.launch {
            djiConnectionHelper.product.collect { product ->
                if (product == null) {
                    _missionState.value = MissionState.IDLE
                } else {
                    val model = product.model
                    if (model != null && (
                                model.displayName.contains("Mavic Mini", ignoreCase = true) ||
                                model.displayName.contains("Spark", ignoreCase = true) ||
                                model.displayName.contains("Tello", ignoreCase = true)
                                )) {
                        Log.e(TAG, "O drone conectado (${model.displayName}) não suporta missões de waypoint.")
                        _missionState.value = MissionState.ERROR
                    }
                }
            }
        }
        if (waypointMissionOperator == null) {
            Log.e(TAG, "WaypointMissionOperator is null. Waypoint missions are not supported.")
            _missionState.value = MissionState.ERROR
        }
        // Adiciona um listener para receber atualizações de status do operador de missão
        waypointMissionOperator?.addListener(object : WaypointMissionOperatorListener {
            override fun onDownloadUpdate(event: WaypointMissionDownloadEvent) {}

            override fun onUploadUpdate(event: WaypointMissionUploadEvent) {
                val currentState = event.currentState
                if (currentState == WaypointMissionState.UPLOADING) {
                    _missionState.value = MissionState.UPLOADING
                } else if (currentState == WaypointMissionState.READY_TO_EXECUTE) {
                    _missionState.value = MissionState.UPLOAD_FINISH
                }
            }
            
            override fun onExecutionStart() {
                _missionState.value = MissionState.EXECUTING
            }

            override fun onExecutionUpdate(event: WaypointMissionExecutionEvent) {
                val currentState = event.currentState
                if (currentState == WaypointMissionState.EXECUTING) {
                    _missionState.value = MissionState.EXECUTING
                } else if (currentState == WaypointMissionState.EXECUTION_PAUSED) {
                    _missionState.value = MissionState.EXECUTION_PAUSED
                }
            }

            override fun onExecutionFinish(error: DJIError?) {
                if (error == null) {
                    _missionState.value = MissionState.FINISHED
                    Log.i(TAG, "Missão v4 concluída com sucesso!")
                } else {
                    _missionState.value = MissionState.ERROR
                    Log.e(TAG, "Erro na conclusão da missão v4: ${error.description}")
                }
            }
        })
    }

    /**
     * Prepara, carrega e faz o upload de uma missão v4 para o drone.
     */
    fun prepareAndUploadMission(missionData: com.sloth.registerapp.data.model.ServerMission) {
        val operator = waypointMissionOperator ?: run {
            Log.e(TAG, "WaypointMissionOperator não está disponível.")
            _missionState.value = MissionState.ERROR
            return
        }

        _missionState.value = MissionState.PREPARING

        val waypointList = missionData.waypoints.map {
            Waypoint(it.latitude, it.longitude, it.altitude.toFloat())
        }

        if (waypointList.isEmpty()) {
            Log.e(TAG, "Não é possível criar missão sem waypoints.")
            _missionState.value = MissionState.ERROR
            return
        }

        val missionBuilder = WaypointMission.Builder().apply {
            finishedAction(WaypointMissionFinishedAction.valueOf(missionData.finished_action))
            headingMode(WaypointMissionHeadingMode.valueOf(missionData.heading_mode))
            autoFlightSpeed(missionData.auto_flight_speed.toFloat())
            maxFlightSpeed(missionData.max_flight_speed.toFloat())
            flightPathMode(WaypointMissionFlightPathMode.valueOf(missionData.flight_path_mode))
            waypointList(waypointList)
            waypointCount(waypointList.size)
        }

        val mission = missionBuilder.build()
        val loadError = operator.loadMission(mission)
        if (loadError != null) {
            Log.e(TAG, "Erro ao carregar a missão v4: ${loadError.description}")
            _missionState.value = MissionState.ERROR
            return
        }

        operator.uploadMission { error ->
            if (error == null) {
                Log.i(TAG, "Upload da missão v4 concluído com sucesso!")
                _missionState.value = MissionState.UPLOAD_FINISH
            } else {
                Log.e(TAG, "Falha no upload da missão v4: ${error.description}")
                _missionState.value = MissionState.ERROR
            }
        }
    }

    fun startMission() {
        if (waypointMissionOperator?.currentState == WaypointMissionState.READY_TO_EXECUTE) {
            waypointMissionOperator?.startMission { error ->
                if (error != null) {
                    Log.e(TAG, "Falha ao iniciar a missão v4: ${error.description}")
                    _missionState.value = MissionState.ERROR
                }
            }
        } else {
            Log.w(TAG, "Não foi possível iniciar a missão. Estado atual: ${waypointMissionOperator?.currentState?.name}")
        }
    }

    fun stopMission() {
        waypointMissionOperator?.stopMission { error ->
            if (error == null) {
                _missionState.value = MissionState.IDLE
            } else {
                Log.e(TAG, "Falha ao parar a missão v4: ${error.description}")
                _missionState.value = MissionState.ERROR
            }
        }
    }

    fun pauseMission() {
        waypointMissionOperator?.pauseMission { error ->
            if (error != null) {
                Log.e(TAG, "Falha ao pausar a missão v4: ${error.description}")
                _missionState.value = MissionState.ERROR
            }
        }
    }

    fun resumeMission() {
        waypointMissionOperator?.resumeMission { error ->
            if (error != null) {
                Log.e(TAG, "Falha ao retomar a missão v4: ${error.description}")
                _missionState.value = MissionState.ERROR
            }
        }
    }
}