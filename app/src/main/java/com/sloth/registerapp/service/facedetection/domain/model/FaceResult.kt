package com.sloth.deteccaofacial.domain.model

import com.sloth.deteccaofacial.data.local.FaceEntity

/**
 * Resultado da operação de registro de rosto
 * Usar sealed class para type-safe handling
 */
sealed class FaceResult {

    /**
     * Rosto registrado com sucesso
     */
    data class Success(
        val id: Long,           // ID do rosto no banco
        val name: String,       // Nome da pessoa
        val embedding: FloatArray  // Embedding gerado
    ) : FaceResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (id != other.id) return false
            if (name != other.name) return false
            if (!embedding.contentEquals(other.embedding)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    /**
     * Rosto já estava cadastrado (duplicata)
     */
    data class Duplicate(
        val existingFace: FaceEntity  // Face encontrada no banco
    ) : FaceResult()

    /**
     * Erro durante a operação
     */
    data class Error(
        val message: String,           // Mensagem de erro
        val exception: Throwable? = null  // Exceção original (opcional)
    ) : FaceResult()
}

/**
 * Estados durante o processo de captura
 */
sealed class CaptureState {

    /**
     * Estado inicial
     */
    object Idle : CaptureState()

    /**
     * Escaneando rosto em tempo real
     */
    object Scanning : CaptureState()

    /**
     * Processando a captura
     */
    object Processing : CaptureState()

    /**
     * Captura bem-sucedida
     */
    data class Success(
        val bitmap: android.graphics.Bitmap,  // Imagem capturada
        val embedding: FloatArray,             // Embedding gerado
        val isDuplicate: Boolean = false,      // Se é uma duplicata
        val existingFace: FaceEntity? = null   // Face duplicada encontrada
    ) : CaptureState()

    /**
     * Erro durante a captura
     */
    data class Error(val message: String) : CaptureState()
}
