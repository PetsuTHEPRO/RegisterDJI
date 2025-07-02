package com.sloth.registerapp.UI

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.face.Face
import com.sloth.registerapp.R
import com.sloth.registerapp.vision.FaceDetectionProcessor
import com.sloth.registerapp.vision.ICameraSource
import com.sloth.registerapp.vision.OverlayView
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import java.nio.ByteBuffer

// 1. A classe agora implementa a nossa interface ICameraSource
class VideoFeedActivity : AppCompatActivity(), ICameraSource {

    private val TAG = "VideoFeedActivity"

    // --- Variáveis de UI e Detecção ---
    private lateinit var videoFeedView: TextureView
    private lateinit var overlayView: OverlayView
    private var faceProcessor: FaceDetectionProcessor? = null
    private var frameListener: ICameraSource.FrameListener? = null

    // --- Variáveis do DJI SDK ---
    private var codecManager: DJICodecManager? = null
    private var videoDataListener: VideoFeeder.VideoDataListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_feed) // Certifique-se que este layout tem OverlayView

        videoFeedView = findViewById(R.id.textureView)
        overlayView = findViewById(R.id.overlay_view)

        setupVisionProcessor()

        // Inicia a fonte de câmera (o drone) e passa o processador como listener
        start(faceProcessor!!)
    }

    private fun setupVisionProcessor() {
        val callback = object : FaceDetectionProcessor.FaceDetectionCallback {
            override fun onFaceDetected(numberOfFaces: Int, faces: List<Face>, frameData: ICameraSource.FrameData) {
                runOnUiThread { overlayView.setFaces(faces, frameData) }
            }
            override fun onFaceDetectionFailed(e: Exception) { Log.e(TAG, "Detecção de rosto falhou", e) }
            override fun onNoFacesDetected(frameData: ICameraSource.FrameData) {
                runOnUiThread { overlayView.setFaces(null, frameData) }
            }
        }
        faceProcessor = FaceDetectionProcessor(callback)
    }

    override fun onDestroy() {
        stop()
        faceProcessor?.close()
        super.onDestroy()
    }

    // --- ICameraSource Implementation ---
    override fun start(frameListener: ICameraSource.FrameListener) {
        this.frameListener = frameListener
        initVideoFeed()
    }

    override fun stop() {
        videoDataListener?.let {
            VideoFeeder.getInstance()?.primaryVideoFeed?.removeVideoDataListener(it)
        }
        codecManager?.cleanSurface()
        codecManager?.destroyCodec()
        codecManager = null
    }

    // --- Lógica de Vídeo do Drone ---
    private fun initVideoFeed() {
        val primaryFeed = VideoFeeder.getInstance()?.primaryVideoFeed
        if (primaryFeed != null) {
            videoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
                codecManager?.sendDataToDecoder(videoBuffer, size)
            }
            videoFeedView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    if (codecManager == null) {
                        // --- CORREÇÃO AQUI ---
                        // 1. Criamos o codec manager com um construtor válido para renderizar o vídeo.
                        codecManager = DJICodecManager(this@VideoFeedActivity, surface, width, height)
                        // 2. DEPOIS de criado, nós registramos nosso callback para interceptar os frames.
                        codecManager?.setYuvDataCallback(onYuvDataReceived)
                        Log.d(TAG, "DJICodecManager inicializado e YUV callback registrado.")
                    }
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    codecManager?.cleanSurface()
                    codecManager = null
                    return false
                }
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
            primaryFeed.addVideoDataListener(videoDataListener!!)
        } else {
            Toast.makeText(this, "Feed de vídeo não disponível", Toast.LENGTH_LONG).show()
        }
    }

    // --- CORREÇÃO FINAL AQUI ---
    // A assinatura do callback agora inclui todos os 5 parâmetros esperados pelo SDK.
    // Usamos '_' para os parâmetros que não vamos utilizar (format e dataSize).
    private val onYuvDataReceived = DJICodecManager.YuvDataCallback { _, yuvFrame, _, width, height ->
        frameListener?.let {
            val frameData = ICameraSource.FrameData(
                data = yuvFrame,
                width = width,
                height = height,
                rotation = 90, // Vídeo do drone geralmente é paisagem, rotação de 90° para ML Kit
                previewWidth = width,
                previewHeight = height,
                displayOrientation = 0, // A tela está travada em paisagem
                cameraSensorOrientation = 0,
                isFrontCamera = false
            )
            // Envia o frame para o processador, exatamente como a PhoneCameraActivity faz
            it.onFrame(frameData)
        }
    }
}