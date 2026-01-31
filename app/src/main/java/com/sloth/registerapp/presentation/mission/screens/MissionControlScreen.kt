package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mapbox.geojson.Point
import com.sloth.registerapp.presentation.mission.components.MapboxMapView
import com.sloth.registerapp.presentation.mission.model.Waypoint
import androidx.compose.ui.graphics.Color

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
    waypoints: List<Waypoint> = emptyList(),
    altitude: String = "0m",
    speed: String = "0 m/s",
    battery: Int = 100,
    gpsSignal: Int = 12,
    errorMessage: String? = null,
    onBackClick: () -> Unit = {},
    onStartMission: () -> Unit = {},
    onPauseMission: () -> Unit = {},
    onResumeMission: () -> Unit = {},
    onStopMission: () -> Unit = {},
    onErrorDismiss: () -> Unit = {}
) {
    // Ocultar barras do sistema (status bar e navigation bar)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            val insetsController = WindowCompat.getInsetsController(it, view)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        onDispose {
            window?.let {
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme

    var showTelemetry by remember { mutableStateOf(true) }
    var showStopDialog by remember { mutableStateOf(false) }

    val statusColor = when (missionStatus) {
        MissionStatus.IDLE -> colorScheme.onSurface.copy(alpha = 0.6f)
        MissionStatus.LOADING -> colorScheme.tertiary
        MissionStatus.READY -> colorScheme.primary
        MissionStatus.RUNNING -> colorScheme.primary
        MissionStatus.PAUSED -> colorScheme.tertiary
        MissionStatus.STOPPED -> colorScheme.error
        MissionStatus.COMPLETED -> colorScheme.primary
        MissionStatus.ERROR -> colorScheme.error
    }

    // Lógica do FAB dinâmico
    data class FABConfig(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val text: String,
        val action: () -> Unit,
        val containerColor: Color,
        val contentColor: Color
    )
    
    val fabConfig = when (missionStatus) {
        MissionStatus.IDLE, MissionStatus.READY -> 
            FABConfig(
                Icons.Default.PlayArrow,
                "Iniciar",
                onStartMission,
                colorScheme.primary,
                colorScheme.onPrimary
            )
        MissionStatus.LOADING ->
            FABConfig(
                Icons.Default.HourglassEmpty,
                "Carregando",
                {},
                colorScheme.tertiary,
                colorScheme.onTertiary
            )
        MissionStatus.RUNNING -> 
            FABConfig(
                Icons.Default.Pause,
                "Pausar",
                onPauseMission,
                colorScheme.tertiary,
                colorScheme.onTertiary
            )
        MissionStatus.PAUSED -> 
            FABConfig(
                Icons.Default.PlayArrow,
                "Retomar",
                onResumeMission,
                colorScheme.primary,
                colorScheme.onPrimary
            )
        MissionStatus.STOPPED, MissionStatus.ERROR ->
            FABConfig(
                Icons.Default.Refresh,
                "Tentar Novamente",
                onStartMission,
                colorScheme.primary,
                colorScheme.onPrimary
            )
        MissionStatus.COMPLETED ->
            FABConfig(
                Icons.Default.CheckCircle,
                "Concluída",
                {},
                colorScheme.primary,
                colorScheme.onPrimary
            )
    }

    // Calcular configuração da câmera com base nos waypoints
    val cameraOptions = remember(waypoints, currentLocation) {
        if (waypoints.isNotEmpty()) {
            // Calcular bounding box para mostrar todos os waypoints
            val lats = waypoints.map { it.latitude }
            val lngs = waypoints.map { it.longitude }
            
            val minLat = lats.minOrNull() ?: currentLocation.latitude()
            val maxLat = lats.maxOrNull() ?: currentLocation.latitude()
            val minLng = lngs.minOrNull() ?: currentLocation.longitude()
            val maxLng = lngs.maxOrNull() ?: currentLocation.longitude()
            
            // Calcular centro e zoom apropriados
            val centerLat = (minLat + maxLat) / 2
            val centerLng = (minLng + maxLng) / 2
            
            // Calcular zoom baseado na distância entre pontos
            val latDiff = maxLat - minLat
            val lngDiff = maxLng - minLng
            val maxDiff = maxOf(latDiff, lngDiff)
            
            val zoom = when {
                maxDiff > 0.1 -> 10.0  // Pontos muito distantes
                maxDiff > 0.05 -> 12.0 // Pontos distantes
                maxDiff > 0.01 -> 14.0 // Pontos médios
                maxDiff > 0.005 -> 15.0 // Pontos próximos
                else -> 16.0            // Pontos muito próximos
            }
            
            Pair(Point.fromLngLat(centerLng, centerLat), zoom)
        } else {
            // Sem waypoints, focar na localização atual
            Pair(currentLocation, 15.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Mapa em tela cheia
        MapboxMapView(
            modifier = Modifier.fillMaxSize(),
            waypoints = waypoints,
            primaryColor = colorScheme.primary,
            onMapReady = { mapView ->
                val mapboxMap = mapView

                // Centralizar na área dos waypoints ou localização atual
                mapboxMap.setCamera(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(cameraOptions.first)
                        .zoom(cameraOptions.second)
                        .build()
                )
            }
        )

        // 2. Barra superior minimalista
        MinimalMissionHeader(
            missionName = missionName,
            statusColor = statusColor,
            battery = battery,
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // 3. Botão de toggle para telemetria
        TelemetryToggleButton(
            isVisible = showTelemetry,
            onToggle = { showTelemetry = !showTelemetry },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        )

        // 4. Card de telemetria (condicional e transparente)
        AnimatedVisibility(
            visible = showTelemetry,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 8.dp)
        ) {
            TransparentTelemetryCard(
                altitude = altitude,
                speed = speed,
                gpsSignal = gpsSignal
            )
        }

        // 5. FAB dinâmico para controle da missão
        DynamicMissionFAB(
            icon = fabConfig.icon,
            text = fabConfig.text,
            onClick = fabConfig.action,
            containerColor = fabConfig.containerColor,
            contentColor = fabConfig.contentColor,
            enabled = missionStatus != MissionStatus.LOADING && missionStatus != MissionStatus.COMPLETED,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // 6. FAB para parar missão (apenas quando em execução/pausada)
        if (missionStatus == MissionStatus.RUNNING || missionStatus == MissionStatus.PAUSED) {
            StopMissionFAB(
                onClick = { showStopDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 16.dp)
            )
        }

        // 7. Diálogo de confirmação
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
        
        // 8. Alerta de erro sobreposto
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = colorScheme.onError,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onErrorDismiss) {
                            Text("OK", color = colorScheme.onError, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalMissionHeader(
    missionName: String,
    statusColor: Color,
    battery: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .padding(8.dp)
            .background(colorScheme.scrim.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botão de voltar
        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = colorScheme.onSurface)
        }
        
        // Título da missão (truncado)
        Text(
            text = missionName,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        
        // Status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, CircleShape)
        )
        
        // Ícone de bateria
        Icon(
            imageVector = Icons.Default.Battery6Bar,
            contentDescription = null,
            tint = if (battery > 20) colorScheme.primary else colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        
        // Porcentagem de bateria
        Text(
            text = "$battery%",
            color = colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TelemetryToggleButton(
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier.size(48.dp),
        containerColor = colorScheme.scrim.copy(alpha = 0.7f),
        contentColor = colorScheme.onSurface
    ) {
        Icon(
            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isVisible) "Ocultar telemetria" else "Mostrar telemetria",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TransparentTelemetryCard(
    altitude: String,
    speed: String,
    gpsSignal: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .background(colorScheme.scrim.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TelemetryRow("⬆️", "Alt", altitude)
        TelemetryRow("⚡", "Vel", speed)
        TelemetryRow("🛰️", "GPS", "$gpsSignal")
    }
}

@Composable
private fun DynamicMissionFAB(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text, fontWeight = FontWeight.Bold) },
        expanded = true
    )
}

@Composable
private fun StopMissionFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        containerColor = colorScheme.error,
        contentColor = colorScheme.onError
    ) {
        Icon(Icons.Default.Stop, contentDescription = "Parar missão")
    }
}

@Composable
private fun TelemetryRow(icon: String, label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = "$label:",
            fontSize = 11.sp,
            color = colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
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
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isDanger) colorScheme.error else colorScheme.primary,
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
                    containerColor = if (isDanger) colorScheme.error else colorScheme.primary
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
