package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.sloth.registerapp.presentation.component.MapboxMapView
import kotlinx.coroutines.delay

// No arquivo de apresentação
enum class MissionStatus {
    IDLE,           // Nada acontecendo
    LOADING,        // Preparando, baixando, enviando - qualquer processo em andamento
    READY,          // Pronto para começar a execução (DOWNLOAD_FINISHED, READY_TO_EXECUTE)
    RUNNING,        // Missão em execução
    PAUSED,         // Missão pausada
    STOPPED,        // Missão interrompida (EXECUTION_STOPPED)
    COMPLETED,      // Missão finalizada com sucesso (FINISHED)
    ERROR           // Erro
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionControlScreen(
    missionName: String = "Missão Alpha",
    missionStatus: MissionStatus = MissionStatus.READY,
    totalWaypoints: Int = 5,
    currentWaypoint: Int = 0,
    progress: Float = 0f,
    altitude: String = "0m",
    speed: String = "0 m/s",
    distance: String = "0m",
    battery: Int = 100,
    gpsSignal: Int = 12,
    onBackClick: () -> Unit = {},
    onStartMission: () -> Unit = {},
    onPauseMission: () -> Unit = {},
    onResumeMission: () -> Unit = {},
    onStopMission: () -> Unit = {},
    onEmergencyStop: () -> Unit = {}
) {
    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenOnline = Color(0xFF22C55E)
    val redDanger = Color(0xFFEF4444)
    val yellowWarning = Color(0xFFF59E0B)

    var isMapTouched by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(darkBg, Color(0xFF1A1F3A))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(darkBg.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = textGray)
                    }

                    Spacer(Modifier.width(16.dp))

                    Text("🚁", fontSize = 32.sp)

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            missionName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textWhite
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                statusText,
                                fontSize = 13.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Indicador de bateria
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = darkBg.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Battery6Bar,
                                contentDescription = null,
                                tint = if (battery > 20) greenOnline else redDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "$battery%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState(), enabled = !isMapTouched)
            ) {
                // Progresso da Missão
                AnimatedVisibility(
                    visible = missionStatus == MissionStatus.RUNNING || missionStatus == MissionStatus.PAUSED,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f)),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Progresso da Missão",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite
                                    )
                                    Text(
                                        "Waypoint $currentWaypoint de $totalWaypoints",
                                        fontSize = 13.sp,
                                        color = textGray
                                    )
                                }

                                Text(
                                    "${(progress * 100).toInt()}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue
                                )
                            }

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = primaryBlue,
                                trackColor = darkBg
                            )
                        }
                    }
                }

                // Mapa
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(350.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isMapTouched = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        isMapTouched = false
                                    }
                                }
                            )
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapboxMapView(
                            modifier = Modifier.fillMaxSize(),
                            waypoints = emptyList(),
                            primaryColor = primaryBlue,
                            onMapReady = { map ->
                                map.setCamera(
                                    com.mapbox.maps.CameraOptions.Builder()
                                        .center(Point.fromLngLat(-44.3025, -2.5307))
                                        .zoom(14.0)
                                        .build()
                                )
                            }
                        )

                        // Overlay de informações no mapa
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cardBg.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
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
                                    TelemetryRow("📏", "Dist", distance)
                                    TelemetryRow("📡", "GPS", "$gpsSignal sats")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Waypoints List
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Waypoints",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = primaryBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "$totalWaypoints pontos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        repeat(totalWaypoints) { index ->
                            WaypointItem(
                                index = index + 1,
                                isActive = index + 1 == currentWaypoint,
                                isCompleted = index + 1 < currentWaypoint,
                                primaryBlue = primaryBlue,
                                greenOnline = greenOnline,
                                textWhite = textWhite,
                                textGray = textGray
                            )

                            if (index < totalWaypoints - 1) {
                                Divider(
                                    color = textGray.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }

            // Controles da Missão
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botões principais
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (missionStatus) {
                            MissionStatus.READY, MissionStatus.IDLE -> {
                                Button(
                                    onClick = onStartMission,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(greenOnline, Color(0xFF16A34A))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Text(
                                                "Iniciar Missão",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
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
                                            "Carregando missão...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = yellowWarning
                                        )
                                    }
                                }
                            }

                            MissionStatus.RUNNING -> {
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

                    // Botão de Emergência
                    if (missionStatus == MissionStatus.RUNNING || missionStatus == MissionStatus.PAUSED) {
                        OutlinedButton(
                            onClick = { showEmergencyDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = redDanger
                            ),
                            border = BorderStroke(2.dp, redDanger)
                        ) {
                            Icon(Icons.Default.Warning, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PARADA DE EMERGÊNCIA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Diálogos
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

        if (showEmergencyDialog) {
            ConfirmDialog(
                title = "Parada de Emergência",
                message = "ATENÇÃO: O drone fará um pouso imediato no local atual. Use apenas em caso de emergência!",
                confirmText = "CONFIRMAR",
                onConfirm = {
                    onEmergencyStop()
                    showEmergencyDialog = false
                },
                onDismiss = { showEmergencyDialog = false },
                isDanger = true
            )
        }
    }
}

@Composable
fun TelemetryRow(icon: String, label: String, value: String) {
    val primaryBlue = Color(0xFF3B82F6)
    val textWhite = Color(0xFFE2E8F0)
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
fun WaypointItem(
    index: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    primaryBlue: Color,
    greenOnline: Color,
    textWhite: Color,
    textGray: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isCompleted -> greenOnline.copy(alpha = 0.2f)
                isActive -> primaryBlue.copy(alpha = 0.2f)
                else -> textGray.copy(alpha = 0.1f)
            },
            border = BorderStroke(
                2.dp,
                when {
                    isCompleted -> greenOnline
                    isActive -> primaryBlue
                    else -> textGray.copy(alpha = 0.3f)
                }
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = greenOnline,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "$index",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isActive -> primaryBlue
                            else -> textGray
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Waypoint $index",
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) textWhite else textGray
            )
            if (isActive) {
                Text(
                    "Em execução...",
                    fontSize = 11.sp,
                    color = primaryBlue
                )
            } else if (isCompleted) {
                Text(
                    "Concluído",
                    fontSize = 11.sp,
                    color = greenOnline
                )
            }
        }

        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = primaryBlue,
                strokeWidth = 2.dp
            )
        }
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