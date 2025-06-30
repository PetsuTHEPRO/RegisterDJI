package com.sloth.registerapp.UI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.DJI.DJIConnectionHelper
import com.sloth.registerapp.VideoFeedActivity
import com.sloth.registerapp.ui.theme.RegisterAppTheme
import com.sloth.registerapp.utils.PermissionHelper
import dji.common.camera.SettingsDefinitions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicia os helpers. Quando as permissões estiverem OK,
        // a ação final será chamar o nosso DJIConnectionHelper.
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = droneStatus, // Usa o estado coletado do helper
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { tirarFoto(this@MainActivity) }) {
                            Text("Tirar Foto (Simulador)")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val intent = Intent(this@MainActivity, VideoFeedActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("Abrir Feed de Vídeo")
                        }
                    }
                }
            }
        }
    }

    // A função tirarFoto agora pega a instância do drone diretamente do Helper.
    private fun tirarFoto(context: Context) {
        val camera = DJIConnectionHelper.getProductInstance()?.camera ?: run {
            Toast.makeText(context, "Drone (Simulador) não conectado ou câmera indisponível!", Toast.LENGTH_SHORT).show()
            return
        }

        camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) { error ->
            if (error == null) {
                // Pequena pausa para garantir a troca de modo
                Thread.sleep(200)
                camera.startShootPhoto { errorDisparo ->
                    runOnUiThread {
                        if (errorDisparo == null) {
                            Toast.makeText(context, "Comando de foto enviado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro ao tirar foto: ${errorDisparo.description}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(context, "Erro ao configurar modo da câmera: ${error.description}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}