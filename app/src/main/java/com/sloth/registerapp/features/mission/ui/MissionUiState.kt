package com.sloth.registerapp.features.mission.ui

import com.sloth.registerapp.features.mission.domain.model.Mission

sealed class MissionUiState {
    object Idle : MissionUiState()
    object Loading : MissionUiState()
    data class Success(val missions: List<Mission>) : MissionUiState()
    data class Error(val message: String) : MissionUiState()
    object Unauthorized : MissionUiState()
}