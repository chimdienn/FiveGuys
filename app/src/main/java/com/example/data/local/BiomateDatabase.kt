package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdventureChallenge
import com.example.data.model.AdventureStory
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityGroup
import com.example.data.model.HikeBuddy
import com.example.data.model.SharedGearItem
import com.example.data.model.SpeciesScan
import com.example.data.model.Trail
import com.example.data.model.TrailMoment
import com.example.data.model.TripMeal
import com.example.data.model.TripParticipant
import com.example.data.model.TripPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Trail::class,
        HikeBuddy::class,
        TripPlan::class,
        TripParticipant::class,
        SharedGearItem::class,
        TripMeal::class,
        ChatMessage::class,
        TrailMoment::class,
        SpeciesScan::class,
        AdventureStory::class,
        CommunityGroup::class,
        AdventureChallenge::class,
        UserProfile::class,
        UserAccount::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BiomateDatabase : RoomDatabase() {

    abstract fun biomateDao(): BiomateDao

    companion object {
        @Volatile
        private var INSTANCE: BiomateDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BiomateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BiomateDatabase::class.java,
                    "biomate_database"
                )
                    .addCallback(BiomateDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class BiomateDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.biomateDao())
                }
            }
        }
    }
}
