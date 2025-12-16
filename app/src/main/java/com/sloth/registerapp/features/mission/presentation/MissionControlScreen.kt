package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.sloth.registerapp.presentation.component.MapboxMapView

enum class MissionStatus {
    IDLE,
    LOADING,
    READY,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionControlScreen(
    missionName: String = "Missão Alpha",
    missionStatus: MissionStatus = MissionStatus.READY,
    currentLocation: Point = Point.fromLngLat(-44.3025, -2.5307),
    droneLocation: Point? = null,
    altitude: String = "0m",
    speed: String = "0 m/s",
    battery: Int = 100,
    gpsSignal: Int = 12,
    onBackClick: () -> Unit = {},
    onStartMission: () -> Unit = {},
    onPauseMission: () -> Unit = {},
    onResumeMission: () -> Unit = {},
    onStopMission: () -> Unit = {}
) {
    // Cores
    val primaryBlue = Color(0xFF3B82F6)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenOnline = Color(0xFF22C55E)
    val redDanger = Color(0xFFEF4444)
    val yellowWarning = Color(0xFFF59E0B)

    var showStopDialog by remember { mutableStateOf(false) }

    val statusColor = when (missionStatus) {
        MissionStatus.IDLE -> textGray
        MissionStatus.LOADING -> yellowWarning
        MissionStatus.READY -> primaryBlue
        MissionStatus.RUNNING -> greenOnline
        MissionStatus.PAUSED -> yellowWarning
        MissionStatus.STOPPED -> redDanger
        MissionStatus.COMPLETED -> greenOnline
        MissionStatus.ERROR -> redDanger
    }

    val statusText = when (missionStatus) {
        MissionStatus.IDLE -> "Aguardando"
        MissionStatus.READY -> "Pronta"
        MissionStatus.LOADING -> "Carregando"
        MissionStatus.RUNNING -> "Em Execução"
        MissionStatus.PAUSED -> "Pausada"
        MissionStatus.STOPPED -> "Interrompida"
        MissionStatus.COMPLETED -> "Concluída"
        MissionStatus.ERROR -> "Erro"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa em tela cheia
        MapboxMapView(
            modifier = Modifier.fillMaxSize(),
            waypoints = emptyList(),
            primaryColor = primaryBlue,
            onMapReady = { mapView ->
                val mapboxMap = mapView

                // Centralizar no local atual
                mapboxMap.setCamera(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(currentLocation)
                        .zoom(15.0)
                        .build()
                )
            }
        )

        // Overlay superior com header compacto
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botão voltar + Nome
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(darkBg.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = textGray)
                        }

                        Text("🚁", fontSize = 24.sp)

                        Column {
                            Text(
                                missionName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                            // Status Label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Text(
                                    statusText,
                                    fontSize = 12.sp,
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Bateria
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = darkBg.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Battery6Bar,
                                contentDescription = null,
                                tint = if (battery > 20) greenOnline else redDanger,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "$battery%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                        }
                    }
                }
            }
        }

        // Card de telemetria (canto superior esquerdo)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 90.dp),
            shape = RoundedCornerShape(16.dp),
            color = cardBg.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f)),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "TELEMETRIA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryBlue,
                    letterSpacing = 1.sp
                )
                TelemetryRow("🎯", "Alt", altitude)
                TelemetryRow("⚡", "Vel", speed)
                TelemetryRow("📡", "GPS", "$gpsSignal sats")
            }
        }

        // Controles na parte inferior
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = cardBg.copy(alpha = 0.95f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botões de controle baseados no status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (missionStatus) {
                        MissionStatus.READY, MissionStatus.IDLE -> {
                            // Botão Play
                            Button(
                                onClick = onStartMission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = greenOnline
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Iniciar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        MissionStatus.LOADING -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = yellowWarning.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, yellowWarning)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = yellowWarning
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Carregando...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = yellowWarning
                                    )
                                }
                            }
                        }

                        MissionStatus.RUNNING -> {
                            // Botão Pause
                            Button(
                                onClick = onPauseMission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = yellowWarning
                                )
                            ) {
                                Icon(Icons.Default.Pause, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pausar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botão Stop
                            Button(
                                onClick = { showStopDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = redDanger
                                )
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Parar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        MissionStatus.PAUSED -> {
                            // Botão Resume (Play)
                            Button(
                                onClick = onResumeMission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = greenOnline
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Retomar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botão Stop
                            Button(
                                onClick = { showStopDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = redDanger
                                )
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Parar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        MissionStatus.STOPPED -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = redDanger.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, redDanger)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.StopCircle,
                                        contentDescription = null,
                                        tint = redDanger
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Missão Interrompida",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = redDanger
                                    )
                                }
                            }
                        }

                        MissionStatus.COMPLETED -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = greenOnline.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, greenOnline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = greenOnline
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Missão Concluída",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = greenOnline
                                    )
                                }
                            }
                        }

                        MissionStatus.ERROR -> {
                            Button(
                                onClick = onStartMission,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryBlue
                                )
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Tentar Novamente",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmação para parar
        if (showStopDialog) {
            ConfirmDialog(
                title = "Parar Missão?",
                message = "Tem certeza que deseja parar a missão? O drone retornará ao ponto de partida.",
                confirmText = "Parar",
                onConfirm = {
                    onStopMission()
                    showStopDialog = false
                },
                onDismiss = { showStopDialog = false },
                isDanger = true
            )
        }
    }
}

@Composable
fun TelemetryRow(icon: String, label: String, value: String) {
    val primaryBlue = Color(0xFF3B82F6)
    val textGray = Color(0xFF94A3B8)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = "$label:",
            fontSize = 11.sp,
            color = textGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false
) {
    val redDanger = Color(0xFFEF4444)
    val primaryBlue = Color(0xFF3B82F6)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isDanger) redDanger else primaryBlue,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(message, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) redDanger else primaryBlue
                )
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}