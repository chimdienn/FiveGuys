package com.example.data

import com.example.data.local.BiomateDatabase
import com.example.data.repository.local.LocalTripRepository
import com.example.domain.model.GearItem
import com.example.domain.model.ReadinessItem
import com.example.domain.model.Trip
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Who may do what to a trip.
 *
 * These are the rules the Firestore security rules mirror. Testing them here means a
 * permission regression fails the build rather than waiting to be caught by a rules
 * deployment.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripPermissionsTest {

    private lateinit var database: BiomateDatabase
    private lateinit var trips: LocalTripRepository

    private val organiser = RepositoryTestHarness.profile("u_organiser", "Alex Rivera")
    private val participant = RepositoryTestHarness.profile("u_participant", "Sarah Chen")
    private val outsider = RepositoryTestHarness.profile("u_outsider", "Marcus Webb")

    @Before
    fun setUp() {
        database = RepositoryTestHarness.inMemoryDatabase()
        trips = LocalTripRepository(RepositoryTestHarness.dao(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun newTrip(participantLimit: Int? = null): String =
        trips.createTrip(
            trip = Trip(
                id = "",
                creatorId = organiser.uid,
                trailId = "trail_1",
                trailName = "Sealers Cove Track",
                title = "Prom Weekend",
                startsAt = 1_800_000_000_000,
                participantLimit = participantLimit
            ),
            organiser = organiser
        ).getOrThrow()

    @Test
    fun `creating a trip makes the creator a joined organiser`() = runTest {
        val tripId = newTrip()
        val members = trips.observeMembers(tripId).first()

        assertEquals(1, members.size)
        assertEquals(organiser.uid, members.first().uid)
        assertEquals(TripRole.ORGANISER, members.first().role)
        assertEquals(TripMemberStatus.JOINED, members.first().status)
    }

    @Test
    fun `a new trip starts in planning`() = runTest {
        val tripId = newTrip()
        assertEquals(TripStatus.PLANNING, trips.observeTrip(tripId).first()?.status)
    }

    @Test
    fun `a trip without a title is rejected`() = runTest {
        val result = trips.createTrip(
            trip = Trip(
                id = "",
                creatorId = organiser.uid,
                trailId = "trail_1",
                trailName = "Trail",
                title = "   ",
                startsAt = 0
            ),
            organiser = organiser
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `only the organiser can invite`() = runTest {
        val tripId = newTrip()

        assertTrue(trips.invite(tripId, participant, byUid = organiser.uid).isSuccess)
        assertTrue(trips.invite(tripId, outsider, byUid = participant.uid).isFailure)
    }

    @Test
    fun `an invitee becomes joined only after joining`() = runTest {
        val tripId = newTrip()
        trips.invite(tripId, participant, byUid = organiser.uid)

        val invited = trips.observeMembers(tripId).first().first { it.uid == participant.uid }
        assertEquals(TripMemberStatus.INVITED, invited.status)
        assertNull(invited.joinedAt)

        trips.join(tripId, participant)
        val joined = trips.observeMembers(tripId).first().first { it.uid == participant.uid }
        assertEquals(TripMemberStatus.JOINED, joined.status)
        assertNotNull(joined.joinedAt)
    }

    @Test
    fun `joining twice does not duplicate a member`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        trips.join(tripId, participant)

        assertEquals(1, trips.observeMembers(tripId).first().count { it.uid == participant.uid })
    }

    @Test
    fun `a participant limit is enforced`() = runTest {
        val tripId = newTrip(participantLimit = 2)
        assertTrue(trips.join(tripId, participant).isSuccess)
        // Organiser plus one participant fills a limit of two.
        assertTrue(trips.join(tripId, outsider).isFailure)
    }

    @Test
    fun `someone already on the trip can rejoin a full trip`() = runTest {
        val tripId = newTrip(participantLimit = 2)
        trips.join(tripId, participant)
        // Not blocked by their own occupancy of the last spot.
        assertTrue(trips.join(tripId, participant).isSuccess)
    }

    @Test
    fun `nobody can join a cancelled trip`() = runTest {
        val tripId = newTrip()
        trips.setStatus(tripId, TripStatus.CANCELLED)
        assertTrue(trips.join(tripId, participant).isFailure)
    }

    @Test
    fun `the organiser cannot leave their own trip`() = runTest {
        val tripId = newTrip()
        val result = trips.leave(tripId, organiser.uid)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cancel", ignoreCase = true) == true)
    }

    @Test
    fun `a participant can leave`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        assertTrue(trips.leave(tripId, participant.uid).isSuccess)

        val member = trips.observeMembers(tripId).first().first { it.uid == participant.uid }
        assertEquals(TripMemberStatus.LEFT, member.status)
    }

    @Test
    fun `completing a trip marks joined members as attended`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        trips.invite(tripId, outsider, byUid = organiser.uid)

        trips.setStatus(tripId, TripStatus.COMPLETED)

        val members = trips.observeMembers(tripId).first()
        assertTrue(members.first { it.uid == organiser.uid }.attended)
        assertTrue(members.first { it.uid == participant.uid }.attended)
        // Someone who only ever had an invite did not attend.
        assertFalse(members.first { it.uid == outsider.uid }.attended)
    }

    @Test
    fun `completing a trip records a completion time`() = runTest {
        val tripId = newTrip()
        trips.setStatus(tripId, TripStatus.COMPLETED)
        assertNotNull(trips.observeTrip(tripId).first()?.completedAt)
    }

    // ---- Gear ------------------------------------------------------------------------

    @Test
    fun `a new trip is seeded with essential gear`() = runTest {
        val tripId = newTrip()
        val gear = trips.observeGear(tripId).first()
        assertTrue(gear.isNotEmpty())
        assertTrue(gear.any { it.isEssential })
    }

    @Test
    fun `an assignee can tick their own gear`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        val item = trips.observeGear(tripId).first().first()

        trips.assignGear(item.id, participant.uid, participant.displayName)
        assertTrue(trips.setGearPacked(item.id, packed = true, byUid = participant.uid).isSuccess)
        assertTrue(trips.observeGear(tripId).first().first { it.id == item.id }.isPacked)
    }

    @Test
    fun `nobody can tick off gear assigned to someone else`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        val item = trips.observeGear(tripId).first().first()
        trips.assignGear(item.id, participant.uid, participant.displayName)

        // A checklist anyone can tick is not a checklist.
        assertTrue(trips.setGearPacked(item.id, packed = true, byUid = outsider.uid).isFailure)
    }

    @Test
    fun `the organiser can tick off anyone's gear`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)
        val item = trips.observeGear(tripId).first().first()
        trips.assignGear(item.id, participant.uid, participant.displayName)

        assertTrue(trips.setGearPacked(item.id, packed = true, byUid = organiser.uid).isSuccess)
    }

    @Test
    fun `unassigned gear can be ticked by any member`() = runTest {
        val tripId = newTrip()
        val item = trips.observeGear(tripId).first().first { it.assignedToUid == null }
        assertTrue(trips.setGearPacked(item.id, packed = true, byUid = participant.uid).isSuccess)
    }

    @Test
    fun `gear needs a name`() = runTest {
        val tripId = newTrip()
        val result = trips.addGear(
            GearItem(id = "", tripId = tripId, name = "  ", category = "Safety")
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `only the organiser or the assignee can remove gear`() = runTest {
        val tripId = newTrip()
        val item = trips.observeGear(tripId).first().first()

        assertTrue(trips.removeGear(item.id, byUid = outsider.uid).isFailure)
        assertTrue(trips.removeGear(item.id, byUid = organiser.uid).isSuccess)
    }

    // ---- Readiness -------------------------------------------------------------------

    @Test
    fun `readiness is recorded per user`() = runTest {
        val tripId = newTrip()
        trips.join(tripId, participant)

        trips.setReadiness(
            tripId, organiser.uid,
            setOf(ReadinessItem.WATER_PACKED, ReadinessItem.FOOTWEAR),
            confidence = 4, notes = ""
        )

        assertEquals(2, trips.observeMyReadiness(tripId, organiser.uid).first().completedCount)
        // One person's answers never leak into another's.
        assertEquals(0, trips.observeMyReadiness(tripId, participant.uid).first().completedCount)
    }

    @Test
    fun `readiness is complete only when every item is ticked`() = runTest {
        val tripId = newTrip()

        trips.setReadiness(tripId, organiser.uid, ReadinessItem.all.dropLast(1).toSet(), null, "")
        assertFalse(trips.observeMyReadiness(tripId, organiser.uid).first().isComplete)

        trips.setReadiness(tripId, organiser.uid, ReadinessItem.all.toSet(), null, "")
        assertTrue(trips.observeMyReadiness(tripId, organiser.uid).first().isComplete)
    }

    @Test
    fun `confidence is clamped to the one to five scale`() = runTest {
        val tripId = newTrip()
        trips.setReadiness(tripId, organiser.uid, emptySet(), confidence = 99, notes = "")
        assertEquals(5, trips.observeMyReadiness(tripId, organiser.uid).first().confidence)
    }

    @Test
    fun `a user with no readiness record reads as empty rather than missing`() = runTest {
        val tripId = newTrip()
        val readiness = trips.observeMyReadiness(tripId, participant.uid).first()
        assertEquals(0, readiness.completedCount)
        assertFalse(readiness.isComplete)
    }
}
