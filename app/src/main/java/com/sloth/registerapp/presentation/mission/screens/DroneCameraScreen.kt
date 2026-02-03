package com.sloth.registerapp.presentation.mission.screens

import android.graphics.SurfaceTexture
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.pm.ActivityInfo
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.features.streaming.data.DjiRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.presentation.app.components.VideoFeedView
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.delay

@Composable
fun DroneCameraScreen(
    droneController: DroneCommandManager,
    onCellCameraClick: () -> Unit,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureDestroyed: () -> Boolean,
    onBackClick: () -> Unit = {}
) {
    val tag = "DroneCameraScreen"
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme

    // Estados
    var visible by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val product by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val canMove = droneState == DroneState.IN_AIR
    val isFeedAvailable = product != null

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val rtmpStreamer = remember { DjiRtmpStreamer(rtmpUrl) }
    val streamState by rtmpStreamer.state.collectAsStateWithLifecycle()
    val previewEnabled = streamState !is StreamState.Streaming && streamState !is StreamState.Connecting

    val codecManager = remember { mutableStateOf<DJICodecManager?>(null) }
    val surfaceRef = remember { mutableStateOf<SurfaceTexture?>(null) }
    val surfaceSize = remember { mutableStateOf(Pair(0, 0)) }
    val videoDataListener = remember {
        VideoFeeder.VideoDataListener { videoBuffer, size ->
            Log.d(tag, "Video frame received: $size bytes")
            codecManager.value?.sendDataToDecoder(videoBuffer, size)
        }
    }

    fun attachPreview() {
        val surface = surfaceRef.value ?: return
        val (width, height) = surfaceSize.value
        if (width <= 0 || height <= 0) return
        if (codecManager.value != null) return
        codecManager.value = DJICodecManager(context, surface, width, height)
        VideoFeeder.getInstance().primaryVideoFeed?.addVideoDataListener(videoDataListener)
        Log.d(tag, "Preview attached")
    }

    fun detachPreview() {
        VideoFeeder.getInstance().primaryVideoFeed?.removeVideoDataListener(videoDataListener)
        codecManager.value?.cleanSurface()
        codecManager.value = null
        Log.d(tag, "Preview detached")
    }

    // Oculta barras do sistema (status/navigation) durante o video feed
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val view = activity?.window?.decorView
        if (window != null && view != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null && view != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Força orientação horizontal durante o video feed
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(rtmpUrl) {
        rtmpStreamer.updateUrl(rtmpUrl)
        if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
            rtmpStreamer.stop()
            rtmpStreamer.start()
        }
    }

    LaunchedEffect(previewEnabled) {
        if (previewEnabled) {
            attachPreview()
        } else {
            detachPreview()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        VideoFeedView(
            modifier = Modifier.fillMaxSize(),
            onSurfaceTextureAvailable = { surface, width, height ->
                Log.d(tag, "Surface available: ${width}x${height}")
                surfaceRef.value = surface
                surfaceSize.value = Pair(width, height)
                if (previewEnabled) {
                    attachPreview()
                }
                onSurfaceTextureAvailable(surface, width, height)
            },
            onSurfaceTextureDestroyed = {
                Log.d(tag, "Surface destroyed")
                detachPreview()
                surfaceRef.value = null
                surfaceSize.value = Pair(0, 0)
                onSurfaceTextureDestroyed()
            }
        )

        if (!isFeedAvailable) {
            Log.d(tag, "Video feed unavailable (product not connected)")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(
                        "Vídeo Feed Indisponível",
                        color = colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Conecte-se ao drone para visualizar",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Gradientes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colorScheme.background.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colorScheme.background.copy(alpha = 0.85f))
                    )
                )
        )

        // Header
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -it }) {
            CompactHeader(
                droneState = droneState,
                batteryLevel = telemetry.batteryLevel,
                onBackClick = onBackClick,
                onToggleControls = { showControls = !showControls }
            )
        }

        // Telemetria
        AnimatedVisibility(
            visible = showTelemetry && visible,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 60.dp)
        ) {
            TelemetryPanel(telemetry = telemetry)
        }
        
        // Controles de Câmera
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 10.dp)
        ) {
            CameraControls(
                isRecording = isRecording,
                streamState = streamState,
                onCellCameraClick = onCellCameraClick,
                onTakePhotoClick = {
                    droneController.takePhoto { success, message ->
                        val text = if (success) "Foto capturada" else "Erro ao fotografar: ${message ?: "desconhecido"}"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                },
                onRecordClick = {
                    if (isRecording) {
                        droneController.stopRecording { success, message ->
                            if (success) {
                                isRecording = false
                            }
                            val text = if (success) "Gravação parada" else "Erro ao parar gravação: ${message ?: "desconhecido"}"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        droneController.startRecording { success, message ->
                            if (success) {
                                isRecording = true
                            }
                            val text = if (success) "Gravação iniciada" else "Erro ao iniciar gravação: ${message ?: "desconhecido"}"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onStreamToggle = {
                    if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
                        rtmpStreamer.stop()
                    } else {
                        rtmpStreamer.start()
                    }
                },
                onToggleTelemetry = { showTelemetry = !showTelemetry },
                showTelemetry = showTelemetry
            )
        }

        if (!previewEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(12.dp)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Preview desativado durante transmissão",
                    color = colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }

        // Botão de Emergência
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInHorizontally { it },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            EmergencyStopButton(onClick = { droneController.emergencyStop() })
        }

        // Controles de Decolar/Pousar
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 16.dp)
        ) {
            FlightActionControls(
                droneState = droneState,
                onTakeoffClick = { droneController.takeOff() },
                onLandClick = { droneController.land() }
            )
        }

        // Controles de Movimento removidos conforme solicitado
    }

    DisposableEffect(Unit) {
        onDispose {
            rtmpStreamer.release()
            droneController.stop()
        }
    }
}

