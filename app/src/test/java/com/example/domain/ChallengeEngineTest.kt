package com.example.domain

import com.example.domain.challenge.ActivitySignal
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.ChallengeType
import com.example.domain.model.DailyChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeEngineTest {

    private fun daily(challengeId: String, target: Int, progress: Int = 0, completedAt: Long? = null) =
        DailyChallenge(
            id = "d1",
            uid = "u1",
            challengeId = challengeId,
            dateKey = "2026-08-22",
            progress = progress,
            target = target,
            completedAt = completedAt
        )

    @Test
    fun `assignment is deterministic for the same user and day`() {
        val first = ChallengeEngine.assignmentFor("user_a", "2026-08-22").map { it.id }
        val second = ChallengeEngine.assignmentFor("user_a", "2026-08-22").map { it.id }
        assertEquals(first, second)
    }

    @Test
    fun `assignment differs across days`() {
        val monday = ChallengeEngine.assignmentFor("user_a", "2026-08-22").map { it.id }
        val tuesday = ChallengeEngine.assignmentFor("user_a", "2026-08-23").map { it.id }
        // Not a hard guarantee for every pair of dates, but these two must differ or the
        // assignment is not actually varying with the date.
        assertTrue(monday != tuesday)
    }

    @Test
    fun `assignment mixes challenge types`() {
        val types = ChallengeEngine.assignmentFor("user_a", "2026-08-22").map { it.type }.toSet()
        assertTrue("Expected a mix of types, got $types", types.size >= 2)
    }

    @Test
    fun `assignment produces the configured number of challenges`() {
        val assignment = ChallengeEngine.assignmentFor("user_a", "2026-08-22")
        assertEquals(ChallengeEngine.DAILY_CHALLENGE_COUNT, assignment.size)
    }

    @Test
    fun `distance progress accumulates`() {
        val start = daily("ch_walk_5km", target = 5)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(distanceKm = 3.0), NOW)
        assertEquals(3, after.progress)
        assertFalse(after.isComplete)
    }

    @Test
    fun `challenge completes when the target is reached`() {
        val start = daily("ch_walk_5km", target = 5, progress = 3)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(distanceKm = 2.0), NOW)
        assertEquals(5, after.progress)
        assertTrue(after.isComplete)
        assertEquals(NOW, after.completedAt)
    }

    @Test
    fun `progress never exceeds the target`() {
        val start = daily("ch_walk_5km", target = 5)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(distanceKm = 99.0), NOW)
        assertEquals(5, after.progress)
    }

    @Test
    fun `an already completed challenge keeps its original completion time`() {
        val completedEarlier = daily("ch_walk_5km", target = 5, progress = 5, completedAt = NOW - 10_000)
        val after = ChallengeEngine.applyProgress(completedEarlier, ActivitySignal(distanceKm = 4.0), NOW)
        assertEquals(NOW - 10_000, after.completedAt)
    }

    @Test
    fun `a zero signal changes nothing`() {
        val start = daily("ch_walk_5km", target = 5, progress = 2)
        assertEquals(start, ChallengeEngine.applyProgress(start, ActivitySignal(), NOW))
    }

    @Test
    fun `group trip uses the largest group rather than a running total`() {
        val start = daily("ch_group_trip", target = 3)
        val afterFirst = ChallengeEngine.applyProgress(
            start, ActivitySignal(tripsCompleted = 1, groupSize = 2), NOW
        )
        assertEquals(2, afterFirst.progress)

        // A second, smaller trip must not push a two-person total up to four.
        val afterSecond = ChallengeEngine.applyProgress(
            afterFirst, ActivitySignal(tripsCompleted = 1, groupSize = 2), NOW
        )
        assertEquals(2, afterSecond.progress)
        assertFalse(afterSecond.isComplete)

        val afterLarger = ChallengeEngine.applyProgress(
            afterSecond, ActivitySignal(tripsCompleted = 1, groupSize = 3), NOW
        )
        assertEquals(3, afterLarger.progress)
        assertTrue(afterLarger.isComplete)
    }

    @Test
    fun `group trip ignores group size without a completed trip`() {
        val start = daily("ch_group_trip", target = 3)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(groupSize = 5), NOW)
        assertEquals(0, after.progress)
    }

    @Test
    fun `trail moment progress counts moments`() {
        val start = daily("ch_three_moments", target = 3)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(momentsCreated = 2), NOW)
        assertEquals(2, after.progress)
        assertNull(after.completedAt)
    }

    @Test
    fun `photo progress requires a verified photo`() {
        val start = daily("ch_photo_mountain", target = 1)
        assertEquals(0, ChallengeEngine.applyProgress(start, ActivitySignal(), NOW).progress)

        val verified = ChallengeEngine.applyProgress(start, ActivitySignal(photosVerified = 1), NOW)
        assertEquals(1, verified.progress)
        assertTrue(verified.isComplete)
    }

    @Test
    fun `distance signal does not advance a photo challenge`() {
        val start = daily("ch_photo_mountain", target = 1)
        val after = ChallengeEngine.applyProgress(start, ActivitySignal(distanceKm = 20.0), NOW)
        assertEquals(0, after.progress)
    }

    @Test
    fun `an unknown challenge id is left untouched`() {
        val start = daily("ch_does_not_exist", target = 5)
        assertEquals(start, ChallengeEngine.applyProgress(start, ActivitySignal(distanceKm = 9.0), NOW))
    }

    @Test
    fun `every catalogue entry has a positive reward and target`() {
        for (challenge in ChallengeEngine.catalogue) {
            assertTrue("${challenge.id} reward", challenge.rewardCoins > 0)
            assertTrue("${challenge.id} target", challenge.target > 0)
        }
    }

    @Test
    fun `every photo challenge declares what the photo must show`() {
        ChallengeEngine.catalogue
            .filter { it.type == ChallengeType.PHOTO }
            .forEach { assertNotNull("${it.id} photoSubject", it.photoSubject) }
    }

    @Test
    fun `date key formats an epoch correctly`() {
        // 2026-08-22T00:00:00Z
        val epoch = 1_787_356_800_000L
        assertEquals("2026-08-22", ChallengeEngine.dateKey(epoch, zoneOffsetMillis = 0))
    }

    @Test
    fun `date key respects a positive timezone offset`() {
        // 2026-08-21T23:00:00Z is already the 22nd in UTC+10.
        val epoch = 1_787_356_800_000L - 3_600_000
        assertEquals("2026-08-22", ChallengeEngine.dateKey(epoch, zoneOffsetMillis = 10 * 3_600_000))
        assertEquals("2026-08-21", ChallengeEngine.dateKey(epoch, zoneOffsetMillis = 0))
    }

    private companion object {
        const val NOW = 1_787_400_000_000L
    }
}
