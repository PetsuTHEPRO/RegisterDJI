package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.sloth.registerapp.data.drone.DroneControllerManager
import com.sloth.registerapp.data.drone.MissionManager
import com.sloth.registerapp.presentation.screen.MissionsTableScreen
import com.sloth.registerapp.ui.mission.MissionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


class MissionActivity : ComponentActivity() {
    // Instancia o ViewModel usando a delegação do KTX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define o conteúdo da tela usando Jetpack Compose
        setContent {
            MaterialTheme { // Aplica o tema do Material Design 3
                MissionsTableScreen(
                    onBackClick = {}
                )
            }
        }
    }
}