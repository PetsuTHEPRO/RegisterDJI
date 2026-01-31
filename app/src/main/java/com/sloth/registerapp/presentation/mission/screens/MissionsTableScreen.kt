package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.registerapp.features.mission.domain.model.Mission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsTableScreen(
    missions: List<Mission> = emptyList(),
    isLoading: Boolean = false,
    onCreateMissionClick: () -> Unit = {},
    onViewMissionClick: (Int) -> Unit = {},
    onEditMissionClick: (Int) -> Unit = {},
    onDeleteMissionClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    // Estado de animação
    var visible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var missionToDelete by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Missões",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
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
                modifier = Modifier.height(80.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMissionClick,
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nova Missão",
                    modifier = Modifier.size(24.dp)
                )
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

            // Conteúdo
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 100)
                )
            ) {
                when {
                    isLoading -> {
                        // Estado de carregamento
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(60.dp),
                                    color = colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "Carregando missões...",
                                    fontSize = 16.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    missions.isEmpty() -> {
                        // Estado vazio
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "📭",
                                    fontSize = 80.sp,
                                    modifier = Modifier.alpha(0.5f)
                                )
                                Text(
                                    text = "Nenhuma missão encontrada",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Crie sua primeira missão para começar",
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onCreateMissionClick,
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Criar Missão",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Lista de missões
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(missions) { mission ->
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
                            
                            // Espaçamento no final
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Dialog de confirmação de exclusão
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Excluir Missão",
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Excluir",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            missionToDelete = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Cabeçalho do card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // ID Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "#${mission.id}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            fontFamily = FontFamily.Monospace // Used here
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Ícone e nome
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🚁",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mission.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }

                // Botão expandir/recolher
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informações principais (sempre visíveis)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Coordenadas
                    InfoChip(
                        icon = "📍",
                        label = "Lat/Long",
                        value = "${String.format("%.4f", mission.latitude)}, ${String.format("%.4f", mission.longitude)}",
                        modifier = Modifier.weight(1f)
                    )

                    // Waypoints
                    InfoChip(
                        icon = "📍",
                        label = "Waypoints",
                        value = "${mission.waypointCount}",
                        accentColor = colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Informações expandidas
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Velocidades
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpeedInfoCard(
                            label = "Velocidade Auto",
                            value = "${mission.autoSpeed} m/s",
                            icon = "⚡",
                            modifier = Modifier.weight(1f)
                        )

                        SpeedInfoCard(
                            label = "Velocidade Máx",
                            value = "${mission.maxSpeed} m/s",
                            icon = "🚀",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(
                        color = colorScheme.outline.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botões de ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botão Visualizar
                        OutlinedButton(
                            onClick = onViewClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorScheme.onSurface
                            ),
                            border = BorderStroke(1.5.dp, colorScheme.onSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Visualizar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Botão Editar
                        Button(
                            onClick = onEditClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Botão Excluir
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorScheme.error
                            ),
                            border = BorderStroke(1.5.dp, colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor ?: colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SpeedInfoCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

// Preview (exemplo de uso)
@Composable
@Preview(showBackground = true)
fun PreviewMissionsTable() {
    val sampleMissions = listOf(
        Mission(1, "Missão Alpha", -2.5387, -44.2827, 5, 5.0f, 15.0f),
        Mission(2, "Missão Beta", -2.5401, -44.2845, 8, 7.5f, 20.0f),
        Mission(3, "Missão Gamma", -2.5365, -44.2798, 12, 10.0f, 25.0f)
    )

    MissionsTableScreen(
        missions = sampleMissions,
        isLoading = false
    )
}
