package com.example.data.weather

import com.example.domain.model.Weather
import com.example.domain.model.WeatherCondition
import com.example.domain.repository.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Live weather from Open-Meteo.
 *
 * Open-Meteo is a free, key-less public API, which keeps the MVP runnable with no
 * credential setup. All of the provider-specific detail — the URL shape, the WMO weather
 * codes, the JSON field names — is confined to this file; everything upstream sees only
 * the [Weather] domain model (spec section 15).
 *
 * Results are cached briefly in memory: the Home screen, the trail detail screen and the
 * ranking engine all ask for the same coordinates within a second of each other, and the
 * weather does not change between those calls.
 */
class OpenMeteoWeatherService(
    private val client: OkHttpClient = defaultClient(),
    private val nowMillis: () -> Long = System::currentTimeMillis
) : WeatherService {

    private data class CacheEntry(val weather: Weather, val fetchedAt: Long)

    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheLock = Any()

    override suspend fun getWeather(latitude: Double, longitude: Double): Result<Weather> =
        withContext(Dispatchers.IO) {
            val key = cacheKey(latitude, longitude)
            synchronized(cacheLock) {
                cache[key]?.let { entry ->
                    if (nowMillis() - entry.fetchedAt < CACHE_TTL_MS) return@withContext Result.success(entry.weather)
                }
            }

            runCatching {
                val url = buildString {
                    append(BASE_URL)
                    append("?latitude=").append(latitude)
                    append("&longitude=").append(longitude)
                    append("&current=temperature_2m,apparent_temperature,precipitation,weather_code,")
                    append("wind_speed_10m,wind_gusts_10m")
                    append("&hourly=precipitation_probability")
                    append("&forecast_days=1&timezone=auto")
                }

                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Weather service returned ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isEmpty()) error("Weather service returned an empty response")
                    parse(body, latitude, longitude)
                }
            }.onSuccess { weather ->
                synchronized(cacheLock) { cache[key] = CacheEntry(weather, nowMillis()) }
            }
        }

    private fun parse(body: String, latitude: Double, longitude: Double): Weather {
        val json = JSONObject(body)
        val current = json.optJSONObject("current") ?: error("Weather response had no current conditions")

        val probability = json.optJSONObject("hourly")
            ?.optJSONArray("precipitation_probability")
            ?.let { if (it.length() > 0) it.optInt(0) else null }

        return Weather(
            temperatureC = current.optDouble("temperature_2m", Double.NaN).orZero(),
            apparentTemperatureC = current.optDouble("apparent_temperature", Double.NaN)
                .takeIf { !it.isNaN() } ?: current.optDouble("temperature_2m", 0.0),
            condition = wmoToCondition(current.optInt("weather_code", -1)),
            precipitationMm = current.optDouble("precipitation", 0.0).orZero(),
            precipitationProbabilityPercent = probability,
            windSpeedKmh = current.optDouble("wind_speed_10m", 0.0).orZero(),
            windGustKmh = current.optDouble("wind_gusts_10m", Double.NaN).takeIf { !it.isNaN() },
            observedAt = nowMillis(),
            locationLabel = null
        )
    }

    private fun Double.orZero() = if (isNaN()) 0.0 else this

    private fun cacheKey(lat: Double, lng: Double) = "%.2f,%.2f".format(lat, lng)

    /**
     * Maps WMO weather interpretation codes to the app's condition vocabulary.
     *
     * See https://open-meteo.com/en/docs — the codes are grouped rather than mapped
     * one-to-one because the UI only distinguishes conditions that change a hiking
     * decision.
     */
    private fun wmoToCondition(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        51, 53, 56, 61, 66, 80 -> WeatherCondition.LIGHT_RAIN
        55, 63, 81 -> WeatherCondition.RAIN
        65, 67, 82 -> WeatherCondition.HEAVY_RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
