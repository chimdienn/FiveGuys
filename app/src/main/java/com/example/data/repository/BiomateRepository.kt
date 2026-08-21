package com.example.data.repository

import com.example.data.ai.ContextualHikeRecommendation
import com.example.data.ai.GeminiOutdoorService
import com.example.data.ai.IdentifiedSpeciesResult
import com.example.data.local.BiomateDao
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BiomateRepository(private val dao: BiomateDao) {

    // Trails
    val allTrails: Flow<List<Trail>> = dao.getAllTrails()

    fun getTrailById(trailId: String): Flow<Trail?> = dao.getTrailById(trailId)

    suspend fun toggleSaveTrail(trail: Trail) {
        dao.updateTrail(trail.copy(isSaved = !trail.isSaved))
    }

    suspend fun toggleVisitedTrail(trail: Trail) {
        dao.updateTrail(trail.copy(isVisited = !trail.isVisited))
    }

    // HikeMatch
    val allHikeBuddies: Flow<List<HikeBuddy>> = dao.getAllHikeBuddies()

    suspend fun updateBuddyStatus(buddy: HikeBuddy, newStatus: String) {
        dao.updateHikeBuddy(buddy.copy(matchStatus = newStatus))
    }

    // Collaborative Trip Planning
    val allTrips: Flow<List<TripPlan>> = dao.getAllTrips()

    fun getTripById(tripId: String): Flow<TripPlan?> = dao.getTripById(tripId)

    fun getParticipantsForTrip(tripId: String): Flow<List<TripParticipant>> =
        dao.getParticipantsForTrip(tripId)

    fun getGearForTrip(tripId: String): Flow<List<SharedGearItem>> =
        dao.getGearForTrip(tripId)

    fun getMealsForTrip(tripId: String): Flow<List<TripMeal>> =
        dao.getMealsForTrip(tripId)

    fun getMessagesForTrip(tripId: String): Flow<List<ChatMessage>> =
        dao.getMessagesForTrip(tripId)

    suspend fun createNewTrip(
        title: String,
        trail: Trail,
        date: String,
        meetingPoint: String,
        carpoolSeats: Int,
        organizerName: String = "Alex Rivera (You)"
    ): String {
        val tripId = "trip_" + UUID.randomUUID().toString().take(8)
        val newTrip = TripPlan(
            id = tripId,
            title = title,
            trailId = trail.id,
            trailName = trail.title,
            departureDate = date,
            returnDate = date,
            departureTime = "07:00 AM",
            meetingPoint = meetingPoint,
            status = "PLANNING",
            maxParticipants = 6,
            organizerName = organizerName,
            carpoolSeatsTotal = carpoolSeats,
            carpoolSeatsTaken = 1,
            emergencyContactInfo = "VIC Parks Ranger (03) 8427 2000 • 000 Emergency",
            weatherForecast = "${trail.currentTempC}°C, ${trail.weatherCondition}, Fire Danger: ${trail.fireDangerLevel}",
            safetyScore = 90
        )
        dao.insertTrip(newTrip)

        // Add creator as participant
        val participant = TripParticipant(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            name = organizerName,
            role = "Trip Leader & Organizer",
            avatarColor = 0xFF1A5340,
            carpoolRole = "Driver ($carpoolSeats seats)",
            isReady = true
        )
        dao.insertParticipant(participant)

        // Seed basic essential shared gear for the trail
        val basicGear = listOf(
            SharedGearItem(UUID.randomUUID().toString(), tripId, "Wilderness First Aid Kit", "Safety", organizerName, isEssential = true, isPacked = true),
            SharedGearItem(UUID.randomUUID().toString(), tripId, "Microfilter Water Purifier", "Safety", "Unassigned", isEssential = true, isPacked = false),
            SharedGearItem(UUID.randomUUID().toString(), tripId, "Camping Cook Stove + Gas", "Cooking", "Unassigned", isEssential = true, isPacked = false),
            SharedGearItem(UUID.randomUUID().toString(), tripId, "Topographic Offline Map / GPS", "Navigation", organizerName, isEssential = true, isPacked = true)
        )
        dao.insertGearItems(basicGear)

        return tripId
    }

    suspend fun toggleGearPacked(item: SharedGearItem) {
        dao.updateGearItem(item.copy(isPacked = !item.isPacked))
    }

    suspend fun assignGearItem(item: SharedGearItem, assignedTo: String) {
        dao.updateGearItem(item.copy(assignedTo = assignedTo))
    }

    suspend fun addSharedGear(tripId: String, name: String, category: String, assignedTo: String, essential: Boolean) {
        val item = SharedGearItem(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            name = name,
            category = category,
            assignedTo = assignedTo.ifBlank { "Unassigned" },
            isEssential = essential,
            isPacked = false
        )
        dao.insertGearItems(listOf(item))
    }

    suspend fun addTripMeal(tripId: String, mealName: String, mealType: String, assignedTo: String, notes: String) {
        val meal = TripMeal(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            mealName = mealName,
            mealType = mealType,
            assignedTo = assignedTo,
            dietaryInfo = notes
        )
        dao.insertMeal(meal)
    }

    suspend fun sendChatMessage(tripId: String, channel: String, senderName: String, messageText: String, isEmergency: Boolean = false) {
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val msg = ChatMessage(
            tripId = tripId,
            channel = channel,
            senderName = senderName,
            messageText = messageText,
            timestamp = timeStr,
            isEmergency = isEmergency
        )
        dao.insertChatMessage(msg)
    }

    // Trail Moments / Hazard Reporting
    val allMoments: Flow<List<TrailMoment>> = dao.getAllMoments()

    fun getMomentsForTrail(trailId: String): Flow<List<TrailMoment>> = dao.getMomentsForTrail(trailId)

    suspend fun reportTrailMoment(
        trailId: String,
        type: String,
        title: String,
        description: String,
        reportedBy: String,
        kmMarker: Double,
        warningLevel: String = "INFO"
    ) {
        val moment = TrailMoment(
            id = "tm_" + UUID.randomUUID().toString().take(6),
            trailId = trailId,
            type = type,
            title = title,
            description = description,
            reportedBy = reportedBy,
            timeAgo = "Just now",
            upvotes = 1,
            isVerified = false,
            kmMarker = kmMarker,
            warningLevel = warningLevel
        )
        dao.insertTrailMoment(moment)
    }

    suspend fun upvoteMoment(moment: TrailMoment) {
        dao.updateTrailMoment(moment.copy(upvotes = moment.upvotes + 1))
    }

    // Species Scanning & Field Journal
    val allSpeciesScans: Flow<List<SpeciesScan>> = dao.getAllSpeciesScans()

    suspend fun identifyAndRecordSpecies(
        subjectQuery: String,
        location: String,
        tripId: String? = null
    ): IdentifiedSpeciesResult {
        val result = GeminiOutdoorService.identifyOutdoorSubject(subjectQuery, location)
        val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
        val scan = SpeciesScan(
            id = "scan_" + UUID.randomUUID().toString().take(6),
            commonName = result.commonName,
            scientificName = result.scientificName,
            category = result.category,
            confidence = result.confidence,
            description = result.description,
            habitat = result.habitat,
            isNative = result.isNative,
            safetyNote = result.safetyNote,
            foundAtLocation = location,
            timestamp = timeStr,
            tripId = tripId
        )
        dao.insertSpeciesScan(scan)
        return result
    }

    // Contextual suggestions
    suspend fun getContextualSuggestions(temp: Int, weather: String, freeHours: Int, fitness: String): List<ContextualHikeRecommendation> {
        return GeminiOutdoorService.getContextualSuggestions(temp, weather, freeHours, fitness)
    }

    // Adventure Stories & Recap
    val allAdventureStories: Flow<List<AdventureStory>> = dao.getAllAdventureStories()

    suspend fun createAdventureStory(
        trip: TripPlan,
        trail: Trail,
        companions: List<String>,
        milestones: List<String>,
        speciesCount: Int
    ): AdventureStory {
        val storyText = GeminiOutdoorService.generateAdventureStory(
            trailName = trail.title,
            distanceKm = trail.distanceKm,
            durationHours = trail.durationHours,
            companionNames = companions,
            highlights = milestones,
            speciesCount = speciesCount
        )

        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val story = AdventureStory(
            id = "story_" + UUID.randomUUID().toString().take(8),
            tripId = trip.id,
            trailTitle = trail.title,
            dateFormatted = dateStr,
            totalDistanceKm = trail.distanceKm,
            totalDurationHours = trail.durationHours,
            participantsSummary = companions.joinToString(", ") + " (${companions.size} adventurers)",
            keyMilestones = milestones,
            highlightsNarrative = storyText,
            speciesDiscoveredCount = speciesCount,
            elevationGainM = trail.elevationGainM
        )
        dao.insertAdventureStory(story)
        return story
    }

    // Community Groups
    val allCommunityGroups: Flow<List<CommunityGroup>> = dao.getAllCommunityGroups()

    suspend fun toggleJoinGroup(group: CommunityGroup) {
        val newJoined = !group.isJoined
        val newCount = if (newJoined) group.memberCount + 1 else group.memberCount - 1
        dao.updateCommunityGroup(group.copy(isJoined = newJoined, memberCount = newCount))
    }

    // Challenges
    val allChallenges: Flow<List<AdventureChallenge>> = dao.getAllChallenges()

    suspend fun incrementChallengeProgress(challenge: AdventureChallenge, delta: Int) {
        val newProgress = (challenge.progress + delta).coerceAtMost(challenge.target)
        dao.updateChallenge(challenge.copy(progress = newProgress))
    }

    // User Profile
    val userProfile: Flow<UserProfile?> = dao.getUserProfile()

    // Multi-User Accounts & Authentication
    val allUserAccounts: Flow<List<UserAccount>> = dao.getAllUserAccounts()
    val loggedInUser: Flow<UserAccount?> = dao.getLoggedInUserAccount()

    suspend fun login(email: String, password: String): Result<UserAccount> {
        val trimmedEmail = email.trim().lowercase()
        val account = dao.getUserAccountByEmail(trimmedEmail)
        return if (account != null) {
            if (account.password == password.trim()) {
                dao.clearLoggedInStatus()
                dao.setLoggedInUser(account.id)
                // Sync legacy profile
                dao.updateUserProfile(
                    UserProfile(
                        name = account.name,
                        handle = account.handle,
                        bio = account.bio,
                        totalHikes = account.totalHikes,
                        overnightTrips = account.overnightTrips,
                        totalKmExplored = account.totalKmExplored,
                        attendanceRate = account.attendanceRate,
                        repeatHikerCount = account.repeatHikerCount,
                        groupTripsOrganized = account.groupTripsOrganized,
                        fitnessLevel = account.fitnessLevel,
                        preferredPace = account.preferredPace,
                        preferredVibe = account.preferredVibe,
                        verifiedSkills = account.verifiedSkills,
                        badges = account.badges
                    )
                )
                Result.success(account.copy(isLoggedIn = true))
            } else {
                Result.failure(Exception("Incorrect password for $trimmedEmail"))
            }
        } else {
            Result.failure(Exception("No explorer account found with email $trimmedEmail"))
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        handle: String,
        fitnessLevel: String,
        preferredPace: String,
        bio: String
    ): Result<UserAccount> {
        val trimmedEmail = email.trim().lowercase()
        val existing = dao.getUserAccountByEmail(trimmedEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with $trimmedEmail already exists"))
        }

        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "ME" }

        val colors = listOf(0xFFCD744C, 0xFF1B4938, 0xFFD97706, 0xFF2563EB, 0xFF7C3AED, 0xFF0D9488)
        val randomColor = colors.random()

        val newAccount = UserAccount(
            id = "user_" + UUID.randomUUID().toString().take(8),
            email = trimmedEmail,
            password = password.trim(),
            name = name.trim(),
            handle = if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}",
            bio = bio.trim().ifEmpty { "Outdoor explorer looking for new trails and hiking buddies." },
            avatarInitials = initials,
            avatarColorHex = randomColor,
            fitnessLevel = fitnessLevel,
            preferredPace = preferredPace,
            preferredVibe = "Social & Exploration",
            location = "Melbourne, Victoria",
            totalHikes = 1,
            overnightTrips = 0,
            totalKmExplored = 8,
            attendanceRate = 100,
            repeatHikerCount = 0,
            groupTripsOrganized = 0,
            verifiedSkills = listOf("Navigation", "Leave No Trace"),
            badges = listOf("New Trailblazer", "Nature Lover"),
            isLoggedIn = true
        )

        dao.clearLoggedInStatus()
        dao.insertUserAccount(newAccount)
        dao.updateUserProfile(
            UserProfile(
                name = newAccount.name,
                handle = newAccount.handle,
                bio = newAccount.bio,
                fitnessLevel = newAccount.fitnessLevel,
                preferredPace = newAccount.preferredPace,
                verifiedSkills = newAccount.verifiedSkills,
                badges = newAccount.badges
            )
        )
        return Result.success(newAccount)
    }

    suspend fun switchUserAccount(userId: String) {
        dao.clearLoggedInStatus()
        dao.setLoggedInUser(userId)
    }

    suspend fun logout() {
        dao.clearLoggedInStatus()
    }

    suspend fun updateUserProfileDetails(
        userId: String,
        name: String,
        handle: String,
        bio: String,
        fitnessLevel: String,
        preferredPace: String
    ) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "ME" }

        val accounts = dao.getAllUserAccounts()
        // Update account
        dao.getUserAccountById(userId).collect { acc ->
            if (acc != null) {
                val updated = acc.copy(
                    name = name,
                    handle = handle,
                    bio = bio,
                    avatarInitials = initials,
                    fitnessLevel = fitnessLevel,
                    preferredPace = preferredPace
                )
                dao.updateUserAccount(updated)
                dao.updateUserProfile(
                    UserProfile(
                        name = updated.name,
                        handle = updated.handle,
                        bio = updated.bio,
                        fitnessLevel = updated.fitnessLevel,
                        preferredPace = updated.preferredPace,
                        verifiedSkills = updated.verifiedSkills,
                        badges = updated.badges
                    )
                )
            }
        }
    }
}
