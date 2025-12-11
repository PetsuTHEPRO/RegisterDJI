package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showWaypointDialog by remember { mutableStateOf(false) }
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
                                    1 -> "Waypoints"
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
                    .weight(1f) // Takes all remaining vertical space
                    .verticalScroll(rememberScrollState()) // Makes this section scrollable
            ) {
                AnimatedContent(targetState = currentStep, label = "step") { step ->
                    when (step) {
                        0 -> ConfigStep(missionData, { missionData = it }, cardBg, primaryBlue, textWhite, textGray)
                        1 -> WaypointsStep(
                            missionData.waypoints,
                            { showWaypointDialog = true },
                            { wp -> missionData.waypoints.remove(wp); missionData = missionData.copy() },
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


        if (showWaypointDialog) {
            WaypointDialog(
                onDismiss = { showWaypointDialog = false },
                onConfirm = { wp ->
                    wp.id = missionData.waypoints.size + 1
                    missionData.waypoints.add(wp)
                    missionData = missionData.copy()
                    showWaypointDialog = false
                },
                cardBg, primaryBlue, textWhite
            )
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
                
                OutlinedTextField(
                    value = data.pointOfInterest,
                    onValueChange = { onChange(data.copy(pointOfInterest = it)) },
                    label = { Text("Ponto de Interesse") },
                    placeholder = { Text("-2.5367:-44.2792") },
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
                    Text("Cancelar se perder sinal", color = textWhite)
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
                    Text("Rotação do Gimbal", color = textWhite)
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
fun WaypointsStep(
    waypoints: List<Waypoint>,
    onAdd: () -> Unit,
    onDelete: (Waypoint) -> Unit,
    primary: Color,
    cardBg: Color,
    textWhite: Color,
    textGray: Color,
    redAccent: Color
) {
    if (waypoints.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📍", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("Nenhum waypoint", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
            Text("Adicione pontos para criar a rota", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Waypoint")
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍 ${waypoints.size} waypoint${if (waypoints.size != 1) "s" else ""}", 
                         fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                }
            }

            waypoints.forEachIndexed { idx, wp ->
                Card(colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primary.copy(0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${idx + 1}", fontWeight = FontWeight.Bold, color = primary)
                            }
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text("Waypoint ${idx + 1}", fontWeight = FontWeight.Bold, color = textWhite)
                            Text(
                                "${String.format("%.5f", wp.latitude)}, ${String.format("%.5f", wp.longitude)}",
                                fontSize = 12.sp,
                                color = textGray
                            )
                            Text("Alt: ${wp.altitude}m", fontSize = 11.sp, color = textGray)
                        }
                        
                        IconButton(onClick = { onDelete(wp) }) {
                            Icon(Icons.Default.Delete, null, tint = redAccent)
                        }
                    }
                }
            }

            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Waypoint")
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
fun WaypointDialog(
    onDismiss: () -> Unit,
    onConfirm: (Waypoint) -> Unit,
    cardBg: Color,
    primary: Color,
    textWhite: Color
) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var alt by remember { mutableStateOf("") }

    val valid = lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null && alt.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📍 Novo Waypoint") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = lon,
                    onValueChange = { lon = it },
                    label = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = alt,
                    onValueChange = { alt = it },
                    label = { Text("Altitude (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valid) {
                        onConfirm(Waypoint(0, lat.toDouble(), lon.toDouble(), alt.toDouble()))
                    }
                },
                enabled = valid
            ) {
                Text("Adicionar")
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