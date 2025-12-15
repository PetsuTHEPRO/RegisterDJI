package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.sloth.registerapp.presentation.screen.SettingsScreen
import com.sloth.registerapp.presentation.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {

    // Instancia o ViewModel usando a delegação do KTX
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define o conteúdo da tela usando Jetpack Compose
        setContent {
            MaterialTheme { // Aplica o tema do Material Design 3
                SettingsScreen()
            }
        }
    }
}