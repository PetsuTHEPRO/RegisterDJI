package com.sloth.registerapp.presentation.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class para representar uma missão
data class Mission(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val waypointCount: Int,
    val autoSpeed: Float,
    val maxSpeed: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsTableScreen(
    missions: List<Mission> = emptyList(),
    isLoading: Boolean = false,
    onCreateMissionClick: () -> Unit = {},
    onViewMissionClick: (Int) -> Unit = {},
    onDeleteMissionClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val lightBlue = Color(0xFF60A5FA)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenAccent = Color(0xFF22C55E)
    val redAccent = Color(0xFFEF4444)

    // Estado de animação
    var visible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var missionToDelete by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkBg,
                        Color(0xFF1A1F3A),
                        darkBg
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardBg.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Botão voltar
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        darkBg.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = textGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Ícone e título
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = primaryBlue.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "🗂️",
                                        fontSize = 28.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Missões",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textWhite
                                )
                                Text(
                                    text = "Gerencie suas missões autônomas",
                                    fontSize = 13.sp,
                                    color = textGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Botão Nova Missão
                        Button(
                            onClick = onCreateMissionClick,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nova Missão",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

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
                                    color = primaryBlue,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "Carregando missões...",
                                    fontSize = 16.sp,
                                    color = textGray,
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
                                    color = textWhite
                                )
                                Text(
                                    text = "Crie sua primeira missão para começar",
                                    fontSize = 14.sp,
                                    color = textGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onCreateMissionClick,
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryBlue
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
                                    onDeleteClick = {
                                        missionToDelete = mission.id
                                        showDeleteDialog = true
                                    },
                                    primaryBlue = primaryBlue,
                                    darkBlue = darkBlue,
                                    cardBg = cardBg,
                                    textWhite = textWhite,
                                    textGray = textGray,
                                    greenAccent = greenAccent,
                                    redAccent = redAccent,
                                    darkBg = darkBg,
                                    lightBlue = lightBlue
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
                containerColor = cardBg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = redAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Excluir Missão",
                            color = textWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Text(
                        text = "Deseja realmente excluir esta missão? Esta ação não pode ser desfeita.",
                        color = textGray,
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
                            containerColor = redAccent
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
                            contentColor = textGray
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
    onDeleteClick: () -> Unit,
    primaryBlue: Color,
    darkBlue: Color,
    cardBg: Color,
    textWhite: Color,
    textGray: Color,
    greenAccent: Color,
    redAccent: Color,
    darkBg: Color,
    lightBlue: Color
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
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
                        color = primaryBlue.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "#${mission.id}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = lightBlue,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                            color = textWhite
                        )
                    }
                }

                // Botão expandir/recolher
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            darkBg.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir",
                        tint = textGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informações principais (sempre visíveis)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Coordenadas
                InfoChip(
                    icon = "📍",
                    label = "Lat/Long",
                    value = "${String.format("%.4f", mission.latitude)}, ${String.format("%.4f", mission.longitude)}",
                    textGray = textGray,
                    textWhite = textWhite,
                    modifier = Modifier.weight(1f)
                )

                // Waypoints
                InfoChip(
                    icon = "📍",
                    label = "Waypoints",
                    value = "${mission.waypointCount}",
                    textGray = textGray,
                    textWhite = textWhite,
                    accentColor = greenAccent
                )
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
                            textGray = textGray,
                            textWhite = textWhite,
                            primaryBlue = primaryBlue,
                            modifier = Modifier.weight(1f)
                        )

                        SpeedInfoCard(
                            label = "Velocidade Máx",
                            value = "${mission.maxSpeed} m/s",
                            icon = "🚀",
                            textGray = textGray,
                            textWhite = textWhite,
                            primaryBlue = primaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(
                        color = Color(0xFF475569).copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botões de ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botão Visualizar
                        Button(
                            onClick = onViewClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Visualizar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Botão Excluir
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = redAccent
                            ),
                            border = BorderStroke(1.5.dp, redAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Excluir",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
    textGray: Color,
    textWhite: Color,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.6f)
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
                    color = textGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor ?: textWhite,
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
    textGray: Color,
    textWhite: Color,
    primaryBlue: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = primaryBlue.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
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
                    color = textGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textWhite,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

// Preview (exemplo de uso)
@Composable
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