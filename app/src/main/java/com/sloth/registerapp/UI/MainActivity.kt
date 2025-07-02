package com.sloth.registerapp.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.DJI.DJIConnectionHelper
import com.sloth.registerapp.DJI.DroneTelemetryManager
import com.sloth.registerapp.ui.theme.RegisterAppTheme
import com.sloth.registerapp.utils.PermissionHelper
import dji.common.camera.SettingsDefinitions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INÍCIO DO BLOCO DE CÓDIGO PARA MODO IMERSIVO ---
        // 1. Prepara a janela para desenhar por trás das barras do sistema (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Obtém o controlador das barras do sistema
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // 3. Esconde as barras de status e de navegação
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // 4. Configura o comportamento para as barras reaparecerem com um deslize
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // --- FIM DO BLOCO DE CÓDIGO ---

        PermissionHelper.initializeLaunchers(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }
        PermissionHelper.checkAndRequestPermissions(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }
        // --- NOVO: Inicia o gerenciador de telemetria ---
        DroneTelemetryManager.init(lifecycleScope)

        setContent {
            RegisterAppTheme {
                // Coleta o status da conexão do nosso helper como um estado do Compose
                val droneStatus by DJIConnectionHelper.connectionStatus.collectAsState()
                val telemetry by DroneTelemetryManager.telemetryData.collectAsState()

                // Chama a nossa nova tela principal, passando os dados e as ações
                DashboardScreen(
                    droneStatus = droneStatus,
                    telemetry = telemetry,
                    onTakePhotoClick = {
                        val intent = Intent(this, PhoneCameraActivity::class.java)
                        startActivity(intent) },
                    onOpenFeedClick = {
                        val intent = Intent(this, VideoFeedActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}