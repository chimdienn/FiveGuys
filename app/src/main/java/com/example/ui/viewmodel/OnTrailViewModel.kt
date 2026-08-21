package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.data.location.LocationFix
import com.example.domain.challenge.ActivitySignal
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.AdventureSession
import com.example.domain.model.BadgeId
import com.example.domain.model.GeoPoint
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import com.example.domain.model.Trail
import com.example.domain.model.TrailMoment
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.repository.AwardedChallenge
import com.example.domain.session.DistanceTracker
import com.example.domain.session.Geo
import com.example.domain.session.RouteProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

/** Why the map cannot show a live position, if it cannot. */
sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object PermissionRequired : LocationUiState
    data object WaitingForFix : LocationUiState
    data class Available(val fix: LocationFix) : LocationUiState
    data class Failed(val message: String) : LocationUiState
}

/** What the user sees after tapping Finish Adventure. */
data class AdventureSummary(
    val session: AdventureSession,
    val trailName: String,
    val companionCount: Int,
    val momentCount: Int,
    val coinsAwarded: Int,
    val challengesCompleted: List<AwardedChallenge>,
    val badgesEarned: List<BadgeId>
)

class OnTrailViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val distanceTracker = DistanceTracker()
    private var locationJob: Job? = null
    private var tickerJob: Job? = null

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private val _activeTrailId = MutableStateFlow<String?>(null)

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _routeProgress = MutableStateFlow<RouteProgress?>(null)
    val routeProgress: StateFlow<RouteProgress?> = _routeProgress.asStateFlow()

    private val _categoryFilter = MutableStateFlow<MomentCategory?>(null)
    val categoryFilter: StateFlow<MomentCategory?> = _categoryFilter.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _summary = MutableStateFlow<AdventureSummary?>(null)
    val summary: StateFlow<AdventureSummary?> = _summary.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSession: StateFlow<AdventureSession?> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(null)
            else container.sessionRepository.observeActiveSession(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trail: StateFlow<Trail?> = _activeTrailId
        .flatMapLatest { id -> if (id == null) flowOf(null) else container.trailRepository.observeTrail(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allMoments: StateFlow<List<TrailMoment>> = _activeTrailId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else container.trailMomentRepository.observeMomentsForTrail(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val moments: StateFlow<List<TrailMoment>> =
        combine(allMoments, _categoryFilter) { list, filter ->
            if (filter == null) list else list.filter { it.category == filter }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentPoint: StateFlow<GeoPoint?> = _locationState
        .map { (it as? LocationUiState.Available)?.fix?.point }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Restore an in-flight adventure after a process death, so backgrounding the app
        // mid-walk does not silently discard the session.
        viewModelScope.launch {
            activeSession.collect { session ->
                if (session != null && _activeTrailId.value == null) {
                    _activeTrailId.value = session.trailId
                    _distanceKm.value = session.distanceKm
                    distanceTracker.reset(session.distanceKm)
                    startTicker(session.startedAt)
                    startLocationUpdates()
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun dismissSummary() {
        _summary.value = null
    }

    fun setCategoryFilter(category: MomentCategory?) {
        _categoryFilter.value = category
    }

    /** Previews a trail on the map without starting tracking. */
    fun previewTrail(trailId: String) {
        if (activeSession.value == null) _activeTrailId.value = trailId
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _locationState.value = LocationUiState.WaitingForFix
            startLocationUpdates()
        } else {
            _locationState.value = LocationUiState.PermissionRequired
        }
    }

    /**
     * Begins a tracked adventure.
     *
     * The session row is written before location starts, so a crash one second later
     * still leaves a resumable adventure rather than a lost one.
     */
    fun startAdventure(trailId: String, tripId: String?) {
        val me = profileFlow.value ?: return
        if (activeSession.value != null) {
            _message.value = "You already have an adventure in progress."
            return
        }
        viewModelScope.launch {
            container.sessionRepository.start(me.uid, trailId, tripId)
                .onSuccess { session ->
                    _activeTrailId.value = trailId
                    _distanceKm.value = 0.0
                    distanceTracker.reset()
                    tripId?.let { container.tripRepository.setStatus(it, TripStatus.ACTIVE) }
                    startTicker(session.startedAt)
                    startLocationUpdates()
                }
                .onFailure { _message.value = it.message ?: "Could not start that adventure." }
        }
    }

    private fun startLocationUpdates() {
        if (!container.locationProvider.hasPermission()) {
            _locationState.value = LocationUiState.PermissionRequired
            return
        }
        if (locationJob?.isActive == true) return

        _locationState.value = LocationUiState.WaitingForFix
        locationJob = viewModelScope.launch {
            // Seed the map from the last known fix so the marker appears immediately
            // rather than after the first satellite lock.
            container.locationProvider.lastKnownLocation()?.let { onFix(it, accumulate = false) }

            container.locationProvider.locationUpdates()
                .catch { error ->
                    Log.w(TAG, "Location updates stopped", error)
                    _locationState.value = LocationUiState.Failed(
                        error.message ?: "Location updates stopped unexpectedly."
                    )
                }
                .collect { fix -> onFix(fix, accumulate = activeSession.value != null) }
        }
    }

    private suspend fun onFix(fix: LocationFix, accumulate: Boolean) {
        _locationState.value = LocationUiState.Available(fix)

        if (accumulate) {
            val total = distanceTracker.accept(fix.point, fix.accuracyMeters)
            if (total != _distanceKm.value) {
                _distanceKm.value = total
                activeSession.value?.let { session ->
                    container.sessionRepository.updateProgress(session.id, total)
                }
            }
        }

        trail.value?.route?.takeIf { it.size >= 2 }?.let { route ->
            _routeProgress.value = Geo.progressAlongRoute(route, fix.point)
        }
    }

    private fun startTicker(startedAt: Long) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _elapsedSeconds.value = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
                delay(1_000)
            }
        }
    }

    private fun stopTracking() {
        locationJob?.cancel()
        locationJob = null
        tickerJob?.cancel()
        tickerJob = null
    }

    /**
     * Records an observation at the device's verified position.
     *
     * Refuses without a fix rather than falling back to the trail start — a hazard pinned
     * to the wrong place is worse than no hazard report at all (spec section 34).
     */
    fun addMoment(
        category: MomentCategory,
        description: String,
        photoBytes: ByteArray? = null,
        visibility: MomentVisibility = MomentVisibility.PUBLIC
    ) {
        val me = profileFlow.value ?: return
        val trailId = _activeTrailId.value ?: return
        val fix = (_locationState.value as? LocationUiState.Available)?.fix
        if (fix == null) {
            _message.value = "Waiting for your location — a Trail Moment is pinned where you are."
            return
        }

        viewModelScope.launch {
            val photoUrl = photoBytes?.let {
                container.photoStore.save(me.uid, "trail-moments", it).getOrNull()
            }
            container.trailMomentRepository.create(
                author = me,
                trailId = trailId,
                tripId = activeSession.value?.tripId,
                deviceLocation = fix.point,
                category = category,
                description = description,
                photoUrl = photoUrl,
                visibility = visibility
            ).onSuccess {
                _message.value = "Trail Moment shared."
                applyChallengeProgress(me.uid, ActivitySignal(momentsCreated = 1))
            }.onFailure { _message.value = it.message ?: "Could not save that moment." }
        }
    }

    fun upvote(moment: TrailMoment) {
        val me = profileFlow.value ?: return
        viewModelScope.launch {
            container.trailMomentRepository.upvote(moment.id, me.uid)
                .onFailure { _message.value = it.message ?: "Could not upvote that." }
        }
    }

    /**
     * Finishes the adventure and settles everything that depends on it.
     *
     * The order matters: the session is closed first so its numbers are final, then stats
     * are recomputed from those numbers, then challenges and badges are evaluated against
     * the recomputed stats. Doing it the other way round would award against stale totals.
     */
    fun finishAdventure() {
        val me = profileFlow.value ?: return
        val session = activeSession.value ?: run {
            _message.value = "There is no adventure to finish."
            return
        }

        viewModelScope.launch {
            _isFinishing.value = true
            try {
                stopTracking()
                val durationMinutes = ((System.currentTimeMillis() - session.startedAt) / 60_000).coerceAtLeast(0)
                val distance = _distanceKm.value

                val completed = container.sessionRepository
                    .complete(session.id, distance, durationMinutes)
                    .getOrElse {
                        _message.value = it.message ?: "Could not finish that adventure."
                        return@launch
                    }

                session.tripId?.let { container.tripRepository.setStatus(it, TripStatus.COMPLETED) }

                val momentCount = if (session.tripId != null) {
                    container.trailMomentRepository.observeMomentsForTrip(session.tripId).first().size
                } else 0

                val awarded = applyChallengeProgress(
                    uid = me.uid,
                    signal = ActivitySignal(
                        distanceKm = distance,
                        tripsCompleted = 1,
                        groupSize = completed.companionCount + 1,
                        momentsCreated = 0
                    )
                )

                val stats = container.profileRepository.recomputeStats(me.uid)
                val badges = container.badgeRepository.evaluateAndAward(me.uid, stats)

                _summary.value = AdventureSummary(
                    session = completed,
                    trailName = trail.value?.name ?: "Adventure",
                    companionCount = completed.companionCount,
                    momentCount = momentCount,
                    coinsAwarded = awarded.sumOf { it.coins },
                    challengesCompleted = awarded,
                    badgesEarned = badges
                )

                _activeTrailId.value = null
                _distanceKm.value = 0.0
                _elapsedSeconds.value = 0L
                _routeProgress.value = null
                distanceTracker.reset()
            } finally {
                _isFinishing.value = false
            }
        }
    }

    /** Abandons tracking without recording a completed adventure. */
    fun abandonAdventure() {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            stopTracking()
            session.tripId?.let { container.tripRepository.setStatus(it, TripStatus.PLANNING) }
            container.sessionRepository.abandon(session.id)
            _activeTrailId.value = null
            _distanceKm.value = 0.0
            _elapsedSeconds.value = 0L
            distanceTracker.reset()
            _message.value = "Adventure discarded."
        }
    }

    private suspend fun applyChallengeProgress(uid: String, signal: ActivitySignal): List<AwardedChallenge> {
        val zone = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val dateKey = ChallengeEngine.dateKey(now, zone.getOffset(now))
        return container.challengeRepository.applyActivity(uid, dateKey, signal)
    }

    override fun onCleared() {
        stopTracking()
        super.onCleared()
    }

    private companion object {
        const val TAG = "OnTrailViewModel"
    }
}
