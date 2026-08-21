package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.CoinTransaction
import com.example.domain.repository.AwardOutcome
import com.example.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * The BioCoin ledger for the local backend.
 *
 * A balance is never stored; it is `SUM(amount)` over an append-only transaction table.
 * That removes the entire class of bug where a balance and its history disagree, and it
 * means there is no writable "coins" field for a client bug — or a tampered build — to
 * set directly.
 *
 * Duplicate protection is structural rather than procedural: `idempotencyKey` carries a
 * UNIQUE index and the check-then-insert runs inside a single Room transaction, so a
 * replayed award is rejected by SQLite rather than by a race-prone `if` statement.
 *
 * On the Firebase backend the equivalent authority lives in a Cloud Function
 * (`functions/index.js`), because there a client could otherwise write the row itself.
 */
class LocalRewardRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : RewardRepository {

    override fun observeBalance(uid: String): Flow<Int> = dao.observeBalance(uid)

    override fun observeTransactions(uid: String): Flow<List<CoinTransaction>> =
        dao.observeTransactions(uid).map { list -> list.map { it.toDomain() } }

    override suspend fun award(
        uid: String,
        amount: Int,
        reason: String,
        idempotencyKey: String,
        challengeId: String?,
        referenceId: String?
    ): Result<AwardOutcome> = runCatching {
        if (amount <= 0) {
            return@runCatching AwardOutcome.Rejected("Award amount must be positive.")
        }
        if (idempotencyKey.isBlank()) {
            return@runCatching AwardOutcome.Rejected("An idempotency key is required.")
        }

        val candidate = CoinTransaction(
            id = "tx_" + UUID.randomUUID().toString().replace("-", "").take(20),
            uid = uid,
            amount = amount,
            reason = reason,
            challengeId = challengeId,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            createdAt = nowMillis()
        )

        val stored = dao.awardOnce(candidate.toEntity()).toDomain()
        if (stored.id != candidate.id) {
            AwardOutcome.AlreadyAwarded(stored)
        } else {
            AwardOutcome.Granted(stored, dao.getBalance(uid))
        }
    }
}
