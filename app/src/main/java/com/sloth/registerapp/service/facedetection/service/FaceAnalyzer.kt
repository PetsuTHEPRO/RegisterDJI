package com.sloth.deteccaofacial.service

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Analisador de faces em tempo real usando ML Kit
 */
class FaceAnalyzer(
    private val onFaceDetected: (FaceAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()

        FaceDetection.getClient(options)
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
        private const val CENTER_TOLERANCE = 0.20f // Aumentado de 0.15 para 0.20 (mais tolerante)
        private const val MIN_BRIGHTNESS = 45f

        // Sistema de estabilização
        private const val REQUIRED_STABLE_FRAMES = 8 // Precisa estar OK por 8 frames consecutivos
    }

    // Contadores para estabilização
    private var stableFramesCount = 0
    private var lastWasGood = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val result = analyzeFaces(
                        faces,
                        imageProxy.width,
                        imageProxy.height,
                        imageProxy
                    )
                    onFaceDetected(result)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro na detecção: ${e.message}")
                    resetStability()
                    onFaceDetected(FaceAnalysisResult.Error(e.message ?: "Erro desconhecido"))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Analisa as faces detectadas e retorna o resultado
     */
    private fun analyzeFaces(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        imageProxy: ImageProxy
    ): FaceAnalysisResult {
        return when {
            faces.isEmpty() -> {
                resetStability()
                FaceAnalysisResult.NoFace
            }
            faces.size > 1 -> {
                resetStability()
                FaceAnalysisResult.MultipleFaces
            }
            else -> {
                val face = faces[0]
                val isCentered = isFaceCentered(face.boundingBox, imageWidth, imageHeight)
                val brightness = calculateFaceBrightness(face, imageProxy)
                val isWellLit = brightness >= MIN_BRIGHTNESS

                // Verifica se TUDO está OK
                val isCurrentlyGood = isCentered && isWellLit

                // Sistema de estabilização
                if (isCurrentlyGood) {
                    if (lastWasGood) {
                        stableFramesCount++
                    } else {
                        stableFramesCount = 1
                    }
                } else {
                    resetStability()
                }

                lastWasGood = isCurrentlyGood

                // Só considera "pronto para capturar" após frames estáveis
                val isStable = stableFramesCount >= REQUIRED_STABLE_FRAMES

                Log.d(TAG, "🎯 Centralizado: $isCentered | 💡 Iluminado: $isWellLit | ⏱️ Frames estáveis: $stableFramesCount/$REQUIRED_STABLE_FRAMES")

                FaceAnalysisResult.FaceDetected(
                    face = face,
                    isCentered = isCentered,
                    isWellLit = isWellLit,
                    brightness = brightness,
                    isStable = isStable // Novo campo
                )
            }
        }
    }

    private fun resetStability() {
        stableFramesCount = 0
        lastWasGood = false
    }

    /**
     * Verifica se a face está centralizada (com mais tolerância)
     */
    private fun isFaceCentered(boundingBox: Rect, imageWidth: Int, imageHeight: Int): Boolean {
        val faceCenterX = boundingBox.centerX().toFloat() / imageWidth
        val faceCenterY = boundingBox.centerY().toFloat() / imageHeight

        val xCentered = faceCenterX in (0.5f - CENTER_TOLERANCE)..(0.5f + CENTER_TOLERANCE)
        val yCentered = faceCenterY in (0.5f - CENTER_TOLERANCE)..(0.5f + CENTER_TOLERANCE)

        return xCentered && yCentered
    }

    /**
     * Calcula a luminosidade APENAS da região central do rosto
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun calculateFaceBrightness(face: Face, imageProxy: ImageProxy): Float {
        try {
            val image = imageProxy.image ?: return 50f

            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride

            val faceBounds = face.boundingBox
            val faceWidth = faceBounds.width().toFloat()
            val faceHeight = faceBounds.height().toFloat()

            val centerReduction = 0.3f

            val left = (faceBounds.left + faceWidth * centerReduction).toInt()
                .coerceIn(0, imageProxy.width - 1)
            val top = (faceBounds.top + faceHeight * centerReduction).toInt()
                .coerceIn(0, imageProxy.height - 1)
            val right = (faceBounds.right - faceWidth * centerReduction).toInt()
                .coerceIn(left + 1, imageProxy.width)
            val bottom = (faceBounds.bottom - faceHeight * centerReduction).toInt()
                .coerceIn(top + 1, imageProxy.height)

            var totalLuminance = 0L
            var pixelCount = 0

            for (y in top until bottom step 2) {
                for (x in left until right step 2) {
                    try {
                        val index = y * yRowStride + x * yPixelStride
                        if (index >= 0 && index < yBuffer.capacity()) {
                            val luminance = yBuffer.get(index).toInt() and 0xFF
                            totalLuminance += luminance
                            pixelCount++
                        }
                    } catch (e: Exception) {
                        // Ignora
                    }
                }
            }

            if (pixelCount == 0) return 50f

            return (totalLuminance / pixelCount).toFloat()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao calcular luminosidade: ${e.message}", e)
            return 50f
        }
    }

    fun release() {
        detector.close()
    }
}

/**
 * Resultado da análise facial
 */
sealed class FaceAnalysisResult {
    object NoFace : FaceAnalysisResult()
    object MultipleFaces : FaceAnalysisResult()
    data class FaceDetected(
        val face: Face,
        val isCentered: Boolean,
        val isWellLit: Boolean,
        val brightness: Float,
        val isStable: Boolean = false // Novo campo: indica se está estável
    ) : FaceAnalysisResult()
    data class Error(val message: String) : FaceAnalysisResult()
}
