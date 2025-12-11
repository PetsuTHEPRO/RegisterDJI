package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
import com.sloth.registerapp.presentation.screen.MissionsTableScreen
import com.sloth.registerapp.presentation.screen.RegisterScreen
import com.sloth.registerapp.presentation.screen.WelcomeScreen
import com.sloth.registerapp.presentation.theme.RegisterAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PermissionHelper.initializeLaunchers(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }
        PermissionHelper.checkAndRequestAllPermissions(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }

        // --- NOVO: Inicia o gerenciador de telemetria ---
        DroneTelemetryManager.init(lifecycleScope)

        setContent {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            RegisterAppTheme {
                val navController = rememberNavController()

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.route in listOf("welcome", "login", "register")) {
                        // Mostra as barras do sistema
                        WindowCompat.setDecorFitsSystemWindows(window, true)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    } else {
                        // Esconde as barras do sistema (modo edge-to-edge)
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
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
                        DashboardScreen(
                            onMissionsClick = { navController.navigate("mission") },
                            onCreateMissionClick = { navController.navigate("mission-create") }
                        )
                    }
                    composable("mission") {
                        MissionsTableScreen(
                            onBackClick = { navController.popBackStack() },
                            onCreateMissionClick = { navController.navigate("mission-create") }
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
