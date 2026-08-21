package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ContextualHikeRecommendation
import com.example.data.ai.IdentifiedSpeciesResult
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
import com.example.data.repository.BiomateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BiomateScreen {
    DISCOVER,
    HIKE_MATCH,
    TRIP_PLAN,
    MESSAGES,
    ON_TRAIL,
    PHOTO_SCAN,
    MEMORIES,
    COMMUNITY,
    PROFILE,
    AUTH
}


class BiomateViewModel(private val repository: BiomateRepository) : ViewModel() {

    // Current Screen
    private val _currentScreen = MutableStateFlow(BiomateScreen.DISCOVER)
    val currentScreen: StateFlow<BiomateScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: BiomateScreen) {
        _currentScreen.value = screen
    }

    // Selected Trail & Trip
    private val _selectedTrailId = MutableStateFlow<String?>("trail_wilsons_prom")
    val selectedTrailId: StateFlow<String?> = _selectedTrailId.asStateFlow()

    fun selectTrail(trailId: String) {
        _selectedTrailId.value = trailId
    }

    private val _selectedTripId = MutableStateFlow<String?>("trip_wilsons_prom_weekend")
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    // Data Flows from Repository
    val allTrails: StateFlow<List<Trail>> = repository.allTrails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBuddies: StateFlow<List<HikeBuddy>> = repository.allHikeBuddies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrips: StateFlow<List<TripPlan>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMoments: StateFlow<List<TrailMoment>> = repository.allMoments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpeciesScans: StateFlow<List<SpeciesScan>> = repository.allSpeciesScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStories: StateFlow<List<AdventureStory>> = repository.allAdventureStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<CommunityGroup>> = repository.allCommunityGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChallenges: StateFlow<List<AdventureChallenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // Multi-User Accounts & Authentication
    val allAccounts: StateFlow<List<UserAccount>> = repository.allUserAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserAccount?> = repository.loggedInUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAuthenticated: StateFlow<Boolean> = repository.loggedInUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = repository.login(email, password)
            _isAuthLoading.value = false
            result.onSuccess { acc ->
                _userMessage.value = "Welcome back, ${acc.name}!"
                _currentScreen.value = BiomateScreen.DISCOVER
            }.onFailure { err ->
                _authError.value = err.message ?: "Authentication failed"
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        handle: String,
        fitnessLevel: String,
        preferredPace: String,
        bio: String
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = repository.register(name, email, password, handle, fitnessLevel, preferredPace, bio)
            _isAuthLoading.value = false
            result.onSuccess { acc ->
                _userMessage.value = "Welcome to Biomate, ${acc.name}!"
                _currentScreen.value = BiomateScreen.DISCOVER
            }.onFailure { err ->
                _authError.value = err.message ?: "Registration failed"
            }
        }
    }

    fun quickSwitchAccount(userId: String) {
        viewModelScope.launch {
            repository.switchUserAccount(userId)
            val acc = allAccounts.value.firstOrNull { it.id == userId }
            _userMessage.value = "Switched active explorer to ${acc?.name ?: "account"}"
            _currentScreen.value = BiomateScreen.DISCOVER
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _userMessage.value = "Logged out successfully"
            _currentScreen.value = BiomateScreen.AUTH
        }
    }

    fun updateProfile(
        name: String,
        handle: String,
        bio: String,
        fitnessLevel: String,
        preferredPace: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserProfileDetails(user.id, name, handle, bio, fitnessLevel, preferredPace)
            _userMessage.value = "Profile updated successfully!"
        }
    }

    // Selected Trip Sub-elements
    val activeTrip: StateFlow<TripPlan?> = combine(allTrips, selectedTripId) { trips, id ->
        trips.firstOrNull { it.id == id } ?: trips.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _tripParticipants = MutableStateFlow<List<TripParticipant>>(emptyList())
    val tripParticipants: StateFlow<List<TripParticipant>> = _tripParticipants.asStateFlow()

    private val _tripGear = MutableStateFlow<List<SharedGearItem>>(emptyList())
    val tripGear: StateFlow<List<SharedGearItem>> = _tripGear.asStateFlow()

    private val _tripMeals = MutableStateFlow<List<TripMeal>>(emptyList())
    val tripMeals: StateFlow<List<TripMeal>> = _tripMeals.asStateFlow()

    private val _tripMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val tripMessages: StateFlow<List<ChatMessage>> = _tripMessages.asStateFlow()

    // Chat Channel Selection
    private val _selectedChatChannel = MutableStateFlow("TRIP_GROUP") // "TRIP_GROUP", "CAMPSITE_LOCAL", "EMERGENCY_SOS"
    val selectedChatChannel: StateFlow<String> = _selectedChatChannel.asStateFlow()

    fun setChatChannel(channel: String) {
        _selectedChatChannel.value = channel
    }

    // Contextual Activity Suggestions
    private val _contextualSuggestions = MutableStateFlow<List<ContextualHikeRecommendation>>(emptyList())
    val contextualSuggestions: StateFlow<List<ContextualHikeRecommendation>> = _contextualSuggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    // PhotoScan State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _latestScanResult = MutableStateFlow<IdentifiedSpeciesResult?>(null)
    val latestScanResult: StateFlow<IdentifiedSpeciesResult?> = _latestScanResult.asStateFlow()

    // OnTrail Navigation HUD State
    private val _isOnTrailActive = MutableStateFlow(false)
    val isOnTrailActive: StateFlow<Boolean> = _isOnTrailActive.asStateFlow()

    private val _trailProgressPercent = MutableStateFlow(38) // e.g. 38% completed
    val trailProgressPercent: StateFlow<Int> = _trailProgressPercent.asStateFlow()

    private val _currentElevationM = MutableStateFlow(320)
    val currentElevationM: StateFlow<Int> = _currentElevationM.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(4.2)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(5420) // 1h 30m 20s
    val elapsedTimeSeconds: StateFlow<Int> = _elapsedTimeSeconds.asStateFlow()

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    // Story Generation Loading State
    private val _isGeneratingStory = MutableStateFlow(false)
    val isGeneratingStory: StateFlow<Boolean> = _isGeneratingStory.asStateFlow()

    // User message / Banner feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private var navigationSimulationJob: Job? = null

    init {
        // Collect sub-elements when selectedTripId changes
        viewModelScope.launch {
            selectedTripId.collect { tripId ->
                val id = tripId ?: "trip_wilsons_prom_weekend"
                launch { repository.getParticipantsForTrip(id).collect { _tripParticipants.value = it } }
                launch { repository.getGearForTrip(id).collect { _tripGear.value = it } }
                launch { repository.getMealsForTrip(id).collect { _tripMeals.value = it } }
                launch { repository.getMessagesForTrip(id).collect { _tripMessages.value = it } }
            }
        }

        // Load initial contextual recommendations
        refreshContextualSuggestions()
    }

    fun refreshContextualSuggestions() {
        viewModelScope.launch {
            _isLoadingSuggestions.value = true
            try {
                val suggestions = repository.getContextualSuggestions(
                    temp = 24,
                    weather = "Sunny & Low Wind",
                    freeHours = 4,
                    fitness = "Moderate/Advanced"
                )
                _contextualSuggestions.value = suggestions
            } catch (_: Exception) {}
            _isLoadingSuggestions.value = false
        }
    }

    fun toggleSaveTrail(trail: Trail) {
        viewModelScope.launch {
            repository.toggleSaveTrail(trail)
            _userMessage.value = if (!trail.isSaved) "Saved ${trail.title} to your wishlist" else "Removed from saved"
        }
    }

    fun toggleVisitedTrail(trail: Trail) {
        viewModelScope.launch {
            repository.toggleVisitedTrail(trail)
        }
    }

    // HikeMatch Filtering & Preferences
    private val _hikeMatchFitnessFilter = MutableStateFlow("All")
    val hikeMatchFitnessFilter: StateFlow<String> = _hikeMatchFitnessFilter.asStateFlow()

    private val _hikeMatchExperienceFilter = MutableStateFlow("All")
    val hikeMatchExperienceFilter: StateFlow<String> = _hikeMatchExperienceFilter.asStateFlow()

    private val _hikeMatchSocialVibeFilter = MutableStateFlow("All")
    val hikeMatchSocialVibeFilter: StateFlow<String> = _hikeMatchSocialVibeFilter.asStateFlow()

    private val _hikeMatchPaceFilter = MutableStateFlow("All")
    val hikeMatchPaceFilter: StateFlow<String> = _hikeMatchPaceFilter.asStateFlow()

    private val _hikeMatchSearchQuery = MutableStateFlow("")
    val hikeMatchSearchQuery: StateFlow<String> = _hikeMatchSearchQuery.asStateFlow()

    fun setFitnessFilter(filter: String) {
        _hikeMatchFitnessFilter.value = filter
    }

    fun setExperienceFilter(filter: String) {
        _hikeMatchExperienceFilter.value = filter
    }

    fun setSocialVibeFilter(filter: String) {
        _hikeMatchSocialVibeFilter.value = filter
    }

    fun setPaceFilter(filter: String) {
        _hikeMatchPaceFilter.value = filter
    }

    fun setMatchSearchQuery(query: String) {
        _hikeMatchSearchQuery.value = query
    }

    fun resetMatchFilters() {
        _hikeMatchFitnessFilter.value = "All"
        _hikeMatchExperienceFilter.value = "All"
        _hikeMatchSocialVibeFilter.value = "All"
        _hikeMatchPaceFilter.value = "All"
        _hikeMatchSearchQuery.value = ""
    }

    fun sendBuddyInvite(buddy: HikeBuddy, tripId: String? = null, customNote: String = "") {
        viewModelScope.launch {
            repository.updateBuddyStatus(buddy, "INVITED")
            val noteText = if (customNote.isNotBlank()) " with note: \"$customNote\"" else ""
            _userMessage.value = "Hike invite dispatched to ${buddy.name}$noteText!"
        }
    }

    fun superEndorseBuddy(buddy: HikeBuddy) {
        viewModelScope.launch {
            repository.updateBuddyStatus(buddy, "MATCHED")
            _userMessage.value = "🌟 Super Endorsed ${buddy.name}! It's an instant match!"
        }
    }

    fun connectWithBuddy(buddy: HikeBuddy) {
        viewModelScope.launch {
            repository.updateBuddyStatus(buddy, "MATCHED")
            _userMessage.value = "Connected with ${buddy.name}! Added to your companion network."
        }
    }

    fun startChatWithBuddy(buddy: HikeBuddy) {
        viewModelScope.launch {
            repository.updateBuddyStatus(buddy, "CHATTING")
            _currentScreen.value = BiomateScreen.MESSAGES
            _userMessage.value = "Direct chat started with ${buddy.name}"
        }
    }

    fun toggleGearPacked(item: SharedGearItem) {
        viewModelScope.launch {
            repository.toggleGearPacked(item)
        }
    }

    fun assignGear(item: SharedGearItem, assignedTo: String) {
        viewModelScope.launch {
            repository.assignGearItem(item, assignedTo)
            _userMessage.value = "Assigned '${item.name}' to $assignedTo"
        }
    }

    fun addSharedGearItem(name: String, category: String, assignedTo: String, essential: Boolean) {
        val tripId = _selectedTripId.value ?: return
        viewModelScope.launch {
            repository.addSharedGear(tripId, name, category, assignedTo, essential)
            _userMessage.value = "Added '$name' to group gear checklist"
        }
    }

    fun addTripMeal(name: String, type: String, assignedTo: String, notes: String) {
        val tripId = _selectedTripId.value ?: return
        viewModelScope.launch {
            repository.addTripMeal(tripId, name, type, assignedTo, notes)
            _userMessage.value = "Added meal '$name' for $type"
        }
    }

    fun sendMessage(text: String, isEmergency: Boolean = false) {
        if (text.isBlank()) return
        val tripId = _selectedTripId.value ?: return
        val channel = _selectedChatChannel.value
        val userName = currentUser.value?.name ?: "Alex Rivera"
        viewModelScope.launch {
            repository.sendChatMessage(
                tripId = tripId,
                channel = channel,
                senderName = "$userName (You)",
                messageText = text,
                isEmergency = isEmergency
            )
        }
    }

    fun reportTrailMoment(
        type: String,
        title: String,
        description: String,
        kmMarker: Double,
        warningLevel: String = "INFO"
    ) {
        val trailId = _selectedTrailId.value ?: "trail_wilsons_prom"
        val userName = currentUser.value?.name ?: "Alex Rivera"
        viewModelScope.launch {
            repository.reportTrailMoment(
                trailId = trailId,
                type = type,
                title = title,
                description = description,
                reportedBy = userName,
                kmMarker = kmMarker,
                warningLevel = warningLevel
            )
            _userMessage.value = "Published live trail moment to Biomate community!"
        }
    }

    fun upvoteMoment(moment: TrailMoment) {
        viewModelScope.launch {
            repository.upvoteMoment(moment)
        }
    }

    fun startOnTrailNavigation(trail: Trail) {
        _selectedTrailId.value = trail.id
        _isOnTrailActive.value = true
        _currentScreen.value = BiomateScreen.ON_TRAIL
        startSimulation()
    }

    fun pauseOrResumeOnTrail() {
        _isOnTrailActive.value = !_isOnTrailActive.value
        if (_isOnTrailActive.value) {
            startSimulation()
        } else {
            navigationSimulationJob?.cancel()
        }
    }

    private fun startSimulation() {
        navigationSimulationJob?.cancel()
        navigationSimulationJob = viewModelScope.launch {
            while (_isOnTrailActive.value) {
                delay(2000)
                _elapsedTimeSeconds.value += 2
                _trailProgressPercent.value = (_trailProgressPercent.value + 1).coerceAtMost(100)
                // Fluctuate elevation & speed realistically
                _currentElevationM.value = (280..420).random()
                _currentSpeedKmh.value = ((40..52).random()) / 10.0
            }
        }
    }

    fun triggerSosBeacon() {
        _isSosActive.value = !_isSosActive.value
        if (_isSosActive.value) {
            _userMessage.value = "🚨 EMERGENCY SOS BROADCAST: Live GPS coordinates sent to VIC Rangers & Campsite Beacon!"
            val tripId = _selectedTripId.value ?: "trip_wilsons_prom_weekend"
            viewModelScope.launch {
                repository.sendChatMessage(
                    tripId = tripId,
                    channel = "EMERGENCY_SOS",
                    senderName = "Alex Rivera (SOS BEACON)",
                    messageText = "🚨 EMERGENCY SOS ACTIVE at Sealers Cove Track (km 7.4). Group requires medical assistance.",
                    isEmergency = true
                )
            }
        } else {
            _userMessage.value = "SOS Beacon Deactivated. Stand-down signal broadcasted."
        }
    }

    fun scanSpecies(query: String, location: String = "Wilson's Promontory") {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = repository.identifyAndRecordSpecies(
                    subjectQuery = query,
                    location = location,
                    tripId = _selectedTripId.value
                )
                _latestScanResult.value = result
                _userMessage.value = "Identified: ${result.commonName} (${result.confidence}% match)"
            } catch (e: Exception) {
                _userMessage.value = "Scan error: ${e.message}"
            }
            _isScanning.value = false
        }
    }

    fun createAdventureStoryForActiveTrip() {
        val trip = activeTrip.value ?: return
        val trail = allTrails.value.firstOrNull { it.id == trip.trailId } ?: allTrails.value.firstOrNull() ?: return
        val participants = tripParticipants.value.map { it.name.replace(" (You)", "") }
        val moments = allMoments.value.filter { it.trailId == trail.id }.map { it.title }
        val speciesCount = allSpeciesScans.value.size

        viewModelScope.launch {
            _isGeneratingStory.value = true
            try {
                val story = repository.createAdventureStory(
                    trip = trip,
                    trail = trail,
                    companions = participants,
                    milestones = if (moments.isNotEmpty()) moments else listOf("Sealers Cove Overlook", "Rainforest Creek", "Summit Lookout"),
                    speciesCount = speciesCount
                )
                _userMessage.value = "Generated Adventure Story: ${story.trailTitle}!"
                _currentScreen.value = BiomateScreen.MEMORIES
            } catch (e: Exception) {
                _userMessage.value = "Story generation error: ${e.message}"
            }
            _isGeneratingStory.value = false
        }
    }

    fun toggleJoinGroup(group: CommunityGroup) {
        viewModelScope.launch {
            repository.toggleJoinGroup(group)
            _userMessage.value = if (!group.isJoined) "Joined ${group.name}!" else "Left group"
        }
    }

    fun completeChallengeStep(challenge: AdventureChallenge) {
        viewModelScope.launch {
            repository.incrementChallengeProgress(challenge, 5)
            _userMessage.value = "Challenge Progress updated: +5 ${challenge.unit}"
        }
    }

    fun createNewTrip(title: String, trail: Trail, date: String, meetingPoint: String, carpoolSeats: Int) {
        viewModelScope.launch {
            val newId = repository.createNewTrip(title, trail, date, meetingPoint, carpoolSeats)
            _selectedTripId.value = newId
            _selectedTrailId.value = trail.id
            _userMessage.value = "Created adventure group: '$title'!"
            _currentScreen.value = BiomateScreen.TRIP_PLAN
        }
    }
}

class BiomateViewModelFactory(private val repository: BiomateRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiomateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BiomateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
