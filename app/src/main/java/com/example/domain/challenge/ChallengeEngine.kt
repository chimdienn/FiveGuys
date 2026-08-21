package com.example.domain.challenge

import com.example.domain.model.Challenge
import com.example.domain.model.ChallengeType
import com.example.domain.model.DailyChallenge
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * The catalogue of challenges and the rules that decide when one is complete.
 *
 * Assignment is deterministic per (user, day) rather than random: the same user opening
 * the app twice on the same day must see the same three challenges, and a reassignment
 * must never silently discard progress.
 */
object ChallengeEngine {

    const val DAILY_CHALLENGE_COUNT = 3

    val catalogue: List<Challenge> = listOf(
        Challenge(
            id = "ch_walk_5km",
            title = "Walk 5 km outdoors",
            description = "Cover five kilometres on any outdoor route today.",
            type = ChallengeType.DISTANCE,
            target = 5,
            unit = "km",
            rewardCoins = 50
        ),
        Challenge(
            id = "ch_walk_3km",
            title = "Get 3 km in",
            description = "A short outing counts. Three kilometres outdoors.",
            type = ChallengeType.DISTANCE,
            target = 3,
            unit = "km",
            rewardCoins = 30
        ),
        Challenge(
            id = "ch_complete_trail",
            title = "Complete one trail",
            description = "Finish an adventure from start to finish.",
            type = ChallengeType.TRIP_COMPLETE,
            target = 1,
            unit = "trail",
            rewardCoins = 100
        ),
        Challenge(
            id = "ch_group_trip",
            title = "Complete a trip with 3 people",
            description = "Finish a trip with at least three people in the group.",
            type = ChallengeType.GROUP_TRIP,
            target = 3,
            unit = "people",
            rewardCoins = 120
        ),
        Challenge(
            id = "ch_trail_moment",
            title = "Record one trail observation",
            description = "Leave a Trail Moment for the community.",
            type = ChallengeType.TRAIL_MOMENT,
            target = 1,
            unit = "moment",
            rewardCoins = 20
        ),
        Challenge(
            id = "ch_three_moments",
            title = "Report three observations",
            description = "Hazards, water, wildlife — three Trail Moments today.",
            type = ChallengeType.TRAIL_MOMENT,
            target = 3,
            unit = "moments",
            rewardCoins = 60
        ),
        Challenge(
            id = "ch_photo_mountain",
            title = "Photograph a mountain",
            description = "Capture a mountain or a big view.",
            type = ChallengeType.PHOTO,
            target = 1,
            unit = "photo",
            rewardCoins = 50,
            photoSubject = "a mountain, hill or distant summit"
        ),
        Challenge(
            id = "ch_photo_water",
            title = "Photograph water",
            description = "A creek, waterfall, lake or the coast.",
            type = ChallengeType.PHOTO,
            target = 1,
            unit = "photo",
            rewardCoins = 40,
            photoSubject = "a natural body of water such as a creek, waterfall, lake or coastline"
        ),
        Challenge(
            id = "ch_photo_tree",
            title = "Photograph a native tree",
            description = "Find a tree worth a second look.",
            type = ChallengeType.PHOTO,
            target = 1,
            unit = "photo",
            rewardCoins = 40,
            photoSubject = "a tree or dense native vegetation"
        )
    )

    fun challengeById(id: String): Challenge? = catalogue.firstOrNull { it.id == id }

    /**
     * Choose today's challenges for a user.
     *
     * Deterministic on (uid, dateKey) so it is stable across app restarts and across
     * devices, and always includes a mix of types so a user is never handed three photo
     * tasks in a row.
     */
    fun assignmentFor(uid: String, dateKey: String): List<Challenge> {
        val seed = "$uid|$dateKey".hashCode().absoluteValue
        val distance = catalogue.filter { it.type == ChallengeType.DISTANCE }
        val photo = catalogue.filter { it.type == ChallengeType.PHOTO }
        val other = catalogue.filter { it.type !in setOf(ChallengeType.DISTANCE, ChallengeType.PHOTO) }

        return listOfNotNull(
            distance.getOrNull(seed % distance.size),
            other.getOrNull((seed / 7) % other.size),
            photo.getOrNull((seed / 13) % photo.size)
        ).distinctBy { it.id }
    }

    fun dateKey(epochMillis: Long, zoneOffsetMillis: Int): String {
        val days = Math.floorDiv(epochMillis + zoneOffsetMillis, 86_400_000L)
        val date = java.time.LocalDate.ofEpochDay(days)
        return String.format(Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)
    }

    /**
     * Recompute a daily challenge against an activity signal.
     *
     * Progress only ever moves forward, and [DailyChallenge.completedAt] is set exactly
     * once — a later, larger signal will not "re-complete" an already complete challenge
     * and so cannot trigger a second reward.
     */
    fun applyProgress(
        daily: DailyChallenge,
        signal: ActivitySignal,
        nowMillis: Long
    ): DailyChallenge {
        val challenge = challengeById(daily.challengeId) ?: return daily
        val delta = when (challenge.type) {
            ChallengeType.DISTANCE -> signal.distanceKm.toInt()
            ChallengeType.TRIP_COMPLETE -> signal.tripsCompleted
            ChallengeType.GROUP_TRIP -> if (signal.tripsCompleted > 0) signal.groupSize else 0
            ChallengeType.TRAIL_MOMENT -> signal.momentsCreated
            ChallengeType.PHOTO -> signal.photosVerified
            ChallengeType.MANUAL_DEV -> signal.manualIncrement
        }
        if (delta <= 0) return daily

        val newProgress = when (challenge.type) {
            // Group size is a snapshot of one trip, not a running total.
            ChallengeType.GROUP_TRIP -> maxOf(daily.progress, delta)
            else -> daily.progress + delta
        }.coerceAtMost(daily.target)

        val completed = daily.completedAt ?: if (newProgress >= daily.target) nowMillis else null
        return daily.copy(progress = newProgress, completedAt = completed)
    }
}

/**
 * What the user actually did, expressed in units the challenge rules understand.
 *
 * Built by the repository from persisted activity — never supplied directly by a screen,
 * so a UI bug cannot mint progress.
 */
data class ActivitySignal(
    val distanceKm: Double = 0.0,
    val tripsCompleted: Int = 0,
    val groupSize: Int = 0,
    val momentsCreated: Int = 0,
    val photosVerified: Int = 0,
    val manualIncrement: Int = 0
)
