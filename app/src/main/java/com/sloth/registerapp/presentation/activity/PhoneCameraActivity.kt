package com.sloth.registerapp.presentation.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.face.Face
import com.sloth.registerapp.data.vision.FaceRecognitionManager
import com.sloth.registerapp.databinding.ActivityPhoneCameraBinding
import com.sloth.registerapp.presentation.component.FaceOverlayView
import com.sloth.registerapp.vision.FaceAnalysisResult
import com.sloth.registerapp.vision.FaceAnalyzer
import com.sloth.registerapp.vision.FaceAnalyzerConfig
import com.sloth.registerapp.vision.FaceAnalyzerListener
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class PhoneCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneCameraBinding
    private lateinit var faceDetector: FaceAnalyzer
    private lateinit var faceOverlay: FaceOverlayView
    private lateinit var recognitionManager: FaceRecognitionManager
    private var useFrontCamera = true
    private var imageWidth = 0
    private var imageHeight = 0
    private val recognizedNames = mutableMapOf<Int, String>()

    // Para evitar múltiplas requisições simultâneas
    private var isRecognizing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        binding = ActivityPhoneCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: Atividade iniciada.")

        binding.statusConnectionText.text = "Câmera Celular"

        // Inicializa o gerenciador de reconhecimento
        recognitionManager = FaceRecognitionManager(this)

        setupFaceOverlay()
        setupFaceDetector()
        setupButtons()

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun setupFaceOverlay() {
        faceOverlay = FaceOverlayView(this).apply {
            layoutParams = binding.imageView.layoutParams
        }

        val parent = binding.imageView.parent as android.view.ViewGroup
        parent.addView(faceOverlay)
    }

    private fun setupFaceDetector() {
        val config = FaceAnalyzerConfig(
            disallowMultipleFaces = false
        )
        faceDetector = FaceAnalyzer(object : FaceAnalyzerListener {
            override fun onResult(result: FaceAnalysisResult) {
                runOnUiThread {
                    when (result) {
                        is FaceAnalysisResult.FaceDetected -> {
                            val faces = listOf(result.face)
                            faceOverlay.setFaces(faces, recognizedNames, imageWidth, imageHeight, useFrontCamera, 0)
                            if (!isRecognizing && result.bitmap != null) {
                                recognizeFirstFace(result.face, result.bitmap, 0)
                            }
                        }
                        is FaceAnalysisResult.AdvancedResult -> {
                            val faces = listOf(result.face)
                            faceOverlay.setFaces(faces, recognizedNames, imageWidth, imageHeight, useFrontCamera, 0)
                            if (!isRecognizing && result.bitmap != null && result.isStable) {
                                recognizeFirstFace(result.face, result.bitmap, 0)
                            }
                        }
                        is FaceAnalysisResult.MultipleFaces -> {
                            // The config allows multiple faces, but we only recognize the first one.
                            // We can update the overlay with all faces if we want.
                            // For now, just clear.
                            faceOverlay.clear()
                            isRecognizing = false
                        }
                        is FaceAnalysisResult.NoFace -> {
                            faceOverlay.clear()
                            isRecognizing = false
                        }
                        is FaceAnalysisResult.Error -> {
                            faceOverlay.clear()
                            Log.e(TAG, "Erro: ${result.message}")
                        }
                    }
                }
            }
        }, config)
    }

    /**
     * Reconhece a primeira face detectada
     */
    private fun recognizeFirstFace(face: Face, frameBitmap: Bitmap, faceIndex: Int) {
        isRecognizing = true

        lifecycleScope.launch {
            // Aplica rotação se necessário (suponha rotationDegrees)
            frameBitmap

            // Crop da face usando bounding box
            val croppedFace = Bitmap.createBitmap(
                frameBitmap,
                face.boundingBox.left.coerceAtLeast(0),
                face.boundingBox.top.coerceAtLeast(0),
                face.boundingBox.width().coerceAtMost(frameBitmap.width - face.boundingBox.left),
                face.boundingBox.height().coerceAtMost(frameBitmap.height - face.boundingBox.top)
            )

            // Redimensiona para 112x112 (entrada do modelo)
            val resizedFace = Bitmap.createScaledBitmap(croppedFace, 112, 112, true)

            recognitionManager.recognizePerson(resizedFace) { name ->

                runOnUiThread {
                    recognizedNames[faceIndex] = name // associa nome ao índice

                    Log.d(TAG, "Reconhecido: $name")
                    isRecognizing = false
                }
            }
        }
    }

    private fun setupButtons() {
        binding.buttonDroneCamera.setOnClickListener {
            val intent = Intent(this, VideoFeedActivity::class.java)
            startActivity(intent)
        }

        binding.cameraSwitch.setOnCheckedChangeListener { _, isChecked ->
            useFrontCamera = !isChecked
            startCamera()
        }

        binding.buttonToggleOverlay.setOnCheckedChangeListener { _, isChecked ->
            faceOverlay.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewContainer.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height
                        faceDetector.analyze(imageProxy)
                    }
                }

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                Log.d(TAG, "✅ Câmera iniciada (${if(useFrontCamera) "frontal" else "traseira"})")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao iniciar câmera: ${e.message}", e)
                Toast.makeText(this, "❌ Erro na câmera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "❌ Permissão negada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetector.release()
    }

    companion object {
        private const val TAG = "PhoneCamera"
        private const val CAMERA_PERMISSION_CODE = 100
    }
}