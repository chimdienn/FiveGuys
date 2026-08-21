package com.example.data

import com.example.data.ai.MockPhotoVerificationService
import com.example.data.local.BiomateDatabase
import com.example.data.repository.local.LocalChallengeRepository
import com.example.data.repository.local.LocalRewardRepository
import com.example.data.repository.local.PhotoStore
import com.example.domain.challenge.ActivitySignal
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.SubmissionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Challenge completion, photo submission immutability, and the guarantee that a completed
 * challenge pays exactly once no matter how the completion is reached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChallengeRewardTest {

    private lateinit var database: BiomateDatabase
    private lateinit var rewards: LocalRewardRepository
    private lateinit var challenges: LocalChallengeRepository

    private val uid = "u_alex"
    private val dateKey = "2026-08-22"

    /** Records what it was given so tests can assert the photo was actually stored. */
    private class RecordingPhotoStore : PhotoStore {
        var saved = 0
        override suspend fun save(uid: String, folder: String, bytes: ByteArray): Result<String> {
            saved++
            return Result.success("file:///photos/$folder/$uid/$saved.jpg")
        }
    }

    private lateinit var photoStore: RecordingPhotoStore

    @Before
    fun setUp() {
        database = RepositoryTestHarness.inMemoryDatabase()
        val dao = RepositoryTestHarness.dao(database)
        rewards = LocalRewardRepository(dao)
        photoStore = RecordingPhotoStore()
        challenges = LocalChallengeRepository(
            dao = dao,
            rewards = rewards,
            // No artificial delay: the verifier's determinism is what matters here.
            photoVerification = MockPhotoVerificationService(artificialDelayMs = 0),
            photoStore = photoStore
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Bytes that the deterministic mock verifier passes for the given challenge. */
    private fun bytesThatPass(challengeId: String): ByteArray =
        findBytes(challengeId, shouldPass = true)

    private fun bytesThatFail(challengeId: String): ByteArray =
        findBytes(challengeId, shouldPass = false)

    private fun findBytes(challengeId: String, shouldPass: Boolean): ByteArray {
        val verifier = MockPhotoVerificationService(artificialDelayMs = 0)
        for (seed in 0..500) {
            val candidate = ByteArray(2048) { ((it + seed) % 251).toByte() }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(candidate + challengeId.toByteArray())
            val passes = (digest[0].toInt() and 0xFF) % 4 != 0
            if (passes == shouldPass) return candidate
        }
        error("No suitable image found for $challengeId")
    }

    @Test
    fun `challenges are assigned once per day`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        challenges.ensureAssigned(uid, dateKey)

        val assigned = challenges.observeDailyChallenges(uid, dateKey).first()
        assertEquals(ChallengeEngine.DAILY_CHALLENGE_COUNT, assigned.size)
    }

    @Test
    fun `reassignment does not reset progress`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = 2.0))

        val before = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }.daily.progress

        challenges.ensureAssigned(uid, dateKey)

        val after = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }.daily.progress
        assertEquals(before, after)
    }

    @Test
    fun `completing a challenge awards its coins once`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val distanceChallenge = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }

        val awarded = challenges.applyActivity(
            uid, dateKey, ActivitySignal(distanceKm = distanceChallenge.challenge.target.toDouble())
        )

        assertTrue(awarded.any { it.challenge.id == distanceChallenge.challenge.id })
        assertEquals(distanceChallenge.challenge.rewardCoins, rewards.observeBalance(uid).first())
    }

    @Test
    fun `re-applying the same activity does not pay twice`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val target = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }.challenge

        challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = target.target.toDouble()))
        val balanceAfterFirst = rewards.observeBalance(uid).first()

        repeat(5) {
            challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = target.target.toDouble()))
        }

        assertEquals(balanceAfterFirst, rewards.observeBalance(uid).first())
    }

    @Test
    fun `a completed challenge produces exactly one ledger entry`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val target = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }.challenge

        repeat(4) {
            challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = target.target.toDouble()))
        }

        val entries = rewards.observeTransactions(uid).first()
            .filter { it.challengeId == target.id }
        assertEquals(1, entries.size)
    }

    @Test
    fun `a completed challenge is stamped as rewarded`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val target = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }.challenge

        challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = target.target.toDouble()))

        val daily = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.id == target.id }.daily
        assertTrue(daily.isComplete)
        assertTrue(daily.isRewarded)
    }

    // ---- Photo submissions -----------------------------------------------------------

    @Test
    fun `a passing photo completes the challenge and stores the image`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        val submission = challenges.submitPhoto(
            uid, photo.daily.id, bytesThatPass(photo.challenge.id)
        ).getOrThrow()

        assertEquals(SubmissionState.PASSED, submission.state)
        assertEquals(1, photoStore.saved)
        assertNotNull(submission.photoUrl)
        assertTrue(rewards.observeBalance(uid).first() >= photo.challenge.rewardCoins)
    }

    @Test
    fun `a failing photo does not award coins`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        val submission = challenges.submitPhoto(
            uid, photo.daily.id, bytesThatFail(photo.challenge.id)
        ).getOrThrow()

        assertEquals(SubmissionState.FAILED, submission.state)
        assertEquals(0, rewards.observeBalance(uid).first())
    }

    @Test
    fun `a submission is immutable once made`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        challenges.submitPhoto(uid, photo.daily.id, bytesThatFail(photo.challenge.id)).getOrThrow()

        // A user cannot resubmit a passing photo after seeing a fail (spec section 46).
        val second = challenges.submitPhoto(uid, photo.daily.id, bytesThatPass(photo.challenge.id))
        assertTrue(second.isFailure)
        assertTrue(second.exceptionOrNull()?.message?.contains("already", ignoreCase = true) == true)
    }

    @Test
    fun `a failed submission stays failed`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        challenges.submitPhoto(uid, photo.daily.id, bytesThatFail(photo.challenge.id))
        challenges.submitPhoto(uid, photo.daily.id, bytesThatPass(photo.challenge.id))

        assertEquals(
            SubmissionState.FAILED,
            challenges.observeSubmission(photo.daily.id).first()?.state
        )
        assertEquals(0, rewards.observeBalance(uid).first())
    }

    @Test
    fun `a user cannot submit against someone else's challenge`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        val result = challenges.submitPhoto("u_someone_else", photo.daily.id, bytesThatPass(photo.challenge.id))
        assertTrue(result.isFailure)
    }

    @Test
    fun `submitting to an unknown challenge fails`() = runTest {
        val result = challenges.submitPhoto(uid, "not_a_real_id", ByteArray(2048))
        assertTrue(result.isFailure)
    }

    @Test
    fun `a tiny image always fails verification`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val photo = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.photoSubject != null }

        // Not a coin toss: too few bytes to be a camera frame is a real signal.
        val submission = challenges.submitPhoto(uid, photo.daily.id, ByteArray(10)).getOrThrow()
        assertEquals(SubmissionState.FAILED, submission.state)
    }

    @Test
    fun `awards are scoped per user`() = runTest {
        challenges.ensureAssigned("u_a", dateKey)
        challenges.ensureAssigned("u_b", dateKey)

        challenges.applyActivity("u_a", dateKey, ActivitySignal(distanceKm = 99.0))

        assertTrue(rewards.observeBalance("u_a").first() > 0)
        assertEquals(0, rewards.observeBalance("u_b").first())
    }

    @Test
    fun `mock verification is deterministic`() = runTest {
        val verifier = MockPhotoVerificationService(artificialDelayMs = 0)
        val challenge = ChallengeEngine.challengeById("ch_photo_mountain")!!
        val image = ByteArray(4096) { it.toByte() }

        val first = verifier.verify(image, challenge)
        val second = verifier.verify(image, challenge)

        assertEquals(first.passed, second.passed)
        assertEquals(first.confidence, second.confidence, 0.0001f)
    }

    @Test
    fun `mock verification is not a rubber stamp`() = runTest {
        val verifier = MockPhotoVerificationService(artificialDelayMs = 0)
        val challenge = ChallengeEngine.challengeById("ch_photo_mountain")!!

        // Both outcomes must be reachable, or the failure UI never gets exercised.
        val outcomes = (0 until 60).map { seed ->
            verifier.verify(ByteArray(2048) { ((it + seed * 7) % 251).toByte() }, challenge).passed
        }.toSet()

        assertTrue(outcomes.contains(true))
        assertTrue(outcomes.contains(false))
    }

    @Test
    fun `a challenge not yet complete is not rewarded`() = runTest {
        challenges.ensureAssigned(uid, dateKey)
        val target = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.type.name == "DISTANCE" }

        challenges.applyActivity(uid, dateKey, ActivitySignal(distanceKm = 1.0))

        val daily = challenges.observeDailyChallenges(uid, dateKey).first()
            .first { it.challenge.id == target.challenge.id }.daily
        assertFalse(daily.isComplete)
        assertFalse(daily.isRewarded)
        assertEquals(0, rewards.observeBalance(uid).first())
    }
}
