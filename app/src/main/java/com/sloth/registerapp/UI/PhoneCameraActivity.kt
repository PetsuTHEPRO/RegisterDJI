package com.sloth.registerapp.UI

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log // Importe a classe Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sloth.registerapp.databinding.ActivityPhoneCameraBinding
import com.sloth.registerapp.vision.CameraPreviewManager
import com.sloth.registerapp.vision.FaceDetectionController
import com.sloth.registerapp.vision.Permissions

class PhoneCameraActivity : AppCompatActivity() {

    // Adicione uma TAG para filtrar os logs
    private val TAG = "CameraDebug"

    private lateinit var binding: ActivityPhoneCameraBinding
    private var cameraPreviewManager: CameraPreviewManager? = null
    private var faceDetectionController: FaceDetectionController? = null

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

        binding = ActivityPhoneCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: Atividade iniciada.")
        setupUI()
        if (Permissions.checkAndRequestPermissions(this)) {
            Log.d(TAG, "onCreate: Permissões já concedidas. Inicializando a câmera.")
            initializeCameraAndDetection()
        } else {
            Log.d(TAG, "onCreate: Permissões não concedidas. Solicitando agora.")
        }
    }

    private fun setupUI() {
        binding.statusConnectionText.text = "Câmera Celular"
        binding.statusBatteryText.text = "98%"

        binding.buttonDroneCamera.setOnClickListener {
            val intent = Intent(this, VideoFeedActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonSwitchCamera.setOnClickListener {
            // DEBUG: 1. O clique está funcionando?
            Log.d(TAG, "onClick: Botão switchCamera foi clicado.")

            // DEBUG: 2. O cameraPreviewManager é nulo?
            if (cameraPreviewManager == null) {
                Log.e(TAG, "onClick: ERRO! cameraPreviewManager está NULO. A função switchCamera() não será chamada.")
            } else {
                Log.d(TAG, "onClick: cameraPreviewManager existe. Chamando switchCamera().")
                cameraPreviewManager?.switchCamera()
            }
        }
    }

    private fun initializeCameraAndDetection() {
        Log.d(TAG, "initializeCameraAndDetection: Tentando inicializar os managers.")
        if (cameraPreviewManager == null) {
            cameraPreviewManager = CameraPreviewManager(this)
            Log.d(TAG, "initializeCameraAndDetection: cameraPreviewManager foi CRIADO.")
        }
        if (faceDetectionController == null) {
            faceDetectionController = FaceDetectionController(this)
            Log.d(TAG, "initializeCameraAndDetection: faceDetectionController foi CRIADO.")
        }
        faceDetectionController?.start()
    }

    // Nota: Esta função parece redundante com a de cima. Pode ser uma fonte de bugs.
    private fun startCameraAndDetection() {
        Log.d(TAG, "startCameraAndDetection: Função chamada (geralmente após permissão).")
        cameraPreviewManager = CameraPreviewManager(this)
        Log.d(TAG, "startCameraAndDetection: cameraPreviewManager foi (RE)CRIADO.")
        faceDetectionController = FaceDetectionController(this)
        faceDetectionController?.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "onRequestPermissionsResult: Permissões CONCEDIDAS pelo usuário.")
                startCameraAndDetection() // Esta função é chamada
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Permissões NEGADAS pelo usuário.")
                Toast.makeText(this, "As permissões são necessárias para usar o aplicativo.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // ... o resto do seu código (onPause, onResume, companion object) ...
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Atividade pausada.")
        faceDetectionController?.stop()
        cameraPreviewManager?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Atividade retomada.")
        if (faceDetectionController != null && cameraPreviewManager != null) {
            cameraPreviewManager?.restartCamera()
            faceDetectionController?.start()
        } else {
            Log.w(TAG, "onResume: Managers ainda nulos, não foi possível reiniciar a câmera.")
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}