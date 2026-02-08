package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.auth.data.manager.LoginHistoryManager
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import com.sloth.registerapp.features.auth.domain.model.LoginHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentLoginsScreen(
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val manager = remember(context) { LoginHistoryManager.getInstance(context) }
    var logs by remember { mutableStateOf<List<LoginHistory>>(emptyList()) }

    LaunchedEffect(Unit) {
        logs = manager.getRecentLogins(limit = 30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Últimos Logins") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = colorScheme.surface
            )
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum login registrado ainda.",
                    color = colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { item ->
                    LoginHistoryCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun LoginHistoryCard(item: LoginHistory) {
    val colorScheme = MaterialTheme.colorScheme
    val success = item.status == LoginAttemptStatus.SUCCESS
    val statusColor = if (success) colorScheme.secondary else colorScheme.error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (success) "Login realizado" else "Falha de login",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Usuário: ${item.usernameSnapshot}", color = colorScheme.onSurface)
            Text(
                text = "Dispositivo: ${item.deviceLabel ?: "Não informado"}",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = "Data: ${item.createdAtMs.toDateTimeLabel()}",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            if (!item.ipOrNetwork.isNullOrBlank()) {
                Text(
                    text = "Rede: ${item.ipOrNetwork}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun Long.toDateTimeLabel(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(this))
}
