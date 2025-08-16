package com.sloth.registerapp.UI

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sloth.registerapp.databinding.ActivityPhoneCameraBinding // Importe a classe de binding gerada
import com.sloth.registerapp.vision.CameraPreviewManager
import com.sloth.registerapp.vision.FaceDetectionController
import com.sloth.registerapp.vision.Permissions

class PhoneCameraActivity : AppCompatActivity() {

    // 1. Declaração do objeto de View Binding
    private lateinit var binding: ActivityPhoneCameraBinding

    private var cameraPreviewManager: CameraPreviewManager? = null
    private var faceDetectionController: FaceDetectionController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Infla o layout usando o View Binding
        binding = ActivityPhoneCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissionsAndInitialize()
    }

    // Configura a interface e os listeners
    private fun setupUI() {
        // Simulação de atualização da barra de status
        binding.statusConnectionText.text = "Câmera Celular"
        binding.statusBatteryText.text = "98%"

        // Listener para o botão de trocar de câmera
        binding.buttonSwitchCamera.setOnClickListener {
            // ALTERADO: O botão agora apenas inverte o estado do Switch.
            // Ele não chama mais o cameraPreviewManager diretamente.
            binding.cameraSwitch.toggle()
        }

        // Listener para o Switch
        binding.cameraSwitch.setOnCheckedChangeListener { _, isChecked ->
            // ALTERADO: Agora, este é o ÚNICO lugar que comanda a troca de câmera.
            // Ele passa o novo estado (isChecked) para o método.
            // Funciona tanto se o usuário clicar no Switch quanto no ImageButton.
            cameraPreviewManager?.switchCamera(isChecked)
        }

    }

    // Verifica as permissões e inicializa a câmera
    private fun checkPermissionsAndInitialize() {
        if (Permissions.checkAndRequestPermissions(this)) {
            startCameraAndDetection()
        }
    }

    private fun startCameraAndDetection() {
        // 3. Acessa as views de forma segura através do objeto 'binding'
        cameraPreviewManager = CameraPreviewManager(this)

        faceDetectionController = FaceDetectionController(this)
        faceDetectionController?.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCameraAndDetection()
            } else {
                Toast.makeText(this, "As permissões são necessárias para usar o aplicativo.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        faceDetectionController?.stop()
        cameraPreviewManager?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (faceDetectionController != null && cameraPreviewManager != null) {
            cameraPreviewManager?.restartCamera()
            faceDetectionController?.start()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}