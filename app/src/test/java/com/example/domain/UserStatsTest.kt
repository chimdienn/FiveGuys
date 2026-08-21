package com.example.domain

import com.example.domain.model.UserStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserStatsTest {

    @Test
    fun `attendance is null when no group trips were joined`() {
        assertNull(UserStats(uid = "u1", groupTripsJoined = 0).attendanceRatePercent)
    }

    @Test
    fun `attendance is a percentage of joined group trips`() {
        val stats = UserStats(uid = "u1", groupTripsJoined = 4, groupTripsCompleted = 3)
        assertEquals(75, stats.attendanceRatePercent)
    }

    @Test
    fun `full attendance is one hundred percent`() {
        val stats = UserStats(uid = "u1", groupTripsJoined = 3, groupTripsCompleted = 3)
        assertEquals(100, stats.attendanceRatePercent)
    }

    @Test
    fun `attendance never exceeds one hundred`() {
        // Defensive: a data inconsistency must not render as 150%.
        val stats = UserStats(uid = "u1", groupTripsJoined = 2, groupTripsCompleted = 3)
        assertEquals(100, stats.attendanceRatePercent)
    }
}
