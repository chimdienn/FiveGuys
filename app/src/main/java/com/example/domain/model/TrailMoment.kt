package com.example.domain.model

enum class MomentCategory(val label: String) {
    HAZARD("Hazard"),
    NOTE("Note"),
    PHOTO("Photo"),
    WILDLIFE("Wildlife"),
    VIEWPOINT("Viewpoint"),
    WATER("Water"),
    TRAIL_CONDITION("Trail Condition");

    companion object {
        fun fromNameOrNull(value: String?): MomentCategory? = entries.firstOrNull { it.name == value }
    }
}

enum class MomentVisibility { PUBLIC, CONNECTIONS, TRIP }

/**
 * An observation left at the author's real physical location.
 *
 * The coordinates are always the device's verified position at creation time — Biomate
 * deliberately offers no arbitrary map-pin placement (spec section 34), because a hazard
 * report is only worth anything if it is where it says it is.
 */
data class TrailMoment(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val trailId: String,
    val tripId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: MomentCategory,
    val description: String,
    val photoUrl: String? = null,
    val visibility: MomentVisibility = MomentVisibility.PUBLIC,
    val createdAt: Long,
    val upvotes: Int = 0
) {
    val location: GeoPoint get() = GeoPoint(latitude, longitude)

    /**
     * Hazards and trail conditions decay; a viewpoint does not.
     *
     * Used by the UI to visually de-emphasise stale reports rather than hide them, so an
     * old report is never presented as current (spec section 37).
     */
    val isTimeSensitive: Boolean
        get() = category == MomentCategory.HAZARD ||
            category == MomentCategory.TRAIL_CONDITION ||
            category == MomentCategory.WATER

    fun ageLabel(nowMillis: Long): String {
        val deltaMs = (nowMillis - createdAt).coerceAtLeast(0)
        val minutes = deltaMs / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minute${plural(minutes)} ago"
            hours < 24 -> "$hours hour${plural(hours)} ago"
            days < 30 -> "$days day${plural(days)} ago"
            else -> "${days / 30} month${plural(days / 30)} ago"
        }
    }

    private fun plural(n: Long) = if (n == 1L) "" else "s"
}
