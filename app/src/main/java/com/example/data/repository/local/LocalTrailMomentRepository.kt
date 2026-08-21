package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.GeoPoint
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import com.example.domain.model.TrailMoment
import com.example.domain.model.UserProfile
import com.example.domain.repository.TrailMomentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalTrailMomentRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : TrailMomentRepository {

    override fun observeMomentsForTrail(trailId: String): Flow<List<TrailMoment>> =
        dao.observeMomentsForTrail(trailId).map { list -> list.map { it.toDomain() } }

    override fun observeMomentsForTrip(tripId: String): Flow<List<TrailMoment>> =
        dao.observeMomentsForTrip(tripId).map { list -> list.map { it.toDomain() } }

    override fun observeMomentsByUser(uid: String): Flow<List<TrailMoment>> =
        dao.observeMomentsByUser(uid).map { list -> list.map { it.toDomain() } }

    /**
     * Creates a moment at the device's current position.
     *
     * There is no coordinate parameter a caller could supply from a map tap — the only
     * position accepted is the one the location provider reported (spec section 34), so a
     * hazard report always means "this is here", never "this is somewhere I pointed at".
     */
    override suspend fun create(
        author: UserProfile,
        trailId: String,
        tripId: String?,
        deviceLocation: GeoPoint,
        category: MomentCategory,
        description: String,
        photoUrl: String?,
        visibility: MomentVisibility
    ): Result<TrailMoment> = runCatching {
        val text = description.trim()
        require(text.isNotEmpty()) { "Describe what you saw." }
        require(deviceLocation.latitude in -90.0..90.0 && deviceLocation.longitude in -180.0..180.0) {
            "Your location is not available yet."
        }

        val moment = TrailMoment(
            id = "moment_" + UUID.randomUUID().toString().replace("-", "").take(20),
            creatorId = author.uid,
            creatorName = author.displayName,
            trailId = trailId,
            tripId = tripId,
            latitude = deviceLocation.latitude,
            longitude = deviceLocation.longitude,
            category = category,
            description = text,
            photoUrl = photoUrl,
            visibility = visibility,
            createdAt = nowMillis()
        )
        dao.upsertMoment(moment.toEntity())
        moment
    }

    override suspend fun delete(momentId: String, byUid: String): Result<Unit> = runCatching {
        val moment = dao.getMoment(momentId) ?: return@runCatching
        require(moment.creatorId == byUid) { "You can only remove your own Trail Moments." }
        dao.deleteMoment(momentId)
    }

    override suspend fun upvote(momentId: String, byUid: String): Result<Unit> = runCatching {
        val moment = dao.getMoment(momentId) ?: error("That moment no longer exists.")
        require(moment.creatorId != byUid) { "You cannot upvote your own report." }
        dao.upsertMoment(moment.copy(upvotes = moment.upvotes + 1))
    }
}
