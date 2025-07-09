package com.sloth.registerapp.UI

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.face.Face
import com.sloth.registerapp.R
import com.sloth.registerapp.vision.FaceDetectionProcessor
import com.sloth.registerapp.vision.ICameraSource
import com.sloth.registerapp.vision.OverlayView
import java.nio.ByteBuffer
import kotlin.math.abs

// 1. A classe agora implementa as interfaces necessárias
class PhoneCameraActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback,

    ICameraSource {

// --- Variáveis de Câmera e UI ---

    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var overlayView: OverlayView // Adicionada
    private lateinit var statusBatteryTextView: TextView // Declara o TextView como variável de membro

    // --- Variáveis dos Botões ---
    private lateinit var takePhotoButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var toggleOverlayButton: ToggleButton

// --- Variáveis de Detecção e Lógica ---

    private var faceProcessor: FaceDetectionProcessor? = null
    private var frameListener: ICameraSource.FrameListener? = null

// --- Variáveis de Configuração da Câmera ---

    private var previewWidth = 0
    private var previewHeight = 0
    private var cameraSensorOrientation = 0
    private var displayOrientation = 0
    private var isFrontCamera = false

// --- Android Lifecycle ---

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

        setContentView(R.layout.activity_phone_camera)
        surfaceView = findViewById(R.id.phone_camera_preview)
        overlayView = findViewById(R.id.overlay_view) // Inicializa a OverlayView
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        // Inicializa o TextView
        statusBatteryTextView = findViewById(R.id.status_battery_text)
        switchCameraButton = findViewById(R.id.button_switch_camera)
        takePhotoButton = findViewById(R.id.button_take_photo)
        toggleOverlayButton = findViewById(R.id.button_toggle_overlay)

        setupVisionProcessor()
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        takePhotoButton.setOnClickListener {
            Log.i(TAG, "Tirando foto...")
        }

        switchCameraButton.setOnClickListener {
            // Ação para trocar para a câmera do celular
            val intent = Intent(this, VideoFeedActivity::class.java)
            startActivity(intent)
            finish() // Fecha a tela atual para não ficar empilhando
        }

        toggleOverlayButton.setOnCheckedChangeListener { _, isChecked ->
            // Mostra ou esconde o overlay com os retângulos
            overlayView.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE
        }
    }

    private val batteryBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Pegamos o nível e a escala da bateria do Intent recebido
            val level: Int = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level != -1 && scale != -1) {
                val batteryPct = (level / scale.toFloat()) * 100
                // Atualiza o TextView com o novo nível
                statusBatteryTextView.text = "${batteryPct.toInt()}%"
            }
        }
    }

    private fun setupVisionProcessor() {
        val callback = object : FaceDetectionProcessor.FaceDetectionCallback {
            override fun onFaceDetected(numberOfFaces: Int, faces: List<Face>, frameData: ICameraSource.FrameData) {
                runOnUiThread { overlayView.setFaces(faces, frameData) }
            }
            override fun onFaceDetectionFailed(e: Exception) {
                Log.e(TAG, "Detecção de face falhou", e)
            }
            override fun onNoFacesDetected(frameData: ICameraSource.FrameData) {
                runOnUiThread { overlayView.setFaces(null, frameData) }
            }
        }
        faceProcessor = FaceDetectionProcessor(callback)
    }



    override fun onResume() {

        super.onResume()

        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryBroadcastReceiver, iFilter)

        // A lógica de permissão agora inicia o ICameraSource

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            start(faceProcessor!!) // Inicia o ICameraSource com o nosso processador como listener

        } else {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)

        }

    }



    override fun onPause() {

        super.onPause()

        // Cancela o registro do ouvinte quando a activity está pausada
        // Isso é MUITO IMPORTANTE para evitar vazamentos de memória (memory leaks)
        unregisterReceiver(batteryBroadcastReceiver)

        stop() // Para o ICameraSource

    }



    override fun onDestroy() {

        super.onDestroy()

        faceProcessor?.close()

    }



// --- ICameraSource Implementation ---

    override fun start(frameListener: ICameraSource.FrameListener) {

        this.frameListener = frameListener

        startCamera()

    }



    override fun stop() {

        stopCamera()

    }



