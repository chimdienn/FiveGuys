package com.example.data

import com.example.data.local.BiomateDatabase
import com.example.data.repository.local.LocalTrailMomentRepository
import com.example.domain.model.GeoPoint
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrailMomentTest {

    private lateinit var database: BiomateDatabase
    private lateinit var moments: LocalTrailMomentRepository

    private val author = RepositoryTestHarness.profile("u_author", "Alex Rivera")
    private val other = RepositoryTestHarness.profile("u_other", "Sarah Chen")

    private val trailhead = GeoPoint(-39.0306, 146.3392)

    @Before
    fun setUp() {
        database = RepositoryTestHarness.inMemoryDatabase()
        moments = LocalTrailMomentRepository(RepositoryTestHarness.dao(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createHazard(
        creator: com.example.domain.model.UserProfile = author,
        visibility: MomentVisibility = MomentVisibility.PUBLIC,
        location: GeoPoint = trailhead,
        description: String = "Tree down across the track"
    ) = moments.create(
        author = creator,
        trailId = "trail_1",
        tripId = null,
        deviceLocation = location,
        category = MomentCategory.HAZARD,
        description = description,
        photoUrl = null,
        visibility = visibility
    )

    @Test
    fun `a moment records the device location it was created at`() = runTest {
        val moment = createHazard().getOrThrow()
        assertEquals(trailhead.latitude, moment.latitude, 0.000001)
        assertEquals(trailhead.longitude, moment.longitude, 0.000001)
    }

    @Test
    fun `a moment is attributed to its author`() = runTest {
        val moment = createHazard().getOrThrow()
        assertEquals(author.uid, moment.creatorId)
        assertEquals(author.displayName, moment.creatorName)
    }

    @Test
    fun `a moment needs a description`() = runTest {
        assertTrue(createHazard(description = "   ").isFailure)
    }

    @Test
    fun `an implausible location is rejected`() = runTest {
        // Guards against a default-initialised or garbage fix being stored as a real
        // hazard position.
        assertTrue(createHazard(location = GeoPoint(999.0, 0.0)).isFailure)
        assertTrue(createHazard(location = GeoPoint(0.0, 999.0)).isFailure)
    }

    @Test
    fun `moments are listed for their trail`() = runTest {
        createHazard()
        createHazard(description = "Very muddy section")
        assertEquals(2, moments.observeMomentsForTrail("trail_1").first().size)
        assertEquals(0, moments.observeMomentsForTrail("trail_other").first().size)
    }

    @Test
    fun `moments are listed for their author`() = runTest {
        createHazard(creator = author)
        createHazard(creator = other)
        assertEquals(1, moments.observeMomentsByUser(author.uid).first().size)
        assertEquals(1, moments.observeMomentsByUser(other.uid).first().size)
    }

    @Test
    fun `only the author can delete a moment`() = runTest {
        val moment = createHazard().getOrThrow()

        assertTrue(moments.delete(moment.id, byUid = other.uid).isFailure)
        assertEquals(1, moments.observeMomentsForTrail("trail_1").first().size)

        assertTrue(moments.delete(moment.id, byUid = author.uid).isSuccess)
        assertEquals(0, moments.observeMomentsForTrail("trail_1").first().size)
    }

    @Test
    fun `deleting a moment that is already gone is not an error`() = runTest {
        assertTrue(moments.delete("does_not_exist", byUid = author.uid).isSuccess)
    }

    @Test
    fun `another user can upvote a moment`() = runTest {
        val moment = createHazard().getOrThrow()
        assertTrue(moments.upvote(moment.id, byUid = other.uid).isSuccess)
        assertEquals(1, moments.observeMomentsForTrail("trail_1").first().first().upvotes)
    }

    @Test
    fun `an author cannot upvote their own moment`() = runTest {
        val moment = createHazard().getOrThrow()
        assertTrue(moments.upvote(moment.id, byUid = author.uid).isFailure)
        assertEquals(0, moments.observeMomentsForTrail("trail_1").first().first().upvotes)
    }

    @Test
    fun `visibility is preserved`() = runTest {
        val moment = createHazard(visibility = MomentVisibility.CONNECTIONS).getOrThrow()
        assertEquals(MomentVisibility.CONNECTIONS, moment.visibility)
        assertEquals(
            MomentVisibility.CONNECTIONS,
            moments.observeMomentsForTrail("trail_1").first().first().visibility
        )
    }

    @Test
    fun `hazards and conditions are treated as time sensitive`() = runTest {
        val hazard = createHazard().getOrThrow()
        assertTrue(hazard.isTimeSensitive)

        val viewpoint = moments.create(
            author = author,
            trailId = "trail_1",
            tripId = null,
            deviceLocation = trailhead,
            category = MomentCategory.VIEWPOINT,
            description = "Great spot for sunset",
            photoUrl = null,
            visibility = MomentVisibility.PUBLIC
        ).getOrThrow()
        // A view does not go stale the way a fallen tree does.
        assertFalse(viewpoint.isTimeSensitive)
    }

    @Test
    fun `age is described in human terms`() = runTest {
        val moment = createHazard().getOrThrow()
        val createdAt = moment.createdAt

        assertEquals("Just now", moment.ageLabel(createdAt))
        assertEquals("5 minutes ago", moment.ageLabel(createdAt + 5 * 60_000))
        assertEquals("1 hour ago", moment.ageLabel(createdAt + 60 * 60_000))
        assertEquals("2 days ago", moment.ageLabel(createdAt + 2 * 24 * 60 * 60_000L))
        assertEquals("1 month ago", moment.ageLabel(createdAt + 31 * 24 * 60 * 60_000L))
    }

    @Test
    fun `age never reads as negative for a clock skew`() = runTest {
        val moment = createHazard().getOrThrow()
        assertEquals("Just now", moment.ageLabel(moment.createdAt - 60_000))
    }
}
