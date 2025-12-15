package com.sloth.registerapp.presentation.activity

import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sloth.registerapp.data.drone.DroneControllerManager
import com.sloth.registerapp.presentation.screen.DroneCameraScreen
import dji.midware.usb.P3.UsbAccessoryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sloth.registerapp.core.utils.yuvToBitmap

import com.sloth.registerapp.features.vision.FaceAnalysisResult
import com.sloth.registerapp.features.vision.FaceAnalyzer
import com.sloth.registerapp.features.vision.FaceAnalyzerListener
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager

class VideoFeedActivity : ComponentActivity() {

    private val TAG = "VideoFeedActivity"

    private lateinit var droneController: DroneControllerManager
    private var codecManager: DJICodecManager? = null
    private var videoDataListener: VideoFeeder.VideoDataListener? = null
    private var faceProcessor: FaceAnalyzer? = null
    
    // NOVO: Estado para a disponibilidade do feed de vídeo
    private val _isFeedAvailable = MutableStateFlow<Boolean>(false)
    val isFeedAvailable = _isFeedAvailable.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        Log.d(TAG, "Activity criada")

        droneController = DroneControllerManager()
        setupVisionProcessor()

        setContent {
            val feedAvailableState by isFeedAvailable.collectAsState() // Observa o estado
            DroneCameraScreen(
                droneController = droneController,
                onCellCameraClick = { switchToPhoneCamera() },
                onSurfaceTextureAvailable = { surface, width, height ->
                    initVideoFeed(surface, width, height)
                },
                onSurfaceTextureDestroyed = {
                    stopVideoFeed()
                    true // Indica que a textura foi liberada
                },
                isFeedAvailable = feedAvailableState // Passa o estado para o Composable
            )
        }
    }
// ... (resto da classe VideoFeedActivity)


    private fun initVideoFeed(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "Inicializando o feed de vídeo com a superfície")
        val primaryFeed = VideoFeeder.getInstance()?.primaryVideoFeed

        if (primaryFeed == null) {
            Toast.makeText(this, "Feed de vídeo não disponível", Toast.LENGTH_LONG).show()
            Log.e(TAG, "VideoFeeder primaryFeed é null")
            _isFeedAvailable.value = false // Atualiza o estado
            return
        }

        // Se o listener já existir, remova-o antes de adicionar um novo
        videoDataListener?.let { primaryFeed.removeVideoDataListener(it) }

        videoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size, UsbAccessoryService.VideoStreamSource.Camera.index)
        }
        
        primaryFeed.addVideoDataListener(videoDataListener!!)
        Log.d(TAG, "VideoDataListener adicionado")
        
        // Inicializa o codec manager
        if (codecManager == null) {
            Log.d(TAG, "Criando novo DJICodecManager")
            codecManager = DJICodecManager(this, surface, width, height, UsbAccessoryService.VideoStreamSource.Camera)
            codecManager?.setYuvDataCallback(onYuvDataReceived)
        }
        _isFeedAvailable.value = true // Atualiza o estado: feed disponível
    }

    private fun stopVideoFeed() {
        videoDataListener?.let {
            VideoFeeder.getInstance()?.primaryVideoFeed?.removeVideoDataListener(it)
            videoDataListener = null
            Log.d(TAG, "VideoDataListener removido")
        }

        codecManager?.destroyCodec()
        codecManager = null
        Log.d(TAG, "Feed de vídeo e codec parados")
        _isFeedAvailable.value = false // Atualiza o estado: feed indisponível
    }

    private val onYuvDataReceived = DJICodecManager.YuvDataCallback { format, yuvFrame, dataSize, width, height ->
        faceProcessor?.let {
            val bytes = ByteArray(yuvFrame.remaining())
            yuvFrame.get(bytes)
            val bitmap = yuvToBitmap(bytes, width, height)
            if (bitmap != null) {
                it.analyze(bitmap, 0)
            }
        }
    }
    
    // ========== Métodos de UI e Ciclo de Vida ==========
    
    private fun setupVisionProcessor() {
        faceProcessor = FaceAnalyzer(object : FaceAnalyzerListener {
            override fun onResult(result: FaceAnalysisResult) {
                when(result) {
                    is FaceAnalysisResult.FaceDetected -> {
                        Log.d(TAG, "Rostos detectados: 1")
                    }
                    is FaceAnalysisResult.MultipleFaces -> {
                        Log.d(TAG, "Rostos detectados: >1")
                    }
                    else -> {}
                }
            }
        })
    }

    private fun switchToPhoneCamera() {
        val intent = Intent(this, PhoneCameraActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        faceProcessor?.release()
    }
}