package com.sloth.registerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.sloth.registerapp.ui.theme.RegisterAppTheme
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager

class MainActivity : ComponentActivity() {

    private val TAG = "ApplicationDJI"

    // Lista de permissões necessárias para o app
    private val REQUIRED_PERMISSION_LIST: Array<String> = arrayOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    )

    // Referência ao produto DJI conectado (drone/simulador)
    private var mProduct: BaseProduct? = null

    // Variável de estado para a UI, para mostrar o status da conexão
    private var droneStatusText by mutableStateOf("Aguardando conexão com o simulador...")

    // Launchers para gerenciar os resultados das solicitações de permissão
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var systemAlertWindowLauncher: ActivityResultLauncher<Intent>
    private lateinit var manageExternalStorageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa os launchers para permissões
        initializePermissionLaunchers()

        // Inicia o processo de verificação e solicitação de permissões
        checkAndRequestPermissions()

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
                        // Texto que exibe o status dinâmico do drone
                        Text(
                            text = droneStatusText,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Botão para enviar o comando de tirar foto
                        Button(onClick = {
                            tirarFoto()
                        }) {
                            Text("Tirar Foto (Simulador)")
                        }
                    }
                }
            }
        }
    }

    private fun initializePermissionLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (permissionsMap.all { it.value }) {
                Log.d(TAG, "Todas as permissões de runtime concedidas.")
                checkSpecialPermissionsAndRegisterSDK()
            } else {
                val deniedPermissions = permissionsMap.filter { !it.value }.keys
                Log.d(TAG, "Permissões de runtime negadas: ${deniedPermissions.joinToString()}")
                Toast.makeText(this, "Permissões necessárias negadas.", Toast.LENGTH_LONG).show()
            }
        }

        systemAlertWindowLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissionsAndRegisterSDK()
        }

        manageExternalStorageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissionsAndRegisterSDK()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSION_LIST.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "Todas as permissões de runtime já concedidas.")
            checkSpecialPermissionsAndRegisterSDK()
        }
    }

    private fun checkSpecialPermissionsAndRegisterSDK() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            systemAlertWindowLauncher.launch(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            manageExternalStorageLauncher.launch(intent)
            return
        }

        startSDKRegistration()
    }

    private fun startSDKRegistration() {
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
                // Uma pequena pausa pode ajudar a garantir que a câmera trocou de modo antes do disparo
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