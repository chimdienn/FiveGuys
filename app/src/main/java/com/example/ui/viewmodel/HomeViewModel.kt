package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.Trail
import com.example.domain.model.Trip
import com.example.domain.model.UserProfile
import com.example.domain.model.Weather
import com.example.domain.repository.DailyChallengeView
import com.example.domain.repository.LeaderboardEntry
import com.example.domain.weather.TrailRanking
import com.example.domain.weather.TrailRecommendation
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
import java.util.Calendar
import java.util.TimeZone

/** Weather is a network call, so it gets a real state machine rather than a nullable. */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Loaded(val weather: Weather, val locationLabel: String) : WeatherUiState
    data class Failed(val message: String) : WeatherUiState
}

class HomeViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _dateKey = MutableStateFlow(todayKey())

    val trails: StateFlow<List<Trail>> = container.trailRepository.observeTrails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyChallenges: StateFlow<List<DailyChallengeView>> =
        combine(profileFlow, _dateKey) { profile, dateKey -> profile?.uid to dateKey }
            .flatMapLatest { (uid, dateKey) ->
                if (uid == null) flowOf(emptyList())
                else container.challengeRepository.observeDailyChallenges(uid, dateKey)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val bioCoins: StateFlow<Int> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(0) else container.rewardRepository.observeBalance(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val leaderboard: StateFlow<List<LeaderboardEntry>> =
        container.profileRepository.observeLeaderboard(limit = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The next trip the user is on, by start time. Past trips are not "upcoming". */
    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingTrip: StateFlow<Trip?> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.tripRepository.observeTripsForUser(profile.uid)
        }
        .map { trips ->
            val now = System.currentTimeMillis()
            trips.filter { it.startsAt >= now && it.status.name != "CANCELLED" }
                .minByOrNull { it.startsAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The single trail Biomate suggests right now.
     *
     * Ranked against live weather, and only from trails matching something the user
     * actually said they were interested in — recommending a cycling route to someone who
     * only ticked hiking is worse than recommending nothing.
     */
    val recommendation: StateFlow<TrailRecommendation?> =
        combine(trails, _weather, profileFlow) { allTrails, weatherState, profile ->
            if (allTrails.isEmpty()) return@combine null
            val candidates = profile?.interests
                ?.takeIf { it.isNotEmpty() }
                ?.let { interests -> allTrails.filter { it.activityTypes.any(interests::contains) } }
                ?.takeIf { it.isNotEmpty() }
                ?: allTrails
            TrailRanking.bestFor(candidates, (weatherState as? WeatherUiState.Loaded)?.weather)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Trails ranked for today, for the "recommended for you" row. */
    val rankedTrails: StateFlow<List<TrailRecommendation>> =
        combine(trails, _weather) { allTrails, weatherState ->
            TrailRanking.rank(allTrails, (weatherState as? WeatherUiState.Loaded)?.weather)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshWeather()
        viewModelScope.launch {
            profileFlow.collect { profile ->
                if (profile != null) {
                    container.challengeRepository.ensureAssigned(profile.uid, _dateKey.value)
                }
            }
        }
    }

    /**
     * Fetches weather for the user's stated home area, or a default.
     *
     * Deliberately does **not** use the device's GPS: Home is not an active adventure, and
     * silently reading precise location to decorate a weather card would be a poor trade
     * (spec section 64). Live position is requested only in OnTrail, where the user has
     * asked for it.
     */
    fun refreshWeather() {
        viewModelScope.launch {
            _weather.value = WeatherUiState.Loading
            val profile = profileFlow.value
            val area = profile?.homeArea?.takeIf { it.isNotBlank() } ?: DEFAULT_AREA_LABEL
            val coordinates = KNOWN_AREAS.entries
                .firstOrNull { (key, _) -> area.contains(key, ignoreCase = true) }
                ?.value ?: DEFAULT_COORDINATES

            container.weatherService.getWeather(coordinates.first, coordinates.second)
                .onSuccess { _weather.value = WeatherUiState.Loaded(it, area) }
                .onFailure {
                    _weather.value = WeatherUiState.Failed(
                        it.message ?: "Could not reach the weather service."
                    )
                }
        }
    }

    /** Re-reads the calendar day, so the app rolls over without a restart. */
    fun refreshDay() {
        val today = todayKey()
        if (_dateKey.value != today) _dateKey.value = today
        profileFlow.value?.let { profile ->
            viewModelScope.launch { container.challengeRepository.ensureAssigned(profile.uid, today) }
        }
    }

    private fun todayKey(): String {
        val zone = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        return ChallengeEngine.dateKey(now, zone.getOffset(now))
    }

    companion object {
        /** Falls back to Melbourne — the demo content is Victorian. */
        private const val DEFAULT_AREA_LABEL = "Melbourne, Victoria"
        private val DEFAULT_COORDINATES = -37.8136 to 144.9631

        /**
         * A small offline gazetteer.
         *
         * Enough to give a sensible forecast without shipping a geocoder or sending the
         * user's stated home town to a third-party lookup service.
         */
        private val KNOWN_AREAS = mapOf(
            "melbourne" to (-37.8136 to 144.9631),
            "geelong" to (-38.1499 to 144.3617),
            "ballarat" to (-37.5622 to 143.8503),
            "bendigo" to (-36.7570 to 144.2794),
            "bright" to (-36.7300 to 146.9600),
            "halls gap" to (-37.1372 to 142.5211),
            "warrnambool" to (-38.3819 to 142.4880),
            "sydney" to (-33.8688 to 151.2093),
            "brisbane" to (-27.4698 to 153.0251),
            "adelaide" to (-34.9285 to 138.6007),
            "perth" to (-31.9523 to 115.8613),
            "hobart" to (-42.8821 to 147.3272),
            "canberra" to (-35.2809 to 149.1300)
        )

        fun greetingFor(hourOfDay: Int): String = when (hourOfDay) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good evening"
        }

        fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
}
