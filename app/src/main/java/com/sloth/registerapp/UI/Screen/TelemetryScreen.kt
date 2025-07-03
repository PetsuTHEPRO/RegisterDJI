package com.sloth.registerapp.UI.Screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.DJI.DroneTelemetryData
import dji.common.flightcontroller.FlightWindWarning
import dji.common.flightcontroller.GPSSignalLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(telemetryData: DroneTelemetryData) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telemetria em Tempo Real") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção de Status Principal
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusItem("Motores", if (telemetryData.areMotorsOn) "LIGADOS" else "DESLIGADOS", if (telemetryData.areMotorsOn) Color.Green else Color.Gray, Modifier.weight(1f))
                StatusItem("Em Voo", if (telemetryData.isFlying) "SIM" else "NÃO", if (telemetryData.isFlying) Color.Green else Color.Gray, Modifier.weight(1f))
            }

            // Seção de Baterias
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard("Bateria Drone", "${telemetryData.droneBatteryPercentage}%", Icons.Default.Flight, Modifier.weight(1f))
                InfoCard("Bateria Controle", "${telemetryData.rcBatteryPercentage}%", Icons.Default.SettingsRemote, Modifier.weight(1f))
            }

            // Seção de Voo
            InfoCard("Modo de Voo", telemetryData.flightMode, Icons.Default.Speed)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard("Altitude (Ultra.)", "%.1f m".format(telemetryData.ultrasonicHeightInMeters), Icons.Default.VerticalAlignBottom, Modifier.weight(1f))
            }
            InfoCard("Tempo de Voo", formatFlightTime(telemetryData.flightTimeInSeconds), Icons.Default.Timer)

            // Seção de Atitude (Orientação)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Atitude (Orientação)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pitch (Frente/Trás): %.2f°".format(telemetryData.attitude.pitch))
                    Text("Roll (Lados): %.2f°".format(telemetryData.attitude.roll))
                    Text("Yaw (Direção): %.2f°".format(telemetryData.attitude.yaw))
                }
            }

            // Seção de Sinais
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard("Satélites", "${telemetryData.satelliteCount}", Icons.Default.SatelliteAlt, Modifier.weight(1f))
                InfoCard("Sinal GPS", formatGpsSignal(telemetryData.gpsSignalLevel), Icons.Default.SignalCellularAlt, Modifier.weight(1f))
            }

            // Seção de Segurança
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (telemetryData.isGoingHome || telemetryData.windWarning != FlightWindWarning.LEVEL_0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alertas de Segurança", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Retornando para Casa: ${if (telemetryData.isGoingHome) "SIM" else "NÃO"}")
                    Text("Aviso de Vento: ${formatWindWarning(telemetryData.windWarning)}")
                }
            }
        }
    }
}

// Componente para cartões de informação simples
@Composable
private fun InfoCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium)
                Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Componente para status booleanos com cor
@Composable
private fun StatusItem(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// Funções auxiliares para formatar os dados
private fun formatFlightTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

private fun formatGpsSignal(level: GPSSignalLevel): String {
    return when (level) {
        GPSSignalLevel.LEVEL_0 -> "Nenhum"
        GPSSignalLevel.LEVEL_1 -> "Muito Fraco"
        GPSSignalLevel.LEVEL_2 -> "Fraco"
        GPSSignalLevel.LEVEL_3 -> "Bom"
        GPSSignalLevel.LEVEL_4 -> "Muito Bom"
        GPSSignalLevel.LEVEL_5 -> "Excelente"
        else -> "Desconhecido"
    }
}

private fun formatWindWarning(warning: FlightWindWarning): String {
    return when (warning) {
        FlightWindWarning.LEVEL_0 -> "Sem Aviso"
        FlightWindWarning.LEVEL_1 -> "Nível 1 (Moderado)"
        FlightWindWarning.LEVEL_2 -> "Nível 2 (Forte)"
        else -> "Desconhecido"
    }
}