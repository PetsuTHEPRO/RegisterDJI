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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.mission.screens.MissionCreateScreen
import com.sloth.registerapp.presentation.mission.screens.MissionsTableScreen
import com.sloth.registerapp.presentation.app.screens.DashboardScreen
import com.sloth.registerapp.presentation.app.screens.WelcomeScreen
import com.sloth.registerapp.presentation.mission.screens.DroneControlScreen
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModelFactory
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListUiState
import com.sloth.registerapp.presentation.components.BottomNavBar
import com.sloth.registerapp.presentation.auth.screens.LoginScreen
import com.sloth.registerapp.presentation.auth.screens.RegisterScreen
import com.sloth.registerapp.presentation.mission.activities.MissionControlActivity
import com.sloth.registerapp.presentation.report.screens.ReportDetailScreen
import com.sloth.registerapp.presentation.report.screens.ReportScreen
import com.sloth.registerapp.presentation.settings.screens.SettingsScreen
import kotlinx.coroutines.launch

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
                val tokenRepository = remember { com.sloth.registerapp.core.auth.TokenRepository.getInstance(context) }
                val scope = rememberCoroutineScope()
                val accessToken by tokenRepository.accessToken.collectAsState(initial = null)
                val startDestination = if (accessToken.isNullOrBlank()) "welcome" else "dashboard"
                val userName by sessionManager.username.collectAsState(initial = "Usuário")
                val userEmail by sessionManager.email.collectAsState(initial = "usuario@labubu.com")

                navController.addOnDestinationChangedListener { _, _, _ ->
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }

                NavHost(navController = navController, startDestination = startDestination) {
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
                        ScreenWithBottomBar(navController = navController) {
                            DashboardScreen(
                                droneStatus = droneStatus,
                                userName = userName ?: "Usuário",
                                onMissionsClick = { navController.navigate("mission") },
                                onMissionControlClick = {
                                    val intent = Intent(context, MissionControlActivity::class.java)
                                    context.startActivity(intent)
                                },
                                onLiveFeedClick = {
                                    val intent = Intent(context, DroneCameraActivity::class.java)
                                    context.startActivity(intent)
                                },
                                onRefreshStatusClick = {
                                    DJIConnectionHelper.tryReconnect()
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                    }
                    composable("settings") {
                        SettingsScreen(
                            userName = userName ?: "Usuário",
                            userEmail = userEmail ?: "usuario@labubu.com",
                            onBackClick = { navController.popBackStack() },
                            onLogout = {
                                scope.launch {
                                    tokenRepository.clearTokens()
                                    sessionManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
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

                        ScreenWithBottomBar(navController = navController) {
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
                    }
                    composable("mission-create") {
                        MissionCreateScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("report") {
                        ScreenWithBottomBar(navController = navController) {
                            ReportScreen(
                                onMissionClick = { missionId ->
                                    navController.navigate("report_detail/$missionId")
                                }
                            )
                        }
                    }
                    composable("report_detail/{missionId}") { backStackEntry ->
                        val missionId = backStackEntry.arguments?.getString("missionId") ?: "unknown"
                        ScreenWithBottomBar(navController = navController) {
                            ReportDetailScreen(
                                missionId = missionId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenWithBottomBar(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onLiveFeedClick = {
                    val intent = Intent(context, DroneCameraActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
