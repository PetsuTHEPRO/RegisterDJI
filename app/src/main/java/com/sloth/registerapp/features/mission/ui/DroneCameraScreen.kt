package com.sloth.registerapp.features.mission.ui

import android.graphics.SurfaceTexture
import android.widget.Toast
import androidx.compose.animation.*
import com.sloth.registerapp.features.mission.data.drone.DroneTelemetry
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
import com.sloth.registerapp.features.mission.data.drone.manager.DroneControllerManager
import com.sloth.registerapp.features.mission.data.drone.DroneState
import com.sloth.registerapp.features.mission.ui.component.VideoFeedView
import kotlinx.coroutines.delay

@Composable
fun DroneCameraScreen(
    droneController: DroneControllerManager,
    onCellCameraClick: () -> Unit,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureDestroyed: () -> Boolean,
    isFeedAvailable: Boolean,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val cardBg = Color(0xFF0F1729)
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val greenAccent = Color(0xFF22C55E)
    val redAccent = Color(0xFFEF4444)
    val orangeAccent = Color(0xFFFFA726)

    // Estados
    var visible by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val canMove = droneState == DroneState.IN_AIR

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoFeedView(
            modifier = Modifier.fillMaxSize(),
            onSurfaceTextureAvailable = onSurfaceTextureAvailable,
            onSurfaceTextureDestroyed = onSurfaceTextureDestroyed
        )

        if (!isFeedAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = primaryBlue.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(
                        "Vídeo Feed Indisponível",
                        color = textWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Conecte-se ao drone para visualizar",
                        color = textGray,
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
                    Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                )
        )

        // Header
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -it }) {
            CompactHeader(
                droneState = droneState,
                batteryLevel = telemetry.batteryLevel,
                onBackClick = onBackClick,
                onSettingsClick = { Toast.makeText(context, "Configurações (TODO)", Toast.LENGTH_SHORT).show() },
                onToggleControls = { showControls = !showControls },
                primaryBlue = primaryBlue,
                textWhite = textWhite,
                textGray = textGray,
                greenAccent = greenAccent,
                redAccent = redAccent,
                orangeAccent = orangeAccent
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
            TelemetryPanel(telemetry = telemetry, primaryBlue = primaryBlue, greenAccent = greenAccent, orangeAccent = orangeAccent)
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
                onCellCameraClick = onCellCameraClick,
                onTakePhotoClick = { Toast.makeText(context, "Capturando Foto (TODO)", Toast.LENGTH_SHORT).show() },
                onRecordClick = { isRecording = !isRecording },
                onToggleTelemetry = { showTelemetry = !showTelemetry },
                showTelemetry = showTelemetry
            )
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
                onLandClick = { droneController.land() },
                greenAccent = greenAccent,
                orangeAccent = orangeAccent
            )
        }

        // Controles de Movimento
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 16.dp)
        ) {
            MovementControls(
                canMove = canMove,
                onUpClick = { droneController.moveUp(1f) },
                onDownClick = { droneController.moveDown(1f) },
                onForwardClick = { droneController.moveForward(2f) },
                onBackwardClick = { droneController.moveBackward(2f) },
                onLeftClick = { droneController.moveLeft(2f) },
                onRightClick = { droneController.moveRight(2f) },
                onRotateLeftClick = { droneController.rotateLeft(30f) },
                onRotateRightClick = { droneController.rotateRight(30f) },
                primaryBlue = primaryBlue,
                greenAccent = greenAccent
            )
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
    onSettingsClick: () -> Unit,
    onToggleControls: () -> Unit,
    primaryBlue: Color,
    textWhite: Color,
    textGray: Color,
    greenAccent: Color,
    redAccent: Color,
    orangeAccent: Color
) {
    val statusInfo = when (droneState) {
        DroneState.ON_GROUND -> Pair("Solo", greenAccent)
        DroneState.IN_AIR -> Pair("Voo", primaryBlue)
        DroneState.TAKING_OFF -> Pair("Decolando", orangeAccent)
        DroneState.LANDING -> Pair("Pousando", orangeAccent)
        DroneState.DISCONNECTED -> Pair("Desconect.", textGray)
        DroneState.EMERGENCY_STOP -> Pair("EMERG.", redAccent)
        DroneState.GOING_HOME -> Pair("Retornando", primaryBlue)
        DroneState.ERROR -> Pair("Erro", redAccent)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ArrowBack, "Voltar", tint = textGray, modifier = Modifier.size(22.dp))
        }
        /*Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(statusInfo.second, CircleShape))
            Text(statusInfo.first, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusInfo.second)
        }*/
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val batteryColor = when {
                batteryLevel > 50 -> greenAccent
                batteryLevel > 20 -> orangeAccent
                else -> redAccent
            }
            Text("$batteryLevel%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = batteryColor)
            Icon(Icons.Default.BatteryFull, null, tint = batteryColor, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onToggleControls, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Tune, "Controles", tint = textGray, modifier = Modifier.size(22.dp))
        }
        /*IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Settings, "Configurações", tint = textGray, modifier = Modifier.size(22.dp))
        }*/
    }
}

