package com.sloth.registerapp.UI.Screen
// No novo arquivo: UI/DashboardScreen.kt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// --- COMPONENTE REUTILIZÁVEL 1: Indicador de Status ---
@Composable
private fun StatusIndicator(status: String, onRetryClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STATUS DA CONEXÃO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
            AnimatedVisibility(visible = !status.startsWith("Conectado a:")) {
                IconButton(onClick = onRetryClick) {
                    Icon(Icons.Default.Refresh, contentDescription = "Tentar novamente", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// --- COMPONENTE REUTILIZÁVEL 2: Botão de Ação ---
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(54.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // O texto do botão já descreve a ação
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

// --- TELA PRINCIPAL QUE MONTA OS COMPONENTES ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    droneStatus: String,
    onTakePhotoClick: () -> Unit,
    onOpenFeedClick: () -> Unit,
    onRetryConnectionClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel de Controle DJI", style = MaterialTheme.typography.titleMedium) }, // Alterado para titleMedium
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seção de Status
            StatusIndicator(
                status = droneStatus,
                onRetryClick = onRetryConnectionClick
            )

            // Seção de Ações
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionButton(
                    text = "Camera de Vídeo",
                    icon = Icons.Default.PhotoCamera,
                    onClick = onTakePhotoClick
                )
                ActionButton(
                    text = "Estatística Drone",
                    icon = Icons.Default.Videocam,
                    onClick = onOpenFeedClick
                )
            }

            // Espaço vazio no final para empurrar os botões para cima
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}