package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.mission.domain.model.Mission
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val colorScheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var visibleCount by remember { mutableIntStateOf(10) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var missionToDelete by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(missions.size) {
        if (visibleCount > missions.size) {
            visibleCount = missions.size.coerceAtLeast(10)
        }
    }

    LaunchedEffect(listState, missions.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it >= (visibleCount - 3).coerceAtLeast(0) }
            .distinctUntilChanged()
            .collectLatest { nearEnd ->
                if (nearEnd && visibleCount < missions.size) {
                    visibleCount = (visibleCount + 8).coerceAtMost(missions.size)
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Missões",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface
                ),
                modifier = Modifier.height(64.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMissionClick,
                containerColor = if (canCreateMission) colorScheme.primary else colorScheme.surfaceVariant,
                contentColor = if (canCreateMission) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background,
                            colorScheme.surfaceVariant,
                            colorScheme.background
                        )
                    )
                )
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Carregando missões...", color = colorScheme.onSurfaceVariant)
                        }
                    }
                }

                missions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Nenhuma missão encontrada", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = createBlockedMessage ?: "Crie sua primeira missão para começar",
                            textAlign = TextAlign.Center,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onCreateMissionClick, enabled = canCreateMission) {
                            Text("Criar missão")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        itemsIndexed(missions.take(visibleCount), key = { _, mission -> mission.id }) { _, mission ->
                            MissionCard(
                                mission = mission,
                                onViewClick = { onViewMissionClick(mission.id) },
                                onEditClick = { onEditMissionClick(mission.id) },
                                onDeleteClick = {
                                    missionToDelete = mission.id
                                    showDeleteDialog = true
                                }
                            )
                        }

                        if (visibleCount < missions.size) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!createBlockedMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = colorScheme.tertiaryContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = createBlockedMessage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Excluir missão", color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "Deseja realmente excluir esta missão? Esta ação não pode ser desfeita.",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            missionToDelete?.let { onDeleteMissionClick(it) }
                            showDeleteDialog = false
                            missionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            missionToDelete = null
                        }
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun MissionCard(
    mission: Mission,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewClick
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier
                        .width(112.dp)
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Markpoint: ${"%.4f".format(mission.latitude)}, ${"%.4f".format(mission.longitude)}",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Vel. padrão: ${mission.autoSpeed} m/s",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Waypoints: ${mission.waypointCount}",
                        fontSize = 12.sp,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ações",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                        border = BorderStroke(1.dp, colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir")
                    }
                }
            }
        }
    }
}
