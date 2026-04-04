package com.sloth.registerapp.presentation.video.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.SurfaceHolder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedro.library.view.OpenGlView
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.features.detection.data.ml.DetectionBox
import com.sloth.registerapp.features.detection.data.ml.PersonDetector
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.features.streaming.data.PhoneRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.presentation.video.components.StreamingControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun CellCameraScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val objectDetectionRepo = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val detectionEnabledPersisted by objectDetectionRepo.detectionEnabled.collectAsStateWithLifecycle(initialValue = false)
    val detectionModelId by objectDetectionRepo.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )

    val openGlView = remember { OpenGlView(context).apply { holder.setFormat(PixelFormat.TRANSLUCENT) } }
    val streamer = remember { PhoneRtmpStreamer(openGlView, rtmpUrl) }
    val streamState by streamer.state.collectAsStateWithLifecycle()
    val pixelCopyThread = remember { HandlerThread("cell-camera-pixel-copy").apply { start() } }
    val pixelCopyHandler = remember(pixelCopyThread) { Handler(pixelCopyThread.looper) }
    val personDetector = remember(context) {
        runCatching {
            if (PersonDetector.isModelAvailable(context)) PersonDetector(context) else null
        }.getOrNull()
    }

    var detectionBoxes by remember { mutableStateOf<List<DetectionBox>>(emptyList()) }
    var lastInferenceMs by remember { mutableStateOf<Long?>(null) }
    var lastDetectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rtmpUrl) {
        streamer.updateUrl(rtmpUrl)
        if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
            streamer.stop()
            streamer.start()
        }
    }

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    DisposableEffect(openGlView) {
        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                streamer.startPreview()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                streamer.stopPreview()
            }
        }
        openGlView.holder.addCallback(callback)
        onDispose {
            openGlView.holder.removeCallback(callback)
            personDetector?.close()
            pixelCopyThread.quitSafely()
            streamer.release()
        }
    }

    LaunchedEffect(detectionEnabledPersisted, detectionModelId, personDetector, activity) {
        val canDetect = detectionEnabledPersisted &&
            detectionModelId == ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID &&
            personDetector != null &&
            activity != null

        if (!canDetect) {
            detectionBoxes = emptyList()
            if (!detectionEnabledPersisted) {
                lastDetectionError = null
            }
            return@LaunchedEffect
        }

        while (isActive) {
            val frameBitmap = captureOpenGlBitmap(
                activity = activity,
                view = openGlView,
                handler = pixelCopyHandler
            )

            if (frameBitmap == null) {
                detectionBoxes = emptyList()
                lastDetectionError = "Falha ao capturar frame da camera"
                delay(400)
                continue
            }

            val inferenceStart = System.currentTimeMillis()
            lastDetectionError = null
            detectionBoxes = runCatching {
                withContext(Dispatchers.Default) {
                    personDetector.detect(frameBitmap)
                }
            }.getOrElse { error ->
                lastDetectionError = error.message ?: "Falha na inferencia"
                emptyList()
            }
            lastInferenceMs = System.currentTimeMillis() - inferenceStart
            frameBitmap.recycle()
            delay(350)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AndroidView(
            factory = { openGlView },
            modifier = Modifier.fillMaxSize()
        )

        if (detectionEnabledPersisted && detectionModelId == ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID) {
            CellDetectionOverlay(
                detections = detectionBoxes,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background.copy(alpha = 0.55f),
                            colorScheme.background.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("Câmera do celular", fontSize = 14.sp)
                    Text(
                        text = when (streamState) {
                            StreamState.Idle -> "Pronto para transmitir"
                            StreamState.Connecting -> "Conectando..."
                            StreamState.Streaming -> "Transmitindo via RTMP"
                            is StreamState.Error -> "Erro na transmissão"
                        },
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val next = !detectionEnabledPersisted
                        lastDetectionError = null
                        detectionBoxes = emptyList()
                        lastInferenceMs = null
                        scope.launch {
                            objectDetectionRepo.setActiveModelId(ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID)
                            objectDetectionRepo.setDetectionEnabled(next)
                        }
                    },
                    enabled = personDetector != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Alternar IA",
                        tint = if (detectionEnabledPersisted && personDetector != null) Color(0xFF00C2FF) else colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = if (detectionEnabledPersisted && personDetector != null) "IA ativa" else "IA desligada",
                        fontSize = 13.sp
                    )
                    Text(
                        text = when {
                            personDetector == null -> "Modelo person_detector.tflite não encontrado"
                            !lastDetectionError.isNullOrBlank() -> lastDetectionError.orEmpty()
                            lastInferenceMs != null -> "Última inferência: ${lastInferenceMs} ms"
                            else -> "Teste local do detector de pessoas"
                        },
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            StreamingControl(
                state = streamState,
                onStart = { streamer.start() },
                onStop = { streamer.stop() }
            )
        }
    }
}

@Composable
private fun CellDetectionOverlay(
    detections: List<DetectionBox>,
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
            val badgeRect = RectF(left, badgeTop, left + labelWidth, badgeTop + 36f)

            drawContext.canvas.nativeCanvas.apply {
                drawRoundRect(badgeRect, 14f, 14f, badgePaint)
                drawText(label, badgeRect.left + 14f, badgeRect.bottom - 11f, labelPaint)
            }
        }
    }
}

private suspend fun captureOpenGlBitmap(
    activity: Activity,
    view: OpenGlView,
    handler: Handler
): Bitmap? {
    if (view.width <= 0 || view.height <= 0) return null

    return withContext(Dispatchers.Main) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        suspendCancellableCoroutine { continuation ->
            PixelCopy.request(activity.window, rect, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap)
                } else {
                    bitmap.recycle()
                    continuation.resume(null)
                }
            }, handler)
        }
    }
}
