package com.sloth.registerapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.unit.dp

sealed class BottomItem(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : BottomItem("dashboard", "Início", { Icon(Icons.Filled.Home, contentDescription = null) })
    object Live : BottomItem("camera", "Ao vivo", { Icon(Icons.Filled.PlayCircle, contentDescription = null) })
    object Missions : BottomItem("mission", "Missões", { Icon(Icons.Filled.List, contentDescription = null) })
    object Reports : BottomItem("report", "Relatório", { Icon(Icons.Filled.Assessment, contentDescription = null) })
}

@Composable
fun BottomNavBar(
    navController: NavController,
    onLiveFeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomItem.Home,
        BottomItem.Live,
        BottomItem.Missions,
        BottomItem.Reports
    )

    val colorScheme = MaterialTheme.colorScheme
    val bg = colorScheme.surface
    val stroke = colorScheme.outline.copy(alpha = 0.10f)
    val activeBg = colorScheme.primary.copy(alpha = 0.12f)
    val activeStroke = colorScheme.primary.copy(alpha = 0.25f)
    val activeText = colorScheme.onSurface.copy(alpha = 0.92f)
    val inactiveText = colorScheme.onSurface.copy(alpha = 0.55f)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        color = bg.copy(alpha = 0.82f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val itemBg = if (selected) activeBg else Color.Transparent
                val itemBorder = if (selected) activeStroke else stroke
                val itemColor = if (selected) activeText else inactiveText

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(itemBg),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = itemColor,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, itemBorder),
                        onClick = {
                            if (item.route == BottomItem.Live.route) {
                                onLiveFeedClick()
                            } else if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                CompositionLocalProvider(LocalContentColor provides itemColor) {
                                    item.icon()
                                    Spacer(Modifier.height(6.dp))
                                    Text(item.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
