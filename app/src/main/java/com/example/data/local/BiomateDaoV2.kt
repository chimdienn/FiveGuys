package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the Biomate domain tables.
 *
 * Every column here is a primitive, so no type converters are involved: list-shaped
 * fields are encoded by `Codec` at the mapper boundary. Reads that feed the UI are
 * exposed as `Flow` so a write anywhere propagates to every observing screen.
 */
@Dao
interface BiomateDaoV2 {

    // ----- Profiles ------------------------------------------------------------------

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    suspend fun getProfile(uid: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY displayName ASC")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE displayNameLower LIKE '%' || :query || '%' ORDER BY displayName ASC LIMIT 30")
    suspend fun searchProfiles(query: String): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfilesIfAbsent(profiles: List<ProfileEntity>)

    // ----- Local credentials ---------------------------------------------------------

    @Query("SELECT * FROM local_credentials WHERE email = :email LIMIT 1")
    suspend fun getCredential(email: String): LocalCredentialEntity?

    @Query("SELECT COUNT(*) FROM local_credentials")
    suspend fun credentialCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredential(credential: LocalCredentialEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCredentialsIfAbsent(credentials: List<LocalCredentialEntity>)

    // ----- Trails --------------------------------------------------------------------

    @Query("SELECT * FROM trails_v2 ORDER BY rating DESC")
    fun observeTrails(): Flow<List<TrailEntity>>

    @Query("SELECT * FROM trails_v2 WHERE id = :id LIMIT 1")
    fun observeTrail(id: String): Flow<TrailEntity?>

    @Query("SELECT * FROM trails_v2 WHERE id = :id LIMIT 1")
    suspend fun getTrail(id: String): TrailEntity?

    @Query("SELECT COUNT(*) FROM trails_v2")
    suspend fun trailCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrails(trails: List<TrailEntity>)

    @Query("SELECT trailId FROM saved_trails WHERE uid = :uid")
    fun observeSavedTrailIds(uid: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrail(entry: SavedTrailEntity)

    @Query("DELETE FROM saved_trails WHERE uid = :uid AND trailId = :trailId")
    suspend fun unsaveTrail(uid: String, trailId: String)

    // ----- Connections ---------------------------------------------------------------

    @Query("SELECT * FROM connections WHERE requesterId = :uid OR addresseeId = :uid ORDER BY createdAt DESC")
    fun observeConnections(uid: String): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE addresseeId = :uid AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeIncomingRequests(uid: String): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE id = :id LIMIT 1")
    suspend fun getConnection(id: String): ConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnection(connection: ConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConnectionsIfAbsent(connections: List<ConnectionEntity>)

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteConnection(id: String)

    // ----- Trips ---------------------------------------------------------------------

    @Query(
        """
        SELECT t.* FROM trips_v2 t
        INNER JOIN trip_members m ON m.tripId = t.id
        WHERE m.uid = :uid AND m.status IN ('INVITED', 'JOINED')
        ORDER BY t.startsAt ASC
        """
    )
    fun observeTripsForUser(uid: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips_v2 WHERE id = :tripId LIMIT 1")
    fun observeTrip(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips_v2 WHERE id = :tripId LIMIT 1")
    suspend fun getTrip(tripId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTripsIfAbsent(trips: List<TripEntity>)

    @Query("UPDATE trips_v2 SET status = :status, updatedAt = :now, completedAt = :completedAt WHERE id = :tripId")
    suspend fun setTripStatus(tripId: String, status: String, now: Long, completedAt: Long?)

    @Query("SELECT * FROM trip_members WHERE tripId = :tripId")
    fun observeMembers(tripId: String): Flow<List<TripMemberEntity>>

    @Query("SELECT * FROM trip_members WHERE tripId = :tripId")
    suspend fun getMembers(tripId: String): List<TripMemberEntity>

    @Query("SELECT * FROM trip_members WHERE tripId = :tripId AND uid = :uid LIMIT 1")
    suspend fun getMember(tripId: String, uid: String): TripMemberEntity?

    @Query("SELECT * FROM trip_members WHERE uid = :uid")
    suspend fun getMembershipsForUser(uid: String): List<TripMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: TripMemberEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembersIfAbsent(members: List<TripMemberEntity>)

    @Query("UPDATE trip_members SET status = :status WHERE tripId = :tripId AND uid = :uid")
    suspend fun setMemberStatus(tripId: String, uid: String, status: String)

    @Query("UPDATE trip_members SET attended = 1 WHERE tripId = :tripId AND status = 'JOINED'")
    suspend fun markJoinedMembersAttended(tripId: String)

    // ----- Gear ----------------------------------------------------------------------

    @Query("SELECT * FROM gear_items WHERE tripId = :tripId ORDER BY isEssential DESC, name ASC")
    fun observeGear(tripId: String): Flow<List<GearItemEntity>>

    @Query("SELECT * FROM gear_items WHERE id = :id LIMIT 1")
    suspend fun getGear(id: String): GearItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGear(item: GearItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGearIfAbsent(items: List<GearItemEntity>)

    @Query("DELETE FROM gear_items WHERE id = :id")
    suspend fun deleteGear(id: String)

    // ----- Readiness -----------------------------------------------------------------

    @Query("SELECT * FROM readiness WHERE tripId = :tripId")
    fun observeReadiness(tripId: String): Flow<List<ReadinessEntity>>

    @Query("SELECT * FROM readiness WHERE tripId = :tripId AND uid = :uid LIMIT 1")
    fun observeMyReadiness(tripId: String, uid: String): Flow<ReadinessEntity?>

    @Query("SELECT * FROM readiness WHERE uid = :uid")
    suspend fun getReadinessForUser(uid: String): List<ReadinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadiness(readiness: ReadinessEntity)

    // ----- Messaging -----------------------------------------------------------------

    @Query("SELECT * FROM conversations WHERE memberIds LIKE '%' || :uid || '%' ORDER BY lastMessageAt DESC")
    fun observeConversations(uid: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE tripId = :tripId LIMIT 1")
    suspend fun getConversationForTrip(tripId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversationsIfAbsent(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIfAbsent(messages: List<MessageEntity>)

    // ----- Trail moments -------------------------------------------------------------

    @Query("SELECT * FROM trail_moments_v2 WHERE trailId = :trailId ORDER BY createdAt DESC")
    fun observeMomentsForTrail(trailId: String): Flow<List<TrailMomentEntity>>

    @Query("SELECT * FROM trail_moments_v2 WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeMomentsForTrip(tripId: String): Flow<List<TrailMomentEntity>>

    @Query("SELECT * FROM trail_moments_v2 WHERE creatorId = :uid ORDER BY createdAt DESC")
    fun observeMomentsByUser(uid: String): Flow<List<TrailMomentEntity>>

    @Query("SELECT COUNT(*) FROM trail_moments_v2 WHERE creatorId = :uid")
    suspend fun momentCountForUser(uid: String): Int

    @Query("SELECT COUNT(*) FROM trail_moments_v2 WHERE tripId = :tripId")
    suspend fun momentCountForTrip(tripId: String): Int

    @Query("SELECT * FROM trail_moments_v2 WHERE id = :id LIMIT 1")
    suspend fun getMoment(id: String): TrailMomentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoment(moment: TrailMomentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMomentsIfAbsent(moments: List<TrailMomentEntity>)

    @Query("DELETE FROM trail_moments_v2 WHERE id = :id")
    suspend fun deleteMoment(id: String)

    // ----- Challenges ----------------------------------------------------------------

    @Query("SELECT * FROM daily_challenges WHERE uid = :uid AND dateKey = :dateKey")
    fun observeDailyChallenges(uid: String, dateKey: String): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE uid = :uid AND dateKey = :dateKey")
    suspend fun getDailyChallenges(uid: String, dateKey: String): List<DailyChallengeEntity>

    @Query("SELECT * FROM daily_challenges WHERE id = :id LIMIT 1")
    suspend fun getDailyChallenge(id: String): DailyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyChallengesIfAbsent(items: List<DailyChallengeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyChallenge(item: DailyChallengeEntity)

    @Query("SELECT * FROM challenge_submissions WHERE dailyChallengeId = :dailyChallengeId LIMIT 1")
    fun observeSubmission(dailyChallengeId: String): Flow<ChallengeSubmissionEntity?>

    @Query("SELECT * FROM challenge_submissions WHERE dailyChallengeId = :dailyChallengeId LIMIT 1")
    suspend fun getSubmissionFor(dailyChallengeId: String): ChallengeSubmissionEntity?

    /**
     * ABORT, not REPLACE: a submission is immutable once written (spec section 46), so a
     * second insert for the same daily challenge must fail rather than overwrite.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubmission(submission: ChallengeSubmissionEntity)

    @Query("UPDATE challenge_submissions SET state = :state, confidence = :confidence, explanation = :explanation, verifiedAt = :verifiedAt WHERE id = :id")
    suspend fun setSubmissionVerdict(
        id: String,
        state: String,
        confidence: Float?,
        explanation: String?,
        verifiedAt: Long
    )

    // ----- BioCoins ------------------------------------------------------------------

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE uid = :uid")
    fun observeBalance(uid: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE uid = :uid")
    suspend fun getBalance(uid: String): Int

    @Query("SELECT * FROM coin_transactions WHERE uid = :uid ORDER BY createdAt DESC")
    fun observeTransactions(uid: String): Flow<List<CoinTransactionEntity>>

    @Query("SELECT * FROM coin_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByKey(key: String): CoinTransactionEntity?

    /** ABORT so the UNIQUE index on `idempotencyKey` rejects a duplicate award. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: CoinTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsIfAbsent(transactions: List<CoinTransactionEntity>)

    @Query("SELECT uid, COALESCE(SUM(amount), 0) AS coins FROM coin_transactions GROUP BY uid ORDER BY coins DESC LIMIT :limit")
    fun observeTopBalances(limit: Int): Flow<List<UidCoins>>

    /**
     * Awards coins if and only if [key] has not been used before.
     *
     * The read and the insert run in one Room transaction, so two concurrent callers
     * cannot both observe "not yet awarded" and both insert.
     */
    @Transaction
    suspend fun awardOnce(transaction: CoinTransactionEntity): CoinTransactionEntity {
        val existing = getTransactionByKey(transaction.idempotencyKey)
        if (existing != null) return existing
        insertTransaction(transaction)
        return transaction
    }

    // ----- Badges --------------------------------------------------------------------

    @Query("SELECT * FROM earned_badges WHERE uid = :uid ORDER BY earnedAt ASC")
    fun observeEarnedBadges(uid: String): Flow<List<EarnedBadgeEntity>>

    @Query("SELECT * FROM earned_badges WHERE uid = :uid")
    suspend fun getEarnedBadges(uid: String): List<EarnedBadgeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<EarnedBadgeEntity>)

    // ----- Sessions ------------------------------------------------------------------

    @Query("SELECT * FROM adventure_sessions WHERE uid = :uid AND status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(uid: String): Flow<AdventureSessionEntity?>

    @Query("SELECT * FROM adventure_sessions WHERE uid = :uid AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedSessions(uid: String): Flow<List<AdventureSessionEntity>>

    @Query("SELECT * FROM adventure_sessions WHERE uid = :uid AND status = 'COMPLETED'")
    suspend fun getCompletedSessions(uid: String): List<AdventureSessionEntity>

    @Query("SELECT * FROM adventure_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): AdventureSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: AdventureSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionsIfAbsent(sessions: List<AdventureSessionEntity>)
}

/** Projection for the leaderboard query. */
data class UidCoins(
    val uid: String,
    val coins: Int
)
