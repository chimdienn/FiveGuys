package com.example.domain.repository

import com.example.domain.challenge.ActivitySignal
import com.example.domain.model.AdventureSession
import com.example.domain.model.BadgeId
import com.example.domain.model.Challenge
import com.example.domain.model.ChallengeSubmission
import com.example.domain.model.CoinTransaction
import com.example.domain.model.Connection
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.Conversation
import com.example.domain.model.DailyChallenge
import com.example.domain.model.EarnedBadge
import com.example.domain.model.GearItem
import com.example.domain.model.GeoPoint
import com.example.domain.model.Message
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import com.example.domain.model.Readiness
import com.example.domain.model.ReadinessItem
import com.example.domain.model.Trail
import com.example.domain.model.TrailMoment
import com.example.domain.model.Trip
import com.example.domain.model.TripMember
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStats
import com.example.domain.model.Weather
import kotlinx.coroutines.flow.Flow

/**
 * The contracts between the UI layer and everything behind it.
 *
 * Screens and ViewModels depend only on these interfaces, never on Firestore, Room or
 * OkHttp types (spec section 4). That is what makes the local and Firebase backends
 * interchangeable, and what keeps `FirebaseFirestore` out of Compose code.
 */

/** The signed-in user, or the absence of one. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val uid: String, val email: String) : AuthState
}

interface AuthRepository {
    val authState: Flow<AuthState>
    val currentUid: String?

    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun register(email: String, password: String, displayName: String): Result<String>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
}

interface ProfileRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    fun observeStats(uid: String): Flow<UserStats>
    fun observeAllProfiles(): Flow<List<UserProfile>>
    fun observeLeaderboard(limit: Int = 10): Flow<List<LeaderboardEntry>>

    suspend fun getProfile(uid: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
    suspend fun searchByDisplayName(query: String): List<UserProfile>
    /** Recomputes [UserStats] from persisted activity. Safe to call repeatedly. */
    suspend fun recomputeStats(uid: String): UserStats
}

data class LeaderboardEntry(
    val rank: Int,
    val uid: String,
    val displayName: String,
    val avatarUrl: String?,
    val avatarColorHex: Long,
    val bioCoins: Int
)

interface TrailRepository {
    fun observeTrails(): Flow<List<Trail>>
    fun observeTrail(id: String): Flow<Trail?>
    fun observeSavedTrailIds(uid: String): Flow<Set<String>>

    suspend fun getTrail(id: String): Trail?
    suspend fun setSaved(uid: String, trailId: String, saved: Boolean)
    /** Ensures the shared trail catalogue exists. Idempotent. */
    suspend fun ensureSeeded()
}

interface ConnectionRepository {
    fun observeConnections(uid: String): Flow<List<Connection>>
    fun observeAcceptedConnectionIds(uid: String): Flow<Set<String>>
    fun observeIncomingRequests(uid: String): Flow<List<Connection>>

    suspend fun sendRequest(requesterId: String, addresseeId: String): Result<Unit>
    suspend fun respond(connectionId: String, status: ConnectionStatus): Result<Unit>
    suspend fun remove(connectionId: String): Result<Unit>
    suspend fun statusBetween(a: String, b: String): ConnectionStatus?
}

interface TripRepository {
    fun observeTripsForUser(uid: String): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    fun observeMembers(tripId: String): Flow<List<TripMember>>
    fun observeGear(tripId: String): Flow<List<GearItem>>
    fun observeReadiness(tripId: String): Flow<List<Readiness>>
    fun observeMyReadiness(tripId: String, uid: String): Flow<Readiness>

    suspend fun createTrip(trip: Trip, organiser: UserProfile): Result<String>
    suspend fun updateTrip(trip: Trip): Result<Unit>
    suspend fun setStatus(tripId: String, status: TripStatus): Result<Unit>
    suspend fun invite(tripId: String, invitee: UserProfile, byUid: String): Result<Unit>
    suspend fun join(tripId: String, profile: UserProfile): Result<Unit>
    suspend fun leave(tripId: String, uid: String): Result<Unit>

    suspend fun addGear(item: GearItem): Result<Unit>
    suspend fun assignGear(itemId: String, uid: String?, displayName: String?): Result<Unit>
    suspend fun setGearPacked(itemId: String, packed: Boolean, byUid: String): Result<Unit>
    suspend fun removeGear(itemId: String, byUid: String): Result<Unit>

