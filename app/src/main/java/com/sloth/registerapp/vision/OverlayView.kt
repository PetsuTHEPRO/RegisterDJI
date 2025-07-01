package com.sloth.registerapp.vision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import com.sloth.registerapp.vision.ICameraSource

// @JvmOverloads permite que o Android Studio use este construtor a partir do XML
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faces: List<Face> = emptyList()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var displayOrientation: Int = 0
    private var cameraSensorOrientation: Int = 0
    private var isFrontCamera: Boolean = false

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // Este método permanece o mesmo, recebendo os dados da nossa arquitetura ICameraSource
    fun setFaces(faces: List<Face>?, frameData: ICameraSource.FrameData?) {
        this.faces = faces ?: emptyList()
        frameData?.let {
            this.previewWidth = it.previewWidth
            this.previewHeight = it.previewHeight
            this.displayOrientation = it.displayOrientation
            this.cameraSensorOrientation = it.cameraSensorOrientation
            this.isFrontCamera = it.isFrontCamera
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (faces.isEmpty() || previewWidth == 0 || previewHeight == 0) {
            return
        }

        // --- INÍCIO DA LÓGICA DE DESENHO IGUAL À VERSÃO ANTIGA ---

        // 1. Determina as dimensões da imagem processada pelo ML Kit
        val rotationForMLKit = getRotationForMLKitInternal()
        val (processedImageWidth, processedImageHeight) = if (rotationForMLKit == 90 || rotationForMLKit == 270) {
            previewHeight to previewWidth // Inverte as dimensões
        } else {
            previewWidth to previewHeight
        }

        // 2. Calcula scaleX e scaleY independentemente para espelhar a distorção da SurfaceView
        val scaleX = width.toFloat() / processedImageWidth
        val scaleY = height.toFloat() / processedImageHeight

        // 3. Itera e desenha os retângulos
        for (face in faces) {
            val box = face.boundingBox

            // Aplica as escalas independentes
            var finalLeft = box.left * scaleX
            var finalTop = box.top * scaleY
            var finalRight = box.right * scaleX
            var finalBottom = box.bottom * scaleY

            // Lógica de espelhamento para câmera frontal (se aplicável)
            if (isFrontCamera) {
                val tempLeft = finalLeft
                finalLeft = width - finalRight
                finalRight = width - tempLeft
            }

            // Desenha o retângulo no canvas
            canvas.drawRect(finalLeft, finalTop, finalRight, finalBottom, paint)
        }
        // --- FIM DA LÓGICA DE DESENHO ---
    }

    private fun getRotationForMLKitInternal(): Int {
        return if (isFrontCamera) {
            (360 - (cameraSensorOrientation + displayOrientation) % 360) % 360
        } else {
            (cameraSensorOrientation - displayOrientation + 360) % 360
        }
    }
}