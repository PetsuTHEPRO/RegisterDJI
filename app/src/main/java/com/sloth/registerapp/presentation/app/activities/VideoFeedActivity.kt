package com.sloth.registerapp.presentation.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.presentation.mission.screens.DroneCameraScreen
import androidx.compose.runtime.remember

class VideoFeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterAppTheme {
                DroneCameraScreen(
                    droneController = remember { com.sloth.registerapp.features.mission.data.drone.manager.DroneControllerManager() },
                    onCellCameraClick = { /* TODO: open phone camera */ },
                    onSurfaceTextureAvailable = { _, _, _ -> /* TODO: bind texture to DJI feed */ },
                    onSurfaceTextureDestroyed = { false },
                    isFeedAvailable = false,
                    onBackClick = { finish() }
                )
            }
        }
    }
}
