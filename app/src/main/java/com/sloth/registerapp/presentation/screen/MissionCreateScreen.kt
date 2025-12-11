package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.sloth.registerapp.presentation.component.MapboxMapView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Waypoint(
    var id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

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
    val primaryBlue = Color(0xFF3B82F6)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenAccent = Color(0xFF22C55E)
    val redAccent = Color(0xFFEF4444)

    var currentStep by remember { mutableStateOf(0) }
    var missionData by remember { mutableStateOf(MissionData()) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canProceed = when (currentStep) {
        0 -> missionData.name.isNotBlank()
        1 -> missionData.waypoints.isNotEmpty()
        else -> true
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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, null, tint = textGray)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("🚁", fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Criar Missão", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textWhite)
                            Text(
                                when (currentStep) {
                                    0 -> "Configurações"
                                    1 -> "Mapa e Waypoints"
                                    else -> "Revisão"
                                },
                                fontSize = 13.sp,
                                color = textGray
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
                                        if (index <= currentStep) primaryBlue else darkBg,
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
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(targetState = currentStep, label = "step") { step ->
                    when (step) {
                        0 -> ConfigStep(missionData, { missionData = it }, cardBg, primaryBlue, textWhite, textGray)
                        1 -> MapStep(
                            missionData,
                            onDataChange = { missionData = it },
                            primaryBlue, cardBg, textWhite, textGray, redAccent
                        )
                        else -> ReviewStep(missionData, cardBg, primaryBlue, textWhite, textGray, greenAccent)
                    }
                }
            }

            // Botões de navegação
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
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
                                        delay(1500)
                                        isLoading = false
                                        showSuccessDialog = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = canProceed && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 2) greenAccent else primaryBlue
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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
                missionData.name,
                cardBg, greenAccent, textWhite
            )
        }
    }
}

@Composable
fun MapStep(
    data: MissionData,
    onDataChange: (MissionData) -> Unit,
    primary: Color,
    cardBg: Color,
    textWhite: Color,
    textGray: Color,
    redAccent: Color
) {
    var showAltitudeDialog by remember { mutableStateOf(false) }
    var pendingCoordinates by remember { mutableStateOf<Point?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = primary.copy(0.15f))) {
             Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Clique no mapa para adicionar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    Text("Primeiro clique: Ponto de Interesse", fontSize = 12.sp, color = textGray)
                    Text("Próximos cliques: Waypoints", fontSize = 12.sp, color = textGray)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            MapboxMapView(
                modifier = Modifier.fillMaxSize(),
                waypoints = data.waypoints,
                primaryColor = primary,
                onMapReady = { map ->
                    map.setCamera(
                        com.mapbox.maps.CameraOptions.Builder()
                            .center(Point.fromLngLat(-44.3025, -2.5307))
                            .zoom(12.0)
                            .build()
                    )
                    
                    map.addOnMapClickListener { point ->
                        pendingCoordinates = point
                        showAltitudeDialog = true
                        true 
                    }
                }
            )
        }

        if (data.waypoints.isNotEmpty()) {
             Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📍 ${data.waypoints.size} waypoint${if (data.waypoints.size != 1) "s" else ""}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
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
                                color = primary.copy(0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primary)
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${String.format("%.5f", wp.latitude)}, ${String.format("%.5f", wp.longitude)}",
                                    fontSize = 11.sp,
                                    color = textGray
                                )
                                Text("Alt: ${wp.altitude}m", fontSize = 10.sp, color = textGray.copy(0.7f))
                            }
                            
                            IconButton(
                                onClick = {
                                    val updatedWaypoints = data.waypoints.toMutableList().apply { removeAt(idx) }
                                    onDataChange(data.copy(waypoints = updatedWaypoints))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, tint = redAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        if (idx < data.waypoints.size - 1) {
                            Divider(color = textGray.copy(0.2f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        if (data.pointOfInterest.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = primary.copy(0.1f))) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📌", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Ponto de Interesse", fontSize = 11.sp, color = textGray)
                        Text(data.pointOfInterest, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    }
                }
            }
        }
    }

    if (showAltitudeDialog && pendingCoordinates != null) {
        AltitudeDialog(
            onDismiss = { showAltitudeDialog = false; pendingCoordinates = null },
            onConfirm = { altitude ->
                val point = pendingCoordinates!!
                
                if (data.pointOfInterest.isEmpty()) {
                    onDataChange(data.copy(pointOfInterest = "${point.latitude()}:${point.longitude()}"))
                } else {
                    val newWaypoint = Waypoint(
                        id = data.waypoints.size + 1,
                        latitude = point.latitude(),
                        longitude = point.longitude(),
                        altitude = altitude
                    )
                    val updatedWaypoints = data.waypoints.toMutableList().apply { add(newWaypoint) }
                    onDataChange(data.copy(waypoints = updatedWaypoints))
                }
                
                showAltitudeDialog = false
                pendingCoordinates = null
            },
            cardBg, primary, textWhite
        )
    }
}

@Composable
fun AltitudeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    cardBg: Color,
    primary: Color,
    textWhite: Color
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
    onChange: (MissionData) -> Unit,
    cardBg: Color,
    primary: Color,
    textWhite: Color,
    textGray: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📝 Informações Básicas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                
                OutlinedTextField(
                    value = data.name,
                    onValueChange = { onChange(data.copy(name = it)) },
                    label = { Text("Nome da Missão") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚡ Velocidades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                
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

        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚙️ Opções", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cancelar se perder sinal", color = textWhite, fontSize = 13.sp)
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
                    Text("Rotação do Gimbal", color = textWhite, fontSize = 13.sp)
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

@Composable
fun ReviewStep(
    data: MissionData,
    cardBg: Color,
    primary: Color,
    textWhite: Color,
    textGray: Color,
    greenAccent: Color
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = greenAccent.copy(0.15f))) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✅", fontSize = 32.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Revisão da Missão", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    Text("Verifique os dados", fontSize = 12.sp, color = textGray)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📝 Básico", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                InfoRow("Nome", data.name, textWhite, textGray)
                InfoRow("POI", data.pointOfInterest, textWhite, textGray)
                InfoRow("Waypoints", "${data.waypoints.size}", textWhite, textGray)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ Velocidades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                InfoRow("Auto", "${data.autoFlightSpeed} m/s", textWhite, textGray)
                InfoRow("Máxima", "${data.maxFlightSpeed} m/s", textWhite, textGray)
                InfoRow("Repetições", "${data.repeatTimes}x", textWhite, textGray)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, textWhite: Color, textGray: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = textGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textWhite)
    }
}

@Composable
fun SuccessDialog(
    onDismiss: () -> Unit,
    name: String,
    cardBg: Color,
    greenAccent: Color,
    textWhite: Color
) {
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
                colors = ButtonDefaults.buttonColors(containerColor = greenAccent)
            ) {
                Text("Concluir", fontWeight = FontWeight.Bold)
            }
        }
    )
}
