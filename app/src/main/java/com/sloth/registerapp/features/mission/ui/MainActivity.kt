package com.sloth.registerapp.features.mission.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.features.mission.data.sdk.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.ui.DashboardScreen
import com.sloth.registerapp.features.mission.ui.DroneControlScreen
import com.sloth.registerapp.features.mission.ui.LoginScreen
import com.sloth.registerapp.features.mission.ui.MissionCreateScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.features.mission.ui.MissionsTableScreen
import com.sloth.registerapp.features.mission.ui.RegisterScreen
import com.sloth.registerapp.features.mission.ui.SettingsScreen
import com.sloth.registerapp.features.mission.ui.WelcomeScreen
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.features.mission.ui.MissionUiState
import com.sloth.registerapp.features.mission.ui.MissionViewModel
import dji.sdk.sdkmanager.DJISDKManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o SDK da DJI imediatamente
        DJIConnectionHelper.registerApp(applicationContext)

        PermissionHelper.initializeLaunchers(this) {}
        PermissionHelper.checkAndRequestAllPermissions(this) {}

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
                                navController.navigate("camera")
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

                        val missions: List<Mission>
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

                    // New: Drone camera feed as a Compose screen
                    composable("camera") {
                        val droneController = remember { com.sloth.registerapp.features.mission.data.drone.DroneControllerManager() }
                        DroneCameraScreen(
                            droneController = droneController,
                            onCellCameraClick = { /* TODO: open phone camera */ },
                            onSurfaceTextureAvailable = { _, _, _ -> /* TODO: bind texture to DJI feed */ },
                            onSurfaceTextureDestroyed = { false },
                            isFeedAvailable = false,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}