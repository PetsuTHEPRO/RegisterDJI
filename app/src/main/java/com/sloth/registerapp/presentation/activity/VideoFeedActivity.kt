package com.sloth.registerapp.presentation.activity

import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.data.drone.DroneControllerManager
import com.sloth.registerapp.data.drone.DroneState
import com.sloth.registerapp.data.vision.FaceAnalyzer
import com.sloth.registerapp.presentation.screen.DroneCameraScreen
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.launch

class VideoFeedActivity : ComponentActivity() {

    private val TAG = "VideoFeedActivity"

    // Drone Controller
    private lateinit var droneController: DroneControllerManager

    // Vídeo
    private var codecManager: DJICodecManager? = null
    private var videoDataListener: VideoFeeder.VideoDataListener? = null
    private var textureView: TextureView? = null

    // Visão
    private var faceProcessor: FaceAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        Log.d(TAG, "Activity criada")

        // Inicializa o drone controller
        droneController = DroneControllerManager()

        // Configura Compose
        setContent {
            DroneCameraScreen(
                droneController = droneController,
                onCellCameraClick = { switchToPhoneCamera() },
                onTextureViewCreated = { textureView = it }
            )
        }

        setupVisionProcessor()
    }

    private fun setupVisionProcessor() {
        val callback = object : FaceAnalyzer.FaceDetectionCallback {
            override fun onFaceDetected(
                numberOfFaces: Int,
                faces: List<com.google.mlkit.vision.face.Face>,
                rotation: Int,
                frameBitmap: android.graphics.Bitmap
            ) {
                Log.d(TAG, "Rostos detectados: $numberOfFaces")
            }

            override fun onFaceDetectionFailed(e: Exception) {
                Log.e(TAG, "Detecção de rosto falhou", e)
            }

            override fun onNoFacesDetected() {
                // Nenhum rosto detectado
            }
        }

        faceProcessor = FaceAnalyzer(callback)
    }

    private fun switchToPhoneCamera() {
        val intent = Intent(this, PhoneCameraActivity::class.java)
        startActivity(intent)
        finish()
    }

    // ========== FEED DE VÍDEO ==========

    fun initVideoFeed(textureView: TextureView) {
        val primaryFeed = VideoFeeder.getInstance()?.primaryVideoFeed

        if (primaryFeed == null) {
            Toast.makeText(this, "Feed de vídeo não disponível", Toast.LENGTH_LONG).show()
            Log.e(TAG, "VideoFeeder primaryFeed é null")
            return
        }

        videoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture disponível: ${width}x${height}")

                if (codecManager == null) {
                    codecManager = DJICodecManager(this@VideoFeedActivity, surface, width, height)
                    codecManager?.setYuvDataCallback(onYuvDataReceived)
                    Log.d(TAG, "DJICodecManager inicializado")
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture redimensionado: ${width}x${height}")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "SurfaceTexture destruído")
                codecManager?.cleanSurface()
                codecManager = null
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        primaryFeed.addVideoDataListener(videoDataListener!!)
        Log.d(TAG, "VideoDataListener adicionado")
    }

    private val onYuvDataReceived = DJICodecManager.YuvDataCallback { format, yuvFrame, dataSize, width, height ->
        faceProcessor?.let { processor ->
            // TODO: Processar frame se necessário
        }
    }

    private fun stopVideoFeed() {
        videoDataListener?.let {
            VideoFeeder.getInstance()?.primaryVideoFeed?.removeVideoDataListener(it)
            videoDataListener = null
        }

        codecManager?.cleanSurface()
        codecManager?.destroyCodec()
        codecManager = null

        Log.d(TAG, "Feed de vídeo parado")
    }

    // ========== CICLO DE VIDA ==========

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        textureView?.let { initVideoFeed(it) }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        stopVideoFeed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopVideoFeed()
    }
}