package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.badge.BadgeRules
import com.example.domain.model.AdventureSession
import com.example.domain.model.Badge
import com.example.domain.model.CoinTransaction
import com.example.domain.model.EarnedBadge
import com.example.domain.model.Trail
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A completed adventure joined to the trail and trip it belongs to, for history display. */
data class HistoryEntry(
    val session: AdventureSession,
    val trail: Trail?,
    val trip: Trip?
)

/** A badge and whether this user holds it, so locked badges can show what to aim for. */
data class BadgeStatus(
    val badge: Badge,
    val earned: EarnedBadge?
) {
    val isEarned: Boolean get() = earned != null
}

class ProfileViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<UserStats?> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(null)
            else container.profileRepository.observeStats(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bioCoins: StateFlow<Int> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(0) else container.rewardRepository.observeBalance(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<CoinTransaction>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.rewardRepository.observeTransactions(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every badge, earned or not — the locked ones are the roadmap. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val badges: StateFlow<List<BadgeStatus>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.badgeRepository.observeEarned(profile.uid)
        }
        .map { earned ->
            val byId = earned.associateBy { it.badgeId }
            BadgeRules.all.map { rule -> BadgeStatus(rule.badge, byId[rule.badge.id]) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val completedSessions: StateFlow<List<AdventureSession>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.sessionRepository.observeCompletedSessions(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val myTrips: StateFlow<List<Trip>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.tripRepository.observeTripsForUser(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val trails: StateFlow<List<Trail>> = container.trailRepository.observeTrails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntry>> =
        combine(completedSessions, trails, myTrips) { sessions, allTrails, trips ->
            val trailById = allTrails.associateBy { it.id }
            val tripById = trips.associateBy { it.id }
            sessions
                .sortedByDescending { it.completedAt ?: it.startedAt }
                .map { session ->
                    HistoryEntry(
                        session = session,
                        trail = trailById[session.trailId],
                        trip = session.tripId?.let(tripById::get)
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Completed trips, for the memories view. */
    val completedTrips: StateFlow<List<Trip>> = myTrips
        .map { list ->
            list.filter { it.status == TripStatus.COMPLETED }
                .sortedByDescending { it.completedAt ?: it.startsAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