    /** Writes only the calling user's own readiness. */
    suspend fun setReadiness(
        tripId: String,
        uid: String,
        items: Set<ReadinessItem>,
        confidence: Int?,
        notes: String
    ): Result<Unit>
}

interface MessagingRepository {
    fun observeConversations(uid: String): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>

    suspend fun ensureDirectConversation(a: UserProfile, b: UserProfile): Result<String>
    suspend fun ensureTripConversation(tripId: String, title: String, memberIds: List<String>): Result<String>
    suspend fun send(conversationId: String, sender: UserProfile, text: String): Result<Unit>
    suspend fun markRead(conversationId: String, uid: String)
    suspend fun syncTripConversationMembers(tripId: String, memberIds: List<String>)
}

interface TrailMomentRepository {
    fun observeMomentsForTrail(trailId: String): Flow<List<TrailMoment>>
    fun observeMomentsForTrip(tripId: String): Flow<List<TrailMoment>>
    fun observeMomentsByUser(uid: String): Flow<List<TrailMoment>>

    /**
     * Creates a moment at the caller's verified current position.
     *
     * [deviceLocation] must come from the location provider, not from a map tap — the
     * implementation has no other way to place a pin.
     */
    suspend fun create(
        author: UserProfile,
        trailId: String,
        tripId: String?,
        deviceLocation: GeoPoint,
        category: MomentCategory,
        description: String,
        photoUrl: String?,
        visibility: MomentVisibility
    ): Result<TrailMoment>

    suspend fun delete(momentId: String, byUid: String): Result<Unit>
    suspend fun upvote(momentId: String, byUid: String): Result<Unit>
}

interface ChallengeRepository {
    fun observeDailyChallenges(uid: String, dateKey: String): Flow<List<DailyChallengeView>>
    fun observeSubmission(dailyChallengeId: String): Flow<ChallengeSubmission?>

    suspend fun ensureAssigned(uid: String, dateKey: String)
    /** Applies real activity to today's challenges and awards any that complete. */
    suspend fun applyActivity(uid: String, dateKey: String, signal: ActivitySignal): List<AwardedChallenge>
    /**
     * Records a final, immutable photo submission.
     *
     * Fails if a submission already exists for [dailyChallengeId] (spec section 46).
     */
    suspend fun submitPhoto(
        uid: String,
        dailyChallengeId: String,
        photoBytes: ByteArray
    ): Result<ChallengeSubmission>
}

data class DailyChallengeView(
    val daily: DailyChallenge,
    val challenge: Challenge
)

data class AwardedChallenge(
    val challenge: Challenge,
    val coins: Int
)

/**
 * BioCoin authority.
 *
 * The client may *request* an award but never sets a balance. Implementations must make
 * [award] idempotent on the supplied key so a retry, a replayed event or two devices
 * racing cannot pay twice (spec sections 41 and 42).
 */
interface RewardRepository {
    fun observeBalance(uid: String): Flow<Int>
    fun observeTransactions(uid: String): Flow<List<CoinTransaction>>

    suspend fun award(
        uid: String,
        amount: Int,
        reason: String,
        idempotencyKey: String,
        challengeId: String? = null,
        referenceId: String? = null
    ): Result<AwardOutcome>
}

sealed interface AwardOutcome {
    data class Granted(val transaction: CoinTransaction, val newBalance: Int) : AwardOutcome
    /** The idempotency key had already been used — no coins moved. */
    data class AlreadyAwarded(val existing: CoinTransaction) : AwardOutcome
    data class Rejected(val reason: String) : AwardOutcome
}

interface BadgeRepository {
    fun observeEarned(uid: String): Flow<List<EarnedBadge>>
    suspend fun evaluateAndAward(uid: String, stats: UserStats): List<BadgeId>
}

interface SessionRepository {
    fun observeActiveSession(uid: String): Flow<AdventureSession?>
    fun observeCompletedSessions(uid: String): Flow<List<AdventureSession>>

    suspend fun start(uid: String, trailId: String, tripId: String?): Result<AdventureSession>
    suspend fun updateProgress(sessionId: String, distanceKm: Double): Result<Unit>
    suspend fun complete(sessionId: String, distanceKm: Double, durationMinutes: Long): Result<AdventureSession>
    suspend fun abandon(sessionId: String): Result<Unit>
}

interface WeatherService {
    suspend fun getWeather(latitude: Double, longitude: Double): Result<Weather>
}
