package com.sloth.registerapp.UI

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.DJI.DJIConnectionHelper
import com.sloth.registerapp.R
import com.sloth.registerapp.UI.Screen.DashboardScreen
import com.sloth.registerapp.UI.Screen.PermissionsScreen
import com.sloth.registerapp.dji.DroneTelemetryManager
import com.sloth.registerapp.ui.theme.RegisterAppTheme
import com.sloth.registerapp.utils.PermissionHelper
import com.sloth.registerapp.vision.FaceDetectionController
import com.sloth.registerapp.vision.Permissions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Prepara a janela para desenhar por trás das barras do sistema (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Obtém o controlador das barras do sistema
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // 3. Esconde as barras de status e de navegação
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // 4. Configura o comportamento para as barras reaparecerem com um deslize
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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

                // Chama a nossa nova tela principal, passando os dados e as ações
                DashboardScreen(
                    droneStatus = droneStatus,
                    onVideoFeedClick = {
                        val intent = Intent(this, VideoFeedActivity::class.java)
                        startActivity(intent)
                    },
                    onGalleryClick = {
                        val intent = Intent(this, GalleryActivity::class.java)
                        startActivity(intent)
                    },
                    onStartMissionClick = {
                        val intent = Intent(this, TelemetryActivity::class.java)
                        startActivity(intent)
                                          },
                    onRetryConnectionClick = {
                        // Simplesmente chama a função de registro novamente
                        DJIConnectionHelper.registerApp(applicationContext)
                    },
                    onSettingsClick = {
                        val intent = Intent(this, PermissionActivity::class.java)
                        startActivity(intent)
                    },
                    onAboutClick = {
                        val intent = Intent(this, AboutActivity::class.java)
                        startActivity(intent)
                        //Permissions.checkAndRequestPermissions(this)
                        //setContentView(R.layout.activity_main)
                        //FaceDetectionController(this);
                    }
                )
            }
        }
    }
}