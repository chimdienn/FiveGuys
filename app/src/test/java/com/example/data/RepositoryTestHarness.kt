package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BiomateDaoV2
import com.example.data.local.BiomateDatabase
import com.example.domain.model.UserProfile

/**
 * Shared setup for repository tests.
 *
 * Uses a real in-memory Room database rather than a mock DAO on purpose: the invariants
 * under test — the UNIQUE index on `idempotencyKey`, the ABORT conflict strategy on
 * submissions, the transactional award — live in SQLite, and a mocked DAO would happily
 * pretend they hold while proving nothing.
 */
object RepositoryTestHarness {

    fun inMemoryDatabase(): BiomateDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BiomateDatabase::class.java
        ).allowMainThreadQueries().build()

    fun dao(database: BiomateDatabase): BiomateDaoV2 = database.daoV2()

    fun profile(uid: String, name: String = uid) = UserProfile(uid = uid, displayName = name)
}
