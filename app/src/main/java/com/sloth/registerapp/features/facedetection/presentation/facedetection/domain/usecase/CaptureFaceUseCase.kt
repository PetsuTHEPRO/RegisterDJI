package com.sloth.deteccaofacial.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.sloth.deteccaofacial.data.repository.FaceRepository
import com.sloth.deteccaofacial.domain.model.CaptureState
import com.sloth.deteccaofacial.service.FaceEmbeddingEngine

/**
 * Use Case para capturar um rosto e gerar seu embedding
 *
 * Responsabilidades:
 * - Gerar embedding do rosto
 * - Verificar se já existe rosto similar
 * - Retornar estado da captura
 */
class CaptureFaceUseCase(
    private val embeddingEngine: FaceEmbeddingEngine,
    private val repository: FaceRepository
) {
    companion object {
        private const val TAG = "CaptureFaceUseCase"
    }

    /**
     * Executa o use case
     *
     * @param bitmap Imagem capturada contendo o rosto
     * @return CaptureState com o resultado
     */
    suspend operator fun invoke(bitmap: Bitmap): CaptureState {
        return try {
            Log.d(TAG, "🔄 Iniciando captura de rosto...")

            // 1. Gera o embedding
            Log.d(TAG, "🧠 Gerando embedding...")
            val embedding = embeddingEngine.generateEmbedding(bitmap)

            if (embedding == null) {
                Log.e(TAG, "❌ Falha ao gerar embedding")
                return CaptureState.Error("Falha ao gerar embedding do rosto")
            }

            Log.d(TAG, "✅ Embedding gerado com sucesso (${embedding.size} dimensões)")

            // 2. Verifica se já existe um rosto similar
            Log.d(TAG, "🔍 Verificando duplicatas...")
            val (isDuplicate, existingFace) = repository.findSimilarFace(
                embedding
            )

            if (isDuplicate && existingFace != null) {
                Log.d(TAG, "⚠️ Rosto duplicado encontrado: ${existingFace.name}")
            } else {
                Log.d(TAG, "✅ Nenhuma duplicata encontrada")
            }

            // 3. Retorna estado de sucesso
            CaptureState.Success(
                bitmap = bitmap,
                embedding = embedding,
                isDuplicate = isDuplicate,
                existingFace = existingFace
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na captura: ${e.message}", e)
            CaptureState.Error(e.message ?: "Erro desconhecido durante a captura")
        }
    }
}
