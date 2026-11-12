package com.sloth.registerapp.presentation.video.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import android.widget.Toast
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Canvas
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.activity.compose.BackHandler
import android.app.Application
import android.content.Intent
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mldetection.data.remote.dto.ModelCatalogEntryDto
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModelFactory
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListUiState
import com.sloth.registerapp.presentation.mission.activities.MissionControlActivity
import com.sloth.registerapp.presentation.app.main.activities.MainActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.mission.ActiveMissionSession
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.mission.ActiveMissionStatus
import com.sloth.registerapp.core.network.ConnectivityMonitor
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.core.settings.MediaStorageSettingsRepository
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.core.utils.MeasurementConverter
import com.sloth.registerapp.features.mldetection.domain.model.DetectionResult
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.presentation.MLDetectionViewModel
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.features.streaming.data.DjiRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.report.data.manager.MissionMediaManager
import com.sloth.registerapp.features.report.domain.model.MissionMediaSource
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSummary
import com.sloth.registerapp.presentation.video.components.VideoFeedView
import com.sloth.registerapp.presentation.mission.components.MapboxMiniMapView
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.sentry.SentryLevel
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private sealed class ToolsMenuScreen {
    object Home : ToolsMenuScreen()
    object AI : ToolsMenuScreen()
    object Missions : ToolsMenuScreen()
    data class MissionDetails(val mission: Mission) : ToolsMenuScreen()
    object Capture : ToolsMenuScreen()
    object Streaming : ToolsMenuScreen()
    object PhoneCamera : ToolsMenuScreen()
    object Settings : ToolsMenuScreen()
}

