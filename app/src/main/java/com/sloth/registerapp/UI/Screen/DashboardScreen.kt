package com.sloth.registerapp.UI.Screen

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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.androidgamesdk.gametextinput.Settings
import com.sloth.registerapp.R

// Cores do Tema IFMA
val IFMAGreen = Color(0xFF006837)
val IFMAYellow = Color(0xFFF9A01B)
val IFMALightGreen = Color(0xFFE8F5E9)

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

// --- COMPONENTE REUTILIZÁVEL 2: Botão de Ação ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    visible: Boolean,
    delay: Int = 0
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400, delayMillis = delay)) +
                slideInVertically(animationSpec = tween(400, delayMillis = delay), initialOffsetY = { it / 2 })
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IFMAGreen,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- TELA PRINCIPAL QUE MONTA OS COMPONENTES ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    droneStatus: String,
    onTakePhotoClick: () -> Unit,
    onOpenFeedClick: () -> Unit,
    onRetryConnectionClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                    // 1. Usar um Row para alinhar o logo e o texto
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ifma_logo),
                            contentDescription = "Logo IFMA",
                            modifier = Modifier
                                .size(42.dp) // Ajuste o tamanho conforme necessário
                                .padding(end = 8.dp)
                        )
                        Text(
                            "Painel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                // 2. Adicionar o parâmetro "actions" para o botão no final
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = Color.White // Garante que o ícone seja branco
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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo do IFMA com animação
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(animationSpec = tween(500), initialOffsetY = { it / 3 })
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ifma_logo),
                    contentDescription = "Logo IFMA",
                    modifier = Modifier
                        .fillMaxWidth(0.1f)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Seção de Status
            StatusIndicator(
                status = droneStatus,
                onRetryClick = onRetryConnectionClick,
                visible = visible
            )

            // Seção de Ações
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionButton(
                    text = "Camera de Vídeo",
                    icon = Icons.Default.Videocam,
                    onClick = onTakePhotoClick,
                    visible = visible,
                    delay = 200
                )

                ActionButton(
                    text = "Capturar Foto",
                    icon = Icons.Default.PhotoCamera,
                    onClick = onOpenFeedClick,
                    visible = visible,
                    delay = 400
                )
            }

            // Espaço inferior
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}