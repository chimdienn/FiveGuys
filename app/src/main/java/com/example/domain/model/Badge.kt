package com.example.domain.model

enum class BadgeId {
    FIRST_STEPS,
    EXPLORER,
    TRAIL_REGULAR,
    SOCIAL_HIKER,
    TRAIL_REPORTER,
    PREPARED
}

data class Badge(
    val id: BadgeId,
    val title: String,
    val description: String,
    val emoji: String
)

data class EarnedBadge(
    val uid: String,
    val badgeId: BadgeId,
    val earnedAt: Long
)
