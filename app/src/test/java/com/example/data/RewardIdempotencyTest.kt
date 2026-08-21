package com.example.data

import com.example.data.local.BiomateDatabase
import com.example.data.repository.local.LocalRewardRepository
import com.example.domain.repository.AwardOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The single most important guarantee in the BioCoin system: a reward is paid once.
 *
 * "A challenge may NEVER award twice" (spec section 42), so these tests attack the
 * property from several directions — repeat calls, concurrent calls, and a balance read
 * afterwards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RewardIdempotencyTest {

    private lateinit var database: BiomateDatabase
    private lateinit var rewards: LocalRewardRepository

    @Before
    fun setUp() {
        database = RepositoryTestHarness.inMemoryDatabase()
        rewards = LocalRewardRepository(RepositoryTestHarness.dao(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a valid award grants coins`() = runTest {
        val outcome = rewards.award("u1", 50, "Challenge complete", "challenge:d1").getOrThrow()
        assertTrue(outcome is AwardOutcome.Granted)
        assertEquals(50, (outcome as AwardOutcome.Granted).newBalance)
    }

    @Test
    fun `the same idempotency key never pays twice`() = runTest {
        val first = rewards.award("u1", 50, "Challenge complete", "challenge:d1").getOrThrow()
        val second = rewards.award("u1", 50, "Challenge complete", "challenge:d1").getOrThrow()

        assertTrue(first is AwardOutcome.Granted)
        assertTrue(second is AwardOutcome.AlreadyAwarded)
        assertEquals(50, rewards.observeBalance("u1").first())
    }

    @Test
    fun `a replayed award returns the original transaction`() = runTest {
        val first = rewards.award("u1", 50, "Challenge complete", "challenge:d1").getOrThrow()
        val second = rewards.award("u1", 50, "Challenge complete", "challenge:d1").getOrThrow()

        val original = (first as AwardOutcome.Granted).transaction
        val replay = (second as AwardOutcome.AlreadyAwarded).existing
        assertEquals(original.id, replay.id)
        assertEquals(original.createdAt, replay.createdAt)
    }

    @Test
    fun `ten repeated awards still pay once`() = runTest {
        repeat(10) { rewards.award("u1", 50, "Challenge complete", "challenge:d1") }
        assertEquals(50, rewards.observeBalance("u1").first())
        assertEquals(1, rewards.observeTransactions("u1").first().size)
    }

    @Test
    fun `concurrent awards with the same key pay once`() = runTest {
        val results = (1..8).map {
            async { rewards.award("u1", 25, "Challenge complete", "challenge:d1") }
        }.awaitAll()

        val granted = results.mapNotNull { it.getOrNull() }.count { it is AwardOutcome.Granted }
        assertEquals(1, granted)
        assertEquals(25, rewards.observeBalance("u1").first())
    }

    @Test
    fun `different keys award separately`() = runTest {
        rewards.award("u1", 50, "Challenge A", "challenge:d1")
        rewards.award("u1", 30, "Challenge B", "challenge:d2")
        assertEquals(80, rewards.observeBalance("u1").first())
    }

    @Test
    fun `awards are scoped to a user`() = runTest {
        rewards.award("u1", 50, "Challenge", "challenge:u1:d1")
        rewards.award("u2", 70, "Challenge", "challenge:u2:d1")
        assertEquals(50, rewards.observeBalance("u1").first())
        assertEquals(70, rewards.observeBalance("u2").first())
    }

    @Test
    fun `a zero or negative award is rejected`() = runTest {
        assertTrue(rewards.award("u1", 0, "Nothing", "k1").getOrThrow() is AwardOutcome.Rejected)
        assertTrue(rewards.award("u1", -100, "Theft", "k2").getOrThrow() is AwardOutcome.Rejected)
        assertEquals(0, rewards.observeBalance("u1").first())
    }

    @Test
    fun `an award without an idempotency key is rejected`() = runTest {
        assertTrue(rewards.award("u1", 50, "Challenge", "").getOrThrow() is AwardOutcome.Rejected)
        assertEquals(0, rewards.observeBalance("u1").first())
    }

    @Test
    fun `balance is the sum of the ledger`() = runTest {
        rewards.award("u1", 50, "A", "k1")
        rewards.award("u1", 30, "B", "k2")
        rewards.award("u1", 20, "C", "k3")

        val transactions = rewards.observeTransactions("u1").first()
        assertEquals(3, transactions.size)
        assertEquals(transactions.sumOf { it.amount }, rewards.observeBalance("u1").first())
    }
}
