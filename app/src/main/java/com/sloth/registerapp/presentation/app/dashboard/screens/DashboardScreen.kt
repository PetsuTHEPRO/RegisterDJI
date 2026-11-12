package com.sloth.registerapp.presentation.app.dashboard.screens

import android.app.Application
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.R
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mldetection.presentation.MLDetectionViewModel
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import com.sloth.registerapp.presentation.app.theme.VantlyMotion
import com.sloth.registerapp.presentation.mission.activities.MissionControlActivity
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListUiState
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModelFactory
import com.sloth.registerapp.presentation.shared.components.AviationCard
import com.sloth.registerapp.presentation.shared.components.StatusDot
import com.sloth.registerapp.presentation.shared.components.TelemetryBadge
import com.sloth.registerapp.presentation.shared.components.TelemetryStatusTone
import com.sloth.registerapp.presentation.shared.components.VantlyDivider
import java.util.Locale

/**
 * Dashboard principal do Vantly Neural.
 * Central de operações moderna, informativa e profissional, seguindo o design system estilo DJI.
 *
 * Estrutura vertical estrita:
 * 1. Cabeçalho Vantly Neural (Central de Operações)
 * 2. Card principal da aeronave (destaque operacional com telemetria real)
 * 3. Seção Últimas Missões (até 3 missões recentes cadastradas)
 * 4. Resumo do Sistema de IA (resumo operacional e acesso à Central de IA)
 */
@Composable
fun DashboardScreen(
    droneStatus: String = "Desconectado",
    userName: String = "Usuário",
    isLoggedIn: Boolean = true,
    onMissionsClick: () -> Unit = {},
    onAiCenterClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRefreshStatusClick: () -> Unit = {},
    onShowAllDronesClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onReportsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Controlador do Drone para telemetria em tempo real
    val droneController = remember { DroneCommandManager() }
    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()

    // Produto DJI
    val product by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val isConnected = product?.isConnected == true || (droneState != DroneState.DISCONNECTED && product != null)
    val modelName = if (isConnected) {
        product?.model?.displayName ?: "Aeronave DJI conectada"
    } else {
        "Nenhuma aeronave conectada"
    }

    // Missões Reais
    val missionListViewModel: MissionListViewModel = viewModel(
        factory = MissionListViewModelFactory(context.applicationContext as Application)
    )
    val missionListUiState by missionListViewModel.uiState.collectAsStateWithLifecycle()
    val activeMissionId by ActiveMissionSessionManager.activeMissionId.collectAsStateWithLifecycle()

    // Sistema de IA
    val objectDetectionRepo = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val detectionEnabled by objectDetectionRepo.detectionEnabled.collectAsStateWithLifecycle(initialValue = false)
    val activeModelId by objectDetectionRepo.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )
    val mlViewModel: MLDetectionViewModel = viewModel()
    val availableModels by mlViewModel.availableModels.collectAsStateWithLifecycle()
    val lastInferenceMs by mlViewModel.lastInferenceMs.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        missionListViewModel.fetchMissions()
    }

    DisposableEffect(Unit) {
        onDispose {
            droneController.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B111A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Cabeçalho Vantly Neural
            DashboardHeader(
                onSettingsClick = onSettingsClick
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(VantlyMotion.Slow, easing = VantlyMotion.Emphasized)) +
                    slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(VantlyMotion.Slow, easing = VantlyMotion.Emphasized))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. Card Principal da Aeronave
                    AircraftPrimaryCard(
                        modelName = modelName,
                        isConnected = isConnected,
                        onRefreshStatusClick = onRefreshStatusClick
                    )

                    FlightTelemetryCard(
                        isConnected = isConnected,
                        droneState = droneState,
                        gpsSatellites = telemetry.gpsSatellites,
                        altitude = telemetry.altitude,
                        speed = telemetry.speed
                    )

                    AiSystemSummaryCard(
                        detectionEnabled = detectionEnabled,
                        activeModelName = availableModels.firstOrNull { it.id == activeModelId }?.name ?: "--",
                        modelsCount = availableModels.size,
                        lastInferenceMs = if (detectionEnabled) lastInferenceMs else null,
                        onDetailsClick = onAiCenterClick
                    )

                    RecentMissionsSection(
                        uiState = missionListUiState,
                        activeMissionId = activeMissionId,
                        onViewAllClick = onMissionsClick,
                        onRefreshClick = { missionListViewModel.fetchMissions() },
                        onMissionClick = { mission ->
                            val intent = Intent(context, MissionControlActivity::class.java).apply {
                                putExtra("MISSION_ID", mission.id)
                            }
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 1. Cabeçalho compacto: Vantly Neural + Central de Operações + Status Dot + Ajustes */
@Composable
private fun DashboardHeader(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color(0xFF0B111A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF111827),
            border = BorderStroke(1.dp, Color(0xFF1F2937))) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF93C5FD),
                    modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vantly Neural",
                color = NeuralColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Central de Operações • Drones & IA",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        Surface(onClick = onSettingsClick, shape = CircleShape, color = Color(0xFF111827),
            border = BorderStroke(1.dp, Color(0xFF1F2937))) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PersonOutline, contentDescription = "Perfil e ajustes",
                    tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Card horizontal com altura mínima; texto e ações podem crescer com a fonte do sistema. */
@Composable
private fun AircraftPrimaryCard(
    modelName: String,
    isConnected: Boolean,
    onRefreshStatusClick: () -> Unit
) {
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) { if (refreshing) { kotlinx.coroutines.delay(2500); refreshing = false } }
    val statusColor = if (isConnected) Color(0xFF34D399) else Color(0xFFFBBF24)
    val illustration = when {
        !isConnected -> R.drawable.dashboard_controller_disconnected
        modelName.removePrefix("DJI ").equals("Mavic Pro", ignoreCase = true) -> R.drawable.dashboard_drone_mavic_pro
        else -> null
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 166.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1623))
            .border(BorderStroke(1.dp, Color(0xFF243044)), RoundedCornerShape(12.dp))
    ) {
        val imageWidth = (maxWidth * if (maxWidth < 340.dp) 0.32f else 0.43f)
            .coerceAtMost(180.dp)
        Image(painter = painterResource(R.drawable.dashboard_mountain), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color(0xA6000000)))
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Color(0xFF0B111A), 0.52f to Color(0xE60B111A), 1f to Color(0x44111827)
        )))
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status da Aeronave", color = Color(0xFFF1F5F9), fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(5.dp), color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                        Text(if (isConnected) "CONECTADO" else "DESCONECTADO", color = statusColor,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                    }
                }
                Text(modelName, color = Color(0xFF94A3B8), fontSize = 12.sp)
                Surface(onClick = { refreshing = true; onRefreshStatusClick() },
                    shape = RoundedCornerShape(8.dp), color = NeuralColors.DashboardBlue) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        if (refreshing) androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Default.Refresh, contentDescription = "Atualizar conexão da aeronave",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (illustration != null) {
                Image(painter = painterResource(illustration), contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(imageWidth).aspectRatio(1.8f))
            } else {
                Spacer(Modifier.width(imageWidth))
            }
        }
    }
}

