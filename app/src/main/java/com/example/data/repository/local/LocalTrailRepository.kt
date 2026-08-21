package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.local.SavedTrailEntity
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.seed.SeedTrails
import com.example.domain.model.Trail
import com.example.domain.repository.TrailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalTrailRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : TrailRepository {

    override fun observeTrails(): Flow<List<Trail>> =
        dao.observeTrails().map { list -> list.map { it.toDomain() } }

    override fun observeTrail(id: String): Flow<Trail?> =
        dao.observeTrail(id).map { it?.toDomain() }

    override fun observeSavedTrailIds(uid: String): Flow<Set<String>> =
        dao.observeSavedTrailIds(uid).map { it.toSet() }

    override suspend fun getTrail(id: String): Trail? = dao.getTrail(id)?.toDomain()

    override suspend fun setSaved(uid: String, trailId: String, saved: Boolean) {
        if (saved) {
            dao.saveTrail(SavedTrailEntity(uid = uid, trailId = trailId, savedAt = nowMillis()))
        } else {
            dao.unsaveTrail(uid, trailId)
        }
    }

    /**
     * Populates the shared trail catalogue on first run.
     *
     * Trails are reference data rather than user data, so this is an upsert of a fixed
     * list and is safe to call on every launch.
     */
    override suspend fun ensureSeeded() {
        if (dao.trailCount() >= SeedTrails.all.size) return
        dao.upsertTrails(SeedTrails.all.map { it.toEntity() })
    }
}
