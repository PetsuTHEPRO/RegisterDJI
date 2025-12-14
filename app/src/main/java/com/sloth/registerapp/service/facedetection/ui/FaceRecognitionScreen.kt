package com.sloth.registerapp.service.facedetection.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.deteccaofacial.FaceRegistrationService
import com.sloth.registerapp.vision.FaceAnalysisResult
import com.sloth.registerapp.vision.FaceAnalyzer
import com.sloth.registerapp.vision.FaceAnalyzerConfig
import com.sloth.registerapp.vision.FaceAnalyzerListener
import com.sloth.registerapp.core.utils.imageProxyToBitmap
import java.util.concurrent.Executors

/**
 * Tela principal de registro facial
 */
@Composable
fun FaceRegistrationScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val faceService = remember {
        FaceRegistrationService.getInstance(context)
    }

    val viewModel: FaceRegistrationViewModel = viewModel(
        factory = FaceRegistrationViewModelFactory(faceService)
    )

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        FaceRegistrationContent(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
    } else {
        PermissionRequestScreen(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * Conteúdo principal da tela com gerenciamento de estados
 */
@Composable
fun FaceRegistrationContent(
    viewModel: FaceRegistrationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        when (val state = uiState) {
            is FaceRegistrationUiState.Scanning -> {
                CameraPreviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onOpenRegistered = {
                        viewModel.faceService.openRegisteredFacesScreen(context)
                    }
                )
            }
            is FaceRegistrationUiState.Processing -> {
                ProcessingScreen(message = "Processando imagem...")
            }
            is FaceRegistrationUiState.Saving -> {
                ProcessingScreen(message = "Salvando cadastro...")
            }
            is FaceRegistrationUiState.Success -> {
                ResultScreen(
                    bitmap = state.bitmap,
                    embedding = state.embedding,
                    isDuplicate = state.isDuplicate,
                    duplicateName = state.existingFace?.name,
                    onSave = { name ->
                        viewModel.saveFace(name, state.embedding)
                    },
                    onReset = { viewModel.resetState() }
                )
            }
            is FaceRegistrationUiState.Saved -> {
                SavedScreen(
                    name = state.name,
                    onContinue = { viewModel.resetState() },
                    onOpenRegistered = {
                        viewModel.faceService.openRegisteredFacesScreen(context)
                    }
                )
            }
            is FaceRegistrationUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
    }
}

/**
 * Tela de preview da câmera com análise em tempo real
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(
    viewModel: FaceRegistrationViewModel,
    onNavigateBack: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisResult by viewModel.analysisResult.collectAsState()

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val canCapture = remember(analysisResult) {
        analysisResult is FaceAnalysisResult.AdvancedResult &&
                (analysisResult as FaceAnalysisResult.AdvancedResult).isStable
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        val config = FaceAnalyzerConfig(advancedAnalysis = true)
                        it.setAnalyzer(
                            cameraExecutor,
                            FaceAnalyzer(
                                listener = object : FaceAnalyzerListener {
                                    override fun onResult(result: FaceAnalysisResult) {
                                        viewModel.updateAnalysisResult(result)
                                    }
                                },
                                config = config
                            )
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("CameraPreviewScreen", "Erro ao iniciar câmera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Scaffold(
        topBar = {
            CameraTopBar(
                onNavigateBack = onNavigateBack,
                onOpenRegistered = onOpenRegistered
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview da câmera
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay escuro nas bordas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Overlay de guia facial
            FaceGuideOverlay(analysisResult = analysisResult)

            // Card de instruções
            InstructionsCard(
                analysisResult = analysisResult,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // Botão de captura
            CaptureButton(
                enabled = canCapture,
                onClick = {
                    capturePhoto(context, imageCapture, viewModel)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * TopBar da tela de câmera com navegação e ações
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTopBar(
    onNavigateBack: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Cadastro Facial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
        },
        actions = {
            // Botão para ver rostos cadastrados
            FilledTonalButton(
                onClick = onOpenRegistered,
                modifier = Modifier.padding(end = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF6366F1).copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ver Cadastros",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    )
}

/**
 * Overlay com guia de posicionamento do rosto
 */
@Composable
fun FaceGuideOverlay(analysisResult: FaceAnalysisResult) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val color = when (analysisResult) {
        is FaceAnalysisResult.AdvancedResult -> {
            when {
                analysisResult.isStable -> Color(0xFF10B981)
                else -> Color(0xFFF59E0B)
            }
        }
        is FaceAnalysisResult.FaceDetected -> Color(0xFFF59E0B)
        else -> Color.White
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scale(if (analysisResult is FaceAnalysisResult.AdvancedResult && analysisResult.isStable) scale else 1f)
    ) {
        val ovalWidth = size.width * 0.65f
        val ovalHeight = size.height * 0.45f
        val left = (size.width - ovalWidth) / 2
        val top = (size.height - ovalHeight) / 2

        // Oval principal
        drawOval(
            color = color,
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 6f)
        )

        // Cantos decorativos
        val cornerLength = 40f
        val cornerOffset = 20f

        // Canto superior esquerdo
        drawLine(
            color = color,
            start = Offset(left - cornerOffset, top),
            end = Offset(left - cornerOffset, top - cornerLength),
            strokeWidth = 6f
        )
        drawLine(
            color = color,
            start = Offset(left, top - cornerOffset),
            end = Offset(left - cornerLength, top - cornerOffset),
            strokeWidth = 6f
        )
    }
}

