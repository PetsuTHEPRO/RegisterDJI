package com.sloth.registerapp.features.facedetection.domain.usecase

import android.util.Log
import com.sloth.registerapp.features.facedetection.data.repository.FaceRepository
import com.sloth.registerapp.features.facedetection.domain.model.FaceResult

/**
 * Use Case para salvar um rosto no banco de dados
 *
 * Responsabilidades:
 * - Validar dados de entrada
 * - Verificar duplicatas
 * - Salvar no banco de dados
 * - Retornar resultado operação
 */
class SaveFaceUseCase(
    private val repository: FaceRepository
) {
    companion object {
        private const val TAG = "SaveFaceUseCase"
    }

    /**
     * Executa o use case
     *
     * @param name Nome da pessoa
     * @param embedding Embedding do rosto
     * @return FaceResult com o resultado da operação
     */
    suspend operator fun invoke(name: String, embedding: FloatArray): FaceResult {
        return try {
            // 1. Valida entrada
            if (name.isBlank()) {
                Log.w(TAG, "⚠️ Nome inválido")
                return FaceResult.Error("Nome não pode estar vazio")
            }

            if (embedding.isEmpty()) {
                Log.w(TAG, "⚠️ Embedding vazio")
                return FaceResult.Error("Embedding inválido")
            }

            Log.d(TAG, "💾 Salvando rosto: $name")

            // 2. Salva no banco usando repository
            val result = repository.saveFace(name, embedding)

            // 3. Mapeia resultado para FaceResult
            result.fold(
                onSuccess = { id ->
                    Log.d(TAG, "✅ Rosto salvo com sucesso! ID: $id")
                    FaceResult.Success(
                        id = id,
                        name = name,
                        embedding = embedding
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Erro ao salvar: ${error.message}")
                    FaceResult.Error(
                        message = error.message ?: "Erro desconhecido ao salvar",
                        exception = error
                    )
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exceção: ${e.message}", e)
            FaceResult.Error(
                message = e.message ?: "Erro desconhecido",
                exception = e
            )
        }
    }
}
