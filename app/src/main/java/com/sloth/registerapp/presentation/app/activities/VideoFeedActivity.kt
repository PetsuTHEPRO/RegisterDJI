package com.sloth.registerapp.presentation.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.presentation.mission.screens.CellCameraScreen
import com.sloth.registerapp.presentation.mission.screens.DroneCameraScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class VideoFeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Recupera o tema do SettingsScreen (pode ser via ViewModel, DataStore, etc. - aqui exemplo simples)
            var theme by remember { mutableStateOf("Escuro") }
            val isDarkTheme = when (theme) {
                "Claro" -> false
                "Escuro" -> true
                else -> isSystemInDarkTheme()
            }
            com.sloth.registerapp.presentation.app.theme.AppTheme(darkTheme = isDarkTheme) {
                var showCellCamera by remember { mutableStateOf(false) }
                if (showCellCamera) {
                    CellCameraScreen(onBackClick = { showCellCamera = false })
                } else {
                    DroneCameraScreen(
                        droneController = remember { com.sloth.registerapp.features.mission.data.drone.manager.DroneControllerManager() },
                        onCellCameraClick = { showCellCamera = true },
                        onSurfaceTextureAvailable = { _, _, _ -> /* TODO: bind texture to DJI feed */ },
                        onSurfaceTextureDestroyed = { false },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
