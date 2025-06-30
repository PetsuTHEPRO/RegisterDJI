package com.sloth.registerapp.UI
// No novo arquivo: UI/DashboardScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
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
fun StatusIndicator(status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "STATUS DO DRONE",
                style = MaterialTheme.typography.bodySmall, // Estilo menor para o título
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Left
            )
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
    onOpenFeedClick: () -> Unit
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
            StatusIndicator(status = droneStatus)

            // Seção de Ações
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionButton(
                    text = "Controlar Drone",
                    icon = Icons.Default.PhotoCamera,
                    onClick = onTakePhotoClick
                )
                ActionButton(
                    text = "Feed de Vídeo",
                    icon = Icons.Default.Videocam,
                    onClick = onOpenFeedClick
                )
            }

            // Espaço vazio no final para empurrar os botões para cima
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}