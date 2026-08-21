package com.example.domain.model

enum class WeatherCondition(val label: String) {
    CLEAR("Clear"),
    PARTLY_CLOUDY("Partly cloudy"),
    CLOUDY("Cloudy"),
    FOG("Fog"),
    LIGHT_RAIN("Light rain"),
    RAIN("Rain"),
    HEAVY_RAIN("Heavy rain"),
    SNOW("Snow"),
    THUNDERSTORM("Thunderstorm"),
    UNKNOWN("Unknown")
}

data class Weather(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val condition: WeatherCondition,
    val precipitationMm: Double,
    val precipitationProbabilityPercent: Int?,
    val windSpeedKmh: Double,
    val windGustKmh: Double?,
    val observedAt: Long,
    val locationLabel: String? = null
) {
    val isWet: Boolean get() = precipitationMm >= 0.5 ||
        condition in setOf(WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN, WeatherCondition.THUNDERSTORM)

    val isWindy: Boolean get() = windSpeedKmh >= 30.0 || (windGustKmh ?: 0.0) >= 45.0

    val isHot: Boolean get() = apparentTemperatureC >= 28.0

    val isCold: Boolean get() = apparentTemperatureC <= 4.0

    /**
     * A short, non-authoritative caution string, or null when nothing stands out.
     *
     * Deliberately worded as guidance: Biomate does not and cannot guarantee safety
     * (spec sections 16 and 96).
     */
    val advisory: String?
        get() = when {
            condition == WeatherCondition.THUNDERSTORM -> "Thunderstorms about — exposed ridgelines are a poor choice today."
            precipitationMm >= 5.0 -> "Heavy rain — expect slippery rock, mud and higher creek levels."
            isWindy -> "Strong wind — exposed sections will feel much harder."
            apparentTemperatureC >= 33.0 -> "Very warm — carry extra water and favour shaded, shorter routes."
            isCold -> "Cold — layers and a windproof shell are worth packing."
            else -> null
        }
}
