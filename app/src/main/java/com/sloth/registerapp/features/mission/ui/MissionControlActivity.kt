package com.sloth.registerapp.features.mission.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.features.mission.data.drone.MissionState
import com.sloth.registerapp.features.mission.data.model.ServerMission
import com.sloth.registerapp.features.mission.data.repository.MissionRepository
import com.sloth.registerapp.features.mission.ui.MissionStatus
import kotlinx.coroutines.launch

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
            apiService = RetrofitClient.getInstance(this),
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
                val missionState = viewModel.missionState.collectAsStateWithLifecycle().value
                val missionData: ServerMission? = viewModel.mission.collectAsStateWithLifecycle().value
                val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

                when {
                    missionData != null -> {
                        MissionControlScreen(
                            missionName = missionData.name,
                            missionStatus = missionState.toUiStatus(),
                            errorMessage = errorMessage,
                            onBackClick = { finish() },
                            onStartMission = { viewModel.startMission() },
                            onPauseMission = { viewModel.pauseMission() },
                            onResumeMission = { viewModel.resumeMission() },
                            onStopMission = { viewModel.stopMission() },
                            onErrorDismiss = { viewModel.clearError() }
                        )
                    }
                    else -> {
                        // Tela de carregamento
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Carregando missão...")
                        }
                    }
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