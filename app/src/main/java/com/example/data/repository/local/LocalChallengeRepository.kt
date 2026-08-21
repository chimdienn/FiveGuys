package com.example.data.repository.local

import android.util.Log
import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.ai.PhotoVerificationService
import com.example.domain.challenge.ActivitySignal
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.ChallengeSubmission
import com.example.domain.model.DailyChallenge
import com.example.domain.model.SubmissionState
import com.example.domain.repository.AwardOutcome
import com.example.domain.repository.AwardedChallenge
import com.example.domain.repository.ChallengeRepository
import com.example.domain.repository.DailyChallengeView
import com.example.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Daily challenge assignment, progress and payout.
 *
 * Two invariants shape this class:
 *
 * 1. A challenge is *awarded* exactly once. The reward call carries an idempotency key
 *    derived from the daily-challenge id, and `rewardedAt` is only stamped after the
 *    ledger confirms the write, so a crash between the two leaves the system able to
 *    retry without paying twice.
 * 2. A photo submission is immutable. `submitPhoto` refuses outright if a submission row
 *    already exists, rather than updating it (spec section 46).
 */
class LocalChallengeRepository(
    private val dao: BiomateDaoV2,
    private val rewards: RewardRepository,
    private val photoVerification: PhotoVerificationService,
    private val photoStore: PhotoStore,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ChallengeRepository {

    override fun observeDailyChallenges(uid: String, dateKey: String): Flow<List<DailyChallengeView>> =
        dao.observeDailyChallenges(uid, dateKey).map { rows ->
            rows.mapNotNull { row ->
                val challenge = ChallengeEngine.challengeById(row.challengeId) ?: return@mapNotNull null
                DailyChallengeView(row.toDomain(), challenge)
            }.sortedBy { it.challenge.rewardCoins }
        }

    override fun observeSubmission(dailyChallengeId: String): Flow<ChallengeSubmission?> =
        dao.observeSubmission(dailyChallengeId).map { it?.toDomain() }

    /**
     * Assigns today's challenges if they are not already assigned.
     *
     * `INSERT OR IGNORE` on a deterministic id makes this safe to call from several
     * screens on the same day without resetting anyone's progress.
     */
    override suspend fun ensureAssigned(uid: String, dateKey: String) {
        val existing = dao.getDailyChallenges(uid, dateKey)
        if (existing.size >= ChallengeEngine.DAILY_CHALLENGE_COUNT) return

        val assignment = ChallengeEngine.assignmentFor(uid, dateKey)
        dao.insertDailyChallengesIfAbsent(
            assignment.map { challenge ->
                DailyChallenge(
                    id = dailyId(uid, dateKey, challenge.id),
                    uid = uid,
                    challengeId = challenge.id,
                    dateKey = dateKey,
                    target = challenge.target
                ).toEntity()
            }
        )
    }

    override suspend fun applyActivity(
        uid: String,
        dateKey: String,
        signal: ActivitySignal
    ): List<AwardedChallenge> {
        ensureAssigned(uid, dateKey)
        val now = nowMillis()
        val awarded = mutableListOf<AwardedChallenge>()

        for (row in dao.getDailyChallenges(uid, dateKey)) {
            val challenge = ChallengeEngine.challengeById(row.challengeId) ?: continue
            val before = row.toDomain()
            val after = ChallengeEngine.applyProgress(before, signal, now)
            if (after == before) continue

            dao.upsertDailyChallenge(after.toEntity())

            if (after.isComplete && !after.isRewarded) {
                grant(uid, after, challenge.rewardCoins, challenge.title)?.let { awarded += it }
            }
        }
        return awarded
    }

    /**
     * Pays out a completed challenge, once.
     *
     * `rewardedAt` is written only after the ledger reports a grant, and the ledger itself
     * rejects a repeated idempotency key — so neither a duplicate call nor a crash between
     * the two writes can produce a second payment.
     */
    private suspend fun grant(
        uid: String,
        daily: DailyChallenge,
        coins: Int,
        title: String
    ): AwardedChallenge? {
        val outcome = rewards.award(
            uid = uid,
            amount = coins,
            reason = "Challenge complete: $title",
            idempotencyKey = "challenge:${daily.id}",
            challengeId = daily.challengeId,
            referenceId = daily.id
        ).getOrNull() ?: return null

        return when (outcome) {
            is AwardOutcome.Granted -> {
                dao.upsertDailyChallenge(daily.copy(rewardedAt = outcome.transaction.createdAt).toEntity())
                ChallengeEngine.challengeById(daily.challengeId)?.let { AwardedChallenge(it, coins) }
            }
            is AwardOutcome.AlreadyAwarded -> {
                // Reconcile local state with the ledger; no coins move.
                dao.upsertDailyChallenge(daily.copy(rewardedAt = outcome.existing.createdAt).toEntity())
                null
            }
            is AwardOutcome.Rejected -> {
                Log.w(TAG, "Award rejected for ${daily.id}: ${outcome.reason}")
                null
            }
        }
    }

    override suspend fun submitPhoto(
        uid: String,
        dailyChallengeId: String,
        photoBytes: ByteArray
    ): Result<ChallengeSubmission> = runCatching {
        val daily = dao.getDailyChallenge(dailyChallengeId)?.toDomain()
            ?: error("That challenge is not assigned to you.")
        require(daily.uid == uid) { "That challenge belongs to someone else." }

        // The immutability rule. A user may retake as often as they like before this
        // point; once a submission exists it is final.
        dao.getSubmissionFor(dailyChallengeId)?.let {
            error("You have already submitted a photo for this challenge.")
        }

        val challenge = ChallengeEngine.challengeById(daily.challengeId)
            ?: error("Unknown challenge.")

        val photoUrl = photoStore.save(uid, "challenge-submissions", photoBytes).getOrNull()
        val submissionId = "sub_" + UUID.randomUUID().toString().replace("-", "").take(20)

        val pending = ChallengeSubmission(
            id = submissionId,
            uid = uid,
            dailyChallengeId = dailyChallengeId,
            challengeId = daily.challengeId,
            photoUrl = photoUrl,
            state = SubmissionState.PENDING,
            submittedAt = nowMillis()
        )
        dao.insertSubmission(pending.toEntity())

        val verdict = photoVerification.verify(photoBytes, challenge)
        val state = if (verdict.passed) SubmissionState.PASSED else SubmissionState.FAILED
        val verifiedAt = nowMillis()
        dao.setSubmissionVerdict(
            id = submissionId,
            state = state.name,
            confidence = verdict.confidence,
            explanation = verdict.explanation,
            verifiedAt = verifiedAt
        )

        if (verdict.passed) {
            val completed = daily.copy(
                progress = daily.target,
                completedAt = daily.completedAt ?: verifiedAt
            )
            dao.upsertDailyChallenge(completed.toEntity())
            grant(uid, completed, challenge.rewardCoins, challenge.title)
        }

        pending.copy(
            state = state,
            confidence = verdict.confidence,
            explanation = verdict.explanation,
            verifiedAt = verifiedAt
        )
    }

    private fun dailyId(uid: String, dateKey: String, challengeId: String) = "$uid|$dateKey|$challengeId"

    private companion object {
        const val TAG = "ChallengeRepository"
    }
}
