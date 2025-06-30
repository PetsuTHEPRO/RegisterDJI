package com.sloth.registerapp.UI

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sloth.registerapp.R
import java.io.IOException

class PhoneCameraActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val TAG = "PhoneCameraActivity"
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_camera)

        surfaceView = findViewById(R.id.phone_camera_preview)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    private fun startCamera() {
        try {
            camera = Camera.open() // Abre a câmera traseira por padrão
            camera?.setPreviewDisplay(surfaceHolder)

            // Configura a orientação correta do preview
            setCameraDisplayOrientation()

            camera?.startPreview()
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao configurar o preview da câmera: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Erro ao abrir a câmera. Ela pode já estar em uso.", e)
            Toast.makeText(this, "Não foi possível acessar a câmera.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun setCameraDisplayOrientation() {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info)
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        // Lógica para determinar a rotação da tela
        // (pode ser simplificada dependendo se a tela pode girar ou não)

        val displayRotation = (info.orientation - degrees + 360) % 360
        camera?.setDisplayOrientation(displayRotation)
    }

    // Métodos do SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        // A câmera será iniciada no onResume após a verificação de permissão
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) {
            return
        }
        // Para e reinicia o preview com as novas configurações de superfície
        try {
            camera?.stopPreview()
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reiniciar o preview: ${e.message}")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopCamera()
    }

    // Gerenciamento do resultado da permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissão de câmera negada. A funcionalidade não está disponível.", Toast.LENGTH_LONG).show()
            }
        }
    }
}