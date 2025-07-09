package com.sloth.registerapp.UI

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.face.Face
import com.sloth.registerapp.R
import com.sloth.registerapp.vision.FaceDetectionProcessor
import com.sloth.registerapp.vision.ICameraSource
import com.sloth.registerapp.vision.OverlayView
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager

// 1. A classe agora implementa a nossa interface ICameraSource
class VideoFeedActivity : AppCompatActivity(), ICameraSource {

    private val TAG = "VideoFeedActivity"

    // --- Variáveis de UI e Detecção ---
    private lateinit var videoFeedView: TextureView
    private lateinit var overlayView: OverlayView

    // --- Variáveis dos Botões ---
    private lateinit var takePhotoButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var toggleOverlayButton: ToggleButton

    private var faceProcessor: FaceDetectionProcessor? = null
    private var frameListener: ICameraSource.FrameListener? = null

    // --- Variáveis do DJI SDK ---
    private var codecManager: DJICodecManager? = null
    private var videoDataListener: VideoFeeder.VideoDataListener? = null

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

        setContentView(R.layout.activity_video_feed) // Certifique-se que este layout tem OverlayView

        videoFeedView = findViewById(R.id.textureView)
        overlayView = findViewById(R.id.overlay_view)
        switchCameraButton = findViewById(R.id.button_switch_camera)
        takePhotoButton = findViewById(R.id.button_take_photo)
        toggleOverlayButton = findViewById(R.id.button_toggle_overlay)

        val toggleButton = findViewById<ToggleButton>(R.id.button_toggle_overlay)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Botão ativado (mais visível)
                toggleButton.alpha = 1.0f
            } else {
                // Botão desativado (um pouco mais transparente)
                toggleButton.alpha = 0.7f
            }
        }

        // Define o estado inicial
        toggleButton.alpha = if (toggleButton.isChecked) 1.0f else 0.7f

        setupVisionProcessor()
        setupButtonListeners()

        // Inicia a fonte de câmera (o drone) e passa o processador como listener
        start(faceProcessor!!)
    }

    private fun setupButtonListeners() {
        takePhotoButton.setOnClickListener {
            Log.i(TAG, "Tirando foto...")
        }

        switchCameraButton.setOnClickListener {
            // Ação para trocar para a câmera do celular
            val intent = Intent(this, PhoneCameraActivity::class.java)
            startActivity(intent)
            finish() // Fecha a tela atual para não ficar empilhando
        }

        toggleOverlayButton.setOnCheckedChangeListener { _, isChecked ->
            // Mostra ou esconde o overlay com os retângulos
            overlayView.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE
        }
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