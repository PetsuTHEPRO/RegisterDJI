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

    // --- INÍCIO DA MUDANÇA ---
    // 1. Coleta o evento de pedido de nome do ViewModel.
    val nameRequestUri by viewModel.nameRequestEvent.collectAsState()

    // 2. Se a URI não for nula, mostra o diálogo de cadastro.
    if (nameRequestUri != null) {
        // Passamos a URI e as funções de callback para o diálogo.
        RegisterNameDialog(
            onConfirm = { name ->
                // Quando o usuário confirma, chamamos a função de registro.
                viewModel.registerFaceFromGallery(nameRequestUri!!, name)
                viewModel.onNameRequestCompleted() // Limpa o evento
            },
            onDismiss = {
                // Quando o usuário cancela, apenas limpamos o evento.
                viewModel.onNameRequestCompleted()
            }
        )
    }
    // --- FIM DA MUDANÇA ---

    LaunchedEffect(Unit) {
        viewModel.refreshTrigger.collect {
            images.refresh()
        }
    }

    LaunchedEffect(saveResult) {
        saveResult?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearSaveResult()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                // Esta chamada agora apenas INICIA o processo, pedindo um nome.
                viewModel.onSaveImageClicked(it)
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(
                count = images.itemCount,
                key = images.itemKey { it.id }
            ) { index ->
                images[index]?.let { image ->
                    val isSelected = selectedImageIds.contains(image.id)
                    GalleryItem(
                        image = image,
                        isSelected = isSelected,
                        modifier = Modifier
                            .animateItemPlacement()
                            .clickable {
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

// --- NOVO COMPOSABLE PARA O DIÁLOGO ---
@Composable
fun RegisterNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Rosto") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nome da pessoa") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                },
                // O botão só fica ativo se o texto não estiver vazio.
                enabled = text.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}