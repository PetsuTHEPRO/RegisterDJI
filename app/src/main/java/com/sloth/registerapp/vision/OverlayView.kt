package com.sloth.registerapp.vision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face


class OverlayView : View {
    private var faces: List<Face>? = ArrayList()
    private var paint: Paint? = null
    private var previewWidth =
        0 // Largura do frame YUV RAW da câmera (ex: 640, sempre a largura real do sensor)
    private var previewHeight =
        0 // Altura do frame YUV RAW da câmera (ex: 480, sempre a altura real do sensor)
    private var displayOrientation = 0 // Rotação do display em graus (0, 90, 180, 270)
    private var cameraSensorOrientation =
        0 // Orientação do sensor da câmera em graus (90 ou 270 tipicamente para retrato)
    private var isFrontCamera = false

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    private fun init() {
        paint = Paint()
        paint!!.color = Color.GREEN
        paint!!.style = Paint.Style.STROKE
        paint!!.strokeWidth = 5f
        paint!!.textSize = 40f
    }

    fun setFaces(
        faces: List<Face?>?, previewWidth: Int, previewHeight: Int,
        displayOrientation: Int, cameraSensorOrientation: Int, isFrontCamera: Boolean
    ) {
        this.faces = faces as List<Face>?
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        this.displayOrientation = displayOrientation
        this.cameraSensorOrientation = cameraSensorOrientation
        this.isFrontCamera = isFrontCamera
        postInvalidate() // Redesenha a view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (faces == null || faces!!.isEmpty() || previewWidth == 0 || previewHeight == 0) {
            return
        }

        // 1. Determine as dimensões da imagem NO CONTEXTO DO DISPOSITIVO.
        // Ou seja, as dimensões que o ML Kit 'viu' após a rotação.
        // Esta é a parte mais crítica.
        var processedImageWidth = previewWidth
        var processedImageHeight = previewHeight

        // Calcula a rotação que o ML Kit realmente usou (baseada no getRotationForMLKit da MainActivity).
        // Se essa rotação for 90 ou 270, o ML Kit processou a imagem como se as dimensões estivessem invertidas.
        // O `rotationForMLKit` já considera a orientação do sensor e do display.
        val rotationForMLKit =
            getRotationForMLKitInternal(cameraSensorOrientation, displayOrientation, isFrontCamera)

        if (rotationForMLKit == 90 || rotationForMLKit == 270) {
            // Se a imagem foi girada 90 ou 270 graus para o ML Kit,
            // as dimensões "percebidas" por ele são invertidas.
            processedImageWidth = previewHeight
            processedImageHeight = previewWidth
        }

        // 2. Dimensões da nossa OverlayView na tela.
        val viewWidth = width
        val viewHeight = height

        // 3. Calcule as escalas mapeando as dimensões processadas do ML Kit para as dimensões da OverlayView.
        val scaleX = viewWidth.toFloat() / processedImageWidth
        val scaleY = viewHeight.toFloat() / processedImageHeight


        // Log para depuração
        // Log.d(TAG, "PW: " + previewWidth + ", PH: " + previewHeight +
        //           " | DisplayRot: " + displayOrientation + ", SensorRot: " + cameraSensorOrientation +
        //           " | MLKitRot: " + rotationForMLKit +
        //           " | ProcIW: " + processedImageWidth + ", ProcIH: " + processedImageHeight +
        //           " | ViewW: " + viewWidth + ", ViewH: " + viewHeight +
        //           " | ScaleX: " + scaleX + ", ScaleY: " + scaleY);
        for (face in faces!!) {
            val originalBoundingBox = face.boundingBox

            // 4. Aplique a escala às coordenadas da bounding box
            var finalLeft = originalBoundingBox.left * scaleX
            val finalTop = originalBoundingBox.top * scaleY
            var finalRight = originalBoundingBox.right * scaleX
            val finalBottom = originalBoundingBox.bottom * scaleY

            // 5. Espelhamento para câmera frontal (se necessário)
            // A SurfaceView da Camera API 1 geralmente não espelha o preview da câmera frontal.
            // O ML Kit já "desespelhou" a imagem. Para alinhar o retângulo com o preview, re-espelhamos.
            if (isFrontCamera) {
                // Espelha horizontalmente em relação ao centro da view
                finalLeft = viewWidth - finalLeft
                finalRight = viewWidth - finalRight

                // Garante que left < right após o espelhamento (apenas para casos onde a inversão ocorre)
                if (finalLeft > finalRight) {
                    val temp = finalLeft
                    finalLeft = finalRight
                    finalRight = temp
                }
            }

            // 6. Desenha o retângulo
            paint!!.color = Color.GREEN
            canvas.drawRect(
                Rect(
                    finalLeft.toInt(),
                    finalTop.toInt(),
                    finalRight.toInt(),
                    finalBottom.toInt()
                ), paint!!
            )
        }
    }

    // Método auxiliar para calcular a rotação que o ML Kit usa, duplicado da MainActivity.
    // É crucial que esta lógica seja EXATAMENTE a mesma da MainActivity.getRotationForMLKit.
    private fun getRotationForMLKitInternal(
        cameraSensorOrientation: Int,
        displayRotation: Int,
        isFrontCamera: Boolean
    ): Int {
        var result: Int
        if (isFrontCamera) {
            result = (cameraSensorOrientation + displayRotation) % 360
            result =
                (360 - result) % 360 // Compensar o espelhamento da câmera frontal para que o ML Kit veja o mundo real
        } else { // Câmera traseira
            result = (cameraSensorOrientation - displayRotation + 360) % 360
        }
        return result
    }

    companion object {
        private const val TAG = "OverlayView"
    }
}