package com.sloth.registerapp.ui.mission

import com.sloth.registerapp.data.model.Mission

sealed interface MissionUiState {
    object Idle : MissionUiState
    object Loading : MissionUiState
    data class Success(val missions: List<Mission>) : MissionUiState
    data class Error(val message: String) : MissionUiState
}
