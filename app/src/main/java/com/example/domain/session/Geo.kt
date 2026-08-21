package com.example.domain.session

import com.example.domain.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodesic helpers for tracking and trail progress.
 *
 * Everything here is pure so it can be unit tested without a device, and the accuracy is
 * deliberately modest: haversine over a spherical earth is good to well under a metre at
 * trail scale, which is far better than consumer GPS itself. Presenting more precision
 * than the sensor supports would be a lie (spec section 32).
 */
object Geo {

    private const val EARTH_RADIUS_M = 6_371_008.8

    /** Great-circle distance in metres. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    fun distanceKm(a: GeoPoint, b: GeoPoint): Double = distanceMeters(a, b) / 1000.0

    /** Total length of a polyline in kilometres. */
    fun pathLengthKm(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) total += distanceMeters(points[i - 1], points[i])
        return total / 1000.0
    }

    /**
     * How far along [route] the given [position] is, as a fraction 0.0..1.0.
     *
     * Finds the nearest segment, then measures the distance from the route start to the
     * projection of the position onto that segment. Returns null when the route is not
     * mapped or the user is implausibly far from it — better to show nothing than to show
     * a confident wrong number.
     *
     * @param maxOffRouteMeters how far off-route the user may be before progress is
     *   considered unknown.
     */
    fun progressAlongRoute(
        route: List<GeoPoint>,
        position: GeoPoint,
        maxOffRouteMeters: Double = 750.0
    ): RouteProgress? {
        if (route.size < 2) return null

        var bestSegment = -1
        var bestT = 0.0
        var bestDistance = Double.MAX_VALUE

        for (i in 0 until route.size - 1) {
            val (t, distance) = projectOntoSegment(position, route[i], route[i + 1])
            if (distance < bestDistance) {
                bestDistance = distance
                bestSegment = i
                bestT = t
            }
        }

        if (bestSegment < 0 || bestDistance > maxOffRouteMeters) return null

        var travelled = 0.0
        for (i in 0 until bestSegment) travelled += distanceMeters(route[i], route[i + 1])
        travelled += distanceMeters(route[bestSegment], route[bestSegment + 1]) * bestT

        val total = pathLengthKm(route) * 1000.0
        if (total <= 0.0) return null

        return RouteProgress(
            fraction = (travelled / total).coerceIn(0.0, 1.0),
            distanceAlongKm = travelled / 1000.0,
            offRouteMeters = bestDistance
        )
    }

    /**
     * Project [p] onto segment [a]-[b].
     *
     * Works in a local flat approximation (metres east/north of [a]), which is accurate
     * for the short segments a trail polyline is made of.
     *
     * @return the clamped parameter t in 0..1 and the perpendicular distance in metres.
     */
    private fun projectOntoSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Pair<Double, Double> {
        val latRad = Math.toRadians(a.latitude)
        val mPerDegLat = 111_132.0
        val mPerDegLon = 111_320.0 * cos(latRad)

        val ax = 0.0
        val ay = 0.0
        val bx = (b.longitude - a.longitude) * mPerDegLon
        val by = (b.latitude - a.latitude) * mPerDegLat
        val px = (p.longitude - a.longitude) * mPerDegLon
        val py = (p.latitude - a.latitude) * mPerDegLat

        val dx = bx - ax
        val dy = by - ay
        val lengthSq = dx * dx + dy * dy
        if (lengthSq == 0.0) return 0.0 to sqrt(px * px + py * py)

        val t = min(1.0, max(0.0, (px * dx + py * dy) / lengthSq))
        val projX = t * dx
        val projY = t * dy
        val distance = sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY))
        return t to distance
    }
}

data class RouteProgress(
    /** 0.0..1.0 along the mapped route. */
    val fraction: Double,
    val distanceAlongKm: Double,
    /** Perpendicular distance from the route, in metres. */
    val offRouteMeters: Double
) {
    val percent: Int get() = (fraction * 100).toInt().coerceIn(0, 100)
}
