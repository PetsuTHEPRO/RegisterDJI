package com.sloth.registerapp.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sloth.registerapp.VideoFeedActivity
import com.sloth.registerapp.DJI.DJIConnectionHelper
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

        setContent {
            RegisterAppTheme {
                // Coleta o status da conexão do nosso helper como um estado do Compose
                val droneStatus by DJIConnectionHelper.connectionStatus.collectAsState()

                // Chama a nossa nova tela principal, passando os dados e as ações
                DashboardScreen(
                    droneStatus = droneStatus,
                    onTakePhotoClick = { tirarFoto(this) },
                    onOpenFeedClick = {
                        val intent = Intent(this, VideoFeedActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun tirarFoto(context: Context) {
        val camera = DJIConnectionHelper.getProductInstance()?.camera ?: run {
            Toast.makeText(context, "Drone não conectado!", Toast.LENGTH_SHORT).show()
            return
        }

        camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) { error ->
            if (error == null) {
                Thread.sleep(200)
                camera.startShootPhoto { errorDisparo ->
                    runOnUiThread {
                        if (errorDisparo == null) {
                            Toast.makeText(context, "Foto capturada!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro ao tirar foto: ${errorDisparo.description}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(context, "Erro ao configurar modo: ${error.description}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}