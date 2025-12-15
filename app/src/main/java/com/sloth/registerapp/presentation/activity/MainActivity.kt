package com.sloth.registerapp.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.data.datasource.DroneTelemetryManager
import com.sloth.registerapp.data.sdk.DJIConnectionHelper
import com.sloth.registerapp.presentation.screen.DashboardScreen
import com.sloth.registerapp.presentation.screen.DroneControlScreen
import com.sloth.registerapp.presentation.screen.LoginScreen
import com.sloth.registerapp.presentation.screen.MissionCreateScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.presentation.screen.MissionsTableScreen
import com.sloth.registerapp.presentation.screen.RegisterScreen
import com.sloth.registerapp.presentation.screen.SettingsScreen
import com.sloth.registerapp.presentation.screen.WelcomeScreen
import com.sloth.registerapp.presentation.theme.RegisterAppTheme
import com.sloth.registerapp.ui.mission.MissionUiState
import com.sloth.registerapp.ui.mission.MissionViewModel
import dagger.hilt.android.AndroidEntryPoint
import dji.sdk.sdkmanager.DJISDKManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o SDK da DJI imediatamente
        DJIConnectionHelper.registerApp(applicationContext)

        PermissionHelper.initializeLaunchers(this) {}
        PermissionHelper.checkAndRequestAllPermissions(this) {}

        // --- NOVO: Inicia o gerenciador de telemetria ---
        DroneTelemetryManager.init(lifecycleScope)

        setContent {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            RegisterAppTheme {
                val navController = rememberNavController()
                val context = LocalContext.current // Obtém o contexto aqui

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    // Deixar o sistema gerenciar as barras de sistema para todas as telas.
                    // Isso garante que a barra de status e a barra de navegação fiquem visíveis.
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            onLoginClick = { navController.navigate("login") },
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("drone_control") {
                        DroneControlScreen(onMissionsClick = {
                            // TODO: Navigate to missions screen
                        })
                    }
                    composable("dashboard") {
                        val droneStatus by DJIConnectionHelper.connectionStatus.collectAsState()
                        DashboardScreen(
                            droneStatus = droneStatus,
                            onMissionsClick = { navController.navigate("mission") },
                            onMissionControlClick = {
                                val intent = Intent(context, MissionControlActivity::class.java)
                                context.startActivity(intent)
                            },
                            onLiveFeedClick = {
                                val intent = Intent(context, VideoFeedActivity::class.java)
                                context.startActivity(intent)
                            },
                            onConnectDroneClick = {
                                DJISDKManager.getInstance().startConnectionToProduct()
                            },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() },
                            onLogout = {
                                // TODO: Implement actual logout logic
                                navController.navigate("login") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("mission") {
                        val viewModel: MissionViewModel = viewModel()
                        val uiState by viewModel.uiState.collectAsState()

                        LaunchedEffect(Unit) {
                            viewModel.fetchMissions()
                        }

                        val missions: List<com.sloth.registerapp.model.Mission>
                        val isLoading: Boolean

                        when (val state = uiState) {
                            is MissionUiState.Success -> {
                                missions = state.missions
                                isLoading = false
                            }
                            is MissionUiState.Loading -> {
                                missions = emptyList()
                                isLoading = true
                            }
                            is MissionUiState.Error -> {
                                missions = emptyList()
                                isLoading = false
                                // TODO: Show error message
                            }
                            is MissionUiState.Idle -> {
                                missions = emptyList()
                                isLoading = false
                            }
                            is MissionUiState.Unauthorized -> {
                                navController.navigate("login") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                                missions = emptyList()
                                isLoading = false
                            }
                        }

                        MissionsTableScreen(
                            missions = missions,
                            isLoading = isLoading,
                            onBackClick = { navController.popBackStack() },
                            onCreateMissionClick = { navController.navigate("mission-create") },
                            onViewMissionClick = { missionId ->
                                val intent = Intent(context, MissionControlActivity::class.java).apply {
                                    putExtra("MISSION_ID", missionId)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    composable("mission-create") {
                        MissionCreateScreen(onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}