package com.sloth.registerapp.UI

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sloth.registerapp.UI.Screen.TelemetryScreen
import com.sloth.registerapp.dji.DroneTelemetryManager
import com.sloth.registerapp.ui.theme.RegisterAppTheme

class TelemetryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opcional: Ativa o modo imersivo também nesta tela
        // 1. Prepara a janela para desenhar por trás das barras do sistema (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Obtém o controlador das barras do sistema
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // 3. Esconde as barras de status e de navegação
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // 4. Configura o comportamento para as barras reaparecerem com um deslize
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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