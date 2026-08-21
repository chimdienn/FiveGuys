package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.domain.model.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A single GPS fix with the accuracy the device reported. */
data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val timestamp: Long
)

/**
 * Foreground location updates.
 *
 * Behind an interface so the OnTrail screen can be driven by a fake provider in tests and
 * on an emulator without a location fix, and so no Compose code touches Play Services.
 */
interface LocationProvider {
    fun hasPermission(): Boolean
    /** Emits while collected; stops as soon as the collector goes away. */
    fun locationUpdates(): Flow<LocationFix>
    suspend fun lastKnownLocation(): LocationFix?
}

/**
 * Real device location via the fused location provider.
 *
 * Foreground only — updates start when the OnTrail screen collects and stop when it does
 * not. Biomate does not request background location for the MVP (spec section 29), so
 * closing the app ends tracking, which is both the honest behaviour and the one users
 * expect.
 *
 * [UPDATE_INTERVAL_MS] is a deliberate compromise: frequent enough that the marker keeps
 * up with walking pace, slow enough not to drain a battery someone may need later in the
 * day (spec section 31).
 */
class FusedLocationProvider(private val context: Context) : LocationProvider {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<LocationFix> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("Location permission has not been granted."))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_M)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toFix()) }
            }
        }

        client.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocation(): LocationFix? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location -> continuation.resume(location?.toFix()) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }

    private fun Location.toFix() = LocationFix(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracy,
        altitudeMeters = if (hasAltitude()) altitude else null,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        timestamp = time
    )

    companion object {
        /** Target interval between fixes while walking. */
        const val UPDATE_INTERVAL_MS = 5_000L
        /** Never accept fixes faster than this, however many apps are asking. */
        const val MIN_UPDATE_INTERVAL_MS = 3_000L
        /** Ignore jitter smaller than this — GPS noise is not distance travelled. */
        const val MIN_UPDATE_DISTANCE_M = 5f
    }
}
