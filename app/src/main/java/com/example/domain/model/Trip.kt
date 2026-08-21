package com.example.domain.model

enum class TripStatus { PLANNING, ACTIVE, COMPLETED, CANCELLED }

enum class TripRole { ORGANISER, PARTICIPANT }

enum class TripMemberStatus { INVITED, JOINED, DECLINED, LEFT }

data class Trip(
    val id: String,
    val creatorId: String,
    val trailId: String,
    val trailName: String,
    val title: String,
    /** Epoch millis of departure. */
    val startsAt: Long,
    val meetingPoint: String = "",
    val participantLimit: Int? = null,
    val carpoolNotes: String = "",
    val foodNotes: String = "",
    val generalNotes: String = "",
    val emergencyNotes: String = "",
    val status: TripStatus = TripStatus.PLANNING,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
)

data class TripMember(
    val tripId: String,
    val uid: String,
    val displayName: String,
    val role: TripRole,
    val status: TripMemberStatus,
    val joinedAt: Long? = null,
    /** Set when the trip completes and this member was credited with attending. */
    val attended: Boolean = false
)

data class GearItem(
    val id: String,
    val tripId: String,
    val name: String,
    val category: String = "General",
    val quantity: Int = 1,
    /** uid of the assignee, or null when nobody has taken it yet. */
    val assignedToUid: String? = null,
    val assignedToName: String? = null,
    val isPacked: Boolean = false,
    val isEssential: Boolean = false
)

/**
 * The canonical readiness checklist. Users tick their own items; the list is fixed so
 * that "5 of 9" means the same thing for every participant.
 */
enum class ReadinessItem(val label: String) {
    WEATHER_CHECKED("Weather checked"),
    WATER_PACKED("Water packed"),
    FOOTWEAR("Suitable footwear"),
    NAVIGATION("Navigation available"),
    FIRST_AID("First aid available"),
    PHONE_CHARGED("Phone charged"),
    EMERGENCY_CONTACT("Emergency contact available"),
    CLOTHING("Appropriate clothing"),
    FOOD_PACKED("Food packed");

    companion object {
        val all: List<ReadinessItem> get() = entries
    }
}

/**
 * One participant's self-reported readiness for one trip.
 *
 * Readiness is per-user and writable only by that user (spec section 25) — no participant
 * may set another's state. This is enforced in the Firestore rules as well as the client.
 */
data class Readiness(
    val tripId: String,
    val uid: String,
    val checkedItems: Set<ReadinessItem> = emptySet(),
    /** 1..5 self-assessment, or null if not answered. */
    val confidence: Int? = null,
    val notes: String = "",
    val updatedAt: Long = 0L
) {
    val isComplete: Boolean get() = checkedItems.size == ReadinessItem.all.size
    val completedCount: Int get() = checkedItems.size
    val totalCount: Int get() = ReadinessItem.all.size
    val progressFraction: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

/**
 * An actual tracked outing, as distinct from the plan (spec section 61).
 *
 * A [Trip] is what people agreed to do; an [AdventureSession] is what one of them
 * actually did, with a real start time, real distance and a real end.
 */
data class AdventureSession(
    val id: String,
    val uid: String,
    val tripId: String?,
    val trailId: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val distanceKm: Double = 0.0,
    val durationMinutes: Long = 0L,
    val momentCount: Int = 0,
    val companionCount: Int = 0,
    val status: SessionStatus = SessionStatus.ACTIVE
)

enum class SessionStatus { ACTIVE, COMPLETED, ABANDONED }
