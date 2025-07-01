package com.sloth.registerapp.vision

import java.nio.ByteBuffer

/**
 * Interface que define um contrato para qualquer fonte de câmera.
 * Ela abstrai a origem do vídeo (drone ou celular).
 */
interface ICameraSource {

    // Listener que receberá os frames prontos para processamento.
    interface FrameListener {
        fun onFrame(frameData: FrameData)
    }

    // Um data class para agrupar os dados de cada frame.
    data class FrameData(
        val data: ByteBuffer,
        val width: Int,
        val height: Int,
        val rotation: Int,
        // Informações para o OverlayView
        val previewWidth: Int,
        val previewHeight: Int,
        val displayOrientation: Int,
        val cameraSensorOrientation: Int,
        val isFrontCamera: Boolean
    )

    // Inicia a fonte da câmera e começa a enviar frames para o listener.
    fun start(frameListener: FrameListener)

    // Para a fonte da câmera e libera os recursos.
    fun stop()
}