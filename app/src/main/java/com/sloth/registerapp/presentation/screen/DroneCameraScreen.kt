package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.TextureView
import androidx.compose.ui.unit.Dp
import com.sloth.registerapp.data.drone.DroneControllerManager
import com.sloth.registerapp.data.drone.DroneState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DroneCameraScreen(
    droneController: DroneControllerManager,
    onCellCameraClick: () -> Unit,
    onTextureViewCreated: (TextureView) -> Unit
) {
    // Estados
    var showTelemetry by remember { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }

    // Observa o estado do drone
    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()

    // Verifica se pode mover
    val canMove = droneState == DroneState.IN_AIR

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Feed (TextureView)
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    onTextureViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gradiente superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Status Bar Superior
        StatusBar(
            droneState = droneState,
            batteryLevel = "${telemetry.batteryLevel}%",
            onSettingsClick = { /* Implementar */ }
        )

        // Painel de Telemetria
        AnimatedVisibility(
            visible = showTelemetry,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 70.dp)
        ) {
            TelemetryPanel(
                altitude = String.format("%.1f m", telemetry.altitude),
                speed = String.format("%.1f m/s", telemetry.speed),
                distance = String.format("%.0f m", telemetry.distanceFromHome),
                gpsSignal = "${telemetry.gpsSatellites} sats"
            )
        }

        // Controles de Câmera (Direita Superior)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CameraControls(
                showOverlay = showOverlay,
                isRecording = isRecording,
                onCellCameraClick = onCellCameraClick,
                onTakePhotoClick = { /* Implementar captura */ },
                onRecordClick = { isRecording = !isRecording },
                onToggleOverlay = { showOverlay = !showOverlay },
                onToggleTelemetry = { showTelemetry = !showTelemetry }
            )

            EmergencyStopButton(
                onClick = { droneController.emergencyStop() }
            )
        }

        // Controles de Voo (Esquerda Inferior)
        FlightControlsLeft(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 16.dp),
            droneState = droneState,
            onTakeoffClick = { droneController.takeOff() },
            onLandClick = { droneController.land() }
        )

        // Controles de Movimento (Direita Inferior)
        MovementControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 15.dp),
            enabled = canMove,
            onUpClick = { droneController.moveUp(1f) },
            onDownClick = { droneController.moveDown(1f) },
            onForwardClick = { droneController.moveForward(2f) },
            onBackwardClick = { droneController.moveBackward(2f) },
            onLeftClick = { droneController.moveLeft(2f) },
            onRightClick = { droneController.moveRight(2f) },
            onRotateLeftClick = { droneController.rotateLeft(30f) },
            onRotateRightClick = { droneController.rotateRight(30f) }
        )
    }
}

@Composable
fun StatusBar(
    droneState: DroneState,
    batteryLevel: String,
    onSettingsClick: () -> Unit
) {
    val statusText = when (droneState) {
        DroneState.ON_GROUND -> "Drone no Solo"
        DroneState.IN_AIR -> "Drone no Ar"
        DroneState.TAKING_OFF -> "Decolando..."
        DroneState.LANDING -> "Pousando..."
        DroneState.DISCONNECTED -> "Desconectado"
        DroneState.EMERGENCY_STOP -> "PARADA DE EMERGÊNCIA"
        DroneState.GOING_HOME -> "Retornando para casa"
        DroneState.ERROR -> "Erro"
    }

    val statusColor = when (droneState) {
        DroneState.ON_GROUND -> Color(0xFF4CAF50)
        DroneState.IN_AIR -> Color(0xFF2196F3)
        DroneState.TAKING_OFF, DroneState.LANDING -> Color(0xFFFFA726)
        DroneState.DISCONNECTED -> Color(0xFF9E9E9E)
        DroneState.EMERGENCY_STOP -> Color(0xFFD32F2F)
        DroneState.GOING_HOME -> Color(0xFF2196F3)
        DroneState.ERROR -> Color(0xFFD32F2F)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status de Conexão
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (droneState != DroneState.DISCONNECTED)
                        Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = "Conexão",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bateria e Configurações
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = batteryLevel,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Bateria",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryPanel(
    altitude: String,
    speed: String,
    distance: String,
    gpsSignal: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "TELEMETRIA",
                color = Color(0xFF1B5E20),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                color = Color.Black.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            TelemetryItem(
                icon = Icons.Default.Height,
                label = "Alt:",
                value = altitude
            )

            TelemetryItem(
                icon = Icons.Default.Speed,
                label = "Vel:",
                value = speed
            )

            TelemetryItem(
                icon = Icons.Default.TripOrigin,
                label = "Dist:",
                value = distance
            )

            TelemetryItem(
                icon = Icons.Default.GpsFixed,
                label = "GPS:",
                value = gpsSignal
            )
        }
    }
}

