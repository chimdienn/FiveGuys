package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.GearItem
import com.example.domain.model.Readiness
import com.example.domain.model.ReadinessItem
import com.example.domain.model.Trip
import com.example.domain.model.TripMember
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalTripRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : TripRepository {

    override fun observeTripsForUser(uid: String): Flow<List<Trip>> =
        dao.observeTripsForUser(uid).map { list -> list.map { it.toDomain() } }

    override fun observeTrip(tripId: String): Flow<Trip?> =
        dao.observeTrip(tripId).map { it?.toDomain() }

    override fun observeMembers(tripId: String): Flow<List<TripMember>> =
        dao.observeMembers(tripId).map { list -> list.map { it.toDomain() } }

    override fun observeGear(tripId: String): Flow<List<GearItem>> =
        dao.observeGear(tripId).map { list -> list.map { it.toDomain() } }

    override fun observeReadiness(tripId: String): Flow<List<Readiness>> =
        dao.observeReadiness(tripId).map { list -> list.map { it.toDomain() } }

    override fun observeMyReadiness(tripId: String, uid: String): Flow<Readiness> =
        dao.observeMyReadiness(tripId, uid).map { it?.toDomain() ?: Readiness(tripId, uid) }

    override suspend fun createTrip(trip: Trip, organiser: UserProfile): Result<String> = runCatching {
        require(trip.title.isNotBlank()) { "Give the trip a title." }
        val now = nowMillis()
        val id = trip.id.ifBlank { "trip_" + UUID.randomUUID().toString().replace("-", "").take(16) }
        val stored = trip.copy(
            id = id,
            creatorId = organiser.uid,
            status = TripStatus.PLANNING,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertTrip(stored.toEntity())

        // The creator is a joined organiser from the outset — a trip with nobody on it
        // would otherwise be invisible to its own author.
        dao.upsertMember(
            TripMember(
                tripId = id,
                uid = organiser.uid,
                displayName = organiser.displayName,
                role = TripRole.ORGANISER,
                status = TripMemberStatus.JOINED,
                joinedAt = now
            ).toEntity()
        )
        dao.insertGearIfAbsent(defaultGear(id).map { it.toEntity() })
        id
    }

    override suspend fun updateTrip(trip: Trip): Result<Unit> = runCatching {
        val existing = dao.getTrip(trip.id) ?: error("Trip not found.")
        dao.upsertTrip(trip.copy(createdAt = existing.createdAt, updatedAt = nowMillis()).toEntity())
    }

    override suspend fun setStatus(tripId: String, status: TripStatus): Result<Unit> = runCatching {
        val now = nowMillis()
        val completedAt = if (status == TripStatus.COMPLETED) now else null
        dao.setTripStatus(tripId, status.name, now, completedAt)
        if (status == TripStatus.COMPLETED) {
            // Everyone who joined is credited with attending (spec section 54).
            dao.markJoinedMembersAttended(tripId)
        }
    }

    override suspend fun invite(tripId: String, invitee: UserProfile, byUid: String): Result<Unit> = runCatching {
        val trip = dao.getTrip(tripId) ?: error("Trip not found.")
        require(trip.creatorId == byUid) { "Only the organiser can invite people." }
        if (dao.getMember(tripId, invitee.uid) != null) return@runCatching
        dao.upsertMember(
            TripMember(
                tripId = tripId,
                uid = invitee.uid,
                displayName = invitee.displayName,
                role = TripRole.PARTICIPANT,
                status = TripMemberStatus.INVITED
            ).toEntity()
        )
    }

    override suspend fun join(tripId: String, profile: UserProfile): Result<Unit> = runCatching {
        val trip = dao.getTrip(tripId) ?: error("Trip not found.")
        require(trip.status != TripStatus.CANCELLED.name) { "That trip was cancelled." }

        val members = dao.getMembers(tripId)
        val joined = members.count { it.status == TripMemberStatus.JOINED.name }
        val limit = trip.participantLimit
        val alreadyOnTrip = members.any { it.uid == profile.uid && it.status == TripMemberStatus.JOINED.name }
        require(limit == null || joined < limit || alreadyOnTrip) { "This trip is full." }

        val existing = dao.getMember(tripId, profile.uid)
        dao.upsertMember(
            TripMember(
                tripId = tripId,
                uid = profile.uid,
                displayName = profile.displayName,
                role = if (trip.creatorId == profile.uid) TripRole.ORGANISER else TripRole.PARTICIPANT,
                status = TripMemberStatus.JOINED,
                joinedAt = existing?.joinedAt ?: nowMillis(),
                attended = existing?.attended ?: false
            ).toEntity()
        )
    }

    override suspend fun leave(tripId: String, uid: String): Result<Unit> = runCatching {
        val trip = dao.getTrip(tripId) ?: error("Trip not found.")
        require(trip.creatorId != uid) {
            "You organised this trip — cancel it instead of leaving."
        }
        dao.setMemberStatus(tripId, uid, TripMemberStatus.LEFT.name)
    }

    override suspend fun addGear(item: GearItem): Result<Unit> = runCatching {
        require(item.name.isNotBlank()) { "Give the item a name." }
        dao.upsertGear(
            item.copy(id = item.id.ifBlank { "gear_" + UUID.randomUUID().toString().take(12) }).toEntity()
        )
    }

    override suspend fun assignGear(itemId: String, uid: String?, displayName: String?): Result<Unit> = runCatching {
        val item = dao.getGear(itemId) ?: error("That item no longer exists.")
        dao.upsertGear(item.copy(assignedToUid = uid, assignedToName = displayName))
    }

    /**
     * Marks an item packed.
     *
     * Only the assignee — or the organiser — may flip the flag. Letting anyone tick
     * anyone else's gear would make the checklist worthless as a pre-departure signal.
     */
    override suspend fun setGearPacked(itemId: String, packed: Boolean, byUid: String): Result<Unit> = runCatching {
        val item = dao.getGear(itemId) ?: error("That item no longer exists.")
        val trip = dao.getTrip(item.tripId) ?: error("Trip not found.")
        require(item.assignedToUid == null || item.assignedToUid == byUid || trip.creatorId == byUid) {
            "Only ${item.assignedToName ?: "the assignee"} can tick that off."
        }
        dao.upsertGear(item.copy(isPacked = packed))
    }

    override suspend fun removeGear(itemId: String, byUid: String): Result<Unit> = runCatching {
        val item = dao.getGear(itemId) ?: return@runCatching
        val trip = dao.getTrip(item.tripId) ?: error("Trip not found.")
        require(trip.creatorId == byUid || item.assignedToUid == byUid) {
            "Only the organiser can remove shared gear."
        }
        dao.deleteGear(itemId)
    }

    /** Writes readiness for [uid] only — there is no parameter for editing anyone else's. */
    override suspend fun setReadiness(
        tripId: String,
        uid: String,
        items: Set<ReadinessItem>,
        confidence: Int?,
        notes: String
    ): Result<Unit> = runCatching {
        dao.upsertReadiness(
            Readiness(
                tripId = tripId,
                uid = uid,
                checkedItems = items,
                confidence = confidence?.coerceIn(1, 5),
                notes = notes,
                updatedAt = nowMillis()
            ).toEntity()
        )
    }

    private fun defaultGear(tripId: String): List<GearItem> = listOf(
        GearItem(newGearId(), tripId, "First Aid Kit", "Safety", isEssential = true),
        GearItem(newGearId(), tripId, "Water Filter", "Safety", isEssential = true),
        GearItem(newGearId(), tripId, "Stove", "Cooking"),
        GearItem(newGearId(), tripId, "Tent", "Shelter"),
        GearItem(newGearId(), tripId, "Map & Compass", "Navigation", isEssential = true)
    )

    private fun newGearId() = "gear_" + UUID.randomUUID().toString().replace("-", "").take(12)
}
