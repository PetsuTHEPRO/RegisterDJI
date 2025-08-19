package com.sloth.registerapp.UI.Screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.ui.theme.IFMAProjectTheme
import com.sloth.registerapp.ui.theme.StatusConnected
import com.sloth.registerapp.ui.theme.StatusError
import com.sloth.registerapp.utils.PermissionUtils
import com.sloth.registerapp.viewmodel.PermissionsViewModel

// --- COMPONENTE PARA UM ÚNICO ITEM DA LISTA DE PERMISSÕES ---
@Composable
fun PermissionItem(
    permission: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val permissionInfo = PermissionUtils.getInfo(permission)
    val backgroundColor by animateColorAsState(if (isGranted) StatusConnected.copy(alpha = 0.1f) else StatusError.copy(alpha = 0.1f))
    val icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning
    val iconColor by animateColorAsState(if (isGranted) StatusConnected else StatusError)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = "Status Icon", tint = iconColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = permissionInfo.name, fontWeight = FontWeight.Bold)
                Text(text = permissionInfo.description, style = MaterialTheme.typography.bodySmall)
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onRequestPermission, shape = RoundedCornerShape(8.dp)) {
                    Text("Conceder")
                }
            }
        }
    }
}

// --- TELA PÚBLICA E COM ESTADO (STATEFUL) ---
// Ela gerencia o ViewModel e coleta o estado para passar para a UI.
@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel
) {
    val context = LocalContext.current
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    // Launcher para solicitar múltiplas permissões.
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Quando o usuário responde, atualizamos o status.
        viewModel.updatePermissionStatus(context)
    }

    // Atualiza o status das permissões sempre que a tela é exibida.
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

// --- TELA PRIVADA E SEM ESTADO (STATELESS) ---
// Contém apenas a UI, recebe os dados e os eventos como parâmetros.
// É fácil de visualizar no Preview.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsContent(
    requiredPermissions: List<String>,
    permissionStatus: Map<String, Boolean>,
    onGrantAllPending: () -> Unit,
    onGrantSingle: (String) -> Unit
) {
    val allPermissionsGranted = permissionStatus.isNotEmpty() && permissionStatus.values.all { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciador de Permissões", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            // Card de Status Geral
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (allPermissionsGranted) StatusConnected.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (allPermissionsGranted) "Todas as permissões foram concedidas!" else "Algumas permissões são necessárias",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!allPermissionsGranted) {
                        Button(onClick = onGrantAllPending) {
                            Text("Conceder Todas as Pendentes")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de permissões
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requiredPermissions) { permission ->
                    PermissionItem(
                        permission = permission,
                        isGranted = permissionStatus[permission] ?: false,
                        onRequestPermission = { onGrantSingle(permission) }
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    IFMAProjectTheme {
        // Agora podemos passar dados falsos diretamente para a UI, sem precisar do ViewModel.
        val required = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val status = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.RECORD_AUDIO to false,
            Manifest.permission.ACCESS_FINE_LOCATION to false
        )

        PermissionsContent(
            requiredPermissions = required,
            permissionStatus = status,
            onGrantAllPending = {}, // Ação vazia para o preview
            onGrantSingle = {}      // Ação vazia para o preview
        )
    }
}