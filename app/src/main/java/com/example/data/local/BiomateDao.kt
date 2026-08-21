package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface BiomateDao {

    // Trails
    @Query("SELECT * FROM trails ORDER BY rating DESC")
    fun getAllTrails(): Flow<List<Trail>>

    @Query("SELECT * FROM trails WHERE id = :trailId LIMIT 1")
    fun getTrailById(trailId: String): Flow<Trail?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrails(trails: List<Trail>)

    @Update
    suspend fun updateTrail(trail: Trail)

    // Hike Matches
    @Query("SELECT * FROM hike_matches ORDER BY matchScore DESC")
    fun getAllHikeBuddies(): Flow<List<HikeBuddy>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHikeBuddies(buddies: List<HikeBuddy>)

    @Update
    suspend fun updateHikeBuddy(buddy: HikeBuddy)

    // Trips
    @Query("SELECT * FROM trips ORDER BY departureDate ASC")
    fun getAllTrips(): Flow<List<TripPlan>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: String): Flow<TripPlan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripPlan)

    @Update
    suspend fun updateTrip(trip: TripPlan)

    // Trip Participants
    @Query("SELECT * FROM trip_participants WHERE tripId = :tripId")
    fun getParticipantsForTrip(tripId: String): Flow<List<TripParticipant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<TripParticipant>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: TripParticipant)

    // Shared Gear
    @Query("SELECT * FROM shared_gear WHERE tripId = :tripId")
    fun getGearForTrip(tripId: String): Flow<List<SharedGearItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGearItems(items: List<SharedGearItem>)

    @Update
    suspend fun updateGearItem(item: SharedGearItem)

    // Trip Meals
    @Query("SELECT * FROM trip_meals WHERE tripId = :tripId")
    fun getMealsForTrip(tripId: String): Flow<List<TripMeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: TripMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<TripMeal>)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE tripId = :tripId ORDER BY id ASC")
    fun getMessagesForTrip(tripId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // Trail Moments / Hazard Reporting
    @Query("SELECT * FROM trail_moments WHERE trailId = :trailId ORDER BY upvotes DESC")
    fun getMomentsForTrail(trailId: String): Flow<List<TrailMoment>>

    @Query("SELECT * FROM trail_moments ORDER BY upvotes DESC")
    fun getAllMoments(): Flow<List<TrailMoment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailMoment(moment: TrailMoment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailMoments(moments: List<TrailMoment>)

    @Update
    suspend fun updateTrailMoment(moment: TrailMoment)

    // Species Scan / Field Journal
    @Query("SELECT * FROM species_scans ORDER BY timestamp DESC")
    fun getAllSpeciesScans(): Flow<List<SpeciesScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeciesScan(scan: SpeciesScan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeciesScans(scans: List<SpeciesScan>)

    // Adventure Stories
    @Query("SELECT * FROM adventure_stories")
    fun getAllAdventureStories(): Flow<List<AdventureStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdventureStory(story: AdventureStory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdventureStories(stories: List<AdventureStory>)

    // Community Groups
    @Query("SELECT * FROM community_groups")
    fun getAllCommunityGroups(): Flow<List<CommunityGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityGroups(groups: List<CommunityGroup>)

    @Update
    suspend fun updateCommunityGroup(group: CommunityGroup)

    // Challenges
    @Query("SELECT * FROM challenges")
    fun getAllChallenges(): Flow<List<AdventureChallenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<AdventureChallenge>)

    @Update
    suspend fun updateChallenge(challenge: AdventureChallenge)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // User Accounts & Authentication
    @Query("SELECT * FROM user_accounts ORDER BY name ASC")
    fun getAllUserAccounts(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    fun getUserAccountById(id: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserAccount(): Flow<UserAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(account: UserAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccounts(accounts: List<UserAccount>)

    @Update
    suspend fun updateUserAccount(account: UserAccount)

    @Query("UPDATE user_accounts SET isLoggedIn = 0")
    suspend fun clearLoggedInStatus()

    @Query("UPDATE user_accounts SET isLoggedIn = 1 WHERE id = :userId")
    suspend fun setLoggedInUser(userId: String)
}
