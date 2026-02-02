package com.sloth.registerapp.presentation.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.registerapp.presentation.app.theme.AppTheme
import kotlinx.coroutines.delay

data class DroneModel(
    val name: String,
    val description: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    droneStatus: String = "Desconectado",
    userName: String = "Usuário",
    onLiveFeedClick: () -> Unit = {},
    onMissionsClick: () -> Unit = {},
    onMissionControlClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onConnectDroneClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    // Estados
    var visible by remember { mutableStateOf(false) }
    // Sistema de Estados do Drone (3 cores dinâmicas)
    val statusColor = when {
        droneStatus.contains("Pronto", ignoreCase = true) -> colorScheme.primary
        droneStatus.contains("Conectado", ignoreCase = true) -> colorScheme.secondaryContainer
        droneStatus.contains("Falha", ignoreCase = true) -> colorScheme.error
        else -> colorScheme.error
    }
    // Modelos de drones
    val droneModels = listOf(
        DroneModel("DJI Phantom 4", "Drone profissional para mapeamento", "🚁"),
        DroneModel("DJI Mavic Pro", "Compacto e portátil", "🛸"),
        DroneModel("DJI Inspire 2", "Alta performance cinematográfica", "✈️"),
        DroneModel("Parrot Anafi", "Leve e dobrável", "🚁"),
        DroneModel("Autel EVO II", "8K de resolução", "🛩️")
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = "Drone",
                            modifier = Modifier.size(28.dp),
                            tint = colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Vantly Neural",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Sistema de Drones",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.primaryContainer
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(64.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bem-vindo - PRIMEIRO
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 100)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Bem-vindo",
                            modifier = Modifier.size(32.dp),
                            tint = colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bem-vindo, $userName!",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Sistema autônomo de drones",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Status da Conexão - SEGUNDO
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 200)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = "Drone Status",
                                modifier = Modifier.size(32.dp),
                                tint = statusColor
                            )
                            Column {
                                Text(
                                    text = "Status do Drone",
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Text(
                                        text = droneStatus,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sobre o Projeto - TERCEIRO (REMOVIDO DAQUI)

            // 5. Carrossel de Drones - QUINTO
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 500)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Drones Compatíveis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(droneModels) { drone ->
                            CompactDroneCard(
                                icon = drone.icon,
                                name = drone.name
                            )
                        }
                    }
                }
            }

            // 6. Patrocinadores - SEXTO
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 600)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apoio Institucional",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactSponsorCard(
                            name = "IFMA",
                            icon = "🎓",
                            modifier = Modifier.weight(1f)
                        )

                        CompactSponsorCard(
                            name = "FAPEMA",
                            icon = "🔬",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6B. Sobre o Projeto - DEPOIS DOS DRONES (MOVIDO)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 650)
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Sobre o Projeto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Sistema autônomo para planejamento e monitoramento de missões de drones em tempo real com IA.",
                            fontSize = 13.sp,
                            color = colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // CÓDIGO ANTIGO - NÃO RENDERIZADO (mantido como referência)
            AnimatedVisibility(
                visible = false,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 700))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Entre em Contato",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ContactButton(icon = Icons.Default.Email, label = "E-mail")
                        ContactButton(icon = Icons.Default.Phone, label = "Telefone")
                        ContactButton(icon = Icons.Default.Language, label = "Website")
                    }

                    Divider(color = colorScheme.onSurface.copy(alpha = 0.2f))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Vantly Neural v1.0",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "© 2024 IFMA. Todos os direitos reservados.",
                            fontSize = 10.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun MainActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(120.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            if (showBadge) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colorScheme.onPrimary, CircleShape)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(40.dp),
                    tint = colorScheme.onPrimary
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun DroneModelCard(
    icon: String,
    name: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surface
    val textWhite = colorScheme.onSurface

    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = icon, fontSize = 36.sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProjectFeatureChip(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryBlue = colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = primaryBlue.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryBlue,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SponsorCard(
    name: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surface
    val textWhite = colorScheme.onSurface

    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactButton(icon: ImageVector, label: String) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryBlue = colorScheme.primary
    val textWhite = colorScheme.onBackground

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = { },
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = primaryBlue.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = primaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = textWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CompactDroneCard(
    icon: String,
    name: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surface
    val textWhite = colorScheme.onSurface

    Surface(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name.split(" ").first(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactSponsorCard(
    name: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surface
    val textWhite = colorScheme.onSurface

    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite
            )
        }
    }
}

@Preview(name = "Dashboard - Light", showBackground = true)
@Composable
fun DashboardScreenPreviewLight() {
    AppTheme(darkTheme = false) {
        DashboardScreen(
            droneStatus = "Conectado a: DJI Mavic",
            userName = "Yuri"
        )
    }
}

@Preview(name = "Dashboard - Dark", showBackground = true)
@Composable
fun DashboardScreenPreviewDark() {
    AppTheme(darkTheme = true) {
        DashboardScreen(
            droneStatus = "Conectado a: DJI Mavic",
            userName = "Yuri"
        )
    }
}
