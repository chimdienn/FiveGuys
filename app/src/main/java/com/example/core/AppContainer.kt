package com.example.core

import android.content.Context
import android.util.Log
import com.example.data.ai.GeminiPhotoVerificationService
import com.example.data.ai.GeminiSpeciesIdentificationService
import com.example.data.ai.MockPhotoVerificationService
import com.example.data.ai.MockSpeciesIdentificationService
import com.example.data.auth.LocalAuthRepository
import com.example.data.auth.SessionStore
import com.example.data.local.BiomateDatabase
import com.example.data.location.FusedLocationProvider
import com.example.data.location.LocationProvider
import com.example.data.repository.local.LocalBadgeRepository
import com.example.data.repository.local.LocalChallengeRepository
import com.example.data.repository.local.LocalConnectionRepository
import com.example.data.repository.local.LocalMessagingRepository
import com.example.data.repository.local.LocalProfileRepository
import com.example.data.repository.local.LocalRewardRepository
import com.example.data.repository.local.LocalSessionRepository
import com.example.data.repository.local.LocalTrailMomentRepository
import com.example.data.repository.local.LocalTrailRepository
import com.example.data.repository.local.LocalTripRepository
import com.example.data.repository.local.PhotoStore
import com.example.data.seed.DevSeeder
import com.example.data.storage.LocalPhotoStore
import com.example.data.weather.OpenMeteoWeatherService
import com.example.domain.ai.PhotoVerificationService
import com.example.domain.ai.SpeciesIdentificationService
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BadgeRepository
import com.example.domain.repository.ChallengeRepository
import com.example.domain.repository.ConnectionRepository
import com.example.domain.repository.MessagingRepository
import com.example.domain.repository.ProfileRepository
import com.example.domain.repository.RewardRepository
import com.example.domain.repository.SessionRepository
import com.example.domain.repository.TrailMomentRepository
import com.example.domain.repository.TrailRepository
import com.example.domain.repository.TripRepository
import com.example.domain.repository.WeatherService
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope

/**
 * Manual dependency graph.
 *
 * Deliberately hand-rolled rather than Hilt: the object graph is small, the wiring is
 * readable in one screen, and adding an annotation processor to a build that already runs
 * KSP for Room buys nothing here (spec section: avoid unnecessary dependencies).
 *
 * The container's real job is choosing a backend. `google-services.json` is not committed
 * — Firebase project configuration belongs to whoever runs the app — so on a fresh clone
 * [FirebaseApp.initializeApp] returns null and every repository resolves to its local,
 * Room-backed implementation. The app is fully usable in that state; what it cannot do is
 * sync between two devices. Drop in a `google-services.json` and the same interfaces are
 * served by Firestore instead, with no change above this file.
 */
class AppContainer(
    private val context: Context,
    private val appScope: CoroutineScope
) {

    /** True when a Firebase project is configured for this build. */
    val isFirebaseConfigured: Boolean by lazy {
        val available = runCatching { FirebaseApp.initializeApp(context) != null }.getOrDefault(false)
        Log.i(
            TAG,
            if (available) {
                "Firebase configured — using cloud backend."
            } else {
                "No google-services.json found — using the local backend. " +
                    "Data stays on this device. See README.md to connect Firebase."
            }
        )
        available
    }

    private val database: BiomateDatabase by lazy {
        BiomateDatabase.getDatabase(context.applicationContext, appScope)
    }

    private val dao by lazy { database.daoV2() }

    private val sessionStore by lazy { SessionStore(context.applicationContext) }

    val photoStore: PhotoStore by lazy { LocalPhotoStore(context.applicationContext) }

    val locationProvider: LocationProvider by lazy { FusedLocationProvider(context.applicationContext) }

    val weatherService: WeatherService by lazy { OpenMeteoWeatherService() }

    /**
     * Photo verification.
     *
     * The mock is not merely a stand-in — it is the fallback the Gemini implementation
     * delegates to when Firebase AI is unavailable, so a network failure degrades to a
     * deterministic verdict rather than an error the user cannot act on.
     */
    val photoVerification: PhotoVerificationService by lazy {
        val mock = MockPhotoVerificationService()
        if (isFirebaseConfigured) GeminiPhotoVerificationService(fallback = mock) else mock
    }

    val speciesIdentification: SpeciesIdentificationService by lazy {
        val mock = MockSpeciesIdentificationService()
        if (isFirebaseConfigured) GeminiSpeciesIdentificationService(fallback = mock) else mock
    }

    /**
     * Completes once first-run seeding has finished.
     *
     * Seeding runs in the background at launch, but the sign-in screen is interactive
     * immediately. Without this gate, a demo login attempted during that window finds no
     * credential row and is told "Incorrect email or password" — which is both wrong and
     * unactionable. Authentication awaits this instead, so it either succeeds or fails
     * for a real reason.
     */
    private val seedingComplete = CompletableDeferred<Unit>()

    suspend fun awaitSeeded() = seedingComplete.await()

    val authRepository: AuthRepository by lazy {
        LocalAuthRepository(dao, sessionStore, awaitReady = ::awaitSeeded)
    }

    val profileRepository: ProfileRepository by lazy { LocalProfileRepository(dao) }

    val trailRepository: TrailRepository by lazy { LocalTrailRepository(dao) }

    val connectionRepository: ConnectionRepository by lazy { LocalConnectionRepository(dao) }

    val tripRepository: TripRepository by lazy { LocalTripRepository(dao) }

    val messagingRepository: MessagingRepository by lazy { LocalMessagingRepository(dao) }

    val trailMomentRepository: TrailMomentRepository by lazy { LocalTrailMomentRepository(dao) }

    val rewardRepository: RewardRepository by lazy { LocalRewardRepository(dao) }

    val badgeRepository: BadgeRepository by lazy { LocalBadgeRepository(dao) }

    val sessionRepository: SessionRepository by lazy { LocalSessionRepository(dao) }

    val challengeRepository: ChallengeRepository by lazy {
        LocalChallengeRepository(
            dao = dao,
            rewards = rewardRepository,
            photoVerification = photoVerification,
            photoStore = photoStore
        )
    }

    /**
     * Prepares first-run data.
     *
     * Trails are reference data and are seeded on every backend. Demo people, credentials
     * and relationships are seeded **only** on the local backend — publishing demo logins
     * into a real Firebase project would be a genuine security problem, not a convenience.
     */
    suspend fun seedIfNeeded() {
        try {
            trailRepository.ensureSeeded()
            if (!isFirebaseConfigured) {
                DevSeeder(dao).seed()
            }
        } catch (e: Exception) {
            // Logged rather than rethrown: a seeding failure should degrade the demo
            // content, not lock every user out of signing in behind an ungated latch.
            Log.e(TAG, "Seeding failed; continuing without demo content", e)
        } finally {
            seedingComplete.complete(Unit)
        }
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
