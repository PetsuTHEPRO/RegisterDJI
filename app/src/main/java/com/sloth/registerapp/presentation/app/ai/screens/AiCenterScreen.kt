package com.sloth.registerapp.presentation.app.ai.screens

import android.app.ActivityManager
import android.os.Process
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mldetection.data.remote.dto.ModelCatalogEntryDto
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.domain.model.MLModelSource
import com.sloth.registerapp.features.mldetection.presentation.MLDetectionViewModel
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.io.File

private enum class ModelFilter { ALL, DOWNLOADED, SERVER }
private val AiCard = NeuralColors.DashboardCard
private val AiInner = NeuralColors.DashboardInner

@Composable
fun AiCenterScreen(onBackClick: () -> Unit = {}, onSettingsClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: MLDetectionViewModel = viewModel()
    val preferences = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val persistedId by preferences.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )
    val models by vm.availableModels.collectAsStateWithLifecycle()
    val activeId by vm.activeModelId.collectAsStateWithLifecycle()
    val engineReady by vm.engineReady.collectAsStateWithLifecycle()
    val inferencing by vm.inferenceInProgress.collectAsStateWithLifecycle()
    val remote by vm.remoteCatalog.collectAsStateWithLifecycle()
    val remoteLoading by vm.remoteCatalogLoading.collectAsStateWithLifecycle()
    val catalogUnavailable by vm.remoteCatalogUnavailable.collectAsStateWithLifecycle()
    val downloadProgress by vm.downloadProgress.collectAsStateWithLifecycle()
    val error by vm.lastError.collectAsStateWithLifecycle()
    val importMessage by vm.importMessage.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(ModelFilter.ALL) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLocalModel(uri)
    }

    LaunchedEffect(persistedId, models) {
        if (models.any { it.id == persistedId } && activeId != persistedId) vm.selectModel(persistedId)
    }

    val installedRemoteIds = models.filter { it.source == MLModelSource.LOCAL_FILE }
        .map { it.id.removePrefix("local-") }.toSet()
    val remoteToShow = remote.filter { entry ->
        MLDetectionViewModel.sanitizeModelFileName(entry.id) !in installedRemoteIds &&
            models.none { it.id == entry.id }
    }
    val shownLocal = if (filter == ModelFilter.SERVER) emptyList() else models
    val shownRemote = if (filter == ModelFilter.DOWNLOADED) emptyList() else remoteToShow
    val totalCount = models.size + remoteToShow.size

    Column(Modifier.fillMaxSize().background(NeuralColors.DashboardBg)) {
        AiHeader()
        HorizontalDivider(color = NeuralColors.DashboardBorder)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HardwareCard(models, engineReady, inferencing)
            if (!importMessage.isNullOrBlank()) InfoCard(importMessage!!)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ViewInAr, null, tint = NeuralColors.DashboardTextSecondary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Modelos de Visão (Edge)", color = NeuralColors.TextPrimary,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("$totalCount pacotes", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                    IconButton(onClick = vm::refreshRemoteCatalog, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar catálogo",
                            tint = NeuralColors.DashboardTextSecondary, modifier = Modifier.size(17.dp))
                    }
                }
                ModelTabs(filter, models.size, remoteToShow.size) { filter = it }
                if (!error.isNullOrBlank()) ErrorCard(error!!)
                if (downloadProgress != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Baixando modelo: ${(downloadProgress!! * 100).toInt()}%",
                            color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                        LinearProgressIndicator(progress = { downloadProgress!!.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(), color = NeuralColors.DashboardBlue)
                    }
                }
                if (remoteLoading && filter != ModelFilter.DOWNLOADED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Consultando catálogo...", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                    }
                }
                shownLocal.forEach { model ->
                    LocalModelCard(model, model.id == (activeId ?: persistedId), remote.any {
                        MLDetectionViewModel.sanitizeModelFileName(it.id) == model.id.removePrefix("local-")
                    }) {
                        scope.launch {
                            vm.selectModel(model.id)
                            preferences.setActiveModelId(model.id)
                        }
                    }
                }
                shownRemote.forEach { entry ->
                    RemoteModelCard(entry, downloadProgress != null) { vm.downloadRemoteModel(entry.id) }
                }
                if (filter != ModelFilter.DOWNLOADED && catalogUnavailable && !remoteLoading) {
                    InfoCard("Catálogo indisponível no momento")
                } else if (shownLocal.isEmpty() && shownRemote.isEmpty() && !remoteLoading) {
                    InfoCard(when (filter) {
                        ModelFilter.ALL -> "Nenhum modelo disponível no momento."
                        ModelFilter.DOWNLOADED -> "Nenhum modelo instalado localmente."
                        ModelFilter.SERVER -> "Nenhum modelo disponível para download."
                    })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cloud, null, tint = NeuralColors.DashboardTextSecondary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Serviços Cloud (Nuvem)", color = NeuralColors.TextPrimary,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Inferência remota no servidor (sem armazenamento local)",
                            color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                    }
                }
                InfoCard("Nenhum serviço de inferência cloud está configurado neste aplicativo.")
            }

            ImportCard { picker.launch(arrayOf("*/*")) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AiHeader() {
    Row(Modifier.fillMaxWidth().statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = NeuralColors.DashboardCard,
            border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Memory, null, tint = NeuralColors.DashboardBlue,
                    modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Central de IA", color = NeuralColors.TextPrimary, fontSize = 17.sp,
                fontWeight = FontWeight.Bold)
            Text("Modelos e recursos", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HardwareCard(
    models: List<MLModel>, engineReady: Boolean, inferencing: Boolean
) {
    val context = LocalContext.current
    val activityManager = remember(context) { context.getSystemService(ActivityManager::class.java) }
    var cpuPercent by remember { mutableStateOf<Int?>(null) }
    var memory by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    LaunchedEffect(Unit) {
        var previousCpu = Process.getElapsedCpuTime()
        var previousTime = SystemClock.elapsedRealtime()
        while (true) {
            delay(2500)
            val nowCpu = Process.getElapsedCpuTime()
            val nowTime = SystemClock.elapsedRealtime()
            val elapsed = (nowTime - previousTime).coerceAtLeast(1L)
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            cpuPercent = (((nowCpu - previousCpu).toDouble() / (elapsed * cores)) * 100)
                .toInt().coerceIn(0, 100)
            previousCpu = nowCpu
            previousTime = nowTime
            val info = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(info)
            if (info.totalMem > 0) memory = (info.totalMem - info.availMem) to info.totalMem
        }
    }
    val localBytes = models.filter { it.source == MLModelSource.LOCAL_FILE }.sumOf { it.sizeBytes }
    val status = when {
        inferencing -> "INFERINDO"
        engineReady -> "MOTOR PRONTO"
        models.isNotEmpty() -> "MODELO DISPONÍVEL"
        else -> "INDISPONÍVEL"
    }
    Surface(shape = RoundedCornerShape(12.dp), color = AiCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HardwareHeading(status)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 285.dp || LocalDensity.current.fontScale >= 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard("CPU do app", cpuPercent?.let { "$it%" } ?: "Indisponível",
                            cpuPercent?.div(100f), Modifier.fillMaxWidth())
                        MetricCard("Memória RAM", memory?.let { formatGigabytes(it.first) + " / " + formatGigabytes(it.second) }
                            ?: "Indisponível", memory?.let { it.first.toFloat() / it.second }, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard("CPU do app", cpuPercent?.let { "$it%" } ?: "Indisponível",
                            cpuPercent?.div(100f), Modifier.weight(1f))
                        MetricCard("Memória RAM", memory?.let { formatGigabytes(it.first) + " / " + formatGigabytes(it.second) }
                            ?: "Indisponível", memory?.let { it.first.toFloat() / it.second }, Modifier.weight(1f))
                    }
                }
            }
            MetricCard("Armazenamento Edge", if (localBytes == 0L) "0 MB utilizados" else "${formatBytes(localBytes)} utilizados",
                null, Modifier.fillMaxWidth())
            HorizontalDivider(color = NeuralColors.DashboardBorder)
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, null, tint = NeuralColors.DashboardTextSecondary, modifier = Modifier.size(15.dp))
                Text("Modelos baixados ficam disponíveis no controle de voo.",
                    color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HardwareHeading(status: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val badge = @Composable {
            StatusPill(status, if (status == "INDISPONÍVEL") NeuralColors.DashboardTextSecondary else NeuralColors.Success)
        }
        if (maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Recursos & Hardware", color = NeuralColors.TextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
                Text("Telemetria do aparelho", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                badge()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Recursos & Hardware", color = NeuralColors.TextPrimary, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("Telemetria do aparelho", color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
                }
                badge()
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, tone: Color = NeuralColors.Success) {
    Surface(shape = RoundedCornerShape(5.dp), color = tone.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.3f))) {
        Text(text, color = tone, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, progress: Float?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = AiInner,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp)
            Text(value, color = NeuralColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (progress != null) LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = NeuralColors.DashboardBlue,
                trackColor = NeuralColors.DashboardBorder)
        }
    }
}

@Composable
private fun ModelTabs(filter: ModelFilter, localCount: Int, serverCount: Int, onSelect: (ModelFilter) -> Unit) {
    Surface(shape = RoundedCornerShape(9.dp), color = AiCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple(ModelFilter.ALL, "Todos", localCount + serverCount),
                Triple(ModelFilter.DOWNLOADED, "Baixados", localCount),
                Triple(ModelFilter.SERVER, "Servidor", serverCount)
            ).forEach { (item, name, count) ->
                val selected = item == filter
                Box(Modifier.weight(1f).background(if (selected) NeuralColors.DashboardBlue else Color.Transparent,
                    RoundedCornerShape(7.dp)).clickable { onSelect(item) }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center) {
                    Text("$name ($count)", color = if (selected) Color.White else NeuralColors.DashboardTextSecondary,
                        fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun LocalModelCard(model: MLModel, selected: Boolean, fromServer: Boolean, onSelect: () -> Unit) {
    val savedOrigin = remember(model.filePath) {
        if (model.source == MLModelSource.LOCAL_FILE) {
            runCatching { File(model.filePath.removeSuffix(".tflite") + ".origin").readText().trim() }.getOrNull()
        } else null
    }
    val hasClassLabels = remember(model.filePath, model.source) {
        model.source == MLModelSource.ASSET ||
            File(model.filePath.removeSuffix(".tflite") + ".txt").isFile
    }
    val origin = when {
        model.source == MLModelSource.ASSET -> "EMBARCADO"
        savedOrigin == "imported" || model.id.removePrefix("local-").startsWith("imported_") -> "IMPORTADO"
        savedOrigin == "downloaded" || fromServer -> "BAIXADO"
        else -> "LOCAL"
    }
    Surface(onClick = onSelect, shape = RoundedCornerShape(12.dp), color = AiCard,
        border = BorderStroke(1.dp, if (selected) NeuralColors.DashboardBlue.copy(alpha = 0.45f) else NeuralColors.DashboardBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModelIcon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(model.name, color = NeuralColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(origin + if (hasClassLabels && model.classes.isNotEmpty())
                    "  •  ${model.classes.joinToString(", ")}" else "",
                    color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp)
                Text(formatBytes(model.sizeBytes), color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp)
            }
            if (selected) StatusPill("SELECIONADO")
        }
    }
}

@Composable
private fun RemoteModelCard(entry: ModelCatalogEntryDto, downloading: Boolean, onDownload: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = AiCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModelIcon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, color = NeuralColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("SERVIDOR" + if (entry.classes.isNotEmpty()) "  •  ${entry.classes.joinToString(", ")}" else "",
                    color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp)
                Text(formatBytes(entry.sizeBytes), color = NeuralColors.DashboardTextSecondary, fontSize = 10.sp)
            }
            Surface(onClick = onDownload, enabled = !downloading, shape = RoundedCornerShape(7.dp),
                color = NeuralColors.DashboardBlue) {
                Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Baixar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ModelIcon() {
    Box(Modifier.size(36.dp).background(AiInner, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Memory, null, tint = NeuralColors.DashboardTextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ImportCard(onClick: () -> Unit) {
    val dashColor = NeuralColors.DashboardBorder
    Column(Modifier.fillMaxWidth().drawBehind {
        drawRoundRect(dashColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))))
    }.clickable(onClick = onClick).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(Icons.Outlined.AddBox, null, tint = NeuralColors.DashboardTextSecondary, modifier = Modifier.size(25.dp))
        Text("Importar Modelo Local (.tflite)", color = NeuralColors.TextPrimary,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Selecione um modelo TFLite compatível com o detector YOLO atual.",
            color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun InfoCard(message: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = AiCard, border = BorderStroke(1.dp, NeuralColors.DashboardBorder)) {
        Text(message, color = NeuralColors.DashboardTextSecondary, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = NeuralColors.Danger.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, NeuralColors.Danger.copy(alpha = 0.3f))) {
        Text(message, color = NeuralColors.Danger, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "Indisponível"
    bytes < 1024L * 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatGigabytes(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
