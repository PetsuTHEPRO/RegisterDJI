package com.sloth.registerapp.presentation.mission.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.report.data.manager.FlightReportManager
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.presentation.mission.screens.MissionControlScreen
import com.sloth.registerapp.presentation.mission.screens.MissionStatus
import com.sloth.registerapp.presentation.mission.model.Waypoint
import com.sloth.registerapp.presentation.mission.viewmodels.DroneExecutionViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.DroneExecutionViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MissionControlActivity : ComponentActivity() {

    private val viewModel: DroneExecutionViewModel by viewModels { DroneExecutionViewModelFactory() }
    private lateinit var flightReportManager: FlightReportManager
    private lateinit var weatherModule: WeatherModule
    private var weatherLat: Double? = null
    private var weatherLon: Double? = null
    private var reportStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val missionId = intent.getIntExtra("MISSION_ID", -1)
        if (missionId == -1) {
            // Handle error: mission ID not provided
            finish()
            return
        }
        ActiveMissionSessionManager.startMissionSession(missionId.toString())
        flightReportManager = FlightReportManager(this)
        weatherModule = WeatherModule.getInstance(this)

        val missionRepository = MissionRepositoryImpl(
            context = this,
            apiService = RetrofitClient.getInstance(this),
            tokenRepository = TokenRepository.getInstance(this)
        )

        lifecycleScope.launch {
            val result = missionRepository.getMission(missionId)
            result.onSuccess { mission ->
                if (mission != null) {
                    weatherLat = mission.poi_latitude
                    weatherLon = mission.poi_longitude
                    viewModel.loadMission(mission)
                    startMissionReport(missionId, mission)
                } else {
                    // Handle error: mission not found
                    finish()
                }
            }.onFailure {
                // Handle error: failed to fetch mission
                finish()
            }
        }

        setContent {
            RegisterAppTheme {
                val missionState = viewModel.missionState.collectAsStateWithLifecycle().value
                val missionData: ServerMissionDto? = viewModel.mission.collectAsStateWithLifecycle().value
                val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

                when {
                    missionData != null -> {
                        MissionControlScreen(
                            missionName = missionData.name,
                            missionStatus = missionState.toUiStatus(),
                            currentLocation = com.mapbox.geojson.Point.fromLngLat(
                                missionData.poi_longitude,
                                missionData.poi_latitude
                            ),
                            waypoints = missionData.waypoints.mapIndexed { index, waypoint ->
                                Waypoint(
                                    id = index + 1,
                                    latitude = waypoint.latitude,
                                    longitude = waypoint.longitude,
                                    altitude = waypoint.altitude,
                                    speed = missionData.auto_flight_speed
                                )
                            },
                            errorMessage = errorMessage,
                            onBackClick = { finish() },
                            onStartMission = { viewModel.startMission() },
                            onPauseMission = { viewModel.pauseMission() },
                            onResumeMission = { viewModel.resumeMission() },
                            onStopMission = { viewModel.stopMission() },
                            onErrorDismiss = { viewModel.clearError() }
                        )
                    }
                    else -> {
                        // Tela de carregamento
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Carregando missão...")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        finishMissionReport()
        ActiveMissionSessionManager.clearMissionSession()
        super.onDestroy()
    }

    private fun startMissionReport(missionId: Int, mission: ServerMissionDto) {
        if (reportStarted) return
        lifecycleScope.launch {
            val startWeather = weatherSnapshot()
            flightReportManager.startReport(
                missionName = mission.name,
                aircraftName = "Drone",
                extraData = buildMap {
                    put("missionId", missionId.toString())
                    put("weather_provider", startWeather?.snapshot?.providerId ?: "N/A")
                    put("weather_start_temperature_c", (startWeather?.snapshot?.temperatureC ?: 0.0).toString())
                    put("weather_start_wind_ms", (startWeather?.snapshot?.windSpeedMs ?: 0.0).toString())
                    put("weather_start_rain_mm", (startWeather?.snapshot?.rainMm ?: 0.0).toString())
                    put("weather_start_lightning_risk", (startWeather?.snapshot?.lightningRisk ?: 0.0).toString())
                    put("weather_start_safety_level", startWeather?.decision?.level?.name ?: "SAFE")
                    put("weather_summary_message", startWeather?.decision?.shortMessage ?: "Clima não disponível")
                }
            )
            reportStarted = true
        }
    }

    private fun finishMissionReport() {
        if (!reportStarted) return
        runBlocking {
            val endWeather = weatherSnapshot()
            if (endWeather != null) {
                flightReportManager.updateExtraData(
                    mapOf(
                        "weather_end_temperature_c" to (endWeather.snapshot.temperatureC ?: 0.0).toString(),
                        "weather_end_wind_ms" to (endWeather.snapshot.windSpeedMs ?: 0.0).toString(),
                        "weather_end_rain_mm" to (endWeather.snapshot.rainMm ?: 0.0).toString(),
                        "weather_end_lightning_risk" to (endWeather.snapshot.lightningRisk ?: 0.0).toString(),
                        "weather_end_safety_level" to endWeather.decision.level.name
                    )
                )
            }
            flightReportManager.finishReport()
        }
    }

    private suspend fun weatherSnapshot() = withTimeoutOrNull(1_500L) {
        val lat = weatherLat ?: return@withTimeoutOrNull null
        val lon = weatherLon ?: return@withTimeoutOrNull null
        weatherModule.getWeatherSummaryUseCase(lat, lon, ttlMs = 60_000L)
    }
}

fun MissionState.toUiStatus(): MissionStatus {
    return when (this) {
        MissionState.IDLE -> MissionStatus.IDLE

        // Agrupa todos os estados de carregamento/processamento
        MissionState.PREPARING,
        MissionState.DOWNLOADING,
        MissionState.UPLOADING ->
            MissionStatus.LOADING

        // Agrupa estados "prontos"
        MissionState.DOWNLOAD_FINISHED,
        MissionState.READY_TO_EXECUTE ->
            MissionStatus.READY

        MissionState.EXECUTING -> MissionStatus.RUNNING
        MissionState.EXECUTION_PAUSED -> MissionStatus.PAUSED
        MissionState.EXECUTION_STOPPED -> MissionStatus.STOPPED
        MissionState.FINISHED -> MissionStatus.COMPLETED
        MissionState.ERROR -> MissionStatus.ERROR
    }
}
