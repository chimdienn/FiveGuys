package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.local.EarnedBadgeEntity
import com.example.data.mapper.toDomain
import com.example.domain.badge.BadgeRules
import com.example.domain.model.BadgeId
import com.example.domain.model.EarnedBadge
import com.example.domain.model.UserStats
import com.example.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalBadgeRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : BadgeRepository {

    override fun observeEarned(uid: String): Flow<List<EarnedBadge>> =
        dao.observeEarnedBadges(uid).map { list -> list.mapNotNull { it.toDomain() } }

    /**
     * Awards any badge the user now qualifies for.
     *
     * Insert conflicts are ignored and the earned row is keyed on (uid, badgeId), so a
     * badge can never be granted twice and `earnedAt` keeps the date it was first earned.
     */
    override suspend fun evaluateAndAward(uid: String, stats: UserStats): List<BadgeId> {
        val already = dao.getEarnedBadges(uid)
            .mapNotNull { entity -> BadgeId.entries.firstOrNull { it.name == entity.badgeId } }
            .toSet()

        val newly = BadgeRules.newlyEarned(stats, already)
        if (newly.isEmpty()) return emptyList()

        val now = nowMillis()
        dao.insertBadges(newly.map { EarnedBadgeEntity(uid = uid, badgeId = it.name, earnedAt = now) })
        return newly.toList()
    }
}
