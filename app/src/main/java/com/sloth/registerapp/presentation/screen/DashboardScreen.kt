package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.R
// Adicione as importações para os novos ícones que usaremos como exemplo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.ui.mission.MissionViewModel

// Cores do Tema IFMA
val IFMAGreen = Color(0xFF006837)
val IFMAYellow = Color(0xFFF9A01B)
val IFMALightGreen = Color(0xFFE8F5E9)

// --- TELA PRINCIPAL QUE MONTA OS COMPONENTES ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    droneStatus: String,
    onRetryConnectionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    // Adicionando callbacks para 4 botões
    onVideoFeedClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onStartMissionClick: () -> Unit,
    onAboutClick: () -> Unit,
    missionViewModel: MissionViewModel = viewModel()
) {
    // Controle de animações sequenciais
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        containerColor = IFMALightGreen,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ifma_logo),
                            contentDescription = "Logo IFMA",
                            modifier = Modifier
                                .size(42.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            "Painel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IFMAGreen,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(IFMALightGreen)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp), // Ajuste no espaçamento geral
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seção de Status (sem alterações)
            StatusIndicator(
                status = droneStatus,
                onRetryClick = onRetryConnectionClick,
                visible = visible
            )

            // Seção de Ações (AGORA EM GRID 2X2)
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Primeira Linha de Botões ---
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardGridButton(
                        text = "Câmera",
                        icon = Icons.Default.PhotoCamera,
                        onClick = onVideoFeedClick,
                        visible = visible,
                        delay = 200
                    )
                    DashboardGridButton(
                        text = "Galeria",
                        icon = Icons.Default.PhotoLibrary, // Ícone de exemplo
                        onClick = onGalleryClick,
                        visible = visible,
                        delay = 300
                    )
                }
                // --- Segunda Linha de Botões ---
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardGridButton(
                        text = "Telemetria",
                        icon = Icons.Default.PlayArrow, // Ícone de exemplo
                        onClick = onStartMissionClick,
                        visible = visible,
                        delay = 400
                    )
                    DashboardGridButton(
                        text = "Sobre",
                        icon = Icons.Default.Info,
                        onClick = { missionViewModel.fetchMissions() },
                        visible = visible,
                        delay = 500
                    )
                }
            }

            // Espaço inferior
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// --- COMPONENTE REUTILIZÁVEL 3: Botão de Ação em Grid ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardGridButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    visible: Boolean,
    delay: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400, delayMillis = delay)) +
                slideInVertically(animationSpec = tween(400, delayMillis = delay), initialOffsetY = { it / 2 })
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .size(width = 150.dp, height = 120.dp), // Tamanho ideal para grid
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IFMAGreen,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 4.dp
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// --- COMPONENTE REUTILIZÁVEL 1: Indicador de Status ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun StatusIndicator(
    status: String,
    onRetryClick: () -> Unit,
    visible: Boolean
) {
    val statusColor = when {
        status.contains("Conectado") -> IFMAGreen
        status.contains("Erro") -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.primary
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 3 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STATUS DA CONEXÃO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                AnimatedVisibility(visible = !status.startsWith("Conectado")) {
                    IconButton(
                        onClick = onRetryClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(IFMAYellow, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Tentar novamente",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}