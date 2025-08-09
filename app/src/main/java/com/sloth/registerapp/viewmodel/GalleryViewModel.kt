package com.sloth.registerapp.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.pdf.LoadParams
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

// Data class para representar cada foto
data class MediaStoreImage(
    val id: Long,
    val uri: android.net.Uri,
    val dateAdded: Long
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    // Novo StateFlow para notificar a UI sobre o resultado do salvamento
    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult = _saveResult.asStateFlow()

    // O Flow que a UI vai observar. Ele é "cachedIn" para sobreviver a rotações de tela.
    val images: Flow<PagingData<MediaStoreImage>> = Pager(
        config = PagingConfig(
            pageSize = 30, // Quantos itens carregar por vez
            enablePlaceholders = false
        ),
        pagingSourceFactory = { MediaStorePagingSource() }
    ).flow.cachedIn(viewModelScope)

    // SUBSTITUA TODA A CLASSE MediaStorePagingSource PELA VERSÃO ABAIXO
    inner class MediaStorePagingSource : PagingSource<Int, MediaStoreImage>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaStoreImage> {
            // A "página" atual. Usamos o número da página como offset (ponto de partida).
            val pageNumber = params.key ?: 0
            val offset = pageNumber * params.loadSize // Calcula o ponto de partida da busca

            try {
                val imageList = mutableListOf<MediaStoreImage>()
                val contentResolver = getApplication<Application>().applicationContext.contentResolver

                // Filtro para a pasta específica
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("DCIM/Drone App/face-search/%")

                // Método moderno (Android 10+) para consulta com paginação
                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // MÉTODO MODERNO (ANDROID 10+) - Este já estava correto
                    val queryArgs = Bundle().apply {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, params.loadSize)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, 1) // 1 = DESCENDING
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                    contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                        queryArgs,
                        null
                    )
                } else {                    // Método legado para versões mais antigas do Android
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT ${params.loadSize} OFFSET $offset"
                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                    selection,
                    selectionArgs,
                    sortOrder // <-- A PAGINAÇÃO FOI REINSERIDA AQUI
                )
            }

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    imageList.add(MediaStoreImage(id, uri, dateAdded))
                }
            }

            val nextKey = if (imageList.size < params.loadSize) {
                null // Chegamos ao fim, não há mais páginas
            } else {
                pageNumber + 1
            }

            return LoadResult.Page(
                data = imageList,
                prevKey = if (pageNumber == 0) null else pageNumber - 1,
                nextKey = nextKey
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

        override fun getRefreshKey(state: PagingState<Int, MediaStoreImage>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
        }
    }

    // NOVO: SharedFlow para disparar o evento de atualização
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    fun saveImageToPublicGallery(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver

            // 1. Definir os metadados da imagem
            val fileName = "face_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                // A mágica acontece aqui: define a subpasta dentro de DCIM
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Drone App/face-search")
                }
            }

            try {
                // 2. Criar uma entrada vazia no MediaStore para obter a URI de destino
                val destinationUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (destinationUri == null) {
                    throw Exception("Falha ao criar arquivo de mídia.")
                }

                // 3. Copiar os dados da imagem original para a nova URI de destino
                contentResolver.openOutputStream(destinationUri).use { outputStream ->
                    if (outputStream == null) {
                        throw Exception("Falha ao abrir o output stream.")
                    }
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                _saveResult.value = "Imagem salva com sucesso!"
                println("Imagem salva em: ${Environment.DIRECTORY_DCIM}/face-search/$fileName")

                // NOVO: Após o sucesso, emitimos o sinal para atualizar a UI
                _refreshTrigger.emit(Unit) // 'Unit' é como um 'void', apenas sinaliza o evento

            } catch (e: Exception) {
                e.printStackTrace()
                _saveResult.value = "Erro ao salvar imagem: ${e.message}"
            }
        }
    }

    // Função para limpar a mensagem de resultado após ser exibida
    fun clearSaveResult() {
        _saveResult.value = null
    }
}