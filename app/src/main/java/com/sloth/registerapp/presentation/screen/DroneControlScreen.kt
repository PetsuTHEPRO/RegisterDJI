package com.sloth.registerapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sloth.registerapp.data.datasource.DroneTelemetryManager
import com.sloth.registerapp.presentation.component.VideoFeedView
import kotlin.math.sqrt

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

@Composable
fun TelemetryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label)
        Text(text = value)
    }
}
