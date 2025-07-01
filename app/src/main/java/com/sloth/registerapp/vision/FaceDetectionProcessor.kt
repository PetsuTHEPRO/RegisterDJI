package com.sloth.registerapp.vision

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionProcessor(
    private val callback: FaceDetectionCallback
) : ICameraSource.FrameListener {

    private val detector: FaceDetector

    interface FaceDetectionCallback {
        fun onFaceDetected(numberOfFaces: Int, faces: List<Face>, frameData: ICameraSource.FrameData)
        fun onFaceDetectionFailed(e: Exception)
        fun onNoFacesDetected(frameData: ICameraSource.FrameData)
    }

    init {
        // --- MUDANÇA AQUI: TORNANDO O DETECTOR MAIS PRECISO ---
        // Alteramos o modo de 'FAST' para 'ACCURATE' e adicionamos
        // a detecção de contornos para ajudar o algoritmo.
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        // --- FIM DA MUDANÇA ---

        detector = FaceDetection.getClient(options)
    }

    override fun onFrame(frameData: ICameraSource.FrameData) {
        // --- LOG 1: Confirma que o frame chegou aqui ---
        //Log.d(TAG, "onFrame: Recebido frame para processamento. Rotação: ${frameData.rotation}, Tamanho: ${frameData.width}x${frameData.height}")

        val image = InputImage.fromByteBuffer(
            frameData.data,
            frameData.width,
            frameData.height,
            frameData.rotation,
            InputImage.IMAGE_FORMAT_NV21
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    // --- LOG 2: Sucesso, mas nenhum rosto encontrado ---
                    //Log.d(TAG, "onSuccess: Nenhum rosto detectado.")
                    callback.onNoFacesDetected(frameData)
                } else {
                    // --- LOG 3: Sucesso, rostos encontrados! ---
                    //Log.d(TAG, "onSuccess: ${faces.size} rosto(s) detectado(s)!")
                    callback.onFaceDetected(faces.size, faces, frameData)
                }
            }
            .addOnFailureListener { e ->
                // --- LOG 4: Ocorreu um erro durante o processamento ---
                // Adicionado o 'e' para imprimir o erro completo no log
                Log.e(TAG, "onFailure: Falha na detecção de faces.", e)
                callback.onFaceDetectionFailed(e)
            }
    }

    fun close() {
        detector.close()
    }

    companion object {
        private const val TAG = "ApplicationDJI"
    }
}