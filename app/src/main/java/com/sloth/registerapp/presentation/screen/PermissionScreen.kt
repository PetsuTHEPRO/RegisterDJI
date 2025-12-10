package com.sloth.registerapp.presentation.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.presentation.theme.IFMAProjectTheme

// Cores do tema IFMA
private val IFMADarkGreen = Color(0xFF004D28)
private val IFMARedError = Color(0xFFD32F2F)

// ===== COMPONENTE: ITEM DE PERMISSÃO =====
@Composable
private fun PermissionItem(
    permission: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isGranted) IFMALightGreen else Color(0xFFFFF3E0),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "background"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isGranted) IFMAGreen else IFMAYellow,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícone de status com fundo circular
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Concedida" else "Pendente",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Informações da permissão
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = PermissionHelper.getName(permission),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IFMADarkGreen
                )
                Text(
                    text = PermissionHelper.getDescription(permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Botão de concessão
            if (!isGranted) {
                FilledTonalButton(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IFMAYellow,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Conceder",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ===== COMPONENTE: CARD DE STATUS GERAL =====
@Composable
private fun StatusCard(
    allPermissionsGranted: Boolean,
    pendingCount: Int,
    totalCount: Int,
    onGrantAllPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (allPermissionsGranted) IFMAGreen else IFMAYellow
    val contentColor = Color.White

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícone principal
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (allPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Título e descrição
            Text(
                text = if (allPermissionsGranted)
                    "Tudo pronto!"
                else
                    "Permissões necessárias",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (allPermissionsGranted)
                    "Todas as $totalCount permissões foram concedidas com sucesso"
                else
                    "$pendingCount de $totalCount permissões aguardando autorização",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            // Botão de ação
            if (!allPermissionsGranted) {
                Button(
                    onClick = onGrantAllPending,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = backgroundColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Conceder Todas ($pendingCount)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ===== TELA PRINCIPAL (STATEFUL) =====
@Composable
fun PermissionsScreen(
    viewModel: com.sloth.registerapp.presentation.viewmodel.PermissionsViewModel
) {
    val context = LocalContext.current
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.updatePermissionStatus(context)
    }

    LaunchedEffect(Unit) {
        viewModel.updatePermissionStatus(context)
    }

    PermissionsContent(
        requiredPermissions = viewModel.requiredPermissions.toList(),
        permissionStatus = permissionStatus,
        onGrantAllPending = {
            val permissionsToRequest = viewModel.requiredPermissions
                .filter { permissionStatus[it] == false }
                .toTypedArray()
            if (permissionsToRequest.isNotEmpty()) {
                permissionsLauncher.launch(permissionsToRequest)
            }
        },
        onGrantSingle = { permission ->
            permissionsLauncher.launch(arrayOf(permission))
        }
    )
}

// ===== CONTEÚDO DA TELA (STATELESS) =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsContent(
    requiredPermissions: List<String>,
    permissionStatus: Map<String, Boolean>,
    onGrantAllPending: () -> Unit,
    onGrantSingle: (String) -> Unit
) {
    val allPermissionsGranted = permissionStatus.isNotEmpty() &&
            permissionStatus.values.all { it }

    val pendingCount = permissionStatus.values.count { !it }
    val totalCount = requiredPermissions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurações",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IFMAGreen,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de status geral
            item {
                StatusCard(
                    allPermissionsGranted = allPermissionsGranted,
                    pendingCount = pendingCount,
                    totalCount = totalCount,
                    onGrantAllPending = onGrantAllPending
                )
            }

            // Seção de permissões
            item {
                Text(
                    text = "Permissões do Aplicativo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IFMADarkGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Lista de permissões
            items(
                items = requiredPermissions,
                key = { it }
            ) { permission ->
                PermissionItem(
                    permission = permission,
                    isGranted = permissionStatus[permission] ?: false,
                    onRequestPermission = { onGrantSingle(permission) }
                )
            }

            // Espaçamento final
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ===== PREVIEW =====
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PermissionsScreenPreview() {
    IFMAProjectTheme {
        val requiredPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionStatus = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.RECORD_AUDIO to false,
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.READ_EXTERNAL_STORAGE to true
        )

        PermissionsContent(
            requiredPermissions = requiredPermissions,
            permissionStatus = permissionStatus,
            onGrantAllPending = {},
            onGrantSingle = {}
        )
    }
}