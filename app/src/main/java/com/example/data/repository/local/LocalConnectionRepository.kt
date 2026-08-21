package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Connection
import com.example.domain.model.ConnectionStatus
import com.example.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalConnectionRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ConnectionRepository {

    override fun observeConnections(uid: String): Flow<List<Connection>> =
        dao.observeConnections(uid).map { list -> list.map { it.toDomain() } }

    override fun observeAcceptedConnectionIds(uid: String): Flow<Set<String>> =
        dao.observeConnections(uid).map { list ->
            list.filter { it.status == ConnectionStatus.ACCEPTED.name }
                .map { if (it.requesterId == uid) it.addresseeId else it.requesterId }
                .toSet()
        }

    override fun observeIncomingRequests(uid: String): Flow<List<Connection>> =
        dao.observeIncomingRequests(uid).map { list -> list.map { it.toDomain() } }

    /**
     * Sends a request, or resurrects a previously rejected one.
     *
     * The id is derived from the two uids, so a simultaneous request in both directions
     * converges on a single document instead of creating a pair of mirror-image requests
     * that each wait for the other.
     */
    override suspend fun sendRequest(requesterId: String, addresseeId: String): Result<Unit> = runCatching {
        require(requesterId != addresseeId) { "You cannot connect with yourself." }
        val id = Connection.connectionIdFor(requesterId, addresseeId)
        val existing = dao.getConnection(id)

        when (existing?.status) {
            ConnectionStatus.ACCEPTED.name -> return@runCatching
            ConnectionStatus.PENDING.name -> {
                // The other person already asked us — treat this as an acceptance.
                if (existing.requesterId == addresseeId) {
                    dao.upsertConnection(
                        existing.copy(status = ConnectionStatus.ACCEPTED.name, respondedAt = nowMillis())
                    )
                }
                return@runCatching
            }
        }

        dao.upsertConnection(
            Connection(
                id = id,
                requesterId = requesterId,
                addresseeId = addresseeId,
                status = ConnectionStatus.PENDING,
                createdAt = nowMillis()
            ).toEntity()
        )
    }

    override suspend fun respond(connectionId: String, status: ConnectionStatus): Result<Unit> = runCatching {
        val existing = dao.getConnection(connectionId) ?: error("That request no longer exists.")
        dao.upsertConnection(existing.copy(status = status.name, respondedAt = nowMillis()))
    }

    override suspend fun remove(connectionId: String): Result<Unit> = runCatching {
        dao.deleteConnection(connectionId)
    }

    override suspend fun statusBetween(a: String, b: String): ConnectionStatus? =
        dao.getConnection(Connection.connectionIdFor(a, b))?.toDomain()?.status
}
