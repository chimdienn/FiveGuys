package com.example.domain

import com.example.domain.model.Difficulty
import com.example.domain.model.GeoPoint
import com.example.domain.model.Trail
import com.example.domain.model.Weather
import com.example.domain.model.WeatherCondition
import com.example.domain.weather.TrailRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailRankingTest {

    private fun trail(
        id: String,
        difficulty: Difficulty = Difficulty.MODERATE,
        distanceKm: Double = 8.0,
        elevationGainM: Int = 300,
        exposed: Boolean = false,
        shaded: Boolean = false,
        rating: Double = 4.0
    ) = Trail(
        id = id,
        name = id,
        region = "Test",
        difficulty = difficulty,
        distanceKm = distanceKm,
        elevationGainM = elevationGainM,
        start = GeoPoint(-37.8, 145.0),
        isExposed = exposed,
        isShaded = shaded,
        rating = rating
    )

    private fun weather(
        temp: Double = 18.0,
        precipitation: Double = 0.0,
        wind: Double = 8.0,
        condition: WeatherCondition = WeatherCondition.CLEAR
    ) = Weather(
        temperatureC = temp,
        apparentTemperatureC = temp,
        condition = condition,
        precipitationMm = precipitation,
        precipitationProbabilityPercent = null,
        windSpeedKmh = wind,
        windGustKmh = null,
        observedAt = 0L
    )

    @Test
    fun `heavy rain demotes exposed trails below sheltered ones`() {
        val exposed = trail("exposed", exposed = true)
        val sheltered = trail("sheltered", difficulty = Difficulty.EASY)
        val wet = weather(precipitation = 9.0, condition = WeatherCondition.HEAVY_RAIN)

        val ranked = TrailRanking.rank(listOf(exposed, sheltered), wet)
        assertEquals("sheltered", ranked.first().trail.id)
    }

    @Test
    fun `heavy rain demotes hard trails`() {
        val hard = TrailRanking.score(trail("hard", difficulty = Difficulty.CHALLENGING), weather(precipitation = 8.0, condition = WeatherCondition.HEAVY_RAIN))
        val easy = TrailRanking.score(trail("easy", difficulty = Difficulty.EASY), weather(precipitation = 8.0, condition = WeatherCondition.HEAVY_RAIN))
        assertTrue("hard=${hard.score} easy=${easy.score}", easy.score > hard.score)
    }

    @Test
    fun `strong wind demotes exposed trails`() {
        val windy = weather(wind = 45.0)
        val exposed = TrailRanking.score(trail("exposed", exposed = true), windy)
        val sheltered = TrailRanking.score(trail("sheltered"), windy)
        assertTrue(sheltered.score > exposed.score)
    }

    @Test
    fun `heat favours shorter shaded routes`() {
        val hot = weather(temp = 34.0)
        val shortShaded = TrailRanking.score(trail("short", distanceKm = 5.0, shaded = true, elevationGainM = 150), hot)
        val longClimb = TrailRanking.score(trail("long", distanceKm = 18.0, elevationGainM = 900), hot)
        assertTrue("short=${shortShaded.score} long=${longClimb.score}", shortShaded.score > longClimb.score)
    }

    @Test
    fun `comfortable weather rewards highly rated trails`() {
        val fine = weather(temp = 19.0)
        val great = TrailRanking.score(trail("great", rating = 4.9), fine)
        val average = TrailRanking.score(trail("average", rating = 3.5), fine)
        assertTrue(great.score > average.score)
    }

    @Test
    fun `cold weather demotes exposed trails`() {
        val cold = weather(temp = 1.0)
        val exposed = TrailRanking.score(trail("exposed", exposed = true), cold)
        val sheltered = TrailRanking.score(trail("sheltered"), cold)
        assertTrue(sheltered.score > exposed.score)
    }

    @Test
    fun `missing weather still returns a ranking`() {
        val ranked = TrailRanking.rank(listOf(trail("a"), trail("b")), weather = null)
        assertEquals(2, ranked.size)
        assertTrue(ranked.all { it.reasons.isNotEmpty() })
    }

    @Test
    fun `scores stay within range`() {
        val extremes = listOf(
            weather(temp = 45.0, precipitation = 30.0, wind = 90.0, condition = WeatherCondition.THUNDERSTORM),
            weather(temp = -5.0),
            weather()
        )
        val trails = listOf(
            trail("a", exposed = true, difficulty = Difficulty.CHALLENGING, distanceKm = 30.0, elevationGainM = 2000),
            trail("b", shaded = true, difficulty = Difficulty.EASY, distanceKm = 2.0, rating = 5.0)
        )
        for (w in extremes) {
            for (t in trails) {
                val score = TrailRanking.score(t, w).score
                assertTrue("score=$score", score in 0..100)
            }
        }
    }

    @Test
    fun `best for returns the top ranked trail`() {
        val fine = weather()
        val best = TrailRanking.bestFor(listOf(trail("a", rating = 3.0), trail("b", rating = 4.9)), fine)
        assertNotNull(best)
        assertEquals("b", best!!.trail.id)
    }

    @Test
    fun `ranking explains itself`() {
        val result = TrailRanking.score(trail("exposed", exposed = true), weather(wind = 60.0))
        assertTrue(result.reasons.isNotEmpty())
        assertTrue(result.reasons.any { it.contains("wind", ignoreCase = true) })
    }

    @Test
    fun `thunderstorm advisory is present and non committal about safety`() {
        val storm = weather(condition = WeatherCondition.THUNDERSTORM, precipitation = 12.0)
        assertNotNull(storm.advisory)
        assertTrue(TrailRanking.SAFETY_DISCLAIMER.contains("do not replace", ignoreCase = true))
    }
}
