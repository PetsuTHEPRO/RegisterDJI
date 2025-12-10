package com.sloth.registerapp.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sloth.deteccaofacial.FaceRegistrationService
import com.sloth.deteccaofacial.domain.model.FaceResult
import com.sloth.registerapp.core.utils.FileManager.saveDebugBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gerenciador de Reconhecimento Facial
 *
 * Integra o FaceRegistrationService para:
 * - Reconhecer pessoas cadastradas
 * - Comparar embeddings
 * - Retornar nome da pessoa
 */
class FaceRecognitionManager(private val context: Context) {

    private val faceService = FaceRegistrationService.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "FaceRecognitionManager"
        private const val SIMILARITY_THRESHOLD = 0.80f
    }

    /**
     * Reconhece uma pessoa a partir de um Bitmap
     *
     * @param faceBitmap Bitmap do rosto
     * @param callback Retorna o nome ou "Desconhecido"
     */
    fun recognizePerson(faceBitmap: Bitmap, callback: (String) -> Unit) {
        scope.launch {
            try {
                Log.d(TAG, "🔍 Iniciando reconhecimento...")

                saveDebugBitmap(context, faceBitmap, "debug_face_after")
                val matrix = android.graphics.Matrix()
                matrix.postRotate(90f)
                val rotateFaceBitmap = Bitmap.createBitmap(faceBitmap, 0, 0, faceBitmap.width, faceBitmap.height, matrix, true)
                saveDebugBitmap(context, rotateFaceBitmap, "debug_face_before")

                val (isDuplicate, existingFace) = faceService.checkDuplicate(rotateFaceBitmap, SIMILARITY_THRESHOLD)

                if (isDuplicate && existingFace != null) {
                    Log.d(TAG, "✅ Reconhecido: ${existingFace.name}")
                    callback(existingFace.name)
                } else {
                    Log.d(TAG, "❓ Desconhecido")
                    callback("Desconhecido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro no reconhecimento: ${e.message}", e)
                callback("Erro")
            }
        }
    }

    /**
     * Retorna lista de todas as pessoas cadastradas
     */
    suspend fun getAllPeople(): List<String> {
        return try {
            faceService.getAllFacesSync().map { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao listar pessoas: ${e.message}")
            emptyList()
        }
    }

    /**
     * Adiciona uma nova pessoa ao banco
     */
    suspend fun addPerson(name: String, faceBitmap: Bitmap): Boolean {
        return try {
            Log.d(TAG, "💾 Adicionando pessoa: $name")

            val result = faceService.registerFace(faceBitmap, name)

            when (result) {
                is FaceResult.Success -> {
                    Log.d(TAG, "✅ Pessoa adicionada com ID: ${result.id}")
                    true
                }
                is FaceResult.Duplicate -> {
                    Log.w(TAG, "⚠️ Pessoa já cadastrada como: ${result.existingFace.name}")
                    false
                }
                is FaceResult.Error -> {
                    Log.e(TAG, "❌ Erro: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exceção ao adicionar pessoa: ${e.message}")
            false
        }
    }

    /**
     * Remove uma pessoa do banco
     */
    suspend fun removePerson(name: String): Boolean {
        return try {
            Log.d(TAG, "🗑️ Removendo pessoa: $name")

            val faces = faceService.getAllFacesSync()
            val face = faces.find { it.name == name }

            if (face != null) {
                faceService.deleteFace(face)
                Log.d(TAG, "✅ Pessoa removida")
                true
            } else {
                Log.w(TAG, "⚠️ Pessoa não encontrada")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao remover: ${e.message}")
            false
        }
    }

}
