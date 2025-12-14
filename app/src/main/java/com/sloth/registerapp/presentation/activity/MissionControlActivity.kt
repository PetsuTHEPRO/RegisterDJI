package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.data.repository.MissionRepository
import com.sloth.registerapp.data.repository.TokenRepository
import com.sloth.registerapp.presentation.screen.MissionControlScreen
import com.sloth.registerapp.presentation.theme.RegisterAppTheme
import com.sloth.registerapp.presentation.viewmodel.MissionControlViewModel
import kotlinx.coroutines.launch
import com.sloth.registerapp.data.model.ServerMission

class MissionControlActivity : ComponentActivity() {

    private val viewModel: MissionControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val missionId = intent.getIntExtra("MISSION_ID", -1)
        if (missionId == -1) {
            // Handle error: mission ID not provided
            finish()
            return
        }

        val missionRepository = MissionRepository(
            apiService = com.sloth.registerapp.data.network.RetrofitClient.getInstance(this),
            tokenRepository = TokenRepository.getInstance(this)
        )

        lifecycleScope.launch {
            val result = missionRepository.getMission(missionId)
            result.onSuccess { mission ->
                if (mission != null) {
                    viewModel.loadMission(mission)
                } else {
                    // Handle error: mission not found
                    finish()
                }
            }.onFailure {
                // Handle error: failed to fetch mission
                finish()
            }
        }

        setContent {
            RegisterAppTheme {
                val missionState = viewModel.missionState.collectAsState().value
                val missionData: ServerMission? = viewModel._mission.collectAsState().value

                if (missionData != null) {
                    MissionControlScreen(
                        missionName = missionData.name,
                        missionStatus = missionState.toUiStatus(),
                        totalWaypoints = missionData.waypoints.size,
                        // TODO: Get currentWaypoint, progress, altitude, speed, distance, battery, gpsSignal from a drone state provider
                        onBackClick = { finish() },
                        onStartMission = { viewModel.startMission() },
                        onPauseMission = { viewModel.pauseMission() },
                        onResumeMission = { viewModel.resumeMission() },
                        onStopMission = { viewModel.stopMission() },
                        onEmergencyStop = { viewModel.stopMission() }
                    )
                }
            }
        }
    }
}

private fun com.sloth.registerapp.data.drone.MissionState.toUiStatus(): com.sloth.registerapp.presentation.screen.MissionStatus {
    return when (this) {
        com.sloth.registerapp.data.drone.MissionState.IDLE -> com.sloth.registerapp.presentation.screen.MissionStatus.IDLE
        com.sloth.registerapp.data.drone.MissionState.PREPARING -> com.sloth.registerapp.presentation.screen.MissionStatus.IDLE // Or a new "PREPARING" state
        com.sloth.registerapp.data.drone.MissionState.UPLOADING -> com.sloth.registerapp.presentation.screen.MissionStatus.IDLE // Or a new "UPLOADING" state
        com.sloth.registerapp.data.drone.MissionState.UPLOAD_FINISH -> com.sloth.registerapp.presentation.screen.MissionStatus.READY
        com.sloth.registerapp.data.drone.MissionState.READY_TO_EXECUTE -> com.sloth.registerapp.presentation.screen.MissionStatus.READY
        com.sloth.registerapp.data.drone.MissionState.EXECUTING -> com.sloth.registerapp.presentation.screen.MissionStatus.RUNNING
        com.sloth.registerapp.data.drone.MissionState.EXECUTION_PAUSED -> com.sloth.registerapp.presentation.screen.MissionStatus.PAUSED
        com.sloth.registerapp.data.drone.MissionState.FINISHED -> com.sloth.registerapp.presentation.screen.MissionStatus.COMPLETED
        com.sloth.registerapp.data.drone.MissionState.ERROR -> com.sloth.registerapp.presentation.screen.MissionStatus.ERROR
    }
}