private enum class ToolsMissionFilter(val label: String) {
    ALL("Todas"),
    PLANNED("Planejadas"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluídas")
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", m, s)
}

private data class DetectionModelOption(
    val id: String,
    val name: String,
    val summary: String,
    val classes: String,
    val installed: Boolean
)

private fun buildDetectionModelOptions(
    installedModels: List<MLModel>
): List<DetectionModelOption> {
    return installedModels.map { model ->
        DetectionModelOption(
            id = model.id,
            name = model.name,
            summary = if (model.source.name == "ASSET") {
                "Modelo embarcado no app e pronto para uso offline."
            } else {
                "Modelo local instalado dinamicamente no dispositivo."
            },
            classes = model.classes.joinToString().ifBlank { "Objeto" },
            installed = true
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DroneCameraScreen(
    droneController: DroneCommandManager,
    onCellCameraClick: () -> Unit,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureDestroyed: () -> Boolean,
    onBackClick: () -> Unit = {}
) {
    val tag = "DroneCameraScreen"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraResumed by remember { mutableStateOf(true) }
    var positionClock by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var lastVideoFrameAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); positionClock = SystemClock.elapsedRealtime() }
    }

    val colorScheme = MaterialTheme.colorScheme
    val missionManager = remember { DroneMissionManager(DJIConnectionHelper) }
    val connectivityMonitor = remember { ConnectivityMonitor.getInstance(context) }

    // Estados
    var visible by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isToolsDrawerOpen by rememberSaveable { mutableStateOf(false) }
    var toolsInitialCategory by remember { mutableStateOf<ToolsMenuScreen>(ToolsMenuScreen.Home) }
    var isMapExpanded by rememberSaveable { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showMissionOverlay by rememberSaveable { mutableStateOf(true) }
    var detectionEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedDetectionModelId by rememberSaveable {
        mutableStateOf(ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID)
    }
    var previewCaptureError by remember { mutableStateOf<String?>(null) }

    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val product by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val isFeedAvailable = product?.isConnected == true && (product as? dji.sdk.products.Aircraft)?.camera != null
    val isVideoActive = isFeedAvailable && cameraResumed && lastVideoFrameAt > 0L &&
        positionClock - lastVideoFrameAt <= 3000L
    val mlDetectionViewModel: MLDetectionViewModel = viewModel()
    val missionListViewModel: MissionListViewModel = viewModel(
        factory = MissionListViewModelFactory(context.applicationContext as Application)
    )
    val missionListUiState by missionListViewModel.uiState.collectAsStateWithLifecycle()
    val loadedMission = (missionListUiState as? MissionListUiState.Success)?.missions
        ?.firstOrNull { it.id.toString() == ActiveMissionSessionManager.activeMissionId.value }
    val missionRoute = remember(loadedMission?.previewPoints) {
        loadedMission?.previewPoints.orEmpty().filter {
            isValidCoordinate(it.latitude, it.longitude)
        }.map { Point.fromLngLat(it.longitude, it.latitude) }
    }
    val remoteCatalog by mlDetectionViewModel.remoteCatalog.collectAsStateWithLifecycle()
    val remoteCatalogLoading by mlDetectionViewModel.remoteCatalogLoading.collectAsStateWithLifecycle()

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val mediaStorageRepo = remember { MediaStorageSettingsRepository.getInstance(context) }
    val measurementRepo = remember { MeasurementSettingsRepository.getInstance(context) }
    val objectDetectionRepo = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val mediaManager = remember { MissionMediaManager.getInstance(context) }
    val weatherModule = remember { WeatherModule.getInstance(context) }
    val activeMissionId by ActiveMissionSessionManager.activeMissionId.collectAsStateWithLifecycle()
    val activeMissionSession by ActiveMissionSessionManager.session.collectAsStateWithLifecycle()
    val hasValidatedInternet by connectivityMonitor.isConnected.collectAsStateWithLifecycle()
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val mediaStorageTarget by mediaStorageRepo.mediaStorageTarget.collectAsStateWithLifecycle(
        initialValue = MediaStorageSettingsRepository.TARGET_PHONE
    )
    val measurementSystem by measurementRepo.measurementSystem.collectAsStateWithLifecycle(
        initialValue = MeasurementSettingsRepository.SYSTEM_METRIC
    )
    val persistedDetectionEnabled by objectDetectionRepo.detectionEnabled.collectAsStateWithLifecycle(initialValue = false)
    val persistedDetectionModelId by objectDetectionRepo.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )
    val availableModels by mlDetectionViewModel.availableModels.collectAsStateWithLifecycle()
    val activeDetectionModelId by mlDetectionViewModel.activeModelId.collectAsStateWithLifecycle()
    val detectionBoxes by mlDetectionViewModel.detectionBoxes.collectAsStateWithLifecycle()
    val lastInferenceMs by mlDetectionViewModel.lastInferenceMs.collectAsStateWithLifecycle()
    val lastDetectionError by mlDetectionViewModel.lastError.collectAsStateWithLifecycle()
    val downloadProgress by mlDetectionViewModel.downloadProgress.collectAsStateWithLifecycle()
    val rtmpStreamer = remember { DjiRtmpStreamer(rtmpUrl) }
    val streamState by rtmpStreamer.state.collectAsStateWithLifecycle()
    val previewEnabled = streamState !is StreamState.Streaming && streamState !is StreamState.Connecting

    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val operatorLocation = locationUiState.location
    val weatherQueryLocation = remember(telemetry.latitude, telemetry.longitude, operatorLocation) {
        resolveWeatherQueryLocation(
            telemetry = telemetry,
            operatorLocation = operatorLocation
        )
    }
    var weatherSummary by remember { mutableStateOf<WeatherSummary?>(null) }
    var lastWeatherRequestMs by remember { mutableLongStateOf(0L) }
    var lastPreviewCaptureBreadcrumbAt by remember { mutableLongStateOf(0L) }
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val codecManager = remember { mutableStateOf<DJICodecManager?>(null) }
    val surfaceRef = remember { mutableStateOf<SurfaceTexture?>(null) }
    val surfaceSize = remember { mutableStateOf(Pair(0, 0)) }
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }
    val detectionModelOptions = remember(availableModels) {
        buildDetectionModelOptions(installedModels = availableModels)
    }
    val videoDataListener = remember {
        VideoFeeder.VideoDataListener { videoBuffer, size ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastVideoFrameAt >= 1000L) lastVideoFrameAt = now
            codecManager.value?.sendDataToDecoder(videoBuffer, size)
        }
    }

    fun attachPreview() {
        val surface = surfaceRef.value ?: return
        val (width, height) = surfaceSize.value
        if (width <= 0 || height <= 0) return
        if (codecManager.value != null || !cameraResumed || !previewEnabled) return
        codecManager.value = DJICodecManager(context, surface, width, height)
        VideoFeeder.getInstance().primaryVideoFeed?.addVideoDataListener(videoDataListener)
        Log.d(tag, "Preview attached")
    }

    fun detachPreview() {
        VideoFeeder.getInstance().primaryVideoFeed?.removeVideoDataListener(videoDataListener)
        codecManager.value?.cleanSurface()
        codecManager.value = null
        Log.d(tag, "Preview detached")
    }

    // Oculta barras do sistema (status/navigation) durante o video feed
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val view = activity?.window?.decorView
        if (window != null && view != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null && view != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        AppObservability.screenOpened("DroneCameraScreen")
        visible = true
        missionListViewModel.fetchMissions()
        if (!locationPermissions.allPermissionsGranted) {
            AppObservability.breadcrumb(
                category = "permissions",
                message = "Drone camera requested location permissions",
                data = mapOf("screen" to "DroneCameraScreen")
            )
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isActive) {
                delay(1000)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    LaunchedEffect(persistedDetectionEnabled, persistedDetectionModelId) {
        detectionEnabled = persistedDetectionEnabled
        selectedDetectionModelId = persistedDetectionModelId
        mlDetectionViewModel.selectModel(persistedDetectionModelId)
    }

    LaunchedEffect(activeDetectionModelId, availableModels) {
        val resolvedModelId = activeDetectionModelId ?: return@LaunchedEffect
        val selectedExists = availableModels.any { it.id == selectedDetectionModelId }
        if (selectedDetectionModelId != resolvedModelId && !selectedExists) {
            selectedDetectionModelId = resolvedModelId
            objectDetectionRepo.setActiveModelId(resolvedModelId)
        }
    }

    LaunchedEffect(rtmpUrl) {
        rtmpStreamer.updateUrl(rtmpUrl)
        if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
            AppObservability.breadcrumb(
                category = "streaming",
                message = "DJI RTMP URL changed while streaming; restarting stream"
            )
            rtmpStreamer.stop()
            rtmpStreamer.start()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> cameraResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    cameraResumed = false
                    detachPreview()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        cameraResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); detachPreview() }
    }

    LaunchedEffect(previewEnabled, cameraResumed, isFeedAvailable) {
        if (previewEnabled && cameraResumed && isFeedAvailable) {
            attachPreview()
        } else {
            detachPreview()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
    }

    LaunchedEffect(activeMissionSession.hasMission) {
        if (activeMissionSession.hasMission) {
            showMissionOverlay = true
        }
    }

    LaunchedEffect(weatherQueryLocation, hasValidatedInternet) {
        val queryLocation = weatherQueryLocation ?: return@LaunchedEffect
        if (!hasValidatedInternet) {
            Log.d(tag, "Internet validada indisponível; ignorando atualização de clima")
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        if (now - lastWeatherRequestMs < WEATHER_MIN_REFRESH_MS) {
            return@LaunchedEffect
        }

        weatherSummary = runCatching {
            weatherModule.getWeatherSummaryUseCase(
                lat = queryLocation.latitude,
                lon = queryLocation.longitude,
                ttlMs = 60_000L
            )
        }.onFailure { error ->
            AppObservability.captureException(
                throwable = error,
                area = "weather",
                operation = "load_weather_summary",
                level = SentryLevel.WARNING,
                extras = mapOf("source" to if (operatorLocation != null) "operator_or_telemetry" else "telemetry")
            )
        }.getOrNull()
        lastWeatherRequestMs = now
    }

    LaunchedEffect(
        detectionEnabled,
        selectedDetectionModelId,
        activeDetectionModelId,
        previewEnabled,
        isFeedAvailable,
        cameraResumed,
        isVideoActive
    ) {
        val canDetect = detectionEnabled &&
            previewEnabled &&
            isVideoActive &&
            selectedDetectionModelId == activeDetectionModelId &&
            availableModels.any { it.id == selectedDetectionModelId }

        if (!canDetect) {
            mlDetectionViewModel.clearDetections()
            if (!detectionEnabled) {
                previewCaptureError = null
            } else {
                AppObservability.breadcrumb(
                    category = "ml",
                    message = "Drone camera detection waiting for prerequisites",
                    data = mapOf(
                        "preview_enabled" to previewEnabled,
                        "feed_available" to isFeedAvailable,
                        "selected_model_id" to selectedDetectionModelId,
                        "active_model_id" to activeDetectionModelId
                    )
                )
            }
            return@LaunchedEffect
        }

        AppObservability.breadcrumb(
            category = "ml",
            message = "Drone camera detection started",
            data = mapOf("model_id" to selectedDetectionModelId)
        )

        while (isActive) {
            val textureView = textureViewRef.value
            val frameBitmap = withContext(Dispatchers.Main) {
                textureView?.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
            }

            if (frameBitmap == null) {
                previewCaptureError = "Preview indisponivel para captura"
                val now = System.currentTimeMillis()
                if (now - lastPreviewCaptureBreadcrumbAt > 30_000L) {
                    lastPreviewCaptureBreadcrumbAt = now
                    AppObservability.breadcrumb(
                        category = "camera",
                        message = "Drone preview frame unavailable",
                        level = SentryLevel.WARNING
                    )
                }
                delay(400)
                continue
            }

            previewCaptureError = null
            mlDetectionViewModel.processFrame(frameBitmap)
            delay(350)
        }
    }

    val onTakePhoto: () -> Unit = {
        if (isFeedAvailable) droneController.takePhoto { success, message ->
            val text = if (success) "Foto capturada" else "Erro ao fotografar: ${message ?: "desconhecido"}"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            if (success && !activeMissionId.isNullOrBlank()) {
                scope.launch {
                    val now = System.currentTimeMillis()
                    if (mediaStorageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) {
                        mediaManager.registerPhotoCapture(
                            missionId = activeMissionId!!,
                            dronePath = "drone://photo/$now.jpg",
                            source = MissionMediaSource.DRONE_SD
                        )
                    } else {
                        mediaManager.registerPhotoCapture(
                            missionId = activeMissionId!!,
                            localPath = "content://vantly/local/photo/$now.jpg",
                            source = MissionMediaSource.PHONE_LOCAL
                        )
                    }
                }
            }
        }
    }

    val onToggleRecord: () -> Unit = {
        if (isFeedAvailable && isRecording) {
            droneController.stopRecording { success, message ->
                if (success) {
                    isRecording = false
                }
                val text = if (success) "Gravação parada" else "Erro ao parar gravação: ${message ?: "desconhecido"}"
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                if (success && !activeMissionId.isNullOrBlank()) {
                    scope.launch {
                        val now = System.currentTimeMillis()
                        if (mediaStorageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) {
                            mediaManager.registerVideoCapture(
                                missionId = activeMissionId!!,
                                dronePath = "drone://video/$now.mp4",
                                source = MissionMediaSource.DRONE_SD
                            )
                        } else {
                            mediaManager.registerVideoCapture(
                                missionId = activeMissionId!!,
                                localPath = "content://vantly/local/video/$now.mp4",
                                source = MissionMediaSource.PHONE_LOCAL
                            )
                        }
                    }
                }
            }
        } else if (isFeedAvailable) {
            droneController.startRecording { success, message ->
                if (success) {
                    isRecording = true
                }
                val text = if (success) "Gravação iniciada" else "Erro ao iniciar gravação: ${message ?: "desconhecido"}"
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onStreamToggle: () -> Unit = {
        if (!isFeedAvailable) {
            Toast.makeText(context, "Conecte o drone para transmitir", Toast.LENGTH_SHORT).show()
        } else {
            if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
                rtmpStreamer.stop()
            } else {
                rtmpStreamer.start()
            }
        }
    }

    val mapWidth by animateDpAsState(
        targetValue = if (isMapExpanded) 260.dp else 140.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "mapWidth"
    )
    val mapHeight by animateDpAsState(
        targetValue = if (isMapExpanded) 180.dp else 98.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "mapHeight"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.DashboardBg)
    ) {
        val compactLandscape = maxWidth < 740.dp
        VideoFeedView(
            modifier = Modifier.fillMaxSize(),
            onTextureViewReady = { textureViewRef.value = it },
            onSurfaceTextureAvailable = { surface, width, height ->
                Log.d(tag, "Surface available: ${width}x${height}")
                surfaceRef.value = surface
                surfaceSize.value = Pair(width, height)
                if (previewEnabled) {
                    attachPreview()
                }
                onSurfaceTextureAvailable(surface, width, height)
            },
            onSurfaceTextureDestroyed = {
                Log.d(tag, "Surface destroyed")
                detachPreview()
                surfaceRef.value = null
                surfaceSize.value = Pair(0, 0)
                onSurfaceTextureDestroyed()
            }
        )

        if (detectionEnabled && isVideoActive && detectionBoxes.isNotEmpty()) {
            PersonDetectionOverlay(
                detections = detectionBoxes,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isVideoActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(
                        "Vídeo Feed Indisponível",
                        color = colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isFeedAvailable) "Aguardando vídeo da aeronave" else "Conecte-se ao drone para visualizar",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Gradientes de leitura da HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                    )
                )
        )

        // Header fino estilo DJI
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -it }) {
            CompactHeader(
                droneState = droneState,
                batteryLevel = telemetry.batteryLevel,
                missionName = activeMissionSession.missionName,
                weatherSummary = weatherSummary,
                measurementSystem = measurementSystem,
                selectedModelName = availableModels.firstOrNull { it.id == selectedDetectionModelId }?.name,
                onBackClick = onBackClick,
                onToggleControls = {
                    toolsInitialCategory = ToolsMenuScreen.Home
                    isToolsDrawerOpen = true
                },
                isFeedAvailable = isFeedAvailable
            )
        }

        Column(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(onClick = onTakePhoto, enabled = isFeedAvailable,
                shape = CircleShape, color = NeuralColors.DashboardCard.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, "Capturar fotografia", modifier = Modifier.size(19.dp),
                        tint = if (isFeedAvailable) Color.White else NeuralColors.DashboardTextSecondary)
                }
            }
            Surface(onClick = onToggleRecord, enabled = isFeedAvailable,
                shape = CircleShape, color = NeuralColors.DashboardCard.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(15.dp).background(
                        if (isRecording) NeuralColors.Danger else if (isFeedAvailable) Color(0xFFFF344A)
                        else NeuralColors.DashboardTextSecondary,
                        if (isRecording) RoundedCornerShape(3.dp) else CircleShape))
                }
            }
        }

        // 1. Mini mapa persistente no canto inferior esquerdo com transição animada entre compacto e expandido
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 14.dp, bottom = 14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(width = mapWidth.coerceAtMost(if (compactLandscape) 110.dp else 260.dp),
                        height = mapHeight.coerceAtMost(if (compactLandscape) 78.dp else 180.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                color = Color.Black.copy(alpha = 0.60f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val dronePoint = if (isFeedAvailable &&
                        telemetry.positionUpdatedAtElapsedMs > 0L &&
                        positionClock - telemetry.positionUpdatedAtElapsedMs <= 5000L &&
                        isValidCoordinate(telemetry.latitude, telemetry.longitude)) {
                        Point.fromLngLat(telemetry.longitude, telemetry.latitude)
                    } else null

                    MapboxMiniMapView(
                        modifier = Modifier.fillMaxSize(),
                        operatorLocation = operatorLocation,
                        droneLocation = dronePoint,
                        routePoints = missionRoute,
                        droneHeadingDegrees = if (dronePoint != null) telemetry.headingDegrees else null,
                        frameRevision = isMapExpanded,
                        operatorAccuracyMeters = locationUiState.accuracyMeters,
                        operatorIsStationary = locationUiState.isStationary,
                        styleUri = Style.DARK
                    )

                    if (dronePoint == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = if (missionRoute.isNotEmpty()) "Aeronave sem posição" else "Aguardando GPS",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Botão para alternar tamanho compacto / expandido do mapa
                    IconButton(
                        onClick = { isMapExpanded = !isMapExpanded },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMapExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isMapExpanded) "Minimizar mapa" else "Expandir mapa",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. HUD de telemetria linear estilo DJI (posicionado no centro inferior)
        val isDroneConnected = isFeedAvailable || (droneState != DroneState.DISCONNECTED && product != null)
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            TelemetryHudStrip(
                telemetry = telemetry,
                measurementSystem = measurementSystem,
                isConnected = isDroneConnected,
                compact = compactLandscape
            )
        }

        // 3. Controles minimalistas de missão (Play, Pause, Stop) no canto inferior direito
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut() + slideOutHorizontally { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp)
        ) {
            MinimalistMissionControls(
                hasMission = activeMissionSession.hasMission,
                missionStatus = activeMissionSession.status,
                isDroneConnected = isDroneConnected,
                onPlayClick = {
                    scope.launch {
                        val action = if (activeMissionSession.status == ActiveMissionStatus.PAUSED) {
                            runCatching { missionManager.resumeMission() }
                        } else {
                            runCatching { missionManager.startMission() }
                        }
                        action.onSuccess {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.EXECUTING)
                            val msg = if (activeMissionSession.status == ActiveMissionStatus.PAUSED) "Missão retomada" else "Missão iniciada"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                            Toast.makeText(
                                context,
                                "Erro na missão: ${it.message ?: "desconhecido"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onPauseClick = {
                    scope.launch {
                        runCatching { missionManager.pauseMission() }
                            .onSuccess {
                                ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.PAUSED)
                                Toast.makeText(context, "Missão pausada", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure {
                                ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                                Toast.makeText(
                                    context,
                                    "Erro ao pausar missão: ${it.message ?: "desconhecido"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                },
                onStopClick = {
                    scope.launch {
                        runCatching { missionManager.stopMission() }
                            .onSuccess {
                                ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.STOPPED)
                                Toast.makeText(context, "Missão parada", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure {
                                ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                                Toast.makeText(
                                    context,
                                    "Erro ao parar missão: ${it.message ?: "desconhecido"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            )
        }

        if (!previewEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(12.dp)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Preview desativado durante transmissão",
                    color = colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }

        // Menu flutuante expansível de ferramentas com navegação hierárquica por categorias
        CameraToolsDrawer(
            isOpen = isToolsDrawerOpen,
            initialCategory = toolsInitialCategory,
            onDismiss = { isToolsDrawerOpen = false },
            activeMissionSession = activeMissionSession,
            activeMissionId = activeMissionId,
            missionListUiState = missionListUiState,
            onRefreshMissions = { missionListViewModel.fetchMissions() },
            showMissionOverlay = showMissionOverlay,
            onToggleMissionOverlay = { showMissionOverlay = !showMissionOverlay },
            onStartMission = {
                if (isDroneConnected && activeMissionSession.hasMission &&
                    activeMissionSession.status == ActiveMissionStatus.READY) scope.launch {
                    runCatching { missionManager.startMission() }
                        .onSuccess {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.EXECUTING)
                            Toast.makeText(context, "Missão iniciada", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                            Toast.makeText(
                                context,
                                "Erro ao iniciar missão: ${it.message ?: "desconhecido"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            },
            onPauseMission = {
                if (isDroneConnected && activeMissionSession.hasMission &&
                    activeMissionSession.status == ActiveMissionStatus.EXECUTING) scope.launch {
                    runCatching { missionManager.pauseMission() }
                        .onSuccess {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.PAUSED)
                            Toast.makeText(context, "Missão pausada", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                            Toast.makeText(
                                context,
                                "Erro ao pausar missão: ${it.message ?: "desconhecido"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            },
            onStopMission = {
                if (isDroneConnected && activeMissionSession.hasMission &&
                    activeMissionSession.status in listOf(ActiveMissionStatus.EXECUTING, ActiveMissionStatus.PAUSED)) scope.launch {
                    runCatching { missionManager.stopMission() }
                        .onSuccess {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.STOPPED)
                            Toast.makeText(context, "Missão parada", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                            Toast.makeText(
                                context,
                                "Erro ao parar missão: ${it.message ?: "desconhecido"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            },
            detectionEnabled = detectionEnabled,
            onToggleDetection = { enabled ->
                detectionEnabled = enabled
                scope.launch {
                    objectDetectionRepo.setDetectionEnabled(enabled)
                }
            },
            selectedModelId = selectedDetectionModelId,
            onSelectModel = { modelId ->
                selectedDetectionModelId = modelId
                mlDetectionViewModel.selectModel(modelId)
                scope.launch {
                    objectDetectionRepo.setActiveModelId(modelId)
                }
            },
            installedModels = availableModels,
            lastInferenceMs = lastInferenceMs,
            lastDetectionError = previewCaptureError ?: lastDetectionError,
            remoteCatalog = remoteCatalog,
            remoteCatalogLoading = remoteCatalogLoading,
            downloadProgress = downloadProgress,
            onDownloadModel = { entry ->
                mlDetectionViewModel.downloadRemoteModel(entry.id)
            },
            isRecording = isRecording,
            recordingDuration = formatDuration(recordingSeconds),
            onTakePhotoClick = onTakePhoto,
            onRecordClick = onToggleRecord,
            storageTarget = mediaStorageTarget,
            streamState = streamState,
            onStreamToggle = onStreamToggle,
            rtmpUrl = rtmpUrl,
            onCellCameraClick = onCellCameraClick,
            productName = product?.model?.displayName,
            droneState = droneState,
            telemetry = telemetry,
            measurementSystem = measurementSystem,
            weatherSummary = weatherSummary,
            isFeedAvailable = isFeedAvailable
        )

    }

    DisposableEffect(Unit) {
        onDispose {
            rtmpStreamer.release()
            droneController.stop()
            missionManager.destroy()
        }
    }
}


@Composable
private fun CameraToolsDrawer(
    isOpen: Boolean,
    initialCategory: ToolsMenuScreen,
    onDismiss: () -> Unit,
    activeMissionSession: ActiveMissionSession,
    activeMissionId: String?,
    missionListUiState: MissionListUiState,
    onRefreshMissions: () -> Unit,
    showMissionOverlay: Boolean,
    onToggleMissionOverlay: () -> Unit,
    onStartMission: () -> Unit,
    onPauseMission: () -> Unit,
    onStopMission: () -> Unit,
    detectionEnabled: Boolean,
    onToggleDetection: (Boolean) -> Unit,
    selectedModelId: String?,
    onSelectModel: (String) -> Unit,
    installedModels: List<MLModel>,
    lastInferenceMs: Long?,
    lastDetectionError: String?,
    remoteCatalog: List<ModelCatalogEntryDto>,
    remoteCatalogLoading: Boolean,
    downloadProgress: Float?,
    onDownloadModel: (ModelCatalogEntryDto) -> Unit,
    isRecording: Boolean,
    recordingDuration: String,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    storageTarget: String,
    streamState: StreamState,
    onStreamToggle: () -> Unit,
    rtmpUrl: String,
    onCellCameraClick: () -> Unit,
    productName: String?,
    droneState: DroneState,
    telemetry: DroneTelemetry,
    measurementSystem: String,
    weatherSummary: WeatherSummary?,
    isFeedAvailable: Boolean
) {
    var currentScreen by remember { mutableStateOf<ToolsMenuScreen>(initialCategory) }

    LaunchedEffect(isOpen, initialCategory) {
        if (isOpen) {
            currentScreen = initialCategory
        }
    }

    BackHandler(enabled = isOpen) {
        when (currentScreen) {
            is ToolsMenuScreen.MissionDetails -> currentScreen = ToolsMenuScreen.Missions
            ToolsMenuScreen.Home -> onDismiss()
            else -> currentScreen = ToolsMenuScreen.Home
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    color = Color(0xF5121212),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Header com navegação dinâmica
                        val isSubmenu = currentScreen != ToolsMenuScreen.Home
                        val titleText = when (currentScreen) {
                            ToolsMenuScreen.Home -> "Ferramentas"
                            ToolsMenuScreen.AI -> "Inteligência Artificial"
                            ToolsMenuScreen.Missions -> "Missões de Voo"
                            is ToolsMenuScreen.MissionDetails -> "Detalhes da Missão"
                            ToolsMenuScreen.Capture -> "Captura e Mídia"
                            ToolsMenuScreen.Streaming -> "Transmissão Ao Vivo"
                            ToolsMenuScreen.PhoneCamera -> "Câmera do Celular"
                            ToolsMenuScreen.Settings -> "Configurações"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (isSubmenu) {
                                    IconButton(
                                        onClick = {
                                            when (currentScreen) {
                                                is ToolsMenuScreen.MissionDetails -> currentScreen = ToolsMenuScreen.Missions
                                                else -> currentScreen = ToolsMenuScreen.Home
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Voltar",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.White.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = titleText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Fechar menu",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        // Conteúdo com transição horizontal animada
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState == ToolsMenuScreen.Home) {
                                    (slideInHorizontally { -it / 2 } + fadeIn(tween(180))) togetherWith
                                        (slideOutHorizontally { it / 2 } + fadeOut(tween(180)))
                                } else if (initialState == ToolsMenuScreen.Home) {
                                    (slideInHorizontally { it / 2 } + fadeIn(tween(180))) togetherWith
                                        (slideOutHorizontally { -it / 2 } + fadeOut(tween(180)))
                                } else if (targetState is ToolsMenuScreen.MissionDetails) {
                                    (slideInHorizontally { it / 2 } + fadeIn(tween(180))) togetherWith
                                        (slideOutHorizontally { -it / 2 } + fadeOut(tween(180)))
                                } else {
                                    (slideInHorizontally { -it / 2 } + fadeIn(tween(180))) togetherWith
                                        (slideOutHorizontally { it / 2 } + fadeOut(tween(180)))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "tools_content"
                        ) { screen ->
                            when (screen) {
                                ToolsMenuScreen.Home -> ToolsHomeScreen(
                                    activeMissionSession = activeMissionSession,
                                    detectionEnabled = detectionEnabled,
                                    selectedModelName = installedModels.firstOrNull { it.id == selectedModelId }?.name,
                                    isRecording = isRecording,
                                    streamState = streamState,
                                    onSelectCategory = { currentScreen = it }
                                )
                                ToolsMenuScreen.AI -> ToolsAiScreen(
                                    detectionEnabled = detectionEnabled,
                                    onToggleDetection = onToggleDetection,
                                    selectedModelId = selectedModelId,
                                    onSelectModel = onSelectModel,
                                    installedModels = installedModels,
                                    lastInferenceMs = lastInferenceMs,
                                    lastDetectionError = lastDetectionError,
                                    remoteCatalog = remoteCatalog,
                                    remoteCatalogLoading = remoteCatalogLoading,
                                    downloadProgress = downloadProgress,
                                    onDownloadModel = onDownloadModel
                                )
                                ToolsMenuScreen.Missions -> ToolsMissionsScreen(
                                    activeMissionSession = activeMissionSession,
                                    activeMissionId = activeMissionId,
                                    missionListUiState = missionListUiState,
                                    onRefreshMissions = onRefreshMissions,
                                    showMissionOverlay = showMissionOverlay,
                                    onToggleMissionOverlay = onToggleMissionOverlay,
                                    onStartMission = onStartMission,
                                    onPauseMission = onPauseMission,
                                    onStopMission = onStopMission,
                                    onSelectMission = { mission ->
                                        currentScreen = ToolsMenuScreen.MissionDetails(mission)
                                    }
                                )
                                is ToolsMenuScreen.MissionDetails -> ToolsMissionDetailsScreen(
                                    mission = screen.mission,
                                    activeMissionId = activeMissionId,
                                    onLoadedSession = {
                                        ActiveMissionSessionManager.startMissionSession(screen.mission.id.toString(), screen.mission.name)
                                        ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.READY)
                                    }
                                )
                                ToolsMenuScreen.Capture -> ToolsCaptureScreen(
                                    isRecording = isRecording,
                                    recordingDuration = recordingDuration,
                                    onTakePhotoClick = onTakePhotoClick,
                                    onRecordClick = onRecordClick,
                                    storageTarget = storageTarget
                                )
                                ToolsMenuScreen.Streaming -> ToolsStreamingScreen(
                                    streamState = streamState,
                                    onStreamToggle = onStreamToggle,
                                    rtmpUrl = rtmpUrl
                                )
                                ToolsMenuScreen.PhoneCamera -> ToolsPhoneCameraScreen(
                                    onCellCameraClick = {
                                        onDismiss()
                                        onCellCameraClick()
                                    }
                                )
                                ToolsMenuScreen.Settings -> ToolsSettingsScreen(
                                    productName = productName,
                                    droneState = droneState,
                                    telemetry = telemetry,
                                    storageTarget = storageTarget,
                                    measurementSystem = measurementSystem,
                                    weatherSummary = weatherSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsHomeScreen(
    activeMissionSession: ActiveMissionSession,
    detectionEnabled: Boolean,
    selectedModelName: String?,
    isRecording: Boolean,
    streamState: StreamState,
    onSelectCategory: (ToolsMenuScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // A. Inteligência Artificial
        ToolsCategoryRow(
            icon = Icons.Default.Psychology,
            title = "Inteligência Artificial",
            subtitle = if (detectionEnabled) {
                selectedModelName ?: "Detecção ativa"
            } else {
                "Pipeline YOLO inativo"
            },
            statusText = if (detectionEnabled) "ATIVO" else null,
            statusColor = if (detectionEnabled) NeuralColors.Success else null,
            onClick = { onSelectCategory(ToolsMenuScreen.AI) }
        )

        // B. Missões
        ToolsCategoryRow(
            icon = Icons.Default.Route,
            title = "Missões de Voo",
            subtitle = if (activeMissionSession.hasMission) {
                activeMissionSession.missionName ?: "Missão carregada"
            } else {
                "Planejamento e rotas"
            },
            statusText = if (activeMissionSession.hasMission) "SESSÃO" else null,
            statusColor = if (activeMissionSession.hasMission) NeuralColors.Success else null,
            onClick = { onSelectCategory(ToolsMenuScreen.Missions) }
        )

        // C. Captura
        ToolsCategoryRow(
            icon = Icons.Default.CameraAlt,
            title = "Captura e Mídia",
            subtitle = if (isRecording) "Gravando vídeo agora..." else "Fotos e vídeos",
            statusText = if (isRecording) "REC" else null,
            statusColor = if (isRecording) NeuralColors.Danger else null,
            onClick = { onSelectCategory(ToolsMenuScreen.Capture) }
        )

        // D. Transmissão
        val isStreaming = streamState is StreamState.Streaming
        ToolsCategoryRow(
            icon = Icons.Default.WifiTethering,
            title = "Transmissão Ao Vivo",
            subtitle = if (isStreaming) "Transmitindo feed RTMP" else "Configurar transmissão",
            statusText = if (isStreaming) "LIVE" else null,
            statusColor = if (isStreaming) NeuralColors.Danger else null,
            onClick = { onSelectCategory(ToolsMenuScreen.Streaming) }
        )

        // E. Câmera do Celular
        ToolsCategoryRow(
            icon = Icons.Default.Smartphone,
            title = "Câmera do Smartphone",
            subtitle = "Sensor óptico local (CameraX)",
            statusText = null,
            statusColor = null,
            onClick = { onSelectCategory(ToolsMenuScreen.PhoneCamera) }
        )

        // F. Configurações
        ToolsCategoryRow(
            icon = Icons.Default.Settings,
            title = "Configurações e Drone",
            subtitle = "Aeronave, unidades e clima",
            statusText = null,
            statusColor = null,
            onClick = { onSelectCategory(ToolsMenuScreen.Settings) }
        )
    }
}

@Composable
private fun ToolsAiScreen(
    detectionEnabled: Boolean,
    onToggleDetection: (Boolean) -> Unit,
    selectedModelId: String?,
    onSelectModel: (String) -> Unit,
    installedModels: List<MLModel>,
    lastInferenceMs: Long?,
    lastDetectionError: String?,
    remoteCatalog: List<ModelCatalogEntryDto>,
    remoteCatalogLoading: Boolean,
    downloadProgress: Float?,
    onDownloadModel: (ModelCatalogEntryDto) -> Unit
) {
    val activeModel = installedModels.firstOrNull { it.id == selectedModelId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Switch Ativar / Desativar detecção
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, if (detectionEnabled) NeuralColors.Success.copy(alpha = 0.4f) else NeuralColors.Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Detecção em Tempo Real",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (detectionEnabled) "Pipeline YOLO ativo sobre o vídeo" else "Inferência desativada",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = detectionEnabled,
                    onCheckedChange = onToggleDetection,
                    enabled = installedModels.isNotEmpty(),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeuralColors.Success,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
        }

        // Métricas de Desempenho e Modelo Ativo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Latência", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    val latencyColor = when {
                        lastInferenceMs == null -> Color.White.copy(alpha = 0.6f)
                        lastInferenceMs < 80L -> NeuralColors.Success
                        lastInferenceMs < 200L -> NeuralColors.Warning
                        else -> NeuralColors.Danger
                    }
                    Text(
                        text = lastInferenceMs?.let { "${it} ms" } ?: "--",
                        color = latencyColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1.4f),
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Modelo Ativo", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    Text(
                        text = activeModel?.name ?: "Nenhum",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Alerta de Erro se houver
        if (!lastDetectionError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.Danger.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, NeuralColors.Danger.copy(alpha = 0.30f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeuralColors.Danger, modifier = Modifier.size(15.dp))
                    Text(
                        text = lastDetectionError,
                        color = NeuralColors.Danger,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Seção: Modelos Instalados
        DrawerSectionHeader(title = "MODELOS INSTALADOS (${installedModels.size})")

        if (installedModels.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Text(
                    text = "Nenhum modelo YOLO instalado localmente. Baixe um modelo abaixo.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            installedModels.forEach { model ->
                val isSelected = model.id == selectedModelId
                Surface(
                    onClick = { onSelectModel(model.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.08f) else NeuralColors.SurfaceAlt,
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else NeuralColors.Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (isSelected) NeuralColors.Success.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeuralColors.Success, modifier = Modifier.size(14.dp))
                            } else {
                                Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (model.classes.isNotEmpty()) {
                                Text(
                                    text = model.classes.take(3).joinToString(", "),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeuralColors.Success.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ATIVO",
                                    color = NeuralColors.Success,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seção: Catálogo Remoto
        DrawerSectionHeader(title = "CATÁLOGO REMOTO")

        if (downloadProgress != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Baixando modelo...", color = Color.White, fontSize = 11.sp)
                        Text("${(downloadProgress * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
        }

        if (remoteCatalogLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Carregando catálogo...", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        } else if (remoteCatalog.isEmpty()) {
            Text(
                text = "Nenhum modelo adicional no catálogo remoto.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            val uninstalled = remoteCatalog.filter { entry -> installedModels.none { it.id == entry.id } }
            if (uninstalled.isEmpty()) {
                Text(
                    text = "Todos os modelos do catálogo já estão instalados.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            } else {
                uninstalled.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeuralColors.SurfaceAlt,
                        border = BorderStroke(1.dp, NeuralColors.Border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                val sizeMb = String.format(java.util.Locale.US, "%.1f MB", entry.sizeBytes / (1024.0 * 1024.0))
                                Text(
                                    text = "$sizeMb • ${entry.classes.size} classes",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { onDownloadModel(entry) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Baixar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsMissionsScreen(
    activeMissionSession: ActiveMissionSession,
    activeMissionId: String?,
    missionListUiState: MissionListUiState,
    onRefreshMissions: () -> Unit,
    showMissionOverlay: Boolean,
    onToggleMissionOverlay: () -> Unit,
    onStartMission: () -> Unit,
    onPauseMission: () -> Unit,
    onStopMission: () -> Unit,
    onSelectMission: (Mission) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ToolsMissionFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card de Missão Ativa no Topo (se houver missão carregada)
        if (activeMissionSession.hasMission) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Success.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MISSÃO NA SESSÃO", color = NeuralColors.Success, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                text = activeMissionSession.missionName ?: "Missão Ativa",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val (statusText, statusColor) = when (activeMissionSession.status) {
                            ActiveMissionStatus.LOADING -> "Carregando" to NeuralColors.Warning
                            ActiveMissionStatus.READY -> "Pronta" to NeuralColors.Success
                            ActiveMissionStatus.EXECUTING -> "Executando" to Color(0xFF00C2FF)
                            ActiveMissionStatus.PAUSED -> "Pausada" to NeuralColors.Warning
                            ActiveMissionStatus.STOPPED -> "Parada" to NeuralColors.Danger
                            ActiveMissionStatus.COMPLETED -> "Concluída" to NeuralColors.Success
                            ActiveMissionStatus.ERROR -> "Erro" to NeuralColors.Danger
                            ActiveMissionStatus.IDLE -> "Inativa" to Color.White.copy(alpha = 0.5f)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Ações de Voo da Missão
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onStartMission,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Success),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onPauseMission,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pausar", color = Color.White, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onStopMission,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeuralColors.Danger.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = NeuralColors.Danger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parar", color = NeuralColors.Danger, fontSize = 11.sp)
                        }
                    }

                    // Switch sobreposição da câmera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exibir HUD na câmera", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                        Switch(
                            checked = showMissionOverlay,
                            onCheckedChange = { onToggleMissionOverlay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeuralColors.Success,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }

        // Campo de busca
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                    cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Pesquisar missão...", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Filtros Rápidos (Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolsMissionFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else NeuralColors.Border)
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Lista de Missões reais
        DrawerSectionHeader(title = "CATÁLOGO DE MISSÕES")

        when (missionListUiState) {
            is MissionListUiState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregando missões...", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
            is MissionListUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = missionListUiState.message,
                        color = NeuralColors.Danger,
                        fontSize = 11.sp
                    )
                    OutlinedButton(
                        onClick = onRefreshMissions,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recarregar", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            is MissionListUiState.Unauthorized -> {
                Text("Autenticação necessária para sincronizar missões.", color = NeuralColors.Warning, fontSize = 11.sp)
            }
            is MissionListUiState.Idle -> {
                OutlinedButton(
                    onClick = onRefreshMissions,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Carregar missões", color = Color.White, fontSize = 11.sp)
                }
            }
            is MissionListUiState.Success -> {
                val allMissions = missionListUiState.missions
                val filtered = allMissions.filter { mission ->
                    val matchesQuery = mission.name.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (selectedFilter) {
                        ToolsMissionFilter.ALL -> true
                        ToolsMissionFilter.PLANNED -> mission.id.toString() != activeMissionId
                        ToolsMissionFilter.IN_PROGRESS -> mission.id.toString() == activeMissionId
                        ToolsMissionFilter.COMPLETED -> false
                    }
                    matchesQuery && matchesFilter
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Nenhuma missão corresponde à busca." else "Nenhuma missão encontrada.",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    filtered.forEach { mission ->
                        val isCurrentActive = mission.id.toString() == activeMissionId
                        Surface(
                            onClick = { onSelectMission(mission) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrentActive) NeuralColors.Success.copy(alpha = 0.08f) else NeuralColors.SurfaceAlt,
                            border = BorderStroke(1.dp, if (isCurrentActive) NeuralColors.Success.copy(alpha = 0.35f) else NeuralColors.Border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.06f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = if (isCurrentActive) NeuralColors.Success else Color.White, modifier = Modifier.size(16.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mission.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${mission.waypointCount} pts • ${mission.altitude.toInt()}m • ${mission.autoSpeed.toInt()}m/s",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 10.sp
                                    )
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsMissionDetailsScreen(
    mission: Mission,
    activeMissionId: String?,
    onLoadedSession: () -> Unit
) {
    val context = LocalContext.current
    val isAlreadyLoaded = mission.id.toString() == activeMissionId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card de Informações da Missão
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mission.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = "ID #${mission.id}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                InfoRow(label = "Waypoints", value = "${mission.waypointCount} pontos")
                InfoRow(label = "Altitude Planejada", value = "${mission.altitude} m")
                InfoRow(label = "Velocidade de Cruzeiro", value = "${mission.autoSpeed} m/s")
                InfoRow(label = "Velocidade Máxima", value = "${mission.maxSpeed} m/s")
                InfoRow(
                    label = "Origem",
                    value = String.format(java.util.Locale.US, "%.4f, %.4f", mission.latitude, mission.longitude)
                )
            }
        }

        // Ações Principais
        Button(
            onClick = {
                val intent = Intent(context, MissionControlActivity::class.java).apply {
                    putExtra("MISSION_ID", mission.id)
                }
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Primary),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = NeuralColors.OnPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Mapa da Missão", color = NeuralColors.OnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                onLoadedSession()
                Toast.makeText(context, "Missão '${mission.name}' preparada para o drone", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isAlreadyLoaded) NeuralColors.Success.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f)),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (isAlreadyLoaded) Icons.Default.CheckCircle else Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = if (isAlreadyLoaded) NeuralColors.Success else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAlreadyLoaded) "Missão Pronta na Sessão" else "Carregar / Preparar no Drone",
                color = if (isAlreadyLoaded) NeuralColors.Success else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
                Text(
                    text = "Ao carregar a missão, seus waypoints e limites são enviados para a sessão operacional da aeronave, permitindo execução direta ou acompanhamento pelo minimapa.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ToolsCaptureScreen(
    isRecording: Boolean,
    recordingDuration: String,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    storageTarget: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DrawerSectionHeader(title = "DISPARO MANUAL")

        // Botão de Captura de Foto
        Surface(
            onClick = onTakePhotoClick,
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Capturar Foto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Salvar snapshot em alta resolução", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }
        }

        // Botão de Gravação de Vídeo
        Surface(
            onClick = onRecordClick,
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, if (isRecording) NeuralColors.Danger.copy(alpha = 0.5f) else NeuralColors.Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isRecording) NeuralColors.Danger.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isRecording) Icons.Default.StopCircle else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (isRecording) NeuralColors.Danger else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Gravando Vídeo" else "Gravação de Vídeo",
                        color = if (isRecording) NeuralColors.Danger else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isRecording) "Tempo: $recordingDuration" else "Iniciar registro contínuo",
                        color = if (isRecording) NeuralColors.Danger.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRecording) NeuralColors.Danger.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = if (isRecording) "PARAR" else "GRAVAR",
                        color = if (isRecording) NeuralColors.Danger else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        DrawerSectionHeader(title = "ARMAZENAMENTO")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(
                    label = "Destino Selecionado",
                    value = if (storageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) "SD Card do Drone" else "Armazenamento do Celular"
                )
                InfoRow(label = "Modo de Gravação", value = "H.264 / MP4")
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
                Text(
                    text = "Fotos e vídeos capturados durante uma missão são automaticamente indexados ao relatório pós-voo para download ou visualização.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ToolsStreamingScreen(
    streamState: StreamState,
    onStreamToggle: () -> Unit,
    rtmpUrl: String
) {
    val isStreaming = streamState is StreamState.Streaming
    val isConnecting = streamState is StreamState.Connecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DrawerSectionHeader(title = "STATUS DO SERVIÇO")

        // Status Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, if (isStreaming) NeuralColors.Danger.copy(alpha = 0.4f) else NeuralColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transmissão RTMP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val (badgeText, badgeColor) = when {
                        isStreaming -> "AO VIVO" to NeuralColors.Danger
                        isConnecting -> "CONECTANDO" to NeuralColors.Warning
                        else -> "INATIVO" to Color.White.copy(alpha = 0.4f)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = onStreamToggle,
                    enabled = !isConnecting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStreaming) NeuralColors.Danger else NeuralColors.Primary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isStreaming) Icons.Default.StopCircle else Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = if (isStreaming) Color.White else NeuralColors.OnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isStreaming -> "Encerrar Transmissão"
                            isConnecting -> "Conectando..."
                            else -> "Iniciar Transmissão RTMP"
                        },
                        color = if (isStreaming) Color.White else NeuralColors.OnPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        DrawerSectionHeader(title = "ENDPOINT CONFIGURADO")

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Servidor RTMP", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                Text(
                    text = rtmpUrl.ifBlank { "rtmp://vantly.cloud:1935/live/drone" },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
                Text(
                    text = "Aviso: Durante transmissões ao vivo com alta taxa de bits, o preview local de vídeo pode ser suspenso temporariamente para garantir a integridade da transmissão e economia de processamento da controladora.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ToolsPhoneCameraScreen(
    onCellCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DrawerSectionHeader(title = "SENSOR ÓPTICO DO SMARTPHONE")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Câmera Integrada", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pipeline CameraX nativo", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                InfoRow(label = "Compatibilidade", value = "CameraX 1.4+")
                InfoRow(label = "Sensores", value = "Traseira / Frontal")
                InfoRow(label = "Uso Recomendado", value = "Solo / Contingência")
            }
        }

        Button(
            onClick = onCellCameraClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Primary),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Camera, contentDescription = null, tint = NeuralColors.OnPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Alternar para Câmera do Celular", color = NeuralColors.OnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
                Text(
                    text = "A câmera do smartphone opera em modo independente, permitindo validação visual preliminar da área de pouso ou registro mesmo sem comunicação com a aeronave.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ToolsSettingsScreen(
    productName: String?,
    droneState: DroneState,
    telemetry: DroneTelemetry,
    storageTarget: String,
    measurementSystem: String,
    weatherSummary: WeatherSummary?
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DrawerSectionHeader(title = "AERONAVE & TELEMETRIA")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Modelo", value = productName ?: "Não conectada")
                InfoRow(label = "Estado de Voo", value = droneState.name)
                InfoRow(label = "Bateria da Aeronave", value = "${telemetry.batteryLevel}%")
                InfoRow(label = "Satélites GPS", value = "${telemetry.gpsSatellites}")
            }
        }

        DrawerSectionHeader(title = "PREFERÊNCIAS OPERACIONAIS")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuralColors.SurfaceAlt,
            border = BorderStroke(1.dp, NeuralColors.Border)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(
                    label = "Destino de Mídia",
                    value = if (storageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) "SD Card do Drone" else "Armazenamento do Celular"
                )
                InfoRow(
                    label = "Sistema de Medidas",
                    value = if (measurementSystem == MeasurementSettingsRepository.SYSTEM_IMPERIAL) "Imperial (ft, mph)" else "Métrico (m, km/h)"
                )
            }
        }

        weatherSummary?.let { summary ->
            DrawerSectionHeader(title = "CONDIÇÃO CLIMÁTICA")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusText = when (summary.decision.level) {
                        WeatherSafetyLevel.SAFE -> "Favorável para Voo"
                        WeatherSafetyLevel.CAUTION -> "Atenção Operacional"
                        WeatherSafetyLevel.NO_FLY -> "Desfavorável (Não Decolar)"
                    }
                    InfoRow(label = "Segurança", value = statusText)
                    val windSpeed = MeasurementConverter.formatSpeed(
                        (summary.snapshot.windSpeedMs ?: 0.0).toFloat(),
                        measurementSystem
                    )
                    InfoRow(label = "Vento", value = windSpeed)
                }
            }
        }

        DrawerSectionHeader(title = "ATALHOS")

        OutlinedButton(
            onClick = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configurações do Aplicativo", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ToolsCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusText: String?,
    statusColor: Color?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.SurfaceAlt,
        border = BorderStroke(1.dp, NeuralColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (statusText != null && statusColor != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.38f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private const val WEATHER_MIN_REFRESH_MS = 60_000L

private data class WeatherQueryLocation(
    val latitude: Double,
    val longitude: Double,
    val source: WeatherLocationSource
)

private enum class WeatherLocationSource {
    DRONE_TELEMETRY,
    OPERATOR_GPS
}

private fun resolveWeatherQueryLocation(
    telemetry: DroneTelemetry,
    operatorLocation: Point?
): WeatherQueryLocation? {
    val telemetryLatitude = telemetry.latitude
    val telemetryLongitude = telemetry.longitude
    if (isValidCoordinate(telemetryLatitude, telemetryLongitude)) {
        return WeatherQueryLocation(
            latitude = telemetryLatitude,
            longitude = telemetryLongitude,
            source = WeatherLocationSource.DRONE_TELEMETRY
        )
    }

    val fallbackLocation = operatorLocation ?: return null
    val fallbackLatitude = fallbackLocation.latitude()
    val fallbackLongitude = fallbackLocation.longitude()
    if (!isValidCoordinate(fallbackLatitude, fallbackLongitude)) {
        return null
    }

    return WeatherQueryLocation(
        latitude = fallbackLatitude,
        longitude = fallbackLongitude,
        source = WeatherLocationSource.OPERATOR_GPS
    )
}

private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    return latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
}

// ===============================================
//               SUB-COMPONENTES
// ===============================================

@Composable
private fun MissionPanelToggleButton(
    expanded: Boolean,
    hasMission: Boolean,
    onClick: () -> Unit
) {
    CompactControlButton(
        icon = if (expanded && hasMission) Icons.Default.VisibilityOff else Icons.Default.Route,
        onClick = onClick,
        enabled = true,
        color = if (hasMission) Color(0xFF00E5A0) else Color.White.copy(alpha = 0.70f),
        size = 38.dp
    )
}

@Composable
private fun MinimalistMissionControls(
    hasMission: Boolean,
    missionStatus: ActiveMissionStatus,
    isDroneConnected: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canPlay = isDroneConnected && hasMission && (missionStatus == ActiveMissionStatus.READY || missionStatus == ActiveMissionStatus.PAUSED)
    val canPause = isDroneConnected && hasMission && (missionStatus == ActiveMissionStatus.EXECUTING)
    val canStop = isDroneConnected && hasMission && (missionStatus == ActiveMissionStatus.EXECUTING || missionStatus == ActiveMissionStatus.PAUSED || missionStatus == ActiveMissionStatus.LOADING)

    val isExecuting = missionStatus == ActiveMissionStatus.EXECUTING
    val isPaused = missionStatus == ActiveMissionStatus.PAUSED

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Indicação sutil se houver missão mas o drone estiver desconectado ou em execução
        if (hasMission && !isDroneConnected) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "Aguardando conexão",
                    color = NeuralColors.Warning.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else if (hasMission && isExecuting) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "Missão em execução",
                    color = NeuralColors.Success,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else if (hasMission && isPaused) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "Missão pausada",
                    color = NeuralColors.Warning,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play (Iniciar / Retomar)
            MissionActionButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = if (isPaused) "Retomar missão" else "Iniciar missão",
                enabled = canPlay,
                activeColor = NeuralColors.Success,
                onClick = onPlayClick
            )

            // Pause
            MissionActionButton(
                icon = Icons.Default.Pause,
                contentDescription = "Pausar missão",
                enabled = canPause,
                activeColor = NeuralColors.Warning,
                onClick = onPauseClick
            )

            // Stop
            MissionActionButton(
                icon = Icons.Default.Stop,
                contentDescription = "Parar missão",
                enabled = canStop,
                activeColor = NeuralColors.Danger,
                onClick = onStopClick
            )
        }
    }
}

@Composable
private fun MissionActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val tint = if (enabled) activeColor else Color.White.copy(alpha = 0.25f)
    val bgColor = if (enabled) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.35f)
    val borderColor = if (enabled) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)

    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun CompactHeader(
    droneState: DroneState,
    batteryLevel: Int,
    missionName: String?,
    weatherSummary: WeatherSummary?,
    measurementSystem: String,
    selectedModelName: String?,
    onBackClick: () -> Unit,
    onToggleControls: () -> Unit,
    isFeedAvailable: Boolean = true
) {
    BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding()
        .padding(horizontal = 10.dp, vertical = 7.dp)) {
        val compact = maxWidth < 700.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(onClick = onBackClick, shape = RoundedCornerShape(10.dp),
                color = NeuralColors.DashboardCard.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White,
                        modifier = Modifier.size(18.dp))
                }
            }
            if (!missionName.isNullOrBlank()) {
                HudChip {
                    Icon(Icons.Default.Route, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                    Text("Missão: $missionName", color = Color.White, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = if (compact) 105.dp else 155.dp))
                }
            }
            if (!compact && weatherSummary != null) {
                HudChip {
                    Icon(Icons.Default.WbSunny, null, tint = when (weatherSummary.decision.level) {
                        WeatherSafetyLevel.SAFE -> NeuralColors.Success
                        WeatherSafetyLevel.CAUTION -> NeuralColors.Warning
                        WeatherSafetyLevel.NO_FLY -> NeuralColors.Danger
                    }, modifier = Modifier.size(14.dp))
                    weatherSummary.snapshot.windSpeedMs?.let { speed ->
                        Text(MeasurementConverter.formatSpeed(speed.toFloat(), measurementSystem),
                            color = Color.White, fontSize = 11.sp)
                    }
                    Text(weatherSummary.decision.shortMessage,
                        color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp,
                        maxLines = 1, modifier = Modifier.widthIn(max = 130.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            HudChip {
                Box(Modifier.size(7.dp).background(
                    if (isFeedAvailable) NeuralColors.Success else NeuralColors.Warning, CircleShape))
                Text(if (isFeedAvailable) "Conectado" else "Desconectado", color = Color.White,
                    fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            HudChip {
                Icon(Icons.Default.BatteryFull, null, tint = NeuralColors.DashboardTextSecondary,
                    modifier = Modifier.size(14.dp))
                Text(if (isFeedAvailable && batteryLevel > 0) "$batteryLevel%" else "--%",
                    color = Color.White, fontSize = 11.sp)
            }
            if (!compact && !selectedModelName.isNullOrBlank()) {
                HudChip {
                    Icon(Icons.Default.Memory, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                    Text(selectedModelName, color = Color.White, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 118.dp))
                }
            }
            Surface(onClick = onToggleControls, shape = RoundedCornerShape(10.dp),
                color = NeuralColors.DashboardCard.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, "Ferramentas", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HudChip(content: @Composable RowScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(9.dp), color = NeuralColors.DashboardCard.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Row(Modifier.heightIn(min = 36.dp).padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp), content = content)
    }
}

@Composable
fun CameraControls(
    isRecording: Boolean,
    streamState: StreamState,
    hasMission: Boolean,
    missionExpanded: Boolean,
    detectionEnabled: Boolean,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStreamToggle: () -> Unit,
    onMissionToggle: () -> Unit,
    onAiToggle: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botão Foto
        CompactControlButton(
            icon = Icons.Default.CameraAlt,
            onClick = onTakePhotoClick,
            enabled = true,
            color = Color.White,
            size = 40.dp
        )

        // Botão Vídeo / Gravação
        CompactControlButton(
            icon = Icons.Default.Videocam,
            onClick = onRecordClick,
            enabled = true,
            color = if (isRecording) NeuralColors.Danger else Color.White,
            size = 40.dp
        )

        // Botão IA
        AiPanelButton(
            enabled = detectionEnabled,
            onClick = onAiToggle
        )

        // Botão Streaming
        SmallStreamingButton(
            streamState = streamState,
            onToggle = onStreamToggle,
            color = Color.White
        )

        // Botão Câmera Celular
        CompactControlButton(
            icon = Icons.Default.PhoneAndroid,
            onClick = onCellCameraClick,
            enabled = true,
            color = Color.White.copy(alpha = 0.85f),
            size = 36.dp
        )

        // Botão Missão
        MissionPanelToggleButton(
            expanded = missionExpanded,
            hasMission = hasMission,
            onClick = onMissionToggle
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Shutter Trigger Button (Grande botão circular vermelho estilo DJI)
        Surface(
            onClick = {
                if (isRecording) onRecordClick() else onTakePhotoClick()
            },
            shape = CircleShape,
            color = if (isRecording) NeuralColors.Danger else Color.White.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, if (isRecording) Color.White else NeuralColors.Danger),
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(if (isRecording) 18.dp else 38.dp)
                        .background(
                            if (isRecording) Color.White else NeuralColors.Danger,
                            if (isRecording) RoundedCornerShape(4.dp) else CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun AiPanelButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    CompactControlButton(
        icon = Icons.Default.Psychology,
        onClick = onClick,
        enabled = true,
        color = if (enabled) Color(0xFF00C2FF) else Color.White.copy(alpha = 0.70f),
        size = 38.dp
    )
}

@Composable
private fun PersonDetectionOverlay(
    detections: List<DetectionResult>,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty()) return

    val boxColor = Color(0xFF00E5A0)
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 34f
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }
    val badgePaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(210, 0, 194, 255)
        }
    }

    Canvas(modifier = modifier) {
        detections.forEach { detection ->
            val left = detection.normalizedRect.left * size.width
            val top = detection.normalizedRect.top * size.height
            val right = detection.normalizedRect.right * size.width
            val bottom = detection.normalizedRect.bottom * size.height

            drawRect(
                color = boxColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(
                    width = (right - left).coerceAtLeast(0f),
                    height = (bottom - top).coerceAtLeast(0f)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            val label = "${detection.label} ${(detection.confidence * 100).toInt()}%"
            val labelWidth = labelPaint.measureText(label) + 28f
            val badgeTop = (top - 44f).coerceAtLeast(8f)
            val badgeRect = RectF(
                left,
                badgeTop,
                left + labelWidth,
                badgeTop + 36f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawRoundRect(badgeRect, 14f, 14f, badgePaint)
                drawText(label, badgeRect.left + 14f, badgeRect.bottom - 11f, labelPaint)
            }
        }
    }
}

@Composable
fun TelemetryHudStrip(
    telemetry: DroneTelemetry,
    measurementSystem: String,
    isConnected: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudMetric(
                label = "A",
                value = if (isConnected) MeasurementConverter.formatAltitude(telemetry.altitude, measurementSystem) else "--"
            )
            HudMetric(
                label = "D",
                value = if (isConnected) MeasurementConverter.formatDistance(telemetry.distanceFromHome, measurementSystem) else "--"
            )
            HudMetric(
                label = "V",
                value = if (isConnected) MeasurementConverter.formatSpeed(telemetry.speed, measurementSystem) else "--"
            )
            HudMetric(
                label = "GPS",
                value = if (isConnected && telemetry.gpsSatellites > 0) "${telemetry.gpsSatellites}" else "--"
            )
        }
    }
}

@Composable
fun WeatherCompactBadge(
    summary: WeatherSummary,
    measurementSystem: String,
    modifier: Modifier = Modifier
) {
    val tint = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Color(0xFF22C55E)
        WeatherSafetyLevel.CAUTION -> Color(0xFFF59E0B)
        WeatherSafetyLevel.NO_FLY -> Color(0xFFEF4444)
    }

    val icon = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Icons.Default.WbSunny
        WeatherSafetyLevel.CAUTION -> Icons.Default.Cloud
        WeatherSafetyLevel.NO_FLY -> Icons.Default.Thunderstorm
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(
            MeasurementConverter.formatSpeed((summary.snapshot.windSpeedMs ?: 0.0).toFloat(), measurementSystem),
            color = Color.White,
            fontSize = 11.sp
        )
        Text(
            "Clima favorável para voo",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HudMetric(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun HudMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SmallStreamingButton(
    streamState: StreamState,
    onToggle: () -> Unit,
    color: Color
) {
    val isConnecting = streamState is StreamState.Connecting
    val isStreaming = streamState is StreamState.Streaming
    val icon = when {
        isConnecting -> Icons.Default.HourglassTop
        isStreaming -> Icons.Default.StopCircle
        else -> Icons.Default.WifiTethering
    }
    val tint = when {
        isStreaming -> MaterialTheme.colorScheme.error
        else -> color
    }
    CompactControlButton(
        icon = icon,
        onClick = onToggle,
        enabled = !isConnecting,
        color = tint,
        size = 36.dp
    )
}

@Composable
fun EmergencyStopButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(colorScheme.error, CircleShape)
            .shadow(8.dp, CircleShape)
    ) {
        Icon(Icons.Default.Warning, "Emergência", tint = colorScheme.onError, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun FlightActionControls(
    modifier: Modifier = Modifier,
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactControlButton(
            icon = Icons.Default.FlightTakeoff,
            onClick = onTakeoffClick,
            enabled = droneState == DroneState.ON_GROUND,
            color = colorScheme.secondary,
            size = 44.dp
        )
        CompactControlButton(
            icon = Icons.Default.FlightLand,
            onClick = onLandClick,
            enabled = droneState == DroneState.IN_AIR,
            color = colorScheme.tertiary,
            size = 44.dp
        )
    }
}

@Composable
fun VerticalMovementControls(
    enabled: Boolean,
    onMoveUpStart: () -> Unit,
    onMoveDownStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowUp,
            enabled = enabled,
            onPressStart = onMoveUpStart,
            onPressEnd = onStop
        )
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowDown,
            enabled = enabled,
            onPressStart = onMoveDownStart,
            onPressEnd = onStop
        )
    }
}

@Composable
fun HoldToMoveButton(
    icon: ImageVector,
    enabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .size(46.dp)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        coroutineScope {
                            val repeatJob = launch {
                                while (isActive) {
                                    onPressStart()
                                    delay(300)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                                onPressEnd()
                            }
                        }
                    }
                )
            },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CompactControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    size: Dp = 38.dp
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) color else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}
