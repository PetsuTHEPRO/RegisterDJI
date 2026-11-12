package com.sloth.registerapp.presentation.mission.screens

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.mission.data.preview.MissionPreviewStorage
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import com.sloth.registerapp.presentation.shared.components.RoutePreview
import com.sloth.registerapp.presentation.shared.components.VantlySearchField
import java.util.Locale

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
    var searchQuery by remember { mutableStateOf("") }
    var missionToDelete by remember { mutableStateOf<Int?>(null) }
    val shown = remember(missions, searchQuery) {
        missions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    Scaffold(containerColor = NeuralColors.DashboardBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onBackClick, shape = RoundedCornerShape(8.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White,
                        modifier = Modifier.size(18.dp))
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Route, null, tint = NeuralColors.DashboardBlue,
                        modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text("Missões", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Planos de voo cadastrados", color = NeuralColors.DashboardTextSecondary,
                    fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = NeuralColors.DashboardBorder)
        LazyColumn(Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                BoxWithConstraints {
                    val narrow = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.4f
                    if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VantlySearchField(searchQuery, { searchQuery = it }, "Buscar missão...")
                            NewMissionButton(canCreateMission, onCreateMissionClick)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                VantlySearchField(searchQuery, { searchQuery = it }, "Buscar missão...")
                            }
                            NewMissionButton(canCreateMission, onCreateMissionClick)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("REPOSITÓRIO DE ROTAS", color = NeuralColors.DashboardTextSecondary,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${missions.size} planos", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                }
            }
            if (isLoading) item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeuralColors.DashboardBlue, modifier = Modifier.size(26.dp))
                }
            } else if (shown.isEmpty()) item {
                Text(createBlockedMessage ?: if (searchQuery.isNotBlank()) "Nenhuma missão encontrada."
                    else "Nenhuma missão cadastrada.", color = NeuralColors.DashboardTextSecondary,
                    fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            } else items(shown, key = { it.id }) { mission ->
                MissionCard(mission, onViewMissionClick) {
                    missionToDelete = mission.id
                }
            }
        }
        }
    }
    if (missionToDelete != null) AlertDialog(
        onDismissRequest = { missionToDelete = null },
        containerColor = NeuralColors.DashboardCard,
        icon = { Icon(Icons.Default.Warning, null, tint = NeuralColors.Danger) },
        title = { Text("Excluir Missão", color = Color.White) },
        text = { Text("Tem certeza que deseja excluir esta missão?", color = NeuralColors.DashboardTextSecondary) },
        confirmButton = {
            Button(onClick = { missionToDelete?.let(onDeleteMissionClick); missionToDelete = null },
                colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Danger)) { Text("Excluir") }
        },
        dismissButton = {
            OutlinedButton(onClick = { missionToDelete = null }) { Text("Cancelar") }
        }
    )
}

@Composable
private fun NewMissionButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(8.dp),
        color = NeuralColors.DashboardBlue) {
        Row(Modifier.height(46.dp).padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Text("Nova Missão", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MissionCard(
    mission: Mission,
    onOpen: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, mission.id, mission.previewPoints) {
        value = MissionPreviewStorage.loadPreview(context, mission.id)
    }
    val points = remember(mission.previewPoints) { mission.previewPoints.map { it.latitude to it.longitude } }
    val poi = remember(mission.pointOfInterestLatitude, mission.pointOfInterestLongitude) {
        (mission.pointOfInterestLatitude to mission.pointOfInterestLongitude)
            .takeIf { it.first != 0.0 || it.second != 0.0 }
    }
    val distance = remember(points) {
        points.zipWithNext().sumOf { (a, b) ->
            val result = FloatArray(1)
            Location.distanceBetween(a.first, a.second, b.first, b.second, result)
            result[0].toDouble()
        }
    }
    Surface(shape = RoundedCornerShape(12.dp), color = NeuralColors.DashboardCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(144.dp)
                .background(NeuralColors.DashboardInner, RoundedCornerShape(8.dp))) {
                if (bitmap != null) {
                    Image(bitmap!!.asImageBitmap(), contentDescription = "Mapa e rota da missão ${mission.name}",
                        contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                } else {
                    RoutePreview(points, poi = poi, lineColor = NeuralColors.DashboardBlue)
                }
                Surface(shape = RoundedCornerShape(5.dp), color = NeuralColors.DashboardBg.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Text("#${mission.id}", color = Color.White, fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(mission.name, color = Color.White, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${mission.waypointCount} pontos  •  ${mission.altitude.toInt()} m  •  " +
                        String.format(Locale.getDefault(), "%.1f km", distance / 1000.0),
                        color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                }
                Surface(onClick = { onOpen(mission.id) }, shape = RoundedCornerShape(7.dp),
                    color = NeuralColors.DashboardInner,
                    border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                    Text("Abrir", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, "Ações da missão", tint = NeuralColors.DashboardTextSecondary)
                    }
                    DropdownMenu(menuExpanded, { menuExpanded = false },
                        modifier = Modifier.background(NeuralColors.DashboardCard)) {
                        DropdownMenuItem(text = { Text("Excluir") }, leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, null)
                        }, onClick = { menuExpanded = false; onDelete() })
                    }
                }
            }
        }
    }
}
