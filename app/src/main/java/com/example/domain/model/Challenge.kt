package com.example.domain.model

/**
 * How a challenge is proven complete.
 *
 * [MANUAL_DEV] exists only so demo builds can exercise the reward path without walking
 * 5 km; it is rejected by the server in non-development configurations.
 */
enum class ChallengeType {
    DISTANCE,
    TRIP_COMPLETE,
    GROUP_TRIP,
    TRAIL_MOMENT,
    PHOTO,
    MANUAL_DEV
}

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    /** Target quantity in the challenge's natural unit (km, trips, moments, photos). */
    val target: Int,
    val unit: String,
    val rewardCoins: Int,
    /** For [ChallengeType.PHOTO]: what the photo must show, in plain language. */
    val photoSubject: String? = null
)

/** One user's assignment of one [Challenge] for one calendar day. */
data class DailyChallenge(
    val id: String,
    val uid: String,
    val challengeId: String,
    /** Local date key, `yyyy-MM-dd`, so a day's assignment is stable and idempotent. */
    val dateKey: String,
    val progress: Int = 0,
    val target: Int,
    val completedAt: Long? = null,
    val rewardedAt: Long? = null
) {
    val isComplete: Boolean get() = completedAt != null
    val isRewarded: Boolean get() = rewardedAt != null
    val progressFraction: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

enum class SubmissionState { PENDING, PASSED, FAILED }

/**
 * A photo submitted against a challenge.
 *
 * Once created the record is immutable (spec section 46): the user may retake freely
 * *before* submitting, but a submission cannot be replaced afterwards. The repository
 * refuses to write a second submission for the same [dailyChallengeId].
 */
data class ChallengeSubmission(
    val id: String,
    val uid: String,
    val dailyChallengeId: String,
    val challengeId: String,
    val photoUrl: String?,
    val state: SubmissionState,
    val confidence: Float? = null,
    val explanation: String? = null,
    val submittedAt: Long,
    val verifiedAt: Long? = null
)

/**
 * A single, immutable movement of BioCoins.
 *
 * The balance is the sum of these rows, never a directly-writable number. [idempotencyKey]
 * is unique per (user, reason, reference) so a retried or replayed award cannot pay twice
 * (spec section 42).
 */
data class CoinTransaction(
    val id: String,
    val uid: String,
    val amount: Int,
    val reason: String,
    val challengeId: String? = null,
    val referenceId: String? = null,
    val idempotencyKey: String,
    val createdAt: Long
)
