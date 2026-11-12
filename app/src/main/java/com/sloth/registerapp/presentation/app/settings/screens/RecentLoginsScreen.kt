package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.auth.data.manager.LoginHistoryManager
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import com.sloth.registerapp.features.auth.domain.model.LoginHistory
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentLoginsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember(context) { LoginHistoryManager.getInstance(context) }
    var logs by remember { mutableStateOf<List<LoginHistory>>(emptyList()) }

    LaunchedEffect(Unit) {
        logs = manager.getRecentLogins(limit = 30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.DashboardBg)
    ) {
        // Header padrão do sistema
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(NeuralColors.DashboardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                shape = RoundedCornerShape(8.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Últimos Logins",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = "${logs.size} registros",
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }
        }

        HorizontalDivider(color = NeuralColors.DashboardBorder)

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuralColors.DashboardInner,
                        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = NeuralColors.DashboardTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Nenhum login registrado ainda.",
                        color = NeuralColors.DashboardTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { item ->
                    LoginHistoryCard(item = item)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun LoginHistoryCard(item: LoginHistory) {
    val success = item.status == LoginAttemptStatus.SUCCESS
    val statusColor = if (success) NeuralColors.Success else NeuralColors.Danger

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = NeuralColors.DashboardCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = NeuralColors.DashboardInner,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (success) "Login realizado" else "Falha de login",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Usuário: ${item.usernameSnapshot}",
                    color = NeuralColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Dispositivo: ${item.deviceLabel ?: "Não informado"}",
                    color = NeuralColors.DashboardTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Data: ${item.createdAtMs.toDateTimeLabel()}",
                    color = NeuralColors.DashboardTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!item.ipOrNetwork.isNullOrBlank()) {
                    Text(
                        text = "Rede: ${item.ipOrNetwork}",
                        color = NeuralColors.DashboardBlueLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun Long.toDateTimeLabel(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(this))
}