@Composable
fun TelemetryPanel(telemetry: DroneTelemetry, primaryBlue: Color, greenAccent: Color, orangeAccent: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactTelemetryRow(icon = Icons.Default.Height, value = String.format("%.1f m", telemetry.altitude), iconColor = primaryBlue)
            CompactTelemetryRow(icon = Icons.Default.Speed, value = String.format("%.1f m/s", telemetry.speed), iconColor = greenAccent)
            CompactTelemetryRow(icon = Icons.Default.TripOrigin, value = String.format("%.0f m", telemetry.distanceFromHome), iconColor = primaryBlue)
            CompactTelemetryRow(icon = Icons.Default.GpsFixed, value = "${telemetry.gpsSatellites}", iconColor = if (telemetry.gpsSatellites >= 6) greenAccent else orangeAccent)
        }
    }
}

@Composable
fun CompactTelemetryRow(icon: ImageVector, value: String, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
fun CameraControls(
    isRecording: Boolean,
    showTelemetry: Boolean,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onToggleTelemetry: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactControlButton(icon = Icons.Default.CameraAlt, onClick = onTakePhotoClick, enabled = true, color = Color(0xFF3B82F6))
        CompactControlButton(icon = Icons.Default.Videocam, onClick = onRecordClick, enabled = true, color = if(isRecording) Color.Red else Color(0xFF3B82F6))
        CompactControlButton(icon = Icons.Default.Dashboard, onClick = onToggleTelemetry, enabled = true, color = if(showTelemetry) Color(0xFF3B82F6) else Color.Gray)
        CompactControlButton(icon = Icons.Default.PhoneAndroid, onClick = onCellCameraClick, enabled = true, color = Color(0xFF3B82F6))
    }
}

@Composable
fun EmergencyStopButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xFFEF4444), CircleShape)
            .shadow(8.dp, CircleShape)
    ) {
        Icon(Icons.Default.Warning, "Emergência", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun FlightActionControls(
    modifier: Modifier = Modifier,
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit,
    greenAccent: Color,
    orangeAccent: Color
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactControlButton(
            icon = Icons.Default.FlightTakeoff,
            onClick = onTakeoffClick,
            enabled = droneState == DroneState.ON_GROUND,
            color = greenAccent,
            size = 50.dp
        )
        CompactControlButton(
            icon = Icons.Default.FlightLand,
            onClick = onLandClick,
            enabled = droneState == DroneState.IN_AIR,
            color = orangeAccent,
            size = 50.dp
        )
    }
}

@Composable
fun MovementControls(
    modifier: Modifier = Modifier,
    canMove: Boolean,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    onForwardClick: () -> Unit,
    onBackwardClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onRotateLeftClick: () -> Unit,
    onRotateRightClick: () -> Unit,
    primaryBlue: Color,
    greenAccent: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        CompactControlButton(icon = Icons.Default.RotateLeft, onClick = onRotateLeftClick, enabled = canMove, color = primaryBlue, size = 44.dp)
        
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Color.Black.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.size(120.dp).padding(8.dp)) {
                val iconColor = if (canMove) greenAccent else greenAccent.copy(alpha = 0.4f)
                IconButton(onClick = { if (canMove) onForwardClick() }, modifier = Modifier.align(Alignment.TopCenter).size(40.dp)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = iconColor, modifier = Modifier.size(30.dp)) }
                IconButton(onClick = { if (canMove) onBackwardClick() }, modifier = Modifier.align(Alignment.BottomCenter).size(40.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = iconColor, modifier = Modifier.size(30.dp)) }
                IconButton(onClick = { if (canMove) onLeftClick() }, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = iconColor, modifier = Modifier.size(30.dp)) }
                IconButton(onClick = { if (canMove) onRightClick() }, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)) { Icon(Icons.Default.KeyboardArrowRight, null, tint = iconColor, modifier = Modifier.size(30.dp)) }
            }
        }
        
        CompactControlButton(icon = Icons.Default.RotateRight, onClick = onRotateRightClick, enabled = canMove, color = primaryBlue, size = 44.dp)
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
    Surface(
        onClick = { if (enabled) onClick() },
        shape = CircleShape,
        color = if (enabled) color.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.size(size),
        shadowElevation = if(enabled) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) color else Color.Gray,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}