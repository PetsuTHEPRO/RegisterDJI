package com.sloth.registerapp.presentation.video.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.settings.AppThemeSettingsRepository
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.presentation.app.theme.AppTheme
import com.sloth.registerapp.presentation.video.screens.CellCameraScreen
import com.sloth.registerapp.presentation.video.screens.DroneCameraScreen

class DroneCameraActivity : ComponentActivity() {

    private var droneCommandManager: DroneCommandManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carrega missão se fornecida via Intent
        val missionIdExtra = intent?.getStringExtra("MISSION_ID")
            ?: intent?.getIntExtra("MISSION_ID", -1)?.takeIf { it != -1 }?.toString()
        val missionNameExtra = intent?.getStringExtra("MISSION_NAME") ?: "Missão Ativa"

        if (!missionIdExtra.isNullOrBlank()) {
            ActiveMissionSessionManager.startMissionSession(missionIdExtra, missionNameExtra)
        }

        val controller = DroneCommandManager()
        droneCommandManager = controller

        setContent {
            val context = LocalContext.current
            val themeRepository = remember(context) { AppThemeSettingsRepository.getInstance(context) }
            val selectedTheme by themeRepository.appTheme.collectAsState(initial = AppThemeSettingsRepository.DEFAULT_THEME)
            val isDarkTheme = when (selectedTheme) {
                AppThemeSettingsRepository.THEME_LIGHT -> false
                AppThemeSettingsRepository.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(controller) {
                onDispose {
                    controller.stop()
                }
            }

            AppTheme(darkTheme = isDarkTheme) {
                var showCellCamera by rememberSaveable { mutableStateOf(false) }
                if (showCellCamera) {
                    CellCameraScreen(onBackClick = { showCellCamera = false })
                } else {
                    DroneCameraScreen(
                        droneController = controller,
                        onCellCameraClick = { showCellCamera = true },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        droneCommandManager?.stop()
        droneCommandManager = null
    }
}
