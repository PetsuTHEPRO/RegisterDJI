package com.sloth.registerapp.UI

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.VideoFeedActivity
import com.sloth.registerapp.ui.theme.RegisterAppTheme
import com.sloth.registerapp.utils.PermissionHelper
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager

class MainActivity : ComponentActivity() {

    private val TAG = "ApplicationDJI"

    // Referência ao produto DJI conectado (drone/simulador)
    private var mProduct: BaseProduct? = null

    // Variável de estado para a UI, para mostrar o status da conexão
    private var droneStatusText by mutableStateOf("Aguardando permissões...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Inicializa os launchers do nosso Helper.
        // A ação final a ser executada (lambda) é `startSDKRegistration`.
        PermissionHelper.initializeLaunchers(this) {
            startSDKRegistration()
        }

        // 2. Inicia o processo de verificação.
        // A ação final (lambda) é a mesma: `startSDKRegistration`.
        PermissionHelper.checkAndRequestPermissions(this) {
            startSDKRegistration()
        }


        // Configura a interface do usuário com Jetpack Compose
        setContent {
            RegisterAppTheme {
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
                            text = droneStatusText,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { tirarFoto() }) {
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

    // A lógica de permissões foi removida daqui.

    // A lógica de registro do SDK e de interação com o drone permanece na MainActivity.
    private fun startSDKRegistration() {
        runOnUiThread { droneStatusText = "Aguardando conexão com o simulador..." }
        Log.d(TAG, "Iniciando o registro do SDK DJI...")
        DJISDKManager.getInstance().registerApp(this.applicationContext, object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.d(TAG, "Registro do SDK bem-sucedido!")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Registro do SDK bem-sucedido!", Toast.LENGTH_SHORT).show()
                    }
                    DJISDKManager.getInstance().startConnectionToProduct()
                } else {
                    Log.d(TAG, "Falha no registro do SDK: ${error?.description}")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Falha no registro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onProductDisconnect() {
                Log.d(TAG, "Produto DJI desconectado.")
                mProduct = null
                runOnUiThread { droneStatusText = "Produto Desconectado." }
            }

            override fun onProductConnect(product: BaseProduct?) {
                mProduct = product
                val modelName = product?.model?.displayName ?: "Modelo Desconhecido"
                Log.d(TAG, "Produto DJI conectado: $modelName")
                runOnUiThread { droneStatusText = "Conectado a: $modelName" }
            }

            override fun onProductChanged(product: BaseProduct?) {
                mProduct = product
            }

            override fun onComponentChange(key: BaseProduct.ComponentKey?, oldC: BaseComponent?, newC: BaseComponent?) {}
            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    private fun tirarFoto() {
        val camera = mProduct?.camera ?: run {
            Toast.makeText(this, "Drone (Simulador) não conectado ou câmera indisponível!", Toast.LENGTH_SHORT).show()
            return
        }

        camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) { error ->
            if (error == null) {
                Log.d(TAG, "Modo da câmera alterado para 'SHOOT_PHOTO'.")
                Thread.sleep(200)
                camera.startShootPhoto { errorDisparo ->
                    runOnUiThread {
                        if (errorDisparo == null) {
                            Toast.makeText(this, "Comando de foto enviado com sucesso!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Comando de foto enviado!")
                        } else {
                            Toast.makeText(this, "Erro ao tirar foto: ${errorDisparo.description}", Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Erro ao tirar foto: ${errorDisparo.description}")
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Erro ao configurar modo da câmera: ${error.description}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Erro ao configurar modo: ${error.description}")
                }
            }
        }
    }
}