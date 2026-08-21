package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Readiness
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStats
import com.example.domain.repository.LeaderboardEntry
import com.example.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

class LocalProfileRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ProfileRepository {

    override fun observeProfile(uid: String): Flow<UserProfile?> =
        dao.observeProfile(uid).map { it?.toDomain() }

    override fun observeAllProfiles(): Flow<List<UserProfile>> =
        dao.observeAllProfiles().map { list -> list.map { it.toDomain() } }

    /**
     * Statistics recomputed from activity on every relevant write.
     *
     * The flow re-derives rather than reading a stored counter, so the number a user sees
     * is always the number their history supports (spec section 53).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStats(uid: String): Flow<UserStats> = combine(
        dao.observeCompletedSessions(uid),
        dao.observeMomentsByUser(uid),
        dao.observeBalance(uid),
        dao.observeEarnedBadges(uid)
    ) { _, _, _, _ -> Unit }
        .flatMapLatest { flow { emit(recomputeStats(uid)) } }

    override fun observeLeaderboard(limit: Int): Flow<List<LeaderboardEntry>> =
        combine(dao.observeTopBalances(limit), dao.observeAllProfiles()) { balances, profiles ->
            val byUid = profiles.associateBy { it.uid }
            balances.mapIndexedNotNull { index, row ->
                val profile = byUid[row.uid] ?: return@mapIndexedNotNull null
                LeaderboardEntry(
                    rank = index + 1,
                    uid = row.uid,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    avatarColorHex = profile.avatarColorHex,
                    bioCoins = row.coins
                )
            }
        }

    override suspend fun getProfile(uid: String): UserProfile? = dao.getProfile(uid)?.toDomain()

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        val existing = dao.getProfile(profile.uid)
        dao.upsertProfile(
            profile.copy(
                createdAt = existing?.createdAt ?: profile.createdAt.takeIf { it > 0 } ?: nowMillis(),
                updatedAt = nowMillis()
            ).toEntity()
        )
    }

    override suspend fun searchByDisplayName(query: String): List<UserProfile> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()
        return dao.searchProfiles(trimmed).map { it.toDomain() }
    }

    override suspend fun recomputeStats(uid: String): UserStats {
        val sessions = dao.getCompletedSessions(uid)
        val memberships = dao.getMembershipsForUser(uid)

        // A trip counts as a "group" trip when someone other than this user was on it.
        // Cancelled trips are excluded on both sides of the attendance ratio so that
        // calling off a trip never damages anyone's rate (spec section 54).
        var groupTripsJoined = 0
        var groupTripsAttended = 0
        for (membership in memberships) {
            val trip = dao.getTrip(membership.tripId) ?: continue
            if (trip.status == TripStatus.CANCELLED.name) continue
            val joinedMembers = dao.getMembers(membership.tripId)
                .count { it.status == TripMemberStatus.JOINED.name }
            if (joinedMembers < 2) continue
            if (membership.status != TripMemberStatus.JOINED.name) continue
            groupTripsJoined++
            if (membership.attended) groupTripsAttended++
        }

        val readinessCompleted = dao.getReadinessForUser(uid)
            .map { it.toDomain() }
            .count(Readiness::isComplete)

        return UserStats(
            uid = uid,
            // One completed session is one trail walked end to end, so a repeat visit
            // counts again — the badge is "complete five trails", not "visit five places".
            trailsCompleted = sessions.size,
            totalDistanceKm = sessions.sumOf { it.distanceKm },
            totalDurationMinutes = sessions.sumOf { it.durationMinutes },
            tripsCompleted = sessions.mapNotNull { it.tripId }.distinct().size,
            groupTripsCompleted = groupTripsAttended,
            groupTripsJoined = groupTripsJoined,
            trailMomentsCreated = dao.momentCountForUser(uid),
            readinessChecklistsCompleted = readinessCompleted,
            bioCoins = dao.getBalance(uid),
            badgeCount = dao.getEarnedBadges(uid).size
        )
    }
}
