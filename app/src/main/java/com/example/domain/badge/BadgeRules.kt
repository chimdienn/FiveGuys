package com.example.domain.badge

import com.example.domain.model.Badge
import com.example.domain.model.BadgeId
import com.example.domain.model.UserStats

/**
 * A single, isolated badge criterion (spec section 44).
 *
 * Keeping each threshold behind its own [BadgeRule] means a new badge is a new object in
 * [BadgeRules.all] rather than another branch in a growing `when`.
 */
interface BadgeRule {
    val badge: Badge
    fun evaluate(stats: UserStats): Boolean
}

object BadgeRules {

    private fun rule(
        id: BadgeId,
        title: String,
        description: String,
        emoji: String,
        predicate: (UserStats) -> Boolean
    ): BadgeRule = object : BadgeRule {
        override val badge = Badge(id, title, description, emoji)
        override fun evaluate(stats: UserStats) = predicate(stats)
    }

    val all: List<BadgeRule> = listOf(
        rule(BadgeId.FIRST_STEPS, "First Steps", "Complete your first trail", "👟") {
            it.trailsCompleted >= 1
        },
        rule(BadgeId.EXPLORER, "Explorer", "Complete five trails", "🧭") {
            it.trailsCompleted >= 5
        },
        rule(BadgeId.TRAIL_REGULAR, "Trail Regular", "Explore 50 km", "⛰️") {
            it.totalDistanceKm >= 50.0
        },
        rule(BadgeId.SOCIAL_HIKER, "Social Hiker", "Complete three group trips", "👥") {
            it.groupTripsCompleted >= 3
        },
        rule(BadgeId.TRAIL_REPORTER, "Trail Reporter", "Create ten Trail Moments", "📍") {
            it.trailMomentsCreated >= 10
        },
        rule(BadgeId.PREPARED, "Prepared", "Complete the readiness checklist for five trips", "✅") {
            it.readinessChecklistsCompleted >= 5
        }
    )

    fun byId(id: BadgeId): Badge = all.first { it.badge.id == id }.badge

    /** Every badge the stats currently satisfy. */
    fun evaluateAll(stats: UserStats): Set<BadgeId> =
        all.filter { it.evaluate(stats) }.map { it.badge.id }.toSet()

    /** Badges satisfied now that were not already held — i.e. what to award. */
    fun newlyEarned(stats: UserStats, alreadyEarned: Set<BadgeId>): Set<BadgeId> =
        evaluateAll(stats) - alreadyEarned
}
