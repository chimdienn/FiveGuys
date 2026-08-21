package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.AdventureSession
import com.example.domain.model.SessionStatus
import com.example.domain.model.TripMemberStatus
import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalSessionRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : SessionRepository {

    override fun observeActiveSession(uid: String): Flow<AdventureSession?> =
        dao.observeActiveSession(uid).map { it?.toDomain() }

    override fun observeCompletedSessions(uid: String): Flow<List<AdventureSession>> =
        dao.observeCompletedSessions(uid).map { list -> list.map { it.toDomain() } }

    override suspend fun start(uid: String, trailId: String, tripId: String?): Result<AdventureSession> = runCatching {
        val session = AdventureSession(
            id = "session_" + UUID.randomUUID().toString().replace("-", "").take(20),
            uid = uid,
            tripId = tripId,
            trailId = trailId,
            startedAt = nowMillis(),
            status = SessionStatus.ACTIVE
        )
        dao.upsertSession(session.toEntity())
        session
    }

    /**
     * Persists distance as the walk progresses.
     *
     * Only the running total is written — individual GPS fixes stay in memory. Storing
     * every point would be a needless privacy liability and would flood the database
     * (spec section 61).
     */
    override suspend fun updateProgress(sessionId: String, distanceKm: Double): Result<Unit> = runCatching {
        val session = dao.getSession(sessionId) ?: return@runCatching
        if (session.status != SessionStatus.ACTIVE.name) return@runCatching
        dao.upsertSession(session.copy(distanceKm = distanceKm))
    }

    override suspend fun complete(
        sessionId: String,
        distanceKm: Double,
        durationMinutes: Long
    ): Result<AdventureSession> = runCatching {
        val stored = dao.getSession(sessionId) ?: error("That adventure is no longer active.")
        // Completing twice must not double the user's statistics.
        if (stored.status == SessionStatus.COMPLETED.name) return@runCatching stored.toDomain()

        val companions = stored.tripId?.let { tripId ->
            (dao.getMembers(tripId).count { it.status == TripMemberStatus.JOINED.name } - 1).coerceAtLeast(0)
        } ?: 0

        val completed = stored.copy(
            completedAt = nowMillis(),
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            momentCount = stored.tripId?.let { dao.momentCountForTrip(it) } ?: 0,
            companionCount = companions,
            status = SessionStatus.COMPLETED.name
        )
        dao.upsertSession(completed)
        completed.toDomain()
    }

    override suspend fun abandon(sessionId: String): Result<Unit> = runCatching {
        val stored = dao.getSession(sessionId) ?: return@runCatching
        if (stored.status != SessionStatus.ACTIVE.name) return@runCatching
        dao.upsertSession(stored.copy(status = SessionStatus.ABANDONED.name, completedAt = nowMillis()))
    }
}
