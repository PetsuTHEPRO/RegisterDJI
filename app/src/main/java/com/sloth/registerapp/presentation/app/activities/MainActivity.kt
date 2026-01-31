package com.sloth.registerapp.presentation.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sloth.registerapp.presentation.mission.screens.CellCameraScreen
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
            // Recupera o tema do SettingsScreen (pode ser via ViewModel, DataStore, etc. - aqui exemplo simples)
            var theme by remember { mutableStateOf("Escuro") }
            val isDarkTheme = when (theme) {
                "Claro" -> false
                "Escuro" -> true
                else -> isSystemInDarkTheme()
            }
            com.sloth.registerapp.presentation.app.theme.AppTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val sessionManager = remember { com.sloth.registerapp.core.auth.SessionManager.getInstance(context) }
                val userName by sessionManager.username.collectAsState(initial = "Usuário")
                val userEmail by sessionManager.email.collectAsState(initial = "usuario@labubu.com")

                navController.addOnDestinationChangedListener { _, _, _ ->
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
                            userName = userName ?: "Usuário",
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
                            userName = userName ?: "Usuário",
                            userEmail = userEmail ?: "usuario@labubu.com",
                            onBackClick = { navController.popBackStack() },
                            onLogout = {
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
                            onCellCameraClick = { navController.navigate("cell_camera") },
                            onSurfaceTextureAvailable = { _, _, _ -> /* TODO: bind texture to DJI feed */ },
                            onSurfaceTextureDestroyed = { false },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("cell_camera") {
                        CellCameraScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
