package com.example.domain.session

import com.example.domain.model.GeoPoint

/**
 * Accumulates distance walked from a stream of GPS fixes.
 *
 * Naively summing the distance between consecutive fixes badly overstates the total: a
 * stationary phone still reports a jittering position, and every wobble is counted as
 * progress. Two filters keep the number honest:
 *
 *  - fixes with poor reported accuracy are discarded outright; and
 *  - a step shorter than the accuracy of the fixes that produced it is treated as noise
 *    rather than movement.
 *
 * The result is a slight under-estimate, which is the right direction to err in: telling
 * someone they walked less than they did is a disappointment, telling them they walked
 * further is a lie that also inflates challenge progress.
 *
 * Pure and frame-by-frame, so it is unit tested without a device.
 */
class DistanceTracker(
    private val maxAcceptableAccuracyMeters: Float = 50f,
    private val minStepMeters: Double = 4.0
) {
    private var lastPoint: GeoPoint? = null
    private var lastAccuracy: Float = 0f

    var totalKm: Double = 0.0
        private set

    /**
     * Feeds one fix in and returns the running total in kilometres.
     *
     * @return the total distance so far, unchanged if the fix was rejected.
     */
    fun accept(point: GeoPoint, accuracyMeters: Float): Double {
        if (accuracyMeters > maxAcceptableAccuracyMeters) return totalKm

        val previous = lastPoint
        if (previous == null) {
            lastPoint = point
            lastAccuracy = accuracyMeters
            return totalKm
        }

        val stepMeters = Geo.distanceMeters(previous, point)
        // A movement smaller than the uncertainty of the measurement is not evidence of
        // movement. Compare against the worse of the two fixes.
        val noiseFloor = maxOf(minStepMeters, maxOf(accuracyMeters, lastAccuracy).toDouble() * 0.5)
        if (stepMeters < noiseFloor) return totalKm

        totalKm += stepMeters / 1000.0
        lastPoint = point
        lastAccuracy = accuracyMeters
        return totalKm
    }

    fun reset(startingKm: Double = 0.0) {
        totalKm = startingKm
        lastPoint = null
        lastAccuracy = 0f
    }
}
