package com.sloth.registerapp.presentation.app.report.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

private data class MissionReport(
    val id: String,
    val name: String,
    val status: MissionStatus,
    val date: String,
    val duration: String,
    val modelName: String
)

private enum class MissionStatus {
    FAILED,
    ABORTED,
    COMPLETED
}

@Composable
fun ReportScreen(
    onMissionClick: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val visible = remember { mutableStateOf(false) }

    val missions = listOf(
        MissionReport("m1", "Perímetro Norte", MissionStatus.COMPLETED, "12 Jan 2026", "18m 42s", "DJI Mavic 3"),
        MissionReport("m2", "Linha de Transmissão", MissionStatus.ABORTED, "09 Jan 2026", "07m 05s", "DJI Phantom 4"),
        MissionReport("m3", "Talhão 07", MissionStatus.FAILED, "05 Jan 2026", "03m 21s", "Autel EVO II"),
        MissionReport("m4", "Alvo Urbano", MissionStatus.COMPLETED, "02 Jan 2026", "22m 10s", "DJI Inspire 2")
    )

    LaunchedEffect(Unit) {
        visible.value = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Relatórios de Missão",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Últimas execuções",
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(missions) { mission ->
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(250)
                )
            ) {
                MissionReportCard(
                    mission = mission,
                    onClick = { onMissionClick(mission.id) }
                )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = colorScheme.surfaceVariant
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onBackClick)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
            Column {
                Text(
                    text = "Relatório da Missão",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "ID: $missionId",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
        ) {
            DroneModelViewer(
                assetPath = "Drone Mavic Pro.glb",
                hintText = "Arraste para girar"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailSection(
            title = "Resumo",
            content = {
                InfoRow(label = "Modelo usado", value = "DJI Mavic 3")
                InfoRow(label = "Nome da missão", value = "Perímetro Norte")
                InfoRow(label = "Duração", value = "18m 42s")
                InfoRow(label = "Execução", value = "12 Jan 2026 • 14:32")
                InfoRow(label = "Observação", value = "Vento moderado, sem anomalias.")
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        DetailSection(
            title = "Machine Learning",
            content = {
                InfoRow(label = "Acurácia", value = "92.4%")
                InfoRow(label = "Objetos detectados", value = "127")
                InfoRow(label = "Classes", value = "Pessoa, Veículo, Estrutura")
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        DetailSection(
            title = "Galeria",
            content = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { idx ->
                        Surface(
                            modifier = Modifier
                                .size(70.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun MissionReportCard(
    mission: MissionReport,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (mission.status) {
        MissionStatus.COMPLETED -> colorScheme.secondary
        MissionStatus.ABORTED -> colorScheme.tertiary
        MissionStatus.FAILED -> colorScheme.error
    }
    val statusLabel = when (mission.status) {
        MissionStatus.COMPLETED -> "Concluída"
        MissionStatus.ABORTED -> "Abortada"
        MissionStatus.FAILED -> "Falha"
    }
    val statusIcon = when (mission.status) {
        MissionStatus.COMPLETED -> Icons.Default.CheckCircle
        MissionStatus.ABORTED -> Icons.Default.Warning
        MissionStatus.FAILED -> Icons.Default.Error
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Timelapse, contentDescription = null)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = mission.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusChip(label = statusLabel, color = statusColor, icon = statusIcon)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${mission.date} • ${mission.duration}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Modelo: ${mission.modelName}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun DroneModelViewer(
    assetPath: String,
    hintText: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.18f),
                        colorScheme.tertiary.copy(alpha = 0.08f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Prévia 3D indisponível",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Placeholder",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Modelo 3D do Drone",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = hintText,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, color = colorScheme.onSurface)
    }
}
