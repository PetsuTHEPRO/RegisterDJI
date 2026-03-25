package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.mission.components.MissionPreviewMapView

private val MissionBg = Color(0xFF050E1F)
private val MissionSurface = Color(0xFF0A1628)
private val MissionSurfaceAlt = Color(0xFF071120)
private val MissionBorder = Color(0xFF0D2040)
private val MissionPrimary = Color(0xFF00C2FF)
private val MissionSecondary = Color(0xFF0066FF)
private val MissionMuted = Color(0xFF4A7FA5)
private val MissionDanger = Color(0xFFFF3B6E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsTableScreen(
    missions: List<Mission> = emptyList(),
    isLoading: Boolean = false,
    canCreateMission: Boolean = true,
    createBlockedMessage: String? = null,
    onCreateMissionClick: () -> Unit = {},
    onViewMissionClick: (Int) -> Unit = {},
    onEditMissionClick: (Int) -> Unit = {},
    onDeleteMissionClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var missionToDelete by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MissionBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Missoes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gerenciador de missoes",
                            color = MissionMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MissionBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMissionClick,
                containerColor = if (canCreateMission) MissionPrimary else MissionSurface,
                contentColor = Color.White
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MissionBg, Color(0xFF091A36), MissionBg)
                    )
                )
        ) {
            when {
                isLoading -> LoadingMissionsState()
                missions.isEmpty() -> EmptyMissionsState(
                    canCreateMission = canCreateMission,
                    createBlockedMessage = createBlockedMessage,
                    onCreateMissionClick = onCreateMissionClick
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!createBlockedMessage.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MissionPrimary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = createBlockedMessage,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = MissionPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(missions, key = { it.id }) { mission ->
                        MissionListCard(
                            mission = mission,
                            onOpenClick = { onViewMissionClick(mission.id) },
                            onEditClick = { onEditMissionClick(mission.id) },
                            onDeleteClick = { missionToDelete = mission.id }
                        )
                    }
                }
            }
        }
    }

    if (missionToDelete != null) {
        AlertDialog(
            onDismissRequest = { missionToDelete = null },
            containerColor = MissionSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MissionDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir missao", color = Color.White)
                }
            },
            text = {
                Text(
                    "Deseja realmente excluir esta missao?",
                    color = MissionMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        missionToDelete?.let(onDeleteMissionClick)
                        missionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissionDanger)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { missionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LoadingMissionsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MissionPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Carregando missoes...", color = MissionMuted)
        }
    }
}

@Composable
private fun EmptyMissionsState(
    canCreateMission: Boolean,
    createBlockedMessage: String?,
    onCreateMissionClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nenhuma missao encontrada", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = createBlockedMessage ?: "Crie sua primeira missao para comecar.",
                color = MissionMuted
            )
            Button(
                onClick = onCreateMissionClick,
                enabled = canCreateMission,
                colors = ButtonDefaults.buttonColors(containerColor = MissionPrimary)
            ) {
                Text("Criar missao")
            }
        }
    }
}

@Composable
private fun MissionListCard(
    mission: Mission,
    onOpenClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MissionSurface,
                            MissionSurfaceAlt
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MissionPreviewPane(mission = mission)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = mission.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoMetric(label = "Velocidade", value = "${mission.autoSpeed} m/s")
                    InfoMetric(label = "Altura", value = "${mission.altitude.toInt()} m")
                    InfoMetric(label = "Waypoints", value = "${mission.waypointCount}")
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ActionMiniButton(
                                modifier = Modifier.fillMaxWidth(),
                                label = "Abrir missao",
                                icon = Icons.Default.OpenInNew,
                                tint = MissionPrimary,
                                onClick = onOpenClick
                            )
                        }
                        ActionIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Editar missao",
                            tint = MissionPrimary,
                            onClick = onEditClick
                        )
                        ActionIconButton(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir missao",
                            tint = MissionDanger,
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MissionMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionMiniButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MissionPreviewPane(mission: Mission) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f),
        shape = RoundedCornerShape(16.dp),
        color = MissionSurfaceAlt,
        border = BorderStroke(1.dp, MissionBorder)
    ) {
        val pointOfInterest = Point.fromLngLat(
            mission.pointOfInterestLongitude,
            mission.pointOfInterestLatitude
        )
        val previewPoints = mission.previewPoints.map { point ->
            Point.fromLngLat(point.longitude, point.latitude)
        }

        if (previewPoints.isNotEmpty() || mission.waypointCount > 0) {
            MissionPreviewMapView(
                modifier = Modifier.fillMaxSize(),
                previewPoints = previewPoints,
                pointOfInterest = pointOfInterest,
                primaryColor = MissionPrimary
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF061018),
                                Color(0xFF0A1628)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                PreviewFallbackOverlay()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = MissionPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sem preview", color = MissionMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PreviewFallbackOverlay() {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = MissionPrimary.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}
