package com.sloth.registerapp.presentation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

// Data class para representar cada foto
data class MediaStoreImage(
    val id: Long,
    val uri: android.net.Uri,
    val dateAdded: Long
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult = _saveResult.asStateFlow()

    // NOVO: StateFlow para sinalizar à UI que um nome é necessário.
    // Ele armazena a URI da imagem que está aguardando o nome.
    private val _nameRequestEvent = MutableStateFlow<android.net.Uri?>(null)
    val nameRequestEvent = _nameRequestEvent.asStateFlow()


    val images: Flow<PagingData<MediaStoreImage>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { MediaStorePagingSource() }
    ).flow.cachedIn(viewModelScope)

    inner class MediaStorePagingSource : PagingSource<Int, MediaStoreImage>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaStoreImage> {
            val pageNumber = params.key ?: 0
            val offset = pageNumber * params.loadSize

            try {
                val imageList = mutableListOf<MediaStoreImage>()
                val contentResolver = getApplication<Application>().applicationContext.contentResolver
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("DCIM/Drone App/face-search/%")

                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val queryArgs = Bundle().apply {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, params.loadSize)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, 1)
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                    contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                        queryArgs,
                        null
                    )
                } else {
                    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT ${params.loadSize} OFFSET $offset"
                    contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                        selection,
                        selectionArgs,
                        sortOrder
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

                val nextKey = if (imageList.size < params.loadSize) null else pageNumber + 1
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

    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    /**
     * Passo 1: A UI chama esta função para iniciar o processo de salvamento.
     * Isso emite um evento para que a UI mostre o diálogo de nome.
     */
    fun onSaveImageClicked(uri: android.net.Uri) {
        _nameRequestEvent.value = uri
    }

    /**
     * Passo 3: A UI chama esta função para registrar o rosto após obter o nome do usuário.
     */
    /*fun registerFaceFromGallery(uri: android.net.Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver
            val tag = "GalleryViewModel"

            try {
                val bitmapToAnalyze = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                //val detector = FaceDetectorProvider.getFaceDetector()
                val image = InputImage.fromBitmap(bitmapToAnalyze, 0)
                //val faces = Tasks.await(detector.process(image))

                /*if (faces.isEmpty()) {
                    Log.d(tag, "Nenhum rosto detectado na imagem. Cadastro cancelado.")
                    //_saveResult.postValue("Nenhum rosto encontrado na foto.")
                    return@launch
                }

                Log.d(tag, "${faces.size} rosto(s) detectado(s). Registrando o primeiro com o nome: $name")

                // Registra o embedding do primeiro rosto encontrado
                val firstFace = faces[0]
                val box = firstFace.boundingBox
*/
                /* --- INÍCIO DA CORREÇÃO ---
                // Garante que a caixa de recorte (bounding box) não saia dos limites do bitmap original.
                val safeBox = android.graphics.Rect(
                    maxOf(0, box.left),
                    maxOf(0, box.top),
                    minOf(bitmapToAnalyze.width, box.right),
                    minOf(bitmapToAnalyze.height, box.bottom)
                )*/

                // Verifica se a caixa de recorte tem dimensões válidas
                if (safeBox.width() <= 0 || safeBox.height() <= 0) {
                    Log.e(tag, "BoundingBox inválido após o ajuste. Cadastro cancelado.")
                    //_saveResult.postValue("Não foi possível recortar o rosto da imagem.")
                    return@launch
                }

                val faceBitmap = Bitmap.createBitmap(
                    bitmapToAnalyze,
                    safeBox.left,
                    safeBox.top,
                    safeBox.width(),
                    safeBox.height()
                )
                // --- FIM DA CORREÇÃO ---

                // ATENÇÃO: Idealmente, FaceEmbedder e FaceDatabase seriam injetados (Hilt/Koin)
                // para melhor performance e gerenciamento de ciclo de vida.
                //val faceEmbedder = FaceEmbedder(context, "mobile_face_net.tflite")
                //val embedding = faceEmbedder.getEmbedding(faceBitmap)
                //val faceDatabase = FaceDatabase(context)
                //faceDatabase.addEmbedding(name, embedding)
                Log.d(tag, "Embedding para '$name' salvo no banco de dados.")

                // Prossegue para salvar a imagem completa na galeria
                val cleanName = name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val fileName = "${cleanName}_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Drone App/face-search")
                    }
                }

                val destinationUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("Falha ao criar arquivo de mídia.")

                contentResolver.openOutputStream(destinationUri).use { outputStream ->
                    requireNotNull(outputStream) { "Falha ao abrir o output stream." }
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                //_saveResult.postValue("Rosto de '$name' cadastrado com sucesso!")
                println("Imagem salva em: ${Environment.DIRECTORY_DCIM}/face-search/$fileName")
                _refreshTrigger.emit(Unit)

            } catch (e: Exception) {
                e.printStackTrace()
                //_saveResult.postValue("Erro ao cadastrar rosto: ${e.message}")
            }
        }
    }*/

    /**
     * A UI deve chamar esta função depois de lidar com o evento de pedido de nome,
     * para resetar o estado e evitar que o diálogo apareça novamente.
     */
    fun onNameRequestCompleted() {
        _nameRequestEvent.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