/**
 * Card com instruções para o usuário
 */
@Composable
fun InstructionsCard(
    analysisResult: FaceAnalysisResult,
    modifier: Modifier = Modifier
) {
    val (message, icon, color) = when (analysisResult) {
        is FaceAnalysisResult.NoFace ->
            Triple("Posicione seu rosto no centro", Icons.Default.FaceRetouchingNatural, Color(0xFFF59E0B))
        is FaceAnalysisResult.MultipleFaces ->
            Triple("Apenas uma pessoa por vez", Icons.Default.Groups, Color(0xFFEF4444))
        is FaceAnalysisResult.AdvancedResult -> {
            when {
                !analysisResult.isCentered ->
                    Triple("Centralize seu rosto", Icons.Default.CenterFocusWeak, Color(0xFFF59E0B))
                !analysisResult.isWellLit ->
                    Triple("Melhore a iluminação", Icons.Default.WbSunny, Color(0xFFF59E0B))
                !analysisResult.isStable ->
                    Triple("Mantenha a posição...", Icons.Default.Timer, Color(0xFF3B82F6))
                else ->
                    Triple("Pronto para capturar!", Icons.Default.CheckCircle, Color(0xFF10B981))
            }
        }
        is FaceAnalysisResult.Error ->
            Triple("Erro na análise", Icons.Default.Error, Color(0xFFEF4444))
        else -> Triple("Aguardando análise", Icons.Default.HourglassEmpty, Color.White)
    }

    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Botão flutuante para capturar foto
 */
@Composable
fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Anel externo pulsante quando pronto
        if (enabled) {
            val infiniteTransition = rememberInfiniteTransition(label = "ring")
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ring"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(ringScale)
                    .background(
                        color = Color(0xFF10B981).copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // Botão principal
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            containerColor = if (enabled) Color(0xFF10B981) else Color(0xFF64748B),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capturar",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * Tela de processamento com loading
 */
@Composable
fun ProcessingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF6366F1),
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Tela de resultado com opção de salvar
 */
@Composable
fun ResultScreen(
    bitmap: Bitmap,
    embedding: FloatArray,
    isDuplicate: Boolean,
    duplicateName: String?,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showEmbedding by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Text(
                text = if (isDuplicate) "Rosto Já Cadastrado" else "Rosto Capturado!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Imagem capturada
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Rosto capturado",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alerta de duplicação
            if (isDuplicate && duplicateName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Rosto já registrado",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Cadastrado como: $duplicateName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Campo de nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da pessoa") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDuplicate,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF475569),
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color(0xFF64748B),
                    disabledBorderColor = Color(0xFF334155),
                    disabledLabelColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDuplicate) Color(0xFF64748B) else Color(0xFF6366F1)
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card de embedding (expansível)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Embedding Gerado",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(onClick = { showEmbedding = !showEmbedding }) {
                            Icon(
                                imageVector = if (showEmbedding) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showEmbedding) "Ocultar" else "Expandir",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Text(
                        text = "Dimensões: ${embedding.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    AnimatedVisibility(visible = showEmbedding) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Primeiros 10 valores:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = embedding.take(10).joinToString(", ") { "%.4f".format(it) },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botões de ação
            if (!isDuplicate) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar Cadastro",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContentColor = Color(0xFF64748B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rosto já cadastrado")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF475569)))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tirar Nova Foto")
            }
        }
    }
}

/**
 * Tela de sucesso ao salvar
 */
@Composable
fun SavedScreen(
    name: String,
    onContinue: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981),
                        Color(0xFF059669)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Ícone animado
            val scale by rememberInfiniteTransition(label = "success").animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cadastro Realizado!",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$name foi cadastrado com sucesso",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botões de ação
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadastrar Novo Rosto",
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenRegistered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color.White))
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ver Todos os Cadastros",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Tela de erro
 */
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7F1D1D),
                        Color(0xFF991B1B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Erro no Processo",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF991B1B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tentar Novamente",
                    color = Color(0xFF991B1B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tela solicitando permissão de câmera
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Botão de voltar
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFF6366F1).copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissão de Câmera",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Para cadastrar rostos, precisamos acessar sua câmera. Garantimos que suas imagens são processadas com segurança.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permitir Acesso à Câmera",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ========== Helpers ==========

/**
 * Captura foto usando CameraX
 */
@androidx.camera.core.ExperimentalGetImage
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: FaceRegistrationViewModel
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()

                if (bitmap != null) {
                    viewModel.processCapture(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CapturePhoto", "Erro ao capturar: ${exception.message}", exception)
            }
        }
    )
}