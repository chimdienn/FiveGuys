package com.example.domain.model

/**
 * A user's cloud profile, keyed by Firebase UID.
 *
 * Note what is *absent*: there is no password field. Credentials live only in Firebase
 * Authentication (spec section 8). There is also no precise home coordinate — [homeArea]
 * is a free-text approximate locality such as "Melbourne, Victoria" and is the only
 * home-location signal the app ever stores or shows (spec section 64).
 */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val birthYear: Int? = null,
    val gender: String? = null,
    val homeArea: String? = null,
    val fitnessLevel: FitnessLevel = FitnessLevel.MODERATE,
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val preferredPace: PreferredPace = PreferredPace.MODERATE,
    val socialStyles: Set<SocialStyle> = emptySet(),
    val interests: Set<ActivityType> = emptySet(),
    val skills: Set<Skill> = emptySet(),
    val avatarColorHex: Long = 0xFFCD744C,
    val onboardingComplete: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val initials: String
        get() = displayName.trim().split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }

    /** Approximate age, or null when the user chose not to share a birth year. */
    fun approximateAge(currentYear: Int): Int? = birthYear?.let { currentYear - it }?.takeIf { it in 5..120 }
}

/**
 * Aggregated, derived statistics for a user.
 *
 * Every field here is computed from persisted activity (completed sessions, moments,
 * coin transactions) rather than stored as an editable number, so the client cannot
 * inflate its own reputation (spec section 53).
 */
data class UserStats(
    val uid: String,
    val trailsCompleted: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationMinutes: Long = 0L,
    val tripsCompleted: Int = 0,
    val groupTripsCompleted: Int = 0,
    val groupTripsJoined: Int = 0,
    val trailMomentsCreated: Int = 0,
    val readinessChecklistsCompleted: Int = 0,
    val bioCoins: Int = 0,
    val badgeCount: Int = 0
) {
    /**
     * attended group trips / joined group trips, as a percentage.
     *
     * Returns null rather than 0 when the user has never joined a group trip — "no data"
     * and "never turned up" are different claims and the UI must not conflate them.
     */
    val attendanceRatePercent: Int?
        get() = if (groupTripsJoined <= 0) null
        else ((groupTripsCompleted.toDouble() / groupTripsJoined) * 100).toInt().coerceIn(0, 100)
}
