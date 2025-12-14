package com.sloth.registerapp.presentation.screen

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
    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenOnline = Color(0xFF22C55E)
    val redOffline = Color(0xFFEF4444)

    // Estados
    var visible by remember { mutableStateOf(false) }
    val isConnected = droneStatus.contains("Conectado", ignoreCase = true)
    val statusColor = if (isConnected) greenOnline else redOffline

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = primaryBlue.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🚁", fontSize = 24.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Mission Control",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                            Text(
                                text = "Autonomous System",
                                fontSize = 10.sp,
                                color = textGray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = primaryBlue
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = primaryBlue.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardBg.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp)) // Spacer to push content down
            // Status da Conexão
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
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
                            Text(text = "🚁", fontSize = 32.sp)
                            Column {
                                Text(
                                    text = "Status do Drone",
                                    fontSize = 14.sp,
                                    color = textGray,
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

            // Ações Principais
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 100)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ações Principais",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MainActionCard(
                            icon = "📡",
                            title = "Live Feed",
                            subtitle = "Transmissão ao vivo",
                            gradient = Brush.linearGradient(
                                colors = listOf(primaryBlue, darkBlue)
                            ),
                            onClick = onLiveFeedClick,
                            modifier = Modifier.weight(1f),
                            showBadge = isConnected,
                            badgeText = "AO VIVO"
                        )

                        MainActionCard(
                            icon = "🗂️",
                            title = "Missões",
                            subtitle = "Gerenciar missões",
                            gradient = Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                            ),
                            onClick = onMissionsClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MainActionCard(
                            icon = "🕹️",
                            title = "Controle",
                            subtitle = "Controle de missão",
                            gradient = Brush.linearGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            ),
                            onClick = onMissionControlClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bem-vindo
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 200)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Bem-vindo, $userName!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textWhite,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sistema de controle autônomo de drones para missões inteligentes",
                            fontSize = 14.sp,
                            color = textGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Carrossel de Drones Compatíveis
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 300)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Drones Compatíveis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(droneModels) { drone ->
                            DroneModelCard(
                                icon = drone.icon,
                                name = drone.name,
                                description = drone.description
                            )
                        }
                    }
                }
            }

            // Sobre o Projeto
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 400)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Sobre o Projeto",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                        }

                        Text(
                            text = "O Mission Control é um sistema autônomo desenvolvido para planejamento e monitoramento de missões de drones em tempo real. Com detecção inteligente via IA, permite criar rotas, waypoints e acompanhar telemetria detalhada durante o voo.",
                            fontSize = 14.sp,
                            color = textGray,
                            lineHeight = 22.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProjectFeatureChip("🎯 Waypoints")
                            ProjectFeatureChip("📡 Telemetria")
                            ProjectFeatureChip("🤖 IA")
                        }
                    }
                }
            }

            // Patrocinadores
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 500)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Apoio Institucional",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SponsorCard(
                            name = "IFMA",
                            description = "Instituto Federal do Maranhão",
                            icon = "🎓",
                            modifier = Modifier.weight(1f)
                        )

                        SponsorCard(
                            name = "FAPEMA",
                            description = "Fundação de Amparo à Pesquisa",
                            icon = "🔬",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }



            // Rodapé de Contato
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 600))
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
                        color = textWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ContactButton(icon = Icons.Default.Email, label = "E-mail")
                        ContactButton(icon = Icons.Default.Phone, label = "Telefone")
                        ContactButton(icon = Icons.Default.Language, label = "Website")
                    }

                    Divider(color = textGray.copy(alpha = 0.2f))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Mission Control v1.0",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textGray
                        )
                        Text(
                            text = "© 2024 IFMA. Todos os direitos reservados.",
                            fontSize = 10.sp,
                            color = textGray.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainActionCard(
    icon: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                Text(text = icon, fontSize = 40.sp)
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
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
    val cardBg = Color(0xFF0F1729)
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val primaryBlue = Color(0xFF3B82F6)

    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
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
                    color = textGray,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProjectFeatureChip(text: String) {
    val primaryBlue = Color(0xFF3B82F6)

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
    val cardBg = Color(0xFF0F1729)
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val primaryBlue = Color(0xFF3B82F6)

    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
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
                color = textGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactButton(icon: ImageVector, label: String) {
    val primaryBlue = Color(0xFF3B82F6)
    val textWhite = Color(0xFFE2E8F0)

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