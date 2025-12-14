package com.sloth.registerapp.presentation.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

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
