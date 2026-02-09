package com.sloth.registerapp.presentation.app.weather.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sloth.registerapp.core.utils.MeasurementConverter
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot
import com.sloth.registerapp.features.weather.domain.model.WeatherSummary
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WeatherOverviewScreen(
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val weatherModule = remember(context) { WeatherModule.getInstance(context) }
    val providerSettings = remember(context) { WeatherProviderSettingsRepository.getInstance(context) }
    val selectedPrimaryProvider by providerSettings.primaryProvider.collectAsStateWithLifecycle(
        initialValue = WeatherProviderSettingsRepository.PROVIDER_WEATHER_API
    )

    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var summary by remember { mutableStateOf<WeatherSummary?>(null) }
    var hourly by remember { mutableStateOf<List<WeatherSnapshot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var lastWeatherRequestMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
    }

    LaunchedEffect(locationUiState.location) {
        val location = locationUiState.location ?: run {
            isLoading = false
            return@LaunchedEffect
        }

        val lat = location.latitude()
        val lon = location.longitude()
        val now = System.currentTimeMillis()
        if (now - lastWeatherRequestMs < WEATHER_MIN_REFRESH_MS) {
            return@LaunchedEffect
        }

        val firstLoad = summary == null
        isLoading = firstLoad
        isRefreshing = !firstLoad
        errorText = null

        val latestSummary = runCatching {
            weatherModule.getWeatherSummaryUseCase(
                lat = lat,
                lon = lon,
                ttlMs = WEATHER_OVERVIEW_TTL_MS
            )
        }.onFailure {
            errorText = "Falha ao carregar dados climáticos"
        }.getOrNull()
        if (latestSummary != null) {
            summary = latestSummary
            lastWeatherRequestMs = now
        }

        val latestHourly = runCatching {
            weatherModule.getWeatherDetailUseCase(
                lat = lat,
                lon = lon
            )
        }.getOrNull()
        if (latestHourly != null) {
            hourly = latestHourly
        }

        isLoading = false
        isRefreshing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Condições Climáticas") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorText != null -> {
                        Text(errorText!!, color = colorScheme.error)
                    }
                    summary == null -> {
                        Text("Aguardando localização para obter clima.", color = colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        val data = summary!!
                        val nowMs = System.currentTimeMillis()
                        val upcomingHours = hourly.filter { it.timestampMs >= nowMs }
                        val rainNowMm = data.snapshot.rainMm ?: 0.0
                        val rainNextHourMm = upcomingHours.firstOrNull()?.rainMm ?: 0.0
                        val rainNext3hMm = upcomingHours.take(3).sumOf { it.rainMm ?: 0.0 }
                        val maxRainProbNext3h = upcomingHours.take(3)
                            .mapNotNull { it.precipitationProbabilityPercent }
                            .maxOrNull()
                        val imminentRain = rainNextHourMm >= 0.2 || (maxRainProbNext3h ?: 0.0) >= 60.0
                        val windKmh = (data.snapshot.windSpeedMs ?: 0.0) * 3.6
                        val lightningPercent = ((data.snapshot.lightningRisk ?: 0.0) * 100.0)
                        if (isRefreshing) {
                            Text(
                                text = "Atualizando clima...",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = "Atualizado às ${data.snapshot.timestampMs.toHourLabel()} • vento de referência: 10m",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        WeatherHeaderCard(data)
                        Spacer(modifier = Modifier.height(10.dp))
                        CriticalFlightMetricsRow(
                            windKmh = windKmh,
                            rainNowMm = rainNowMm,
                            lightningPercent = lightningPercent
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WeatherMetricCard("Temperatura", "${data.snapshot.temperatureC ?: "-"}°C")
                        WeatherMetricCard(
                            "Vento",
                            MeasurementConverter.formatSpeed((data.snapshot.windSpeedMs ?: 0.0).toFloat(), "METRIC")
                        )
                        WeatherMetricCard("Chuva agora", "${"%.1f".format(rainNowMm)} mm")
                        WeatherMetricCard("Chuva próxima 1h", "${"%.1f".format(rainNextHourMm)} mm")
                        WeatherMetricCard("Chuva acumulada próximas 3h", "${"%.1f".format(rainNext3hMm)} mm")
                        WeatherMetricCard(
                            "Prob. de chuva (próx. 3h)",
                            maxRainProbNext3h?.let { "${"%.0f".format(it)}%" } ?: "N/D"
                        )
                        if (imminentRain && rainNowMm <= 0.1) {
                            WeatherImminentRainAlert()
                        }
                        WeatherMetricCard(
                            "Risco de raio (estimado)",
                            "${"%.0f".format(lightningPercent)}%"
                        )
                        WeatherMetricCard("Fonte", data.snapshot.providerId)
                        if (selectedPrimaryProvider != data.snapshot.providerId) {
                            WeatherProviderFallbackAlert(
                                selectedPrimaryProvider = selectedPrimaryProvider,
                                currentProvider = data.snapshot.providerId
                            )
                        }
                        if (hourly.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Próximas horas",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            hourly.forEach { item ->
                                WeatherHourlyCard(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CriticalFlightMetricsRow(
    windKmh: Double,
    rainNowMm: Double,
    lightningPercent: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CriticalFlightMetricCard(
            modifier = Modifier.weight(1f),
            title = "Vento",
            value = "${windKmh.roundToInt()} km/h",
            color = if (windKmh >= 36.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        CriticalFlightMetricCard(
            modifier = Modifier.weight(1f),
            title = "Chuva",
            value = "${"%.1f".format(rainNowMm)} mm",
            color = if (rainNowMm >= 0.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        CriticalFlightMetricCard(
            modifier = Modifier.weight(1f),
            title = "Raio*",
            value = "${lightningPercent.roundToInt()}%",
            color = if (lightningPercent >= 70.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CriticalFlightMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun WeatherHeaderCard(summary: WeatherSummary) {
    val colorScheme = MaterialTheme.colorScheme
    val (icon, tint) = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Icons.Default.WbSunny to colorScheme.primary
        WeatherSafetyLevel.CAUTION -> Icons.Default.Cloud to colorScheme.tertiary
        WeatherSafetyLevel.NO_FLY -> Icons.Default.Thunderstorm to colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Column {
                Text(summary.decision.shortMessage, fontWeight = FontWeight.Bold, color = tint)
                Text(summary.decision.reasons.joinToString(" • "), color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeatherMetricCard(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colorScheme.onSurface)
            Text(value, fontWeight = FontWeight.Bold, color = colorScheme.primary)
        }
    }
}

@Composable
private fun WeatherImminentRainAlert() {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colorScheme.errorContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.4f))
    ) {
        Text(
            text = "Aviso: sem chuva agora, mas há sinal de chuva iminente. Planeje pouso preventivo.",
            color = colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeatherProviderFallbackAlert(
    selectedPrimaryProvider: String,
    currentProvider: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.4f))
    ) {
        Text(
            text = "Fallback ativo: provider configurado $selectedPrimaryProvider falhou e o app usou $currentProvider.",
            color = colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeatherHourlyCard(snapshot: WeatherSnapshot) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snapshot.timestampMs.toHourLabel(),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${snapshot.temperatureC?.let { String.format("%.1f", it) } ?: "-"}°C",
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vento ${snapshot.windSpeedMs?.let { String.format("%.1f", it * 3.6) } ?: "-"} km/h (10m)",
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Chuva ${snapshot.rainMm?.let { String.format("%.1f", it) } ?: "-"} mm",
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Long.toHourLabel(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

private const val WEATHER_MIN_REFRESH_MS = 5 * 60_000L
private const val WEATHER_OVERVIEW_TTL_MS = 5 * 60_000L
