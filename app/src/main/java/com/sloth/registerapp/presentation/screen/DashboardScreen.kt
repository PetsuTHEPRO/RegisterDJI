package com.sloth.registerapp.presentation.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    droneStatus: String = "Desconectado",
    userName: String = "Usuário",
    onLiveFeedClick: () -> Unit = {},
    onMissionsClick: () -> Unit = {},
    onCreateMissionClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onConnectDroneClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val lightBlue = Color(0xFF60A5FA)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)
    val greenOnline = Color(0xFF22C55E)
    val redOffline = Color(0xFFEF4444)

    // Estados de animação
    var visible by remember { mutableStateOf(false) }
    var pulseAnimation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        visible = true
        while (true) {
            pulseAnimation = !pulseAnimation
            delay(2000)
        }
    }

    val isConnected = droneStatus.contains("Conectado", ignoreCase = true)
    val statusColor = if (isConnected) greenOnline else redOffline

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header com informações do usuário
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = primaryBlue.copy(alpha = 0.2f),
                            border = BorderStroke(2.dp, primaryBlue.copy(alpha = 0.5f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Info do usuário
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bem-vindo de volta!",
                                fontSize = 12.sp,
                                color = textGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite
                            )
                        }

                        // Botão de configurações
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    darkBg.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurações",
                                tint = textGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Status da Conexão com Drone
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(400, delayMillis = 100)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🚁",
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Status do Drone",
                                        fontSize = 14.sp,
                                        color = textGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Indicador pulsante
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                        val scale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.3f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "scale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(statusColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = droneStatus,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            // Botão de conexão
                            Button(
                                onClick = onConnectDroneClick,
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isConnected) redOffline else greenOnline
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "Desconectar" else "Conectar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Seção "Ações Principais"
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 200)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ações Principais",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Live Feed e Missões (destaque)
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

                    // Nova Missão (destaque especial)
                    MainActionCard(
                        icon = "➕",
                        title = "Criar Nova Missão",
                        subtitle = "Planejar rota e waypoints",
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                        ),
                        onClick = onCreateMissionClick,
                        modifier = Modifier.fillMaxWidth(),
                        isFullWidth = true
                    )
                }
            }

            // Seção "Gerenciamento"
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 300)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gerenciamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Grid de opções secundárias
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryActionCard(
                            icon = Icons.Default.Analytics,
                            title = "Estatísticas",
                            onClick = onStatisticsClick,
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryActionCard(
                            icon = Icons.Default.Person,
                            title = "Perfil",
                            onClick = onProfileClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Seção "Configurações e Conta"
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(400, delayMillis = 400)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Configurações e Conta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
                    ) {
                        Column {
                            SettingsMenuItem(
                                icon = Icons.Default.Settings,
                                title = "Configurações",
                                subtitle = "Preferências do app",
                                onClick = onSettingsClick
                            )
                            Divider(
                                color = Color(0xFF475569).copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            SettingsMenuItem(
                                icon = Icons.Default.Person,
                                title = "Meu Perfil",
                                subtitle = "Dados pessoais",
                                onClick = onProfileClick
                            )
                            Divider(
                                color = Color(0xFF475569).copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            SettingsMenuItem(
                                icon = Icons.Default.ExitToApp,
                                title = "Sair",
                                subtitle = "Desconectar da conta",
                                onClick = onLogoutClick,
                                isDanger = true
                            )
                        }
                    }
                }
            }

            // Footer
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 500))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mission Control",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )
                    Text(
                        text = "Autonomous System v1.0",
                        fontSize = 10.sp,
                        color = textGray.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Componente de Card Principal de Ação
@Composable
fun MainActionCard(
    icon: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(if (isFullWidth) 100.dp else 140.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Badge "AO VIVO"
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
                verticalArrangement = if (isFullWidth) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                if (isFullWidth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = icon,
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Text(
                        text = icon,
                        fontSize = 40.sp
                    )
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
}

// Componente de Card Secundário
@Composable
fun SecondaryActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF0F1729)
    val primaryBlue = Color(0xFF3B82F6)
    val textWhite = Color(0xFFE2E8F0)

    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryBlue,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textWhite
            )
        }
    }
}

// Componente de Item do Menu de Configurações
@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val redColor = Color(0xFFEF4444)
    val primaryBlue = Color(0xFF3B82F6)

    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isDanger) redColor.copy(alpha = 0.1f) else primaryBlue.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDanger) redColor else primaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDanger) redColor else textWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textGray
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textGray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}