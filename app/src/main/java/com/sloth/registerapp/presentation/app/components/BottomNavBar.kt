package com.sloth.registerapp.presentation.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sloth.registerapp.R
import com.sloth.registerapp.presentation.app.theme.VantlyMotion

sealed class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val isLiveFeedAction: Boolean = false
) {
    object Home : BottomItem("dashboard", R.string.bottom_nav_home, Icons.Outlined.Home)
    object Missions : BottomItem("mission", R.string.bottom_nav_missions, Icons.AutoMirrored.Outlined.List)
    object Camera : BottomItem("camera", R.string.bottom_nav_camera, Icons.Outlined.Videocam, isLiveFeedAction = true)
    object AiCenter : BottomItem("ai_center", R.string.bottom_nav_ai, Icons.Outlined.Memory)
    object Settings : BottomItem("settings", R.string.bottom_nav_settings, Icons.Outlined.Settings)
}

/**
 * Barra de navegação inferior compacta no estilo DJI:
 * - 5 destinos principais: Início, Missões, Câmera (centro), IA, Ajustes
 * - Fundo preto/cinza (#141414) com divisor sutil no topo (#2A2A2A)
 * - Ícones monocromáticos lineares (branco ativo, cinza #A3A3A3 inativo)
 * - Câmera ao vivo abre a DroneCameraActivity em tela cheia horizontal
 * - Altura enxuta respeitando insets do sistema (gestures bar)
 */
@Composable
fun BottomNavBar(
    navController: NavController,
    onLiveFeedClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomItem.Home,
        BottomItem.Missions,
        BottomItem.Camera,
        BottomItem.AiCenter,
        BottomItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B111A))
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1F2937)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = !item.isLiveFeedAction && currentRoute == item.route
                val label = stringResource(item.labelRes)

                val itemColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                    animationSpec = tween(VantlyMotion.Quick, easing = VantlyMotion.Standard),
                    label = "nav_item_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (item.isLiveFeedAction) {
                                onLiveFeedClick()
                            } else if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(Modifier.align(Alignment.TopCenter).size(width = 32.dp, height = 3.dp)
                            .background(Color(0xFF3B82F6), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)))
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = itemColor,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