// --- Gerenciamento da Câmera ---

    private fun startCamera() {

        try {

            val cameraId = findBackFacingCamera()

            if (cameraId == -1) {

                Toast.makeText(this, "Nenhuma câmera traseira encontrada.", Toast.LENGTH_LONG).show()

                return

            }

            camera = Camera.open(cameraId)

            val parameters = camera!!.parameters

            val bestSize = getBestPreviewSize(parameters.supportedPreviewSizes, surfaceView.width, surfaceView.height)

            bestSize?.let {

                previewWidth = it.width

                previewHeight = it.height

                parameters.setPreviewSize(previewWidth, previewHeight)

            }

            parameters.previewFormat = ImageFormat.NV21

            camera?.parameters = parameters

            setCameraDisplayOrientation()



// Configura o callback para receber os frames

            val bufferSize = previewWidth * previewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8

            camera?.addCallbackBuffer(ByteArray(bufferSize))

            camera?.setPreviewCallbackWithBuffer(this)



            camera?.setPreviewDisplay(surfaceHolder)

            camera?.startPreview()

        } catch (e: Exception) {

            Log.e(TAG, "Erro ao iniciar a câmera.", e)

            Toast.makeText(this, "Não foi possível acessar a câmera.", Toast.LENGTH_LONG).show()

        }

    }



    private fun stopCamera() {

        camera?.stopPreview()

        camera?.setPreviewCallbackWithBuffer(null)

        camera?.release()

        camera = null

    }



// --- Camera.PreviewCallback Implementation ---

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

        if (data != null && frameListener != null) {

            val frameData = ICameraSource.FrameData(

                data = ByteBuffer.wrap(data),

                width = previewWidth,

                height = previewHeight,

                rotation = getRotationForMLKit(),

                previewWidth = previewWidth,

                previewHeight = previewHeight,

                displayOrientation = displayOrientation,

                cameraSensorOrientation = cameraSensorOrientation,

                isFrontCamera = isFrontCamera

            )

// Envia o frame para o processador

            frameListener?.onFrame(frameData)

        }

// Devolve o buffer para a câmera para ser reutilizado

        camera?.addCallbackBuffer(data)

    }



// --- SurfaceHolder.Callback ---

    override fun surfaceCreated(holder: SurfaceHolder) {

// A câmera é iniciada no onResume

    }



    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        if (surfaceHolder.surface == null) return

        stop()

        start(faceProcessor!!)

    }



    override fun surfaceDestroyed(holder: SurfaceHolder) {

        stop()

    }



// --- Gerenciamento de Permissões ---

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            start(faceProcessor!!)

        } else {

            Toast.makeText(this, "Permissão de câmera negada.", Toast.LENGTH_LONG).show()

        }

    }



// --- Métodos Auxiliares ---

    private fun findBackFacingCamera(): Int {

        var cameraId = -1

        for (i in 0 until Camera.getNumberOfCameras()) {

            val info = Camera.CameraInfo()

            Camera.getCameraInfo(i, info)

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {

                cameraSensorOrientation = info.orientation

                isFrontCamera = false

                cameraId = i

                break

            }

        }

        return cameraId

    }



    private fun setCameraDisplayOrientation() {

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val display = wm.defaultDisplay

        displayOrientation = when (display.rotation) {

            Surface.ROTATION_0 -> 0

            Surface.ROTATION_90 -> 90

            Surface.ROTATION_180 -> 180

            Surface.ROTATION_270 -> 270

            else -> 0

        }

        val result = (cameraSensorOrientation - displayOrientation + 360) % 360

        camera?.setDisplayOrientation(result)

    }



    private fun getRotationForMLKit(): Int {

        return (cameraSensorOrientation - displayOrientation + 360) % 360

    }



    private fun getBestPreviewSize(sizes: List<Camera.Size>, width: Int, height: Int): Camera.Size? {

        val aspectTolerance = 0.1

        val targetRatio = width.toDouble() / height

        var optimalSize: Camera.Size? = null

        var minDiff = Double.MAX_VALUE



        for (size in sizes) {

            val ratio = size.width.toDouble() / size.height

            if (abs(ratio - targetRatio) > aspectTolerance) continue

            if (abs(size.height - height) < minDiff) {

                optimalSize = size

                minDiff = abs(size.height - height).toDouble()

            }

        }



        if (optimalSize == null) {

            minDiff = Double.MAX_VALUE

            for (size in sizes) {

                if (abs(size.height - height) < minDiff) {

                    optimalSize = size

                    minDiff = abs(size.height - height).toDouble()

                }

            }

        }

        return optimalSize

    }



    companion object {

        private const val TAG = "ApplicationDJI"

        private const val CAMERA_PERMISSION_REQUEST_CODE = 101

    }

}