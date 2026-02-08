package com.sloth.registerapp.core.mission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveMissionSessionManager {
    private val _activeMissionId = MutableStateFlow<String?>(null)
    val activeMissionId: StateFlow<String?> = _activeMissionId.asStateFlow()

    fun startMissionSession(missionId: String) {
        _activeMissionId.value = missionId
    }

    fun clearMissionSession() {
        _activeMissionId.value = null
    }
}
