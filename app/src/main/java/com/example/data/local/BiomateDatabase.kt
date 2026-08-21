package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

/**
 * The Biomate local database.
 *
 * Room is the offline cache and, when no Firebase project is configured, the system of
 * record (spec section 65). Trails, the active trip and the signed-in profile stay
 * readable without a network, which matters most exactly when it is least available —
 * partway along a trail.
 *
 * All list-shaped fields are encoded to `String` columns by `data/mapper/Codec.kt`, so
 * there are no type converters to keep in step with the schema.
 */
@Database(
    entities = [
        ProfileEntity::class,
        LocalCredentialEntity::class,
        TrailEntity::class,
        SavedTrailEntity::class,
        ConnectionEntity::class,
        TripEntity::class,
        TripMemberEntity::class,
        GearItemEntity::class,
        ReadinessEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        TrailMomentEntity::class,
        DailyChallengeEntity::class,
        ChallengeSubmissionEntity::class,
        CoinTransactionEntity::class,
        EarnedBadgeEntity::class,
        AdventureSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BiomateDatabase : RoomDatabase() {

    abstract fun daoV2(): BiomateDaoV2

    companion object {
        @Volatile
        private var INSTANCE: BiomateDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BiomateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BiomateDatabase::class.java,
                    DATABASE_NAME
                )
                    // The prototype's schema shared no tables with this one and stored
                    // plaintext passwords, so there is nothing worth migrating. A new
                    // database file makes the break explicit rather than leaving dead
                    // tables behind on upgraded installs.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }

        private const val DATABASE_NAME = "biomate.db"
    }
}
