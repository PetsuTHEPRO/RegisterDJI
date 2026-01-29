package com.sloth.registerapp.presentation.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.mission.screens.MissionCreateScreen
import com.sloth.registerapp.presentation.mission.screens.MissionsTableScreen
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.presentation.app.screens.DashboardScreen
import com.sloth.registerapp.presentation.app.screens.WelcomeScreen
import com.sloth.registerapp.presentation.mission.screens.DroneCameraScreen
import com.sloth.registerapp.presentation.mission.screens.DroneControlScreen
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModelFactory
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListUiState
import com.sloth.registerapp.presentation.auth.screens.LoginScreen
import com.sloth.registerapp.presentation.auth.screens.RegisterScreen
import com.sloth.registerapp.presentation.mission.activities.MissionControlActivity
import com.sloth.registerapp.presentation.settings.screens.SettingsScreen
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

                navController.addOnDestinationChangedListener { _, _, _ ->
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
                        val viewModel: MissionListViewModel = viewModel(factory = MissionListViewModelFactory(application))
                        val uiState by viewModel.uiState.collectAsState()

                        // Buscar missões quando entrar na tela
                        LaunchedEffect(Unit) {
                            viewModel.fetchMissions()
                        }

                        val (missions, isLoading) = remember(uiState) {
                            when (uiState) {
                                is MissionListUiState.Idle -> emptyList<Mission>() to false
                                is MissionListUiState.Loading -> emptyList<Mission>() to true
                                is MissionListUiState.Success -> {
                                    val data = uiState as MissionListUiState.Success
                                    data.missions to false
                                }
                                is MissionListUiState.Error -> emptyList<Mission>() to false
                                is MissionListUiState.Unauthorized -> {
                                    navController.navigate("login") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                    emptyList<Mission>() to false
                                }
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
                        DroneCameraScreen(
                            droneController = remember { com.sloth.registerapp.features.mission.data.drone.manager.DroneControllerManager() },
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
