package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities backing the Biomate domain model.
 *
 * These are storage records, not domain types: they use primitive columns and comma
 * separated enum-name lists so Room can index them, and they are converted at the
 * repository boundary by `data/mapper/Mappers.kt`. Keeping them separate from
 * `domain.model` means a schema change does not ripple into the UI.
 *
 * Note there is no password column anywhere. Credentials live in Firebase Authentication
 * (spec section 8); when Firebase is not configured, the local auth backend stores a
 * salted PBKDF2 hash in `local_credentials`, never a recoverable password.
 */

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val displayNameLower: String,
    val avatarUrl: String?,
    val bio: String,
    val birthYear: Int?,
    val gender: String?,
    val homeArea: String?,
    val fitnessLevel: String,
    val experienceLevel: String,
    val preferredPace: String,
    val socialStyles: String,
    val interests: String,
    val skills: String,
    val avatarColorHex: Long,
    val onboardingComplete: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Local-only credential store used when Firebase Authentication is unavailable.
 *
 * Stores a PBKDF2-WithHmacSHA256 hash and a per-user random salt. The plaintext password
 * is never written to disk.
 */
@Entity(tableName = "local_credentials")
data class LocalCredentialEntity(
    @PrimaryKey val email: String,
    val uid: String,
    val passwordHash: String,
    val salt: String,
    val iterations: Int,
    val createdAt: Long
)

@Entity(tableName = "trails_v2")
data class TrailEntity(
    @PrimaryKey val id: String,
    val name: String,
    val region: String,
    val stateOrCountry: String,
    val activityTypes: String,
    val description: String,
    val difficulty: String,
    val distanceKm: Double,
    val elevationGainM: Int,
    val estimatedMinutes: Int,
    val startLat: Double,
    val startLng: Double,
    /** Encoded as "lat,lng;lat,lng;..." — see `Converters`. */
    val route: String,
    val waypoints: String,
    val imageUrl: String?,
    val tags: String,
    val isExposed: Boolean,
    val isShaded: Boolean,
    val rating: Double,
    val reviewCount: Int,
    val highlights: String,
    val recommendedGear: String,
    val createdAt: Long
)

@Entity(tableName = "saved_trails", primaryKeys = ["uid", "trailId"])
data class SavedTrailEntity(
    val uid: String,
    val trailId: String,
    val savedAt: Long
)

@Entity(
    tableName = "connections",
    indices = [Index("requesterId"), Index("addresseeId")]
)
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?
)

@Entity(tableName = "trips_v2", indices = [Index("creatorId")])
data class TripEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val trailId: String,
    val trailName: String,
    val title: String,
    val startsAt: Long,
    val meetingPoint: String,
    val participantLimit: Int?,
    val carpoolNotes: String,
    val foodNotes: String,
    val generalNotes: String,
    val emergencyNotes: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)

@Entity(tableName = "trip_members", primaryKeys = ["tripId", "uid"], indices = [Index("uid")])
data class TripMemberEntity(
    val tripId: String,
    val uid: String,
    val displayName: String,
    val role: String,
    val status: String,
    val joinedAt: Long?,
    val attended: Boolean
)

@Entity(tableName = "gear_items", indices = [Index("tripId")])
data class GearItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val assignedToUid: String?,
    val assignedToName: String?,
    val isPacked: Boolean,
    val isEssential: Boolean
)

@Entity(tableName = "readiness", primaryKeys = ["tripId", "uid"])
data class ReadinessEntity(
    val tripId: String,
    val uid: String,
    val checkedItems: String,
    val confidence: Int?,
    val notes: String,
    val updatedAt: Long
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val memberIds: String,
    val title: String,
    val tripId: String?,
    val lastMessagePreview: String,
    val lastMessageAt: Long,
    val lastMessageSenderId: String?,
    /** Encoded as "uid=millis;uid=millis". */
    val lastReadAt: String
)

@Entity(tableName = "messages", indices = [Index("conversationId")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
    val isSystem: Boolean
)

@Entity(
    tableName = "trail_moments_v2",
    indices = [Index("trailId"), Index("tripId"), Index("creatorId")]
)
data class TrailMomentEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val trailId: String,
    val tripId: String?,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val description: String,
    val photoUrl: String?,
    val visibility: String,
    val createdAt: Long,
    val upvotes: Int
)

@Entity(tableName = "daily_challenges", indices = [Index("uid"), Index("dateKey")])
data class DailyChallengeEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val challengeId: String,
    val dateKey: String,
    val progress: Int,
    val target: Int,
    val completedAt: Long?,
    val rewardedAt: Long?
)

@Entity(tableName = "challenge_submissions", indices = [Index(value = ["dailyChallengeId"], unique = true)])
data class ChallengeSubmissionEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val dailyChallengeId: String,
    val challengeId: String,
    val photoUrl: String?,
    val state: String,
    val confidence: Float?,
    val explanation: String?,
    val submittedAt: Long,
    val verifiedAt: Long?
)

/**
 * The BioCoin ledger.
 *
 * [idempotencyKey] carries a UNIQUE index: that constraint, not application logic, is
 * what makes a duplicate award impossible. A second insert with the same key throws
 * rather than silently paying out again.
 */
@Entity(
    tableName = "coin_transactions",
    indices = [Index("uid"), Index(value = ["idempotencyKey"], unique = true)]
)
data class CoinTransactionEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val amount: Int,
    val reason: String,
    val challengeId: String?,
    val referenceId: String?,
    val idempotencyKey: String,
    val createdAt: Long
)

@Entity(tableName = "earned_badges", primaryKeys = ["uid", "badgeId"])
data class EarnedBadgeEntity(
    val uid: String,
    val badgeId: String,
    val earnedAt: Long
)

@Entity(tableName = "adventure_sessions", indices = [Index("uid"), Index("tripId")])
data class AdventureSessionEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val tripId: String?,
    val trailId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val distanceKm: Double,
    val durationMinutes: Long,
    val momentCount: Int,
    val companionCount: Int,
    val status: String
)
