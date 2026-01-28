package com.sloth.registerapp.features.mission.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.core.dji.DJIException
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MissionControlViewModel : ViewModel() {

    companion object {
        private const val TAG = "MissionControlViewModel"
    }

    private val droneMissionManager = DroneMissionManager(DJIConnectionHelper)

    val missionState: StateFlow<MissionState> = droneMissionManager.missionState

    private val _mission = MutableStateFlow<ServerMissionDto?>(null)
    val mission: StateFlow<ServerMissionDto?> = _mission

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadMission(mission: ServerMissionDto) {
        _mission.value = mission
        _errorMessage.value = null // Limpar erro anterior
        
        viewModelScope.launch {
            try {
                Log.i(TAG, "Iniciando carregamento da missão: ${mission.name}")
                droneMissionManager.prepareAndUploadMission(mission)
                Log.i(TAG, "✅ Missão carregada com sucesso!")
                _errorMessage.value = null
            } catch (e: DJIException) {
                Log.e(TAG, "❌ Erro ao carregar missão: ${e.message}")
                _errorMessage.value = e.message ?: "Erro desconhecido ao carregar missão"
                // NÃO relança a exceção - permite que o app continue funcionando
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro inesperado: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = e.message ?: "Erro inesperado ao carregar missão"
                // NÃO relança a exceção - permite que o app continue funcionando
            }
        }
    }

    fun startMission() {
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                droneMissionManager.startMission()
                Log.i(TAG, "✅ Missão iniciada!")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao iniciar missão: ${e.message}")
                _errorMessage.value = e.message ?: "Erro ao iniciar missão"
            }
        }
    }

    fun pauseMission() {
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                droneMissionManager.pauseMission()
                Log.i(TAG, "✅ Missão pausada!")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao pausar missão: ${e.message}")
                _errorMessage.value = e.message ?: "Erro ao pausar missão"
            }
        }
    }

    fun resumeMission() {
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                droneMissionManager.resumeMission()
                Log.i(TAG, "✅ Missão retomada!")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao retomar missão: ${e.message}")
                _errorMessage.value = e.message ?: "Erro ao retomar missão"
            }
        }
    }

    fun stopMission() {
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                droneMissionManager.stopMission()
                Log.i(TAG, "✅ Missão parada!")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao parar missão: ${e.message}")
                _errorMessage.value = e.message ?: "Erro ao parar missão"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
