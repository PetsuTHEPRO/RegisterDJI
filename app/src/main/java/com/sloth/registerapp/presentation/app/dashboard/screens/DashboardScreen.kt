package com.sloth.registerapp.presentation.app.dashboard.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.R

private const val DASHBOARD_TAG = "DashboardScreen"
private val NeuralBg = Color(0xFF050E1F)
private val NeuralSurface = Color(0xFF0A1628)
private val NeuralSurfaceAlt = Color(0xFF071120)
private val NeuralBorder = Color(0xFF0D2040)
private val NeuralBorderStrong = Color(0xFF0D2A50)
private val NeuralPrimary = Color(0xFF00C2FF)
private val NeuralSecondary = Color(0xFF0066FF)
private val NeuralMuted = Color(0xFF4A7FA5)
private val NeuralSuccess = Color(0xFF00E5A0)
private val NeuralWarning = Color(0xFFFFB800)
private val NeuralDanger = Color(0xFFFF3B6E)

data class CompatibleDrone(
    val name: String,
    val model: String,
    val status: String,
    val battery: Float,
    val range: String,
    val imageRes: Int,
    val officialUrl: String,
    val accentColor: Color
)

@Composable
fun DashboardScreen(
    droneStatus: String = "Desconectado",
    userName: String = "Usuario",
    isLoggedIn: Boolean = true,
    onShowAllDronesClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRefreshStatusClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val drones = remember { defaultCompatibleDrones() }
    val statusUi = remember(droneStatus) { dashboardStatus(droneStatus) }
    var visible by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "dashboard_pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralBg)
    ) {
        DashboardGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            DashboardTopBar(
                onSettingsClick = onSettingsClick,
                pulseAlpha = pulseAlpha
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(700))
            ) {
                Column {
                    HeroBanner(
                        userName = userName,
                        isLoggedIn = isLoggedIn
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ConnectionStatusCard(
                            statusUi = statusUi,
                            isRefreshing = false,
                            onRefreshStatusClick = onRefreshStatusClick
                        )

                        SectionTitle(
                            title = "Drones Compativeis",
                            icon = Icons.Default.AirplanemodeActive,
                            actionLabel = "Lista completa",
                            onActionClick = onShowAllDronesClick
                        )
                        DroneCarousel(
                            drones = drones,
                            onDroneClick = { drone ->
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(drone.officialUrl))
                                    val activity = intent.resolveActivity(context.packageManager)
                                    requireNotNull(activity)
                                    context.startActivity(intent)
                                }.onFailure { error ->
                                    when (error) {
                                        is ActivityNotFoundException,
                                        is IllegalArgumentException,
                                        is IllegalStateException -> {
                                            Log.w(DASHBOARD_TAG, "Falha ao abrir URL oficial do drone: ${drone.officialUrl}", error)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.dashboard_open_link_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        else -> {
                                            Log.e(DASHBOARD_TAG, "Erro inesperado ao abrir URL oficial do drone: ${drone.officialUrl}", error)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.dashboard_open_link_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )

                        SectionTitle(
                            title = "Apoio Institucional",
                            icon = Icons.Default.Business
                        )
                        InstitutionalSupportGrid()

                        SectionTitle(
                            title = "Sobre o Projeto",
                            icon = Icons.Default.Info,
                            actionLabel = "Clima",
                            onActionClick = onWeatherClick
                        )
                        ProjectDescriptionCard()

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    onSettingsClick: () -> Unit,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeuralPrimary, NeuralSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vantly Neural",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Visao computacional e rotas",
                color = NeuralMuted,
                fontSize = 10.sp
            )
        }

        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = NeuralSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong.copy(alpha = 0.9f))
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuracoes",
                    tint = NeuralPrimary,
                    modifier = Modifier.alpha(0.95f + pulseAlpha * 0.2f)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    userName: String,
    isLoggedIn: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralBg,
                            Color(0xFF0A2050),
                            NeuralBg
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeuralPrimary.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeuralPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SISTEMA ATIVO",
                            color = NeuralPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Bem-vindo de volta,\n${if (isLoggedIn) userName else "Piloto"}.",
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Gerencie rotas e monitore seus drones com inteligencia artificial em tempo real.",
                    color = NeuralMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeuralPrimary,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

private data class DashboardStatusUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private fun dashboardStatus(raw: String): DashboardStatusUi {
    val normalized = raw.ifBlank { "Status indisponivel" }
    return when {
        raw.contains("conect", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Link ativo com o drone",
                icon = Icons.Default.Wifi,
                accent = NeuralPrimary
            )
        raw.contains("pronto", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Aguardando conexao com o drone",
                icon = Icons.Default.Wifi,
                accent = NeuralWarning
            )
        raw.contains("registr", ignoreCase = true) || raw.contains("falha", ignoreCase = true) || raw.contains("erro", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Verifique SDK, permissao ou rede",
                icon = Icons.Default.WifiOff,
                accent = NeuralDanger
            )
        else -> DashboardStatusUi(
            title = normalized,
            subtitle = "Estado atual informado pelo SDK",
            icon = Icons.Default.WifiOff,
            accent = NeuralDanger
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    statusUi: DashboardStatusUi,
    isRefreshing: Boolean,
    onRefreshStatusClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, statusUi.accent.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurface,
                            if (statusUi.accent == NeuralDanger) Color(0xFF1A0820) else Color(0xFF002040)
                        )
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = statusUi.accent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, statusUi.accent.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = statusUi.accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = statusUi.icon,
                            contentDescription = null,
                            tint = statusUi.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusUi.title,
                    color = if (statusUi.accent == NeuralDanger) NeuralDanger else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusUi.subtitle,
                    color = NeuralMuted,
                    fontSize = 11.sp
                )
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = NeuralPrimary.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeuralPrimary.copy(alpha = 0.28f))
            ) {
                IconButton(onClick = onRefreshStatusClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar status",
                        tint = NeuralPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeuralPrimary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeuralPrimary,
                modifier = Modifier.padding(6.dp).size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(NeuralBorderStrong, Color.Transparent)
                    )
                )
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, color = NeuralPrimary)
            }
        }
    }
}

