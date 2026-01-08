package com.sloth.registerapp.features.mission.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.features.mission.data.drone.DroneControllerManager

@Composable
fun DroneControlScreen(onMissionsClick: () -> Unit) {
    // TODO: Integrate real drone telemetry from DroneControllerManager
    // TODO: Bind DJI video feed surface texture from camera component
    // TODO: Implement emergency stop button with haptic feedback
    
    val droneController = remember { DroneControllerManager() }
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Telemetry summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Alt: ${telemetry.altitude}m")
            Text("Vel: ${telemetry.speed} m/s")
            Text("Bateria: ${telemetry.batteryLevel}%")
        }

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

