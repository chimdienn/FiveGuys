package com.example.domain

import com.example.domain.badge.BadgeRules
import com.example.domain.model.BadgeId
import com.example.domain.model.UserStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeRulesTest {

    private fun stats(
        trails: Int = 0,
        km: Double = 0.0,
        groupTrips: Int = 0,
        moments: Int = 0,
        readiness: Int = 0
    ) = UserStats(
        uid = "u1",
        trailsCompleted = trails,
        totalDistanceKm = km,
        groupTripsCompleted = groupTrips,
        trailMomentsCreated = moments,
        readinessChecklistsCompleted = readiness
    )

    @Test
    fun `a brand new user has earned nothing`() {
        assertTrue(BadgeRules.evaluateAll(stats()).isEmpty())
    }

    @Test
    fun `first steps needs exactly one trail`() {
        assertFalse(BadgeId.FIRST_STEPS in BadgeRules.evaluateAll(stats(trails = 0)))
        assertTrue(BadgeId.FIRST_STEPS in BadgeRules.evaluateAll(stats(trails = 1)))
    }

    @Test
    fun `explorer needs five trails`() {
        assertFalse(BadgeId.EXPLORER in BadgeRules.evaluateAll(stats(trails = 4)))
        assertTrue(BadgeId.EXPLORER in BadgeRules.evaluateAll(stats(trails = 5)))
    }

    @Test
    fun `trail regular needs fifty kilometres`() {
        assertFalse(BadgeId.TRAIL_REGULAR in BadgeRules.evaluateAll(stats(km = 49.9)))
        assertTrue(BadgeId.TRAIL_REGULAR in BadgeRules.evaluateAll(stats(km = 50.0)))
    }

    @Test
    fun `social hiker needs three group trips`() {
        assertFalse(BadgeId.SOCIAL_HIKER in BadgeRules.evaluateAll(stats(groupTrips = 2)))
        assertTrue(BadgeId.SOCIAL_HIKER in BadgeRules.evaluateAll(stats(groupTrips = 3)))
    }

    @Test
    fun `trail reporter needs ten moments`() {
        assertFalse(BadgeId.TRAIL_REPORTER in BadgeRules.evaluateAll(stats(moments = 9)))
        assertTrue(BadgeId.TRAIL_REPORTER in BadgeRules.evaluateAll(stats(moments = 10)))
    }

    @Test
    fun `prepared needs five completed checklists`() {
        assertFalse(BadgeId.PREPARED in BadgeRules.evaluateAll(stats(readiness = 4)))
        assertTrue(BadgeId.PREPARED in BadgeRules.evaluateAll(stats(readiness = 5)))
    }

    @Test
    fun `already earned badges are not reported as newly earned`() {
        val current = stats(trails = 5)
        val newly = BadgeRules.newlyEarned(current, alreadyEarned = setOf(BadgeId.FIRST_STEPS))
        assertEquals(setOf(BadgeId.EXPLORER), newly)
    }

    @Test
    fun `newly earned is empty when nothing changed`() {
        val current = stats(trails = 5)
        val held = setOf(BadgeId.FIRST_STEPS, BadgeId.EXPLORER)
        assertTrue(BadgeRules.newlyEarned(current, held).isEmpty())
    }

    @Test
    fun `crossing a threshold awards only the badge that was crossed`() {
        val before = stats(trails = 1, km = 49.0)
        val held = BadgeRules.evaluateAll(before)
        val after = stats(trails = 1, km = 51.0)
        assertEquals(setOf(BadgeId.TRAIL_REGULAR), BadgeRules.newlyEarned(after, held))
    }

    @Test
    fun `every badge id has exactly one rule`() {
        assertEquals(BadgeId.entries.size, BadgeRules.all.size)
        assertEquals(BadgeId.entries.toSet(), BadgeRules.all.map { it.badge.id }.toSet())
    }

    @Test
    fun `a fully active user earns every badge`() {
        val complete = stats(trails = 12, km = 180.0, groupTrips = 6, moments = 22, readiness = 9)
        assertEquals(BadgeId.entries.toSet(), BadgeRules.evaluateAll(complete))
    }
}