@Composable
private fun DroneCarousel(
    drones: List<CompatibleDrone>,
    onDroneClick: (CompatibleDrone) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        drones.forEachIndexed { index, drone ->
            DroneStatusCard(
                drone = drone,
                index = index,
                onClick = { onDroneClick(drone) }
            )
        }
    }
}

@Composable
internal fun DroneStatusCard(
    drone: CompatibleDrone,
    index: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis = index * 90)) +
            slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(600, delayMillis = index * 90))
    ) {
        Surface(
            modifier = Modifier
                .width(170.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, drone.accentColor.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NeuralSurface,
                                drone.accentColor.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuralSurfaceAlt
                    ) {
                        Image(
                            painter = painterResource(id = drone.imageRes),
                            contentDescription = drone.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColorForDrone(drone.status), CircleShape)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = drone.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = drone.model,
                        color = NeuralMuted,
                        fontSize = 10.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = null,
                            tint = NeuralMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(drone.battery * 100).toInt()}%",
                            color = NeuralMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = drone.range,
                            color = NeuralMuted,
                            fontSize = 10.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = drone.battery,
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = batteryColor(drone.battery),
                        trackColor = NeuralBorder
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColorForDrone(drone.status).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = drone.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColorForDrone(drone.status),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun statusColorForDrone(status: String): Color {
    return when {
        status.contains("Disponivel", ignoreCase = true) -> NeuralSuccess
        status.contains("miss", ignoreCase = true) -> NeuralPrimary
        else -> NeuralWarning
    }
}

private fun batteryColor(value: Float): Color {
    return when {
        value > 0.5f -> NeuralSuccess
        value > 0.25f -> NeuralWarning
        else -> NeuralDanger
    }
}

@Composable
private fun InstitutionalSupportGrid() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportCard(
            modifier = Modifier.weight(1f),
            name = "IFMA",
            subtitle = "Instituto Federal",
            imageRes = R.drawable.ifma_logo
        )
        SupportCard(
            modifier = Modifier.weight(1f),
            name = "FAPEMA",
            subtitle = "Pesquisa e inovacao",
            icon = Icons.Default.Science
        )
    }
}

@Composable
private fun SupportCard(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String,
    icon: ImageVector? = null,
    imageRes: Int? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = NeuralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = NeuralPrimary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        imageRes != null -> Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = name,
                            modifier = Modifier.padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                        icon != null -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeuralPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NeuralMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProjectDescriptionCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurface,
                            NeuralSurfaceAlt
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Vantly Neural e uma plataforma avancada de gerenciamento de drones autonomos com processamento de imagem em tempo real via inteligencia artificial embarcada.",
                color = Color(0xFF8AB4CE),
                fontSize = 13.sp,
                lineHeight = 21.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProjectStat(value = "98.7%", label = "Precisao IA")
                VerticalDivider()
                ProjectStat(value = "< 20ms", label = "Latencia")
                VerticalDivider()
                ProjectStat(value = "50km2", label = "Cobertura")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TechTag("IA Embarcada")
                TechTag("Tempo Real")
                TechTag("5G Ready")
            }
        }
    }
}

@Composable
private fun ProjectStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(
                brush = Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary)),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = label,
            color = NeuralMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(NeuralBorderStrong)
    )
}

@Composable
private fun TechTag(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NeuralPrimary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralPrimary.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = NeuralPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DashboardGridBackground() {
    val transition = rememberInfiniteTransition(label = "dashboard_grid")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "dashboard_grid_offset"
    )
    val glow by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboard_grid_glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        var x = -step + offset
        while (x < size.width + step) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.04f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.035f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
        drawCircle(
            color = NeuralPrimary.copy(alpha = glow),
            radius = size.minDimension * 0.18f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f)
        )
    }
}

fun defaultCompatibleDrones(): List<CompatibleDrone> = listOf(
    CompatibleDrone(
        name = "Falcon X9",
        model = "DJI Mavic 3 Pro",
        status = "Disponivel",
        battery = 0.87f,
        range = "12 km",
        imageRes = R.drawable.ic_drone_takeoff,
        officialUrl = "https://www.dji.com/br/mavic-3-enterprise",
        accentColor = NeuralPrimary
    ),
    CompatibleDrone(
        name = "Aguia Thermal",
        model = "Autel EVO II",
        status = "Em missao",
        battery = 0.43f,
        range = "9 km",
        imageRes = R.drawable.ic_drone_land,
        officialUrl = "https://www.autelrobotics.com/productdetail/45.html",
        accentColor = NeuralSecondary
    ),
    CompatibleDrone(
        name = "Scout V3",
        model = "Parrot ANAFI AI",
        status = "Manutencao",
        battery = 0.12f,
        range = "6 km",
        imageRes = R.drawable.ic_drone_takeoff,
        officialUrl = "https://www.parrot.com/en/drones/anafi-ai",
        accentColor = NeuralSuccess
    )
)
