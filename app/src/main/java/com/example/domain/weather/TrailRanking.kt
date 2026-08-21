package com.example.domain.weather

import com.example.domain.model.Difficulty
import com.example.domain.model.Trail
import com.example.domain.model.Weather
import com.example.domain.model.WeatherCondition

/**
 * Rule-based trail ranking for the current weather.
 *
 * Explicitly not ML (spec section 16). Each trail starts from a neutral base score and
 * accumulates signed adjustments from a small set of legible rules; the reasons shown to
 * the user are the same rules that moved the number, so the ranking can always be
 * explained rather than merely asserted.
 *
 * The output is a preference ordering, not a safety judgement — see [SAFETY_DISCLAIMER].
 */
object TrailRanking {

    const val SAFETY_DISCLAIMER =
        "Outdoor conditions change quickly. Recommendations are informational and do not " +
            "replace official park and weather advice."

    private const val BASE_SCORE = 60

    fun rank(trails: List<Trail>, weather: Weather?): List<TrailRecommendation> =
        trails.map { score(it, weather) }.sortedByDescending { it.score }

    fun bestFor(trails: List<Trail>, weather: Weather?): TrailRecommendation? =
        rank(trails, weather).firstOrNull()

    fun score(trail: Trail, weather: Weather?): TrailRecommendation {
        if (weather == null) {
            return TrailRecommendation(trail, BASE_SCORE, listOf("Weather unavailable — showing general ranking"))
        }

        var score = BASE_SCORE
        val reasons = mutableListOf<String>()

        // --- Wet weather -------------------------------------------------------------
        val heavyRain = weather.precipitationMm >= 5.0 ||
            weather.condition == WeatherCondition.HEAVY_RAIN ||
            weather.condition == WeatherCondition.THUNDERSTORM
        if (heavyRain) {
            if (trail.isExposed) {
                score -= 25
                reasons += "Exposed route in heavy rain"
            }
            if (trail.difficulty >= Difficulty.HARD) {
                score -= 15
                reasons += "Hard route while wet"
            }
            if (trail.steepnessMPerKm > 80) {
                score -= 12
                reasons += "Steep and slippery when wet"
            }
            if (!trail.isExposed && trail.difficulty <= Difficulty.MODERATE) {
                score += 8
                reasons += "Sheltered, easier option for wet weather"
            }
        } else if (weather.isWet) {
            if (trail.isExposed) {
                score -= 10
                reasons += "Exposed route with rain about"
            }
        }

        // --- Wind --------------------------------------------------------------------
        if (weather.isWindy) {
            if (trail.isExposed) {
                score -= 20
                reasons += "Strong wind on an exposed route"
            } else {
                score += 5
                reasons += "Sheltered from the wind"
            }
        }

        // --- Heat --------------------------------------------------------------------
        if (weather.isHot) {
            if (trail.isShaded) {
                score += 12
                reasons += "Shaded route for a warm day"
            }
            if (trail.distanceKm > 12) {
                score -= 14
                reasons += "Long route in the heat"
            } else if (trail.distanceKm <= 8) {
                score += 8
                reasons += "Shorter route suits the heat"
            }
            if (trail.elevationGainM > 600) {
                score -= 12
                reasons += "Big climb in warm conditions"
            }
        }

        // --- Cold --------------------------------------------------------------------
        if (weather.isCold && trail.isExposed) {
            score -= 12
            reasons += "Exposed and cold — wind chill will bite"
        }

        // --- Fine weather ------------------------------------------------------------
        val fine = !weather.isWet && !weather.isWindy && !weather.isHot && !weather.isCold
        if (fine) {
            score += 10
            reasons += "Good conditions for this route"
            if (trail.rating >= 4.6) {
                score += 5
                reasons += "Highly rated"
            }
        }

        return TrailRecommendation(trail, score.coerceIn(0, 100), reasons.take(3))
    }
}

data class TrailRecommendation(
    val trail: Trail,
    /** 0..100 preference score for the current conditions. Not a safety rating. */
    val score: Int,
    val reasons: List<String>
)
