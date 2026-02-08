package com.sloth.registerapp.presentation.video.screens

import android.graphics.SurfaceTexture
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.features.streaming.data.DjiRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.report.data.manager.MissionMediaManager
import com.sloth.registerapp.presentation.video.components.VideoFeedView
import com.sloth.registerapp.presentation.mission.components.MapboxMiniMapView
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.maps.Style
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
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
    val scope = rememberCoroutineScope()

    val colorScheme = MaterialTheme.colorScheme

    // Estados
    var visible by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val product by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val canMove = droneState == DroneState.IN_AIR
    val isFeedAvailable = product != null

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val mediaManager = remember { MissionMediaManager.getInstance(context) }
    val activeMissionId by ActiveMissionSessionManager.activeMissionId.collectAsStateWithLifecycle()
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val rtmpStreamer = remember { DjiRtmpStreamer(rtmpUrl) }
    val streamState by rtmpStreamer.state.collectAsStateWithLifecycle()
    val previewEnabled = streamState !is StreamState.Streaming && streamState !is StreamState.Connecting

    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

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

    LaunchedEffect(Unit) {
        visible = true
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
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

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
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

        // Gradientes de leitura da HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                    )
                )
        )

        // Header fino estilo DJI
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -it }) {
            CompactHeader(
                droneState = droneState,
                batteryLevel = telemetry.batteryLevel,
                onBackClick = onBackClick,
                onToggleControls = { showControls = !showControls }
            )
        }

        // HUD de telemetria linear (sem card)
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            TelemetryHudStrip(telemetry = telemetry)
        }

        // Mini mapa no canto inferior esquerdo + ações de voo sobrepostas
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                AnimatedVisibility(
                    visible = showControls && visible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FlightActionControls(
                        droneState = droneState,
                        onTakeoffClick = { droneController.takeOff() },
                        onLandClick = { droneController.land() }
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(width = 164.dp, height = 118.dp)
                        .border(1.dp, colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (locationUiState.location != null) {
                            MapboxMiniMapView(
                                modifier = Modifier.fillMaxSize(),
                                operatorLocation = locationUiState.location!!,
                                styleUri = Style.STANDARD
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = locationUiState.errorMessage ?: "Aguardando GPS...",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Controles de câmera
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 10.dp)
        ) {
            CameraControls(
                isRecording = isRecording,
                streamState = streamState,
                onCellCameraClick = onCellCameraClick,
                onTakePhotoClick = {
                    droneController.takePhoto { success, message ->
                        val text = if (success) "Foto capturada" else "Erro ao fotografar: ${message ?: "desconhecido"}"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        if (success && !activeMissionId.isNullOrBlank()) {
                            scope.launch {
                                mediaManager.registerPhotoCapture(
                                    missionId = activeMissionId!!,
                                    dronePath = "drone://photo/${System.currentTimeMillis()}.jpg"
                                )
                            }
                        }
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
                            if (success && !activeMissionId.isNullOrBlank()) {
                                scope.launch {
                                    mediaManager.registerVideoCapture(
                                        missionId = activeMissionId!!,
                                        dronePath = "drone://video/${System.currentTimeMillis()}.mp4"
                                    )
                                }
                            }
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
                    if (!isFeedAvailable) {
                        Toast.makeText(context, "Conecte o drone para transmitir", Toast.LENGTH_SHORT).show()
                        return@CameraControls
                    }
                    if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
                        rtmpStreamer.stop()
                    } else {
                        rtmpStreamer.start()
                    }
                }
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
                .padding(end = 14.dp)
        ) {
            EmergencyStopButton(onClick = { droneController.emergencyStop() })
        }

        // Controles verticais (subir/descer)
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
        ) {
            VerticalMovementControls(
                enabled = canMove,
                onMoveUpStart = { droneController.moveUp() },
                onMoveDownStart = { droneController.moveDown() },
                onStop = { droneController.stopMovement() }
            )
        }
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
    val statusText = when (droneState) {
        DroneState.ON_GROUND -> "Pronto"
        DroneState.IN_AIR -> "No ar"
        DroneState.TAKING_OFF -> "Decolando"
        DroneState.LANDING -> "Pousando"
        DroneState.DISCONNECTED -> "Desconectado"
        DroneState.EMERGENCY_STOP -> "Emergência"
        DroneState.GOING_HOME -> "Retornando"
        DroneState.ERROR -> "Erro"
    }
    val colorScheme = MaterialTheme.colorScheme
    val safeBattery = batteryLevel.coerceIn(0, 100)
    val batteryColor = when {
        safeBattery > 50 -> colorScheme.secondary
        safeBattery > 20 -> colorScheme.tertiary
        else -> colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = statusText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${safeBattery}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(Icons.Default.BatteryFull, null, tint = batteryColor, modifier = Modifier.size(18.dp))
        }
        IconButton(
            onClick = onToggleControls,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Controles",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CameraControls(
    isRecording: Boolean,
    streamState: StreamState,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStreamToggle: () -> Unit
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
            icon = Icons.Default.PhoneAndroid,
            onClick = onCellCameraClick,
            enabled = true,
            color = buttonGray
        )
    }
}

@Composable
fun TelemetryHudStrip(telemetry: DroneTelemetry) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudMetric(icon = Icons.Default.Height, value = String.format("%.1fm", telemetry.altitude))
        HudMetric(icon = Icons.Default.Speed, value = String.format("%.1fm/s", telemetry.speed))
        HudMetric(icon = Icons.Default.TripOrigin, value = String.format("%.0fm", telemetry.distanceFromHome))
        HudMetric(icon = Icons.Default.GpsFixed, value = "${telemetry.gpsSatellites}")
    }
}

@Composable
fun HudMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
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
            size = 44.dp
        )
        CompactControlButton(
            icon = Icons.Default.FlightLand,
            onClick = onLandClick,
            enabled = droneState == DroneState.IN_AIR,
            color = colorScheme.tertiary,
            size = 44.dp
        )
    }
}

@Composable
fun VerticalMovementControls(
    enabled: Boolean,
    onMoveUpStart: () -> Unit,
    onMoveDownStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowUp,
            enabled = enabled,
            onPressStart = onMoveUpStart,
            onPressEnd = onStop
        )
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowDown,
            enabled = enabled,
            onPressStart = onMoveDownStart,
            onPressEnd = onStop
        )
    }
}

@Composable
fun HoldToMoveButton(
    icon: ImageVector,
    enabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .size(46.dp)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        coroutineScope {
                            val repeatJob = launch {
                                while (isActive) {
                                    onPressStart()
                                    delay(300)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                                onPressEnd()
                            }
                        }
                    }
                )
            },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
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
