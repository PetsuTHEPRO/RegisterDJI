package com.sloth.registerapp.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

/**
 * Analisador de faces em tempo real usando ML Kit
 *
 * Responsabilidade ÚNICA:
 * - Detectar rostos na câmera em tempo real
 * - Notificar via callback quando faces são detectadas
 */
class FaceAnalyzer(
    private val callback: FaceDetectionCallback? = null
) : ImageAnalysis.Analyzer {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.08f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image: Image = imageProxy.image ?: return null
        if (image.format != ImageFormat.YUV_420_888) return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val mediaImage = imageProxy.image
        val rotation = imageProxy.imageInfo.rotationDegrees

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, rotation)

            // Converta ImageProxy para Bitmap aqui
            val bitmap = imageProxyToBitmap(imageProxy) // método que converte ImageProxy para Bitmap

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        Log.d(TAG, "✅ ${faces.size} rosto(s) detectado(s)")

                        if (bitmap != null) {
                            callback?.onFaceDetected(faces.size, faces, rotation, bitmap)
                        }
                    } else {
                        callback?.onNoFacesDetected()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Erro na detecção: ${e.message}")
                    callback?.onFaceDetectionFailed(e)
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }

    fun close() {
        detector.close()
        Log.d(TAG, "✅ Detector liberado")
    }

    /**
     * Callback apenas para detecção
     */
    interface FaceDetectionCallback {
        fun onFaceDetected(numberOfFaces: Int, faces: List<Face>, rotation: Int, frameBitmap: Bitmap)
        fun onFaceDetectionFailed(e: Exception)
        fun onNoFacesDetected()
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
    }
}