// ===============================================
//               SUB-COMPONENTES
// ===============================================

@Composable
fun CompactHeader(
    droneState: DroneState,
    batteryLevel: Int,
    onBackClick: () -> Unit,
    onToggleControls: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusInfo = when (droneState) {
        DroneState.ON_GROUND -> Pair("Solo", colorScheme.secondary)
        DroneState.IN_AIR -> Pair("Voo", colorScheme.primary)
        DroneState.TAKING_OFF -> Pair("Decolando", colorScheme.tertiary)
        DroneState.LANDING -> Pair("Pousando", colorScheme.tertiary)
        DroneState.DISCONNECTED -> Pair("Desconect.", colorScheme.onSurfaceVariant)
        DroneState.EMERGENCY_STOP -> Pair("EMERG.", colorScheme.error)
        DroneState.GOING_HOME -> Pair("Retornando", colorScheme.primary)
        DroneState.ERROR -> Pair("Erro", colorScheme.error)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceVariant.copy(alpha = 0.78f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = colorScheme.onSurface
            )
        }
        /*Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(statusInfo.second, CircleShape))
            Text(statusInfo.first, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusInfo.second)
        }*/
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val batteryColor = when {
                batteryLevel > 50 -> colorScheme.secondary
                batteryLevel > 20 -> colorScheme.tertiary
                else -> colorScheme.error
            }
            Text("$batteryLevel%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = batteryColor)
            Icon(Icons.Default.BatteryFull, null, tint = batteryColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.size(36.dp))
        /*IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Settings, "Configurações", tint = textGray, modifier = Modifier.size(22.dp))
        }*/
    }
}

@Composable
fun TelemetryPanel(telemetry: DroneTelemetry) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactTelemetryRow(
                icon = Icons.Default.Height,
                value = String.format("%.1f m", telemetry.altitude),
                iconColor = colorScheme.primary
            )
            CompactTelemetryRow(
                icon = Icons.Default.Speed,
                value = String.format("%.1f m/s", telemetry.speed),
                iconColor = colorScheme.secondary
            )
            CompactTelemetryRow(
                icon = Icons.Default.TripOrigin,
                value = String.format("%.0f m", telemetry.distanceFromHome),
                iconColor = colorScheme.primary
            )
            CompactTelemetryRow(
                icon = Icons.Default.GpsFixed,
                value = "${telemetry.gpsSatellites}",
                iconColor = if (telemetry.gpsSatellites >= 6) colorScheme.secondary else colorScheme.tertiary
            )
        }
    }
}

@Composable
fun CompactTelemetryRow(icon: ImageVector, value: String, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun CameraControls(
    isRecording: Boolean,
    streamState: StreamState,
    showTelemetry: Boolean,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStreamToggle: () -> Unit,
    onToggleTelemetry: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val buttonGray = colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactControlButton(
            icon = Icons.Default.CameraAlt,
            onClick = onTakePhotoClick,
            enabled = true,
            color = buttonGray
        )
        SmallStreamingButton(
            streamState = streamState,
            onToggle = onStreamToggle,
            color = buttonGray
        )
        CompactControlButton(
            icon = Icons.Default.Videocam,
            onClick = onRecordClick,
            enabled = true,
            color = if (isRecording) colorScheme.error else buttonGray
        )
        CompactControlButton(
            icon = Icons.Default.Dashboard,
            onClick = onToggleTelemetry,
            enabled = true,
            color = buttonGray
        )
        CompactControlButton(
            icon = Icons.Default.PhoneAndroid,
            onClick = onCellCameraClick,
            enabled = true,
            color = buttonGray
        )
    }
}

@Composable
fun SmallStreamingButton(
    streamState: StreamState,
    onToggle: () -> Unit,
    color: Color
) {
    val isConnecting = streamState is StreamState.Connecting
    val isStreaming = streamState is StreamState.Streaming
    val icon = when {
        isConnecting -> Icons.Default.HourglassTop
        isStreaming -> Icons.Default.StopCircle
        else -> Icons.Default.WifiTethering
    }
    val tint = when {
        isStreaming -> MaterialTheme.colorScheme.error
        else -> color
    }
    CompactControlButton(
        icon = icon,
        onClick = onToggle,
        enabled = !isConnecting,
        color = tint,
        size = 36.dp
    )
}

@Composable
fun EmergencyStopButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(colorScheme.error, CircleShape)
            .shadow(8.dp, CircleShape)
    ) {
        Icon(Icons.Default.Warning, "Emergência", tint = colorScheme.onError, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun FlightActionControls(
    modifier: Modifier = Modifier,
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactControlButton(
            icon = Icons.Default.FlightTakeoff,
            onClick = onTakeoffClick,
            enabled = droneState == DroneState.ON_GROUND,
            color = colorScheme.secondary,
            size = 50.dp
        )
        CompactControlButton(
            icon = Icons.Default.FlightLand,
            onClick = onLandClick,
            enabled = droneState == DroneState.IN_AIR,
            color = colorScheme.tertiary,
            size = 50.dp
        )
    }
}

@Composable
fun CompactControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    size: Dp = 36.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = { if (enabled) onClick() },
        shape = CircleShape,
        color = if (enabled) color.copy(alpha = 0.2f) else colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.5f) else colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.size(size),
        shadowElevation = if(enabled) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) color else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
