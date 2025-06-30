package com.sloth.registerapp.vision

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.nio.ByteBuffer


class FaceDetectionProcessor(
    private val context: Context,
    private val callback: FaceDetectionCallback?
) {
    private val detector: FaceDetector?

    // Interface de Callback para retornar os resultados
    interface FaceDetectionCallback {
        fun onFaceDetected(
            numberOfFaces: Int,
            faces: List<Face?>?,
            imageWidth: Int,
            imageHeight: Int
        ) // Garanta que 'faces' seja List<Face>

        fun onFaceDetectionFailed(e: java.lang.Exception?)
        fun onNoFacesDetected(imageWidth: Int, imageHeight: Int)
    }

    init {
        val options =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

        detector = FaceDetection.getClient(options)
    }

    // NOVO MÉTODO para processar diretamente os dados do preview
    fun processFrame(
        frameData: ByteArray?,
        previewWidth: Int,
        previewHeight: Int,
        rotationDegrees: Int
    ) {
        if (frameData == null || previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "Dados de frame inválidos.")
            callback?.onFaceDetectionFailed(IllegalArgumentException("Dados de frame ou dimensões inválidas."))
            return
        }

        // Cria um InputImage a partir do buffer YUV
        // O ImageFormat.NV21 é o formato padrão para Câmera API 1
        val image = InputImage.fromByteBuffer(
            ByteBuffer.wrap(frameData),
            previewWidth,
            previewHeight,
            rotationDegrees,  // Rotação da imagem em relação ao dispositivo (ex: 90, 270)
            InputImage.IMAGE_FORMAT_NV21
        )

        // Processa a imagem
        detector!!.process(image)
            .addOnSuccessListener { faces: List<Face?> ->
                if (faces.isEmpty()) {
                    // Log.d(TAG, "Nenhuma face detectada no frame.");
                    callback?.onNoFacesDetected(previewWidth, previewHeight)
                } else {
                    // Log.d(TAG, faces.size() + " face(s) detectada(s) no frame.");
                    callback?.onFaceDetected(faces.size, faces, previewWidth, previewHeight)
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(
                    TAG,
                    "Erro na detecção de faces do frame: " + e.message
                )
                callback?.onFaceDetectionFailed(e)
            }
    }

    // O método processImage(File imageFile) antigo pode ser mantido se ainda quiser processar arquivos salvos,
    // mas não será usado para detecção em tempo real.
    // Ou remova-o se ele não for mais necessário para outra função.
    fun close() {
        detector?.close()
    }

    companion object {
        private const val TAG = "FaceDetectionProcessor"
    }
}