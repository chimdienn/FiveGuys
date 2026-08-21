package com.example.domain.model

/** A single point on the earth. Used for trail routes, moments and live position. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/** A named point of interest along a trail route. */
data class TrailWaypoint(
    val name: String,
    val kmMarker: Double,
    val elevationM: Int,
    val type: String
)

data class Trail(
    val id: String,
    val name: String,
    val region: String,
    val stateOrCountry: String = "Victoria, Australia",
    val activityTypes: Set<ActivityType> = setOf(ActivityType.HIKING),
    val description: String = "",
    val difficulty: Difficulty = Difficulty.MODERATE,
    val distanceKm: Double = 0.0,
    val elevationGainM: Int = 0,
    val estimatedMinutes: Int = 0,
    val start: GeoPoint,
    /** Ordered polyline describing the route. May be empty for trails without a mapped route. */
    val route: List<GeoPoint> = emptyList(),
    val waypoints: List<TrailWaypoint> = emptyList(),
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    /** True when the route runs mostly above tree line or along cliffs — used by weather ranking. */
    val isExposed: Boolean = false,
    /** True when the route is predominantly under canopy — used by hot-weather ranking. */
    val isShaded: Boolean = false,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val highlights: List<String> = emptyList(),
    val recommendedGear: List<String> = emptyList(),
    val createdAt: Long = 0L
) {
    val estimatedDurationLabel: String
        get() {
            val h = estimatedMinutes / 60
            val m = estimatedMinutes % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }

    /** Average metres of climb per kilometre — a rough steepness proxy for the ranking rules. */
    val steepnessMPerKm: Double
        get() = if (distanceKm > 0.0) elevationGainM / distanceKm else 0.0
}
