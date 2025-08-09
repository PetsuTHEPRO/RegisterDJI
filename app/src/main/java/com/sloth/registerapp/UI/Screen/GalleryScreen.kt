package com.sloth.registerapp.UI.Screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.paging.compose.itemKey
import com.sloth.registerapp.viewmodel.GalleryViewModel
import com.sloth.registerapp.viewmodel.MediaStoreImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    // Coleta o fluxo de dados do ViewModel como itens de paginação para a LazyGrid
    val images = viewModel.images.collectAsLazyPagingItems()
    var selectedImageIds by remember { mutableStateOf(setOf<Long>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Coleta o resultado do salvamento do ViewModel
    val saveResult by viewModel.saveResult.collectAsState()

    // Efeito que reage à mudança no resultado do salvamento
    // NOVO: Efeito que ouve o gatilho de atualização do ViewModel
    LaunchedEffect(Unit) {
        viewModel.refreshTrigger.collect {
            // Quando o gatilho é disparado, chamamos o método refresh()
            images.refresh()
        }
    }

    LaunchedEffect(saveResult) {
        saveResult?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
            // Limpa o resultado no ViewModel para não mostrar a mensagem novamente
            viewModel.clearSaveResult()
        }
    }

    // Atualiza o imagePickerLauncher para chamar a nova função
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.saveImageToPublicGallery(it) // <-- CHAMANDO A NOVA FUNÇÃO
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Galeria de Rostos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            // 2. Adicionar o novo botão para "fazer upload"
            FloatingActionButton(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Adicionar Imagem")
            }
        }
    ) { paddingValues ->
        // LazyVerticalGrid é a grade que vai exibir as imagens de forma eficiente
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp), // Cria colunas adaptáveis
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            // "items" é a função que conecta a LazyGrid aos dados da paginação
            items(
                count = images.itemCount,
                key = images.itemKey { it.id } // Chave única para cada item (melhora performance)
            ) { index ->
                images[index]?.let { image ->
                    val isSelected = selectedImageIds.contains(image.id)
                    GalleryItem(
                        image = image,
                        isSelected = isSelected,
                        modifier = Modifier
                            .animateItemPlacement() // Animação padrão para adição/remoção/movimentação
                            .clickable {
                                // Lógica de seleção
                                selectedImageIds = if (isSelected) {
                                    selectedImageIds - image.id
                                } else {
                                    selectedImageIds + image.id
                                }
                            }
                    )
                }
            }
        }
    }
}

// Composable para cada item da galeria
@Composable
fun GalleryItem(image: MediaStoreImage, isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f) // Garante que o item seja um quadrado
    ) {
        // Carrega a imagem usando a biblioteca Coil
        AsyncImage(
            model = image.uri,
            contentDescription = "Foto da galeria",
            contentScale = ContentScale.Crop, // Corta a imagem para preencher o espaço
            modifier = Modifier.fillMaxSize()
        )

        // Anima a aparição do ícone de seleção
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selecionado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}