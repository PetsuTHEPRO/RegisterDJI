package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.data.drone.MissionState
import com.sloth.registerapp.data.repository.MissionRepository
import com.sloth.registerapp.data.repository.TokenRepository
import com.sloth.registerapp.presentation.screen.MissionControlScreen
import com.sloth.registerapp.presentation.theme.RegisterAppTheme
import com.sloth.registerapp.presentation.viewmodel.MissionControlViewModel
import kotlinx.coroutines.launch
import com.sloth.registerapp.data.model.ServerMission
import com.sloth.registerapp.presentation.screen.MissionStatus

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

fun MissionState.toUiStatus(): MissionStatus {
    return when (this) {
        MissionState.IDLE -> MissionStatus.IDLE

        // Agrupa todos os estados de carregamento/processamento
        MissionState.PREPARING,
        MissionState.DOWNLOADING,
        MissionState.UPLOADING ->
            MissionStatus.LOADING

        // Agrupa estados "prontos"
        MissionState.DOWNLOAD_FINISHED,
        MissionState.READY_TO_EXECUTE ->
            MissionStatus.READY

        MissionState.EXECUTING -> MissionStatus.RUNNING
        MissionState.EXECUTION_PAUSED -> MissionStatus.PAUSED
        MissionState.EXECUTION_STOPPED -> MissionStatus.STOPPED
        MissionState.FINISHED -> MissionStatus.COMPLETED
        MissionState.ERROR -> MissionStatus.ERROR
    }
}