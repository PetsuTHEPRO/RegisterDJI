package com.sloth.registerapp.features.mission.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar a UI de missões de drone
 * 
 * Exemplo de implementação da refatoração DroneMissionManager
 */
class MissionViewModel(
    private val missionManager: DroneMissionManager
) : ViewModel() {

    companion object {
        private const val TAG = "MissionViewModel"
    }

    // Estados da UI
    private val _uiState = MutableStateFlow<MissionUiState>(MissionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Eventos para toast/snackbar
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeMissionState()
    }

    /**
     * Observa mudanças no estado da missão
     */
    private fun observeMissionState() {
        viewModelScope.launch {
            missionManager.missionState.collect { state ->
                val newUiState = when (state) {
                    MissionState.IDLE -> MissionUiState.Idle
                    MissionState.PREPARING -> MissionUiState.Preparing
                    MissionState.DOWNLOADING -> MissionUiState.Downloading
                    MissionState.DOWNLOAD_FINISHED -> MissionUiState.DownloadFinished
                    MissionState.UPLOADING -> MissionUiState.Uploading
                    MissionState.READY_TO_EXECUTE -> MissionUiState.ReadyToExecute
                    MissionState.EXECUTING -> MissionUiState.Executing
                    MissionState.EXECUTION_PAUSED -> MissionUiState.Paused
                    MissionState.EXECUTION_STOPPED -> MissionUiState.Stopped
                    MissionState.FINISHED -> MissionUiState.Finished
                    MissionState.ERROR -> MissionUiState.Error
                }
                _uiState.value = newUiState
            }
        }
    }

    /**
     * Prepara e faz upload de uma missão para o drone
     */
    fun prepareAndUploadMission(missionData: ServerMissionDto) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📤 Iniciando upload da missão...")
                missionManager.prepareAndUploadMission(missionData)
                _uiEvent.emit(
                    UiEvent.ShowMessage("✅ Missão pronta para executar!")
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "❌ Validação falhou: ${e.message}")
                _uiEvent.emit(
                    UiEvent.ShowError("Erro de validação: ${e.message}")
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao fazer upload: ${e.message}", e)
                _uiEvent.emit(
                    UiEvent.ShowError("Erro ao fazer upload: ${e.message}")
                )
            }
        }
    }

    /**
     * Inicia a execução da missão
     */
    fun startMission() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "▶️ Iniciando missão...")
                missionManager.startMission()
                _uiEvent.emit(
                    UiEvent.ShowMessage("✅ Missão iniciada!")
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao iniciar missão: ${e.message}", e)
                _uiEvent.emit(
                    UiEvent.ShowError("Erro ao iniciar: ${e.message}")
                )
            }
        }
    }

    /**
     * Pausa a execução da missão
     */
    fun pauseMission() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⏸️ Pausando missão...")
                missionManager.pauseMission()
                _uiEvent.emit(
                    UiEvent.ShowMessage("⏸️ Missão pausada")
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao pausar missão: ${e.message}", e)
                _uiEvent.emit(
                    UiEvent.ShowError("Erro ao pausar: ${e.message}")
                )
            }
        }
    }

    /**
     * Retoma a execução da missão
     */
    fun resumeMission() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "▶️ Retomando missão...")
                missionManager.resumeMission()
                _uiEvent.emit(
                    UiEvent.ShowMessage("▶️ Missão retomada")
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao retomar missão: ${e.message}", e)
                _uiEvent.emit(
                    UiEvent.ShowError("Erro ao retomar: ${e.message}")
                )
            }
        }
    }

    /**
     * Para a execução da missão
     */
    fun stopMission() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⏹️ Parando missão...")
                missionManager.stopMission()
                _uiEvent.emit(
                    UiEvent.ShowMessage("⏹️ Missão parada")
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao parar missão: ${e.message}", e)
                _uiEvent.emit(
                    UiEvent.ShowError("Erro ao parar: ${e.message}")
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpando recursos...")
        missionManager.destroy()
    }
}

/**
 * Estados da UI baseados nos estados da missão
 */
sealed class MissionUiState {
    object Idle : MissionUiState()
    object Preparing : MissionUiState()
    object Downloading : MissionUiState()
    object DownloadFinished : MissionUiState()
    object Uploading : MissionUiState()
    object ReadyToExecute : MissionUiState()
    object Executing : MissionUiState()
    object Paused : MissionUiState()
    object Stopped : MissionUiState()
    object Finished : MissionUiState()
    object Error : MissionUiState()

    fun isLoading(): Boolean = this is Preparing || this is Uploading || this is Downloading

    fun isExecuting(): Boolean = this is Executing || this is Paused

    fun isReadyToStart(): Boolean = this is ReadyToExecute
}

/**
 * Eventos que disparam ações na UI (Toasts, Snackbars, etc)
 */
sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}
