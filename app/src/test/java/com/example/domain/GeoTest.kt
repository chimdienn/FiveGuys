package com.example.domain

import com.example.domain.model.GeoPoint
import com.example.domain.session.DistanceTracker
import com.example.domain.session.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {

    // Melbourne CBD to Geelong, roughly 64 km apart.
    private val melbourne = GeoPoint(-37.8136, 144.9631)
    private val geelong = GeoPoint(-38.1499, 144.3617)

    @Test
    fun `distance between a point and itself is zero`() {
        assertEquals(0.0, Geo.distanceMeters(melbourne, melbourne), 0.001)
    }

    @Test
    fun `distance matches a known separation`() {
        val km = Geo.distanceKm(melbourne, geelong)
        assertEquals(64.0, km, 2.0)
    }

    @Test
    fun `distance is symmetric`() {
        assertEquals(
            Geo.distanceMeters(melbourne, geelong),
            Geo.distanceMeters(geelong, melbourne),
            0.001
        )
    }

    @Test
    fun `path length of a single point is zero`() {
        assertEquals(0.0, Geo.pathLengthKm(listOf(melbourne)), 0.0001)
        assertEquals(0.0, Geo.pathLengthKm(emptyList()), 0.0001)
    }

    @Test
    fun `path length sums its segments`() {
        val mid = GeoPoint(-37.98, 144.66)
        val direct = Geo.distanceKm(melbourne, mid) + Geo.distanceKm(mid, geelong)
        assertEquals(direct, Geo.pathLengthKm(listOf(melbourne, mid, geelong)), 0.001)
    }

    // ----- Route progress ---------------------------------------------------------------

    private val route = listOf(
        GeoPoint(-37.8000, 145.0000),
        GeoPoint(-37.8100, 145.0000),
        GeoPoint(-37.8200, 145.0000)
    )

    @Test
    fun `progress at the route start is zero`() {
        val progress = Geo.progressAlongRoute(route, route.first())
        assertNotNull(progress)
        assertEquals(0, progress!!.percent)
    }

    @Test
    fun `progress at the route end is one hundred`() {
        val progress = Geo.progressAlongRoute(route, route.last())
        assertNotNull(progress)
        assertEquals(100, progress!!.percent)
    }

    @Test
    fun `progress at the midpoint is about half`() {
        val progress = Geo.progressAlongRoute(route, route[1])
        assertNotNull(progress)
        assertEquals(50.0, progress!!.percent.toDouble(), 2.0)
    }

    @Test
    fun `progress is null when the route is not mapped`() {
        assertNull(Geo.progressAlongRoute(emptyList(), melbourne))
        assertNull(Geo.progressAlongRoute(listOf(melbourne), melbourne))
    }

    @Test
    fun `progress is null when far off the route`() {
        // Deliberately refuses to guess rather than reporting a confident wrong number.
        assertNull(Geo.progressAlongRoute(route, GeoPoint(-38.5000, 143.0000)))
    }

    @Test
    fun `slightly off route still reports progress with an offset`() {
        val nearby = GeoPoint(-37.8100, 145.0010)
        val progress = Geo.progressAlongRoute(route, nearby)
        assertNotNull(progress)
        assertTrue("offRoute=${progress!!.offRouteMeters}", progress.offRouteMeters in 1.0..200.0)
    }

    @Test
    fun `progress increases monotonically along the route`() {
        val first = Geo.progressAlongRoute(route, GeoPoint(-37.8050, 145.0000))!!
        val second = Geo.progressAlongRoute(route, GeoPoint(-37.8150, 145.0000))!!
        assertTrue(second.fraction > first.fraction)
    }

    // ----- Distance tracker -------------------------------------------------------------

    @Test
    fun `tracker starts at zero`() {
        assertEquals(0.0, DistanceTracker().totalKm, 0.0)
    }

    @Test
    fun `tracker accumulates real movement`() {
        val tracker = DistanceTracker()
        tracker.accept(GeoPoint(-37.8000, 145.0000), accuracyMeters = 5f)
        tracker.accept(GeoPoint(-37.8100, 145.0000), accuracyMeters = 5f)
        assertEquals(1.11, tracker.totalKm, 0.05)
    }

    @Test
    fun `tracker ignores GPS jitter while stationary`() {
        val tracker = DistanceTracker()
        val base = GeoPoint(-37.8000, 145.0000)
        tracker.accept(base, accuracyMeters = 8f)
        // A metre of wobble, repeated — the classic source of phantom distance.
        repeat(50) { i ->
            tracker.accept(GeoPoint(base.latitude + i % 2 * 0.00001, base.longitude), accuracyMeters = 8f)
        }
        assertEquals(0.0, tracker.totalKm, 0.001)
    }

    @Test
    fun `tracker discards inaccurate fixes`() {
        val tracker = DistanceTracker(maxAcceptableAccuracyMeters = 20f)
        tracker.accept(GeoPoint(-37.8000, 145.0000), accuracyMeters = 5f)
        tracker.accept(GeoPoint(-37.8100, 145.0000), accuracyMeters = 500f)
        assertEquals(0.0, tracker.totalKm, 0.0001)
    }

    @Test
    fun `tracker resumes after a discarded fix`() {
        val tracker = DistanceTracker(maxAcceptableAccuracyMeters = 20f)
        tracker.accept(GeoPoint(-37.8000, 145.0000), accuracyMeters = 5f)
        tracker.accept(GeoPoint(-37.8050, 145.0000), accuracyMeters = 900f)
        tracker.accept(GeoPoint(-37.8100, 145.0000), accuracyMeters = 5f)
        assertEquals(1.11, tracker.totalKm, 0.05)
    }

    @Test
    fun `tracker reset clears the total`() {
        val tracker = DistanceTracker()
        tracker.accept(GeoPoint(-37.8000, 145.0000), accuracyMeters = 5f)
        tracker.accept(GeoPoint(-37.8100, 145.0000), accuracyMeters = 5f)
        tracker.reset()
        assertEquals(0.0, tracker.totalKm, 0.0)
    }
}
