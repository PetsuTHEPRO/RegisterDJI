package com.sloth.registerapp.presentation.app.settings.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private data class PermissionUi(
    val label: String,
    val description: String,
    val permission: String,
    val granted: Boolean
)

@Composable
fun PermissionsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissions = remember { buildPermissions(context) }
    val grantedCount = permissions.count { it.granted }

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
                    text = "Permissões",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = "$grantedCount de ${permissions.size} concedidas",
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }
        }

        HorizontalDivider(color = NeuralColors.DashboardBorder)

        // Summary card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.DashboardCard,
            border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (grantedCount == permissions.size) NeuralColors.Success else NeuralColors.Warning,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (grantedCount == permissions.size)
                        "Todas as permissões estão concedidas"
                    else
                        "$grantedCount/${permissions.size} permissões concedidas — algumas funcionalidades podem estar limitadas",
                    color = if (grantedCount == permissions.size) NeuralColors.Success else NeuralColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(permissions, key = { it.permission }) { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuralColors.DashboardCard,
                    border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = NeuralColors.DashboardInner,
                            border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (item.granted) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (item.granted) NeuralColors.Success else NeuralColors.Danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = NeuralColors.TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = NeuralColors.DashboardTextSecondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (item.granted) NeuralColors.Success.copy(alpha = 0.12f) else NeuralColors.Danger.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (item.granted) NeuralColors.Success.copy(alpha = 0.30f) else NeuralColors.Danger.copy(alpha = 0.30f)
                            )
                        ) {
                            Text(
                                text = if (item.granted) "CONCEDIDA" else "NÃO CONCEDIDA",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (item.granted) NeuralColors.Success else NeuralColors.Danger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.DashboardBlue)
            ) {
                Text(
                    text = "Abrir configurações do sistema",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun buildPermissions(context: Context): List<PermissionUi> {
    val list = listOf(
        Triple("Câmera", "Acesso à câmera do dispositivo para FPV e inspeção", Manifest.permission.CAMERA),
        Triple("Localização fina", "Precisão GPS para telemetria e navegação de missões", Manifest.permission.ACCESS_FINE_LOCATION),
        Triple("Localização aproximada", "Localização de rede para suporte a mapas e clima", Manifest.permission.ACCESS_COARSE_LOCATION),
        Triple("Armazenamento", "Gravação de mídias, logs e relatórios operacionais", Manifest.permission.WRITE_EXTERNAL_STORAGE),
        Triple("Microfone", "Captura de áudio durante gravações e transmissões", Manifest.permission.RECORD_AUDIO),
        Triple("Bluetooth", "Comunicação e pareamento com o rádio controle / drone", Manifest.permission.BLUETOOTH),
        Triple("Bluetooth Admin", "Gerenciamento de conexões sem fio do ecossistema", Manifest.permission.BLUETOOTH_ADMIN)
    )
    return list.map { (label, desc, permission) ->
        PermissionUi(
            label = label,
            description = desc,
            permission = permission,
            granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
}
