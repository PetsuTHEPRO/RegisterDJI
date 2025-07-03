package com.sloth.registerapp.UI

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sloth.registerapp.UI.Screen.TelemetryScreen
import com.sloth.registerapp.dji.DroneTelemetryManager
import com.sloth.registerapp.ui.theme.RegisterAppTheme

class TelemetryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opcional: Ativa o modo imersivo também nesta tela

        setContent {
            RegisterAppTheme {
                // Coleta os dados de telemetria do nosso gerenciador
                val telemetryData by DroneTelemetryManager.telemetryData.collectAsState()
                // Passa os dados para a nossa tela de UI
                TelemetryScreen(telemetryData = telemetryData)
            }
        }
    }
}