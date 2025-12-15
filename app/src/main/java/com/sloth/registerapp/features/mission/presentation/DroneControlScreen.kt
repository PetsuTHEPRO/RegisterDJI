package com.sloth.registerapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp



import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import com.sloth.registerapp.data.datasource.DroneTelemetryManager

import androidx.compose.runtime.remember

import com.sloth.registerapp.data.drone.DroneControllerManager

// ... (imports for telemetry)

@Composable
fun DroneControlScreen(onMissionsClick: () -> Unit) {
    val telemetryData by DroneTelemetryManager.telemetryData.collectAsState()
    val droneController = remember { DroneControllerManager() }

    Column(modifier = Modifier.fillMaxSize()) {
        // ... (Video Feed and Telemetry)

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { droneController.takeOff() }) {
                Text("Take Off")
            }
            Button(onClick = { droneController.land() }) {
                Text("Land")
            }
            Button(onClick = { droneController.returnToHome() }) {
                Text("Return Home")
            }
            Button(onClick = onMissionsClick) {
                Text("Missions")
            }
        }
    }
}
