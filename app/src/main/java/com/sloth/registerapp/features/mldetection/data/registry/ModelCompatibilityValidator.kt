package com.sloth.registerapp.features.mldetection.data.registry

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.channels.FileChannel

/** Tensor layout consumed by YoloInferenceEngine: float32 [1, N, N, 3] -> [1, 4+classes, boxes]. */
internal object ModelCompatibilityValidator {
    fun validate(file: File) {
        require(file.length() > 0L) { "Arquivo vazio" }
        file.inputStream().channel.use { channel ->
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            Interpreter(buffer).use { interpreter ->
                require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) {
                    "O detector atual aceita um tensor de entrada e um de saída"
                }
                val input = interpreter.getInputTensor(0)
                val output = interpreter.getOutputTensor(0)
                val inputShape = input.shape()
                val outputShape = output.shape()
                require(input.dataType() == DataType.FLOAT32 && inputShape.size == 4 &&
                    inputShape[0] == 1 && inputShape[1] > 0 && inputShape[1] == inputShape[2] &&
                    inputShape[3] == 3 && output.dataType() == DataType.FLOAT32 &&
                    outputShape.size == 3 && outputShape[0] == 1 && outputShape[1] >= 5 &&
                    outputShape[2] > 0) {
                    "Tensor incompatível com o detector YOLO atual"
                }
            }
        }
    }
}
