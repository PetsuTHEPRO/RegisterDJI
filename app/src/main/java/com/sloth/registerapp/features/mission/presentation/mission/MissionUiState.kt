package com.sloth.registerapp.ui.mission

import com.sloth.registerapp.model.Mission

sealed class MissionUiState {
    object Idle : MissionUiState()
    object Loading : MissionUiState()
    data class Success(val missions: List<Mission>) : MissionUiState()
    data class Error(val message: String) : MissionUiState()
    object Unauthorized : MissionUiState()
}