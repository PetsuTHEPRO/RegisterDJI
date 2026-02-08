package com.sloth.registerapp.presentation.app.report.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.report.data.manager.FlightReportManager
import com.sloth.registerapp.features.report.data.manager.MissionMediaManager
import com.sloth.registerapp.features.report.domain.model.FlightReport
import com.sloth.registerapp.features.report.domain.model.MissionMedia
import com.sloth.registerapp.features.mission.domain.model.MissionExecutionMode
import com.sloth.registerapp.features.mission.domain.model.MissionOutcomeStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class MissionReport(
    val id: String, // id do relatório
    val missionId: String, // id da missão para abrir detalhe/galeria
    val name: String,
    val status: MissionOutcomeStatus,
    val executionMode: MissionExecutionMode,
    val date: String,
    val duration: String,
    val modelName: String
)

@Composable
fun ReportScreen(
    onMissionClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val flightReportManager = remember { FlightReportManager(context) }
    val reports by flightReportManager.reports.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<MissionOutcomeStatus?>(null) }

    val missions = remember(reports) { reports.map { it.toUiModel() } }

    val normalizedQuery = query.trim().lowercase()
    val filteredMissions = missions.filter { mission ->
        val matchesQuery = normalizedQuery.isBlank() ||
            mission.name.lowercase().contains(normalizedQuery) ||
            mission.date.lowercase().contains(normalizedQuery) ||
            mission.modelName.lowercase().contains(normalizedQuery)
        val matchesStatus = selectedStatus == null || mission.status == selectedStatus
        matchesQuery && matchesStatus
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Relatórios de Missão",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                },
                label = { Text("Buscar missão") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Todos") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatus == MissionOutcomeStatus.COMPLETED,
                        onClick = { selectedStatus = MissionOutcomeStatus.COMPLETED },
                        label = { Text("Concluídas") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatus == MissionOutcomeStatus.FAILED,
                        onClick = { selectedStatus = MissionOutcomeStatus.FAILED },
                        label = { Text("Falhas") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatus == MissionOutcomeStatus.ABORTED,
                        onClick = { selectedStatus = MissionOutcomeStatus.ABORTED },
                        label = { Text("Abortadas") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (filteredMissions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (missions.isEmpty()) {
                            "Nenhum relatório disponível ainda."
                        } else {
                            "Nenhum relatório encontrado com os filtros atuais."
                        },
                        modifier = Modifier.padding(14.dp),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredMissions, key = { it.id }) { mission ->
                MissionReportCard(mission = mission, onClick = { onMissionClick(mission.missionId) })
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    missionId: String,
    onBackClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaManager = remember { MissionMediaManager.getInstance(context) }
    val droneConnected by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    var missionMedia by remember(missionId) { mutableStateOf<List<MissionMedia>>(emptyList()) }

    fun refreshGallery() {
        scope.launch {
            missionMedia = mediaManager.getMediaByMission(missionId)
        }
    }

    LaunchedEffect(missionId) {
        refreshGallery()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text("Relatório da Missão", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text("ID: $missionId", color = colorScheme.onSurfaceVariant)
        Text(
            text = "Detalhamento completo da missão será exibido aqui.",
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Galeria da Missão",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = colorScheme.onSurface
        )

        when {
            missionMedia.isEmpty() && droneConnected == null -> {
                Text(
                    text = "Conecte o drone para ver as mídias.",
                    color = colorScheme.onSurfaceVariant
                )
            }

            missionMedia.isEmpty() -> {
                val text = if (droneConnected == null) {
                    "Conecte o drone para ver as mídias."
                } else {
                    "Nenhuma mídia registrada nesta missão."
                }
                Text(text = text, color = colorScheme.onSurfaceVariant)
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(missionMedia, key = { it.id }) { media ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surface,
                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (media.mediaType.name == "PHOTO") "Foto" else "Vídeo",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = media.dronePath ?: media.localPath ?: "Mídia sem caminho",
                                            fontSize = 12.sp,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                if (media.isDownloaded) "No telefone" else "No drone",
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (media.isDownloaded && !media.localPath.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                runCatching {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(media.localPath.toUri(), "*/*")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                }.onFailure {
                                                    Toast.makeText(context, "Não foi possível abrir a mídia.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text("Abrir")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                if (droneConnected == null) {
                                                    Toast.makeText(context, "Conecte o drone para baixar a mídia.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    scope.launch {
                                                        mediaManager.markDownloaded(
                                                            mediaId = media.id,
                                                            localPath = "content://vantly/local/${media.id}"
                                                        )
                                                        refreshGallery()
                                                        Toast.makeText(context, "Mídia marcada como baixada no telefone.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Baixar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionReportCard(
    mission: MissionReport,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (mission.status) {
        MissionOutcomeStatus.COMPLETED -> colorScheme.secondary
        MissionOutcomeStatus.ABORTED -> colorScheme.tertiary
        MissionOutcomeStatus.FAILED -> colorScheme.error
    }

    val statusLabel = when (mission.status) {
        MissionOutcomeStatus.COMPLETED -> "Concluída"
        MissionOutcomeStatus.ABORTED -> "Abortada"
        MissionOutcomeStatus.FAILED -> "Falha"
    }

    val statusIcon = when (mission.status) {
        MissionOutcomeStatus.COMPLETED -> Icons.Default.CheckCircle
        MissionOutcomeStatus.ABORTED -> Icons.Default.Warning
        MissionOutcomeStatus.FAILED -> Icons.Default.Error
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🚁", fontSize = 30.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mission.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mission.modelName,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "${mission.date} • ${mission.duration}",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                AssistChip(
                    onClick = {},
                    label = { Text(statusLabel, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                )
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Ações")
            }
        }
    }
}

private fun FlightReport.toUiModel(): MissionReport {
    val status = extraData["status"]
        ?.let { runCatching { MissionOutcomeStatus.valueOf(it) }.getOrNull() }
        ?: MissionOutcomeStatus.COMPLETED
    val executionMode = extraData["executionMode"]
        ?.let { runCatching { MissionExecutionMode.valueOf(it) }.getOrNull() }
        ?: MissionExecutionMode.UNKNOWN
    val missionId = extraData["missionId"] ?: id

    return MissionReport(
        id = id,
        missionId = missionId,
        name = missionName,
        status = status,
        executionMode = executionMode,
        date = createdAtMs.toShortDate(),
        duration = durationMs.toDurationLabel(),
        modelName = aircraftName
    )
}

private fun Long.toShortDate(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale("pt", "BR"))
    return formatter.format(Date(this))
}

private fun Long.toDurationLabel(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds.toString().padStart(2, '0')}s"
}
