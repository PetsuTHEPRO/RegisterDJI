package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.remote.dto.WaypointDto
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.mission.domain.usecase.CreateMissionUseCase
import com.sloth.registerapp.presentation.mission.components.MapboxMapView
import com.sloth.registerapp.presentation.mission.model.Waypoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

data class MissionData(
    var name: String = "",
    var pointOfInterest: String = "",
    var autoFlightSpeed: Float = 5f,
    var maxFlightSpeed: Float = 15f,
    var exitOnSignalLost: Boolean = false,
    var gimbalPitchRotationEnabled: Boolean = false,
    var repeatTimes: Int = 1,
    var waypoints: MutableList<Waypoint> = mutableListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionCreateScreen(onBackClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(0) }
    var missionData by remember { mutableStateOf(MissionData()) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val createMissionUseCase = remember {
        CreateMissionUseCase(
            MissionRepositoryImpl(
                RetrofitClient.getInstance(context),
                TokenRepository.getInstance(context)
            )
        )
    }
    
    // Estado para controlar a interação com o mapa
    var isMapTouched by remember { mutableStateOf(false) }

    val canProceed = when (currentStep) {
        0 -> missionData.name.isNotBlank()
        1 -> missionData.waypoints.isNotEmpty()
        else -> true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colorScheme.background, colorScheme.surfaceVariant)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, null, tint = colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("🚁", fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Criar Missão",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                when (currentStep) {
                                    0 -> "Configurações"
                                    1 -> "Mapa e Waypoints"
                                    else -> "Revisão"
                                },
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        if (index <= currentStep) colorScheme.primary else colorScheme.surfaceVariant,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    // O scroll é desativado quando o mapa está sendo tocado
                    .verticalScroll(rememberScrollState(), enabled = !isMapTouched)
            ) {
                AnimatedContent(targetState = currentStep, label = "step") { step ->
                    when (step) {
                        0 -> ConfigStep(missionData, { missionData = it })
                        1 -> MapStep(
                            data = missionData,
                            onDataChange = { missionData = it },
                            onMapTouch = { isTouched -> isMapTouched = isTouched }
                        )
                        else -> ReviewStep(missionData)
                    }
                }
            }

            // Botões de navegação
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("Voltar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                in 0..1 -> if (canProceed) currentStep++
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        val missionDto = missionDataToServerDto(missionData)
                                        val result = withContext(Dispatchers.IO) {
                                            createMissionUseCase(missionDto)
                                        }
                                        result.onSuccess {
                                            showSuccessDialog = true
                                        }.onFailure { e ->
                                            errorMessage = when (e) {
                                                is IllegalArgumentException -> e.message ?: "Dados inválidos"
                                                is HttpException -> "Erro ${e.code()} ao salvar missão"
                                                else -> e.message ?: "Erro ao salvar missão"
                                            }
                                        }
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = canProceed && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 2) colorScheme.secondary else colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                when (currentStep) {
                                    0 -> "Continuar"
                                    1 -> "Revisar"
                                    else -> "Salvar"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (showSuccessDialog) {
            SuccessDialog(
                onDismiss = { showSuccessDialog = false; onBackClick() },
                missionData.name
            )
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text("Falha ao salvar missão") },
                text = { Text(errorMessage ?: "Erro desconhecido") },
                confirmButton = {
                    Button(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun MapStep(
    data: MissionData,
    onDataChange: (MissionData) -> Unit,
    onMapTouch: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var showAltitudeDialog by remember { mutableStateOf(false) }
    var pendingCoordinates by remember { mutableStateOf<Point?>(null) }
    
    // As variáveis mapboxMap, pointAnnotationManager e polylineAnnotationManager são controladas pelo MapboxMapView agora
    // e passadas via callback onMapReady. O LaunchedEffect para desenhar waypoints também foi movido.

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(0.15f))) {
             Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Clique no mapa para adicionar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        "Primeiro clique: Ponto de Interesse",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Próximos cliques: Waypoints",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onMapTouch(true) // Desativa o scroll da tela
                            try {
                                awaitRelease()
                            } finally {
                                onMapTouch(false) // Reativa o scroll da tela
                            }
                        }
                    )
                }
        ) {
            MapboxMapView(
                modifier = Modifier.fillMaxSize(),
                waypoints = data.waypoints,
                primaryColor = colorScheme.primary,
                onMapReady = { map -> // 'map' aqui é MapboxMap
                    // Configura a câmera inicial
                    map.setCamera(
                        com.mapbox.maps.CameraOptions.Builder()
                            .center(Point.fromLngLat(-44.3025, -2.5307))
                            .zoom(12.0)
                            .build()
                    )
                    
                    // Adiciona o listener de cliques ao mapa
                    map.addOnMapClickListener { point ->
                        pendingCoordinates = point
                        showAltitudeDialog = true
                        true 
                    }
                }
            )
        }

        if (data.waypoints.isNotEmpty()) {
             Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📍 ${data.waypoints.size} waypoint${if (data.waypoints.size != 1) "s" else ""}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    data.waypoints.forEachIndexed { idx, wp ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primary.copy(0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${String.format("%.5f", wp.latitude)}, ${String.format("%.5f", wp.longitude)}",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Alt: ${wp.altitude}m",
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    val updatedWaypoints = data.waypoints.toMutableList().apply { removeAt(idx) }
                                    onDataChange(data.copy(waypoints = updatedWaypoints))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        if (idx < data.waypoints.size - 1) {
                            Divider(
                                color = colorScheme.outline.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (data.pointOfInterest.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(0.1f))) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📌", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Ponto de Interesse", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            data.pointOfInterest,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showAltitudeDialog && pendingCoordinates != null) {
        AltitudeDialog(
            onDismiss = { showAltitudeDialog = false; pendingCoordinates = null },
            onConfirm = { dialogAltitude -> // Renomeado para evitar conflito de nome
                val point = pendingCoordinates!!
                
                if (data.pointOfInterest.isEmpty()) {
                    onDataChange(data.copy(pointOfInterest = "${point.latitude()}:${point.longitude()}"))
                } else {
                    val newWaypoint = Waypoint(
                        id = data.waypoints.size + 1,
                        latitude = point.latitude(),
                        longitude = point.longitude(),
                        altitude = dialogAltitude // Usando a altitude fornecida pelo diálogo
                    )
                    val updatedWaypoints = data.waypoints.toMutableList().apply { add(newWaypoint) }
                    onDataChange(data.copy(waypoints = updatedWaypoints))
                }
                
                showAltitudeDialog = false
                pendingCoordinates = null
            },
        )
    }
}

@Composable
fun AltitudeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var altitude by remember { mutableStateOf("50") }
    val valid = altitude.toDoubleOrNull() != null && altitude.toDouble() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✈️ Definir Altitude") },
        text = {
            OutlinedTextField(
                value = altitude,
                onValueChange = { altitude = it },
                label = { Text("Altitude") },
                suffix = { Text("metros") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onConfirm(altitude.toDouble()) },
                enabled = valid
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConfigStep(
    data: MissionData,
    onChange: (MissionData) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "📝 Informações Básicas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = data.name,
                    onValueChange = { onChange(data.copy(name = it)) },
                    label = { Text("Nome da Missão") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "⚡ Velocidades",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = data.autoFlightSpeed.toString(),
                        onValueChange = { onChange(data.copy(autoFlightSpeed = it.toFloatOrNull() ?: 5f)) },
                        label = { Text("Auto") },
                        suffix = { Text("m/s") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    
                    OutlinedTextField(
                        value = data.maxFlightSpeed.toString(),
                        onValueChange = { onChange(data.copy(maxFlightSpeed = it.toFloatOrNull() ?: 15f)) },
                        label = { Text("Máx") },
                        suffix = { Text("m/s") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "⚙️ Opções",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cancelar se perder sinal", color = colorScheme.onSurface, fontSize = 13.sp)
                    Switch(
                        checked = data.exitOnSignalLost,
                        onCheckedChange = { onChange(data.copy(exitOnSignalLost = it)) }
                    )
                }
                
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rotação do Gimbal", color = colorScheme.onSurface, fontSize = 13.sp)
                    Switch(
                        checked = data.gimbalPitchRotationEnabled,
                        onCheckedChange = { onChange(data.copy(gimbalPitchRotationEnabled = it)) }
                    )
                }
                
                OutlinedTextField(
                    value = data.repeatTimes.toString(),
                    onValueChange = { onChange(data.copy(repeatTimes = it.toIntOrNull() ?: 1)) },
                    label = { Text("Repetições") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

private fun missionDataToServerDto(data: MissionData): ServerMissionDto {
    val (poiLat, poiLng) = parsePoi(data.pointOfInterest, data.waypoints)
    return ServerMissionDto(
        auto_flight_speed = data.autoFlightSpeed.toDouble(),
        exit_on_signal_lost = data.exitOnSignalLost,
        finished_action = "NO_ACTION",
        flight_path_mode = "NORMAL",
        gimbal_pitch_rotation_enabled = data.gimbalPitchRotationEnabled,
        goto_first_waypoint_mode = "SAFELY",
        heading_mode = "AUTO",
        id = 0,
        max_flight_speed = data.maxFlightSpeed.toDouble(),
        name = data.name,
        poi_latitude = poiLat,
        poi_longitude = poiLng,
        repeat_times = data.repeatTimes,
        waypoints = data.waypoints.map {
            WaypointDto(
                actions = emptyList(),
                altitude = it.altitude,
                latitude = it.latitude,
                longitude = it.longitude,
                turn_mode = "CLOCKWISE"
            )
        }
    )
}

private fun parsePoi(pointOfInterest: String, waypoints: List<Waypoint>): Pair<Double, Double> {
    val parts = pointOfInterest.split(":")
    val lat = parts.getOrNull(0)?.toDoubleOrNull()
    val lng = parts.getOrNull(1)?.toDoubleOrNull()
    if (lat != null && lng != null) {
        return lat to lng
    }
    val fallback = waypoints.firstOrNull()
    return (fallback?.latitude ?: 0.0) to (fallback?.longitude ?: 0.0)
}

@Composable
fun ReviewStep(
    data: MissionData
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.secondary.copy(0.15f))) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✅", fontSize = 32.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Revisão da Missão",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text("Verifique os dados", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📝 Básico", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                InfoRow("Nome", data.name)
                InfoRow("POI", data.pointOfInterest)
                InfoRow("Waypoints", "${data.waypoints.size}")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ Velocidades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                InfoRow("Auto", "${data.autoFlightSpeed} m/s")
                InfoRow("Máxima", "${data.maxFlightSpeed} m/s")
                InfoRow("Repetições", "${data.repeatTimes}x")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
    }
}

@Composable
fun SuccessDialog(
    onDismiss: () -> Unit,
    name: String
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("✅", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("Missão Salva!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "A missão \"$name\" foi criada com sucesso!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary)
            ) {
                Text("Concluir", fontWeight = FontWeight.Bold)
            }
        }
    )
}