@Composable
private fun AircraftMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val icon = when (label) {
        "Estado de Voo" -> Icons.Default.Flight
        "Satélites GPS" -> Icons.Default.GpsFixed
        "Altitude" -> Icons.Default.Timeline
        else -> Icons.Default.Speed
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF151E2E),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 62.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(30.dp).background(Color(0xFF0B111A), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
                Text(value, color = Color(0xFFF1F5F9), fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FlightTelemetryCard(
    isConnected: Boolean,
    droneState: DroneState,
    gpsSatellites: Int,
    altitude: Float,
    speed: Float
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val flightState = when {
        !isConnected -> "Indisponível"
        droneState == DroneState.ON_GROUND -> "No solo"
        droneState == DroneState.IN_AIR -> "Em voo"
        droneState == DroneState.TAKING_OFF -> "Decolando"
        droneState == DroneState.LANDING -> "Pousando"
        droneState == DroneState.GOING_HOME -> "Retornando"
        droneState == DroneState.EMERGENCY_STOP -> "Parada de emergência"
        droneState == DroneState.ERROR -> "Erro de sistema"
        else -> "Pronto para voo"
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF111827),
        border = BorderStroke(1.dp, Color(0xFF1F2937))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 32.dp).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(9.dp))
                Text("Telemetria de Voo", color = Color(0xFFF1F5F9), fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, contentDescription = if (expanded) "Recolher telemetria" else "Expandir telemetria",
                    tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp).rotate(if (expanded) 0f else -90f))
            }
            if (expanded) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.3f
                    val gps = if (isConnected && gpsSatellites > 0) "$gpsSatellites" else "--"
                    val altitudeText = if (isConnected) String.format(Locale.getDefault(), "%.1f m", altitude) else "--"
                    val speedText = if (isConnected) String.format(Locale.getDefault(), "%.1f m/s", speed) else "--"
                    if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AircraftMetricCell("Estado de Voo", flightState, Modifier.fillMaxWidth())
                            AircraftMetricCell("Satélites GPS", gps, Modifier.fillMaxWidth())
                            AircraftMetricCell("Altitude", altitudeText, Modifier.fillMaxWidth())
                            AircraftMetricCell("Velocidade", speedText, Modifier.fillMaxWidth())
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AircraftMetricCell("Estado de Voo", flightState, Modifier.weight(1f))
                                AircraftMetricCell("Satélites GPS", gps, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AircraftMetricCell("Altitude", altitudeText, Modifier.weight(1f))
                                AircraftMetricCell("Velocidade", speedText, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 3. Seção Últimas Missões: até 3 missões reais cadastradas recentemente */
@Composable
private fun RecentMissionsSection(
    uiState: MissionListUiState,
    activeMissionId: String?,
    onViewAllClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMissionClick: (Mission) -> Unit
) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF111827),
        border = BorderStroke(1.dp, Color(0xFF1F2937))) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Últimas missões",
                color = NeuralColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Ver todas",
                    color = NeuralColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeuralColors.TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        when (uiState) {
            is MissionListUiState.Loading -> {
                // Skeleton discreto de carregamento
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF111827),
                            border = BorderStroke(1.dp, Color(0xFF1F2937))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NeuralColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            is MissionListUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NeuralColors.Warning, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Não foi possível carregar as missões.",
                                color = NeuralColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onRefreshClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tentar novamente", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            is MissionListUiState.Unauthorized -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Text(
                        text = "Faça login para sincronizar suas missões de voo.",
                        color = NeuralColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            is MissionListUiState.Idle -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Text(
                        text = "Aguardando sincronização de missões.",
                        color = NeuralColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            is MissionListUiState.Success -> {
                val recentMissions = uiState.missions.sortedByDescending { it.id }.take(3)
                if (recentMissions.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF111827),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Text(
                            text = "Nenhuma missão cadastrada.",
                            color = NeuralColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentMissions.forEach { mission ->
                            val isActive = mission.id.toString() == activeMissionId
                            RecentMissionItem(
                                mission = mission,
                                isActive = isActive,
                                onClick = { onMissionClick(mission) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun RecentMissionItem(
    mission: Mission,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF151E2E),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF3B82F6) else Color(0xFF26354D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = mission.name,
                        color = NeuralColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "NA SESSÃO",
                                color = Color(0xFF60A5FA),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "#${mission.id} • ${mission.waypointCount} waypoints • ${mission.altitude.toInt()}m alt • Cadastrada",
                    color = NeuralColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Preview ilustrativo fornecido pelo projeto; métricas e estado continuam reais. */
@Composable
private fun AiSystemSummaryCard(
    detectionEnabled: Boolean,
    activeModelName: String,
    modelsCount: Int,
    lastInferenceMs: Long?,
    onDetailsClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF111827),
        border = BorderStroke(1.dp, Color(0xFF1F2937))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(9.dp))
                Text("Sistema de IA", color = Color(0xFFF1F5F9), fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val badgeColor = if (detectionEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                Surface(shape = RoundedCornerShape(5.dp), color = badgeColor.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))) {
                    Text(if (detectionEnabled) "ATIVO" else "INATIVO", color = badgeColor,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 285.dp || LocalDensity.current.fontScale >= 1.3f
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painter = painterResource(R.drawable.dashboard_person_detection),
                            contentDescription = "Imagem ilustrativa de detecção de pessoas",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(8.dp)))
                        AiDetails(activeModelName, modelsCount, lastInferenceMs)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painter = painterResource(R.drawable.dashboard_person_detection),
                            contentDescription = "Imagem ilustrativa de detecção de pessoas",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.weight(5f).aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(8.dp)))
                        AiDetails(activeModelName, modelsCount, lastInferenceMs, Modifier.weight(7f))
                    }
                }
            }
            Surface(onClick = onDetailsClick, shape = RoundedCornerShape(8.dp),
                color = Color(0xFF151E2E), border = BorderStroke(1.dp, Color(0xFF243044))) {
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 38.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Ver detalhes e modelos", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                        tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun AiDetails(
    activeModelName: String,
    modelsCount: Int,
    lastInferenceMs: Long?,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Visão Computacional", color = Color(0xFFF1F5F9), fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold)
        Text("Detecção de pessoas", color = Color(0xFF94A3B8), fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AiMiniMetric("Modelo", activeModelName, Modifier.weight(1.25f))
            AiMiniMetric("Latência", lastInferenceMs?.let { "$it ms" } ?: "--", Modifier.weight(0.9f))
            AiMiniMetric("Modelos", if (modelsCount > 0) "$modelsCount inst." else "--", Modifier.weight(0.9f))
        }
    }
}

@Composable
private fun AiMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = NeuralColors.TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF151E2E),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = NeuralColors.TextSecondary, fontSize = 9.sp)
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
