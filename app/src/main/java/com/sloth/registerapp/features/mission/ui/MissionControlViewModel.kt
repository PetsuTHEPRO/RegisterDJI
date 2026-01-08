package com.sloth.registerapp.features.mission.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.features.mission.data.drone.DroneMissionManager
import com.sloth.registerapp.features.mission.data.drone.MissionState
import com.sloth.registerapp.features.mission.data.model.ServerMission
import com.sloth.registerapp.features.mission.data.sdk.DJIConnectionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MissionControlViewModel : ViewModel() {

    private val droneMissionManager = DroneMissionManager(DJIConnectionHelper)

    val missionState: StateFlow<MissionState> = droneMissionManager.missionState

    private val _mission = MutableStateFlow<ServerMission?>(null)
    val mission: StateFlow<ServerMission?> = _mission

    fun loadMission(mission: ServerMission) {
        _mission.value = mission
        viewModelScope.launch {
            droneMissionManager.prepareAndUploadMission(mission)
        }
    }

    fun startMission() {
        viewModelScope.launch {
            droneMissionManager.startMission()
        }
    }

    fun pauseMission() {
        viewModelScope.launch {
            droneMissionManager.pauseMission()
        }
    }

    fun resumeMission() {
        viewModelScope.launch {
            droneMissionManager.resumeMission()
        }
    }

    fun stopMission() {
        viewModelScope.launch {
            droneMissionManager.stopMission()
        }
    }
}
