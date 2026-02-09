package com.sloth.registerapp.features.weather.data.provider

import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.features.weather.data.remote.WeatherRetrofitFactory
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot
import com.sloth.registerapp.features.weather.domain.provider.WeatherProvider
import java.text.SimpleDateFormat
import java.util.Locale

class OpenMeteoWeatherProvider : WeatherProvider {
    override val providerId: String = WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO

    override suspend fun getCurrent(lat: Double, lon: Double): WeatherSnapshot {
        val response = WeatherRetrofitFactory.openMeteo().getCurrent(lat, lon)
        val current = response.current ?: throw IllegalStateException("Open-Meteo sem dados atuais")
        return WeatherSnapshot(
            timestampMs = System.currentTimeMillis(),
            temperatureC = current.temperature2m,
            rainMm = current.precipitation,
            precipitationProbabilityPercent = current.precipitationProbability?.toDouble(),
            windSpeedMs = current.windSpeed10m,
            windGustMs = current.windGusts10m,
            lightningRisk = current.weatherCode?.let { if (it in THUNDER_CODES) 0.9 else 0.0 },
            conditionCode = current.weatherCode?.toString(),
            providerId = providerId
        )
    }

    override suspend fun getHourly(lat: Double, lon: Double): List<WeatherSnapshot> {
        val response = WeatherRetrofitFactory.openMeteo().getHourly(lat, lon)
        val hourly = response.hourly ?: return listOf(getCurrent(lat, lon))
        val times = hourly.time.orEmpty()
        if (times.isEmpty()) return listOf(getCurrent(lat, lon))

        return times.indices.take(HOURLY_LIMIT).map { index ->
            WeatherSnapshot(
                timestampMs = parseOpenMeteoTime(times[index]),
                temperatureC = hourly.temperature2m?.getOrNull(index),
                rainMm = hourly.precipitation?.getOrNull(index),
                precipitationProbabilityPercent = hourly.precipitationProbability?.getOrNull(index)?.toDouble(),
                windSpeedMs = hourly.windSpeed10m?.getOrNull(index),
                windGustMs = hourly.windGusts10m?.getOrNull(index),
                lightningRisk = hourly.weatherCode?.getOrNull(index)?.let { if (it in THUNDER_CODES) 0.9 else 0.0 },
                conditionCode = hourly.weatherCode?.getOrNull(index)?.toString(),
                providerId = providerId
            )
        }
    }

    companion object {
        private const val HOURLY_LIMIT = 12
        private val THUNDER_CODES = setOf(95, 96, 99)
    }

    private fun parseOpenMeteoTime(raw: String): Long {
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(raw)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}
