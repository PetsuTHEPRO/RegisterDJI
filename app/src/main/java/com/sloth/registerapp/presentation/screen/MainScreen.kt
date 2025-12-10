package com.sloth.registerapp.presentation.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sloth.registerapp.ui.mission.MissionViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val missionViewModel: MissionViewModel = viewModel()

    NavHost(navController = navController, startDestination = "drone_control") {
        composable("drone_control") {
            DroneControlScreen(onMissionsClick = {
                navController.navigate("missions")
            })
        }
        composable("missions") {
            MissionScreen(
                viewModel = missionViewModel,
                onMissionClick = { missionId ->
                    // TODO: Implement mission loading
                    println("Selected mission: $missionId")
                    navController.popBackStack()
                }
            )
        }
    }
}
