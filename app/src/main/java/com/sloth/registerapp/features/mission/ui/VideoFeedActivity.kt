package com.sloth.registerapp.features.mission.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.features.mission.data.drone.DroneControllerManager

class VideoFeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterAppTheme {
                val droneController = remember { DroneControllerManager() }
                DroneCameraScreen(
                    droneController = droneController,
                    onCellCameraClick = {},
                    onSurfaceTextureAvailable = { _, _, _ -> },
                    onSurfaceTextureDestroyed = { false },
                    isFeedAvailable = false,
                    onBackClick = { finish() }
                )
            }
        }
    }
}
