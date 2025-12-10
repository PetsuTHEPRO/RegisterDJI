package com.sloth.registerapp.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sloth.registerapp.data.model.Mission
import androidx.compose.material3.Button

import com.sloth.registerapp.ui.mission.MissionUiState
import com.sloth.registerapp.ui.mission.MissionViewModel

@Composable
fun MissionScreen(
    viewModel: MissionViewModel,
    onMissionClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is MissionUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is MissionUiState.Error -> {
                    Text("Error: ${state.message}")
                }
                is MissionUiState.Success -> {
                    MissionList(
                        missions = state.missions,
                        onMissionClick = onMissionClick
                    )
                }
                is MissionUiState.Idle -> {
                    Text("Fetching missions...")
                    viewModel.fetchMissions()
                }
            }
        }
    }
}

@Composable
fun MissionList(
    missions: List<Mission>,
    onMissionClick: (String) -> Unit
) {
    LazyColumn {
        items(missions) { mission ->
            Button(onClick = { onMissionClick(mission.id) }) {
                Text(text = mission.name)
            }
        }
    }
}