@Composable
fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color(0xFF424242),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color(0xFF2E7D32),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CameraControls(
    showOverlay: Boolean,
    isRecording: Boolean,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleTelemetry: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = Icons.Default.PhoneAndroid,
            tint = Color(0xFF4CAF50),
            onClick = onCellCameraClick
        )

        ControlButton(
            icon = Icons.Default.CameraAlt,
            tint = Color(0xFF4CAF50),
            onClick = onTakePhotoClick
        )

        ControlButton(
            icon = Icons.Default.Videocam,
            tint = if (isRecording) Color(0xFFD32F2F) else Color(0xFF4CAF50),
            onClick = onRecordClick
        )

        ControlButton(
            icon = if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            tint = Color(0xFF4CAF50),
            onClick = onToggleOverlay
        )

        ControlButton(
            icon = Icons.Default.Dashboard,
            tint = Color(0xFF4CAF50),
            onClick = onToggleTelemetry
        )
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmergencyStopButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F)
        ),
        shape = RoundedCornerShape(30.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergência",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "PARADA",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FlightControlsLeft(
    modifier: Modifier = Modifier,
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit
) {
    val canTakeoff = droneState == DroneState.ON_GROUND
    val canLand = droneState == DroneState.IN_AIR

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularActionButton(
            icon = Icons.Default.Flight,
            backgroundColor = Color(0xFF4CAF50),
            onClick = onTakeoffClick,
            size = 50.dp,
            enabled = canTakeoff
        )

        CircularActionButton(
            icon = Icons.Default.FlightLand,
            backgroundColor = Color(0xFFFF5722),
            onClick = onLandClick,
            size = 50.dp,
            enabled = canLand
        )
    }
}

@Composable
fun CircularActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    size: Dp = 42.dp,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.3f),
        onClick = { if (enabled) onClick() },
        shadowElevation = if (enabled) 8.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(size - 20.dp)
            )
        }
    }
}

@Composable
fun MovementControls(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    onForwardClick: () -> Unit,
    onBackwardClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onRotateLeftClick: () -> Unit,
    onRotateRightClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Controles Verticais (Esquerda)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularActionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    onClick = onUpClick,
                    enabled = enabled
                )
                CircularActionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    onClick = onDownClick,
                    enabled = enabled
                )
            }

            // Controle Direcional Central
            DirectionalControl(
                enabled = enabled,
                onForwardClick = onForwardClick,
                onBackwardClick = onBackwardClick,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick
            )

            // Controles de Rotação (Direita)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularActionButton(
                    icon = Icons.Default.RotateLeft,
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    onClick = onRotateLeftClick,
                    enabled = enabled
                )
                CircularActionButton(
                    icon = Icons.Default.RotateRight,
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    onClick = onRotateRightClick,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
fun DirectionalControl(
    enabled: Boolean,
    onForwardClick: () -> Unit,
    onBackwardClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        color = if (enabled) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.3f),
        shadowElevation = if (enabled) 6.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val iconTint = if (enabled) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.3f)

            // Frente
            IconButton(
                onClick = onForwardClick,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Frente",
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Trás
            IconButton(
                onClick = onBackwardClick,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Trás",
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Esquerda
            IconButton(
                onClick = onLeftClick,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Esquerda",
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Direita
            IconButton(
                onClick = onRightClick,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Direita",
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Logo Central
            Text(
                text = "IFMA",
                color = iconTint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}