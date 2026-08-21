package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.model.ActivityType
import com.example.domain.model.Difficulty
import com.example.domain.model.Trail
import com.example.domain.model.TrailMoment
import com.example.domain.model.UserProfile
import com.example.domain.model.Weather
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Active Discover filters. All-null means "show everything". */
data class TrailFilters(
    val query: String = "",
    val activity: ActivityType? = null,
    val difficulty: Difficulty? = null,
    val maxDistanceKm: Double? = null,
    val maxDurationMinutes: Int? = null,
    val region: String? = null,
    val savedOnly: Boolean = false
) {
    val activeCount: Int
        get() = listOfNotNull(activity, difficulty, maxDistanceKm, maxDurationMinutes, region).size +
            if (savedOnly) 1 else 0
}

class DiscoverViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _filters = MutableStateFlow(TrailFilters())
    val filters: StateFlow<TrailFilters> = _filters.asStateFlow()

    private val _selectedTrailId = MutableStateFlow<String?>(null)
    val selectedTrailId: StateFlow<String?> = _selectedTrailId.asStateFlow()

    private val _detailWeather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val detailWeather: StateFlow<WeatherUiState> = _detailWeather.asStateFlow()

    val allTrails: StateFlow<List<Trail>> = container.trailRepository.observeTrails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedTrailIds: StateFlow<Set<String>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptySet())
            else container.trailRepository.observeSavedTrailIds(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val visibleTrails: StateFlow<List<Trail>> =
        combine(allTrails, _filters, savedTrailIds) { trails, filters, saved ->
            trails.filter { trail -> matches(trail, filters, saved) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val regions: StateFlow<List<String>> = allTrails
        .map { trails -> trails.map { it.region }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTrail: StateFlow<Trail?> = _selectedTrailId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else container.trailRepository.observeTrail(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Community reports for the open trail, newest first. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTrailMoments: StateFlow<List<TrailMoment>> = _selectedTrailId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else container.trailMomentRepository.observeMomentsForTrail(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun setActivity(activity: ActivityType?) {
        _filters.value = _filters.value.copy(activity = activity)
    }

    fun setDifficulty(difficulty: Difficulty?) {
        _filters.value = _filters.value.copy(difficulty = difficulty)
    }

    fun setMaxDistance(km: Double?) {
        _filters.value = _filters.value.copy(maxDistanceKm = km)
    }

    fun setMaxDuration(minutes: Int?) {
        _filters.value = _filters.value.copy(maxDurationMinutes = minutes)
    }

    fun setRegion(region: String?) {
        _filters.value = _filters.value.copy(region = region)
    }

    fun setSavedOnly(savedOnly: Boolean) {
        _filters.value = _filters.value.copy(savedOnly = savedOnly)
    }

    fun clearFilters() {
        _filters.value = TrailFilters(query = _filters.value.query)
    }

    fun openTrail(trailId: String) {
        _selectedTrailId.value = trailId
        loadWeatherForTrail(trailId)
    }

    fun closeTrail() {
        _selectedTrailId.value = null
    }

    fun toggleSaved(trailId: String) {
        val uid = profileFlow.value?.uid ?: return
        val currentlySaved = trailId in savedTrailIds.value
        viewModelScope.launch {
            container.trailRepository.setSaved(uid, trailId, !currentlySaved)
        }
    }

    /** Weather at the trailhead, which is the coordinate that actually matters for a walk. */
    private fun loadWeatherForTrail(trailId: String) {
        viewModelScope.launch {
            _detailWeather.value = WeatherUiState.Loading
            val trail = container.trailRepository.getTrail(trailId)
            if (trail == null) {
                _detailWeather.value = WeatherUiState.Failed("Trail not found.")
                return@launch
            }
            container.weatherService.getWeather(trail.start.latitude, trail.start.longitude)
                .onSuccess { _detailWeather.value = WeatherUiState.Loaded(it, trail.region) }
                .onFailure {
                    _detailWeather.value = WeatherUiState.Failed(
                        it.message ?: "Could not load conditions for this trail."
                    )
                }
        }
    }

    private fun matches(trail: Trail, filters: TrailFilters, saved: Set<String>): Boolean {
        val query = filters.query.trim()
        if (query.isNotEmpty()) {
            val haystack = listOf(trail.name, trail.region, trail.description) + trail.tags
            if (haystack.none { it.contains(query, ignoreCase = true) }) return false
        }
        filters.activity?.let { if (it !in trail.activityTypes) return false }
        filters.difficulty?.let { if (it != trail.difficulty) return false }
        filters.maxDistanceKm?.let { if (trail.distanceKm > it) return false }
        filters.maxDurationMinutes?.let { if (trail.estimatedMinutes > it) return false }
        filters.region?.let { if (!trail.region.equals(it, ignoreCase = true)) return false }
        if (filters.savedOnly && trail.id !in saved) return false
        return true
    }
}
