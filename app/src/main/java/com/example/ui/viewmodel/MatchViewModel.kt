package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.match.Compatibility
import com.example.domain.match.CompatibilityResult
import com.example.domain.model.Connection
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.UserProfile
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

/** A suggested person, with the score and the reasons behind it. */
data class MatchCandidate(
    val profile: UserProfile,
    val compatibility: CompatibilityResult,
    val connectionStatus: ConnectionStatus?
) {
    val score: Int get() = compatibility.score
    val canConnect: Boolean get() = connectionStatus == null || connectionStatus == ConnectionStatus.REJECTED
}

class MatchViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _skippedUids = MutableStateFlow<Set<String>>(emptySet())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _viewedProfile = MutableStateFlow<MatchCandidate?>(null)
    val viewedProfile: StateFlow<MatchCandidate?> = _viewedProfile.asStateFlow()

    private val allProfiles: StateFlow<List<UserProfile>> =
        container.profileRepository.observeAllProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val connections: StateFlow<List<Connection>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.connectionRepository.observeConnections(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomingRequests: StateFlow<List<Pair<Connection, UserProfile>>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.connectionRepository.observeIncomingRequests(profile.uid)
        }
        .combineWithProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Accepted connections, as full profiles. */
    val connectedProfiles: StateFlow<List<UserProfile>> =
        combine(connections, allProfiles, profileFlow) { list, profiles, me ->
            if (me == null) return@combine emptyList()
            val byUid = profiles.associateBy { it.uid }
            list.filter { it.status == ConnectionStatus.ACCEPTED }
                .mapNotNull { byUid[it.otherParty(me.uid)] }
                .sortedBy { it.displayName }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The HikeMatch deck.
     *
     * Excludes the user, anyone already connected or with a request in flight, and anyone
     * skipped this session. Sorted by score so the best suggestion is the first card,
     * because a deck that opens on a 30% match teaches people to stop swiping.
     */
    val candidates: StateFlow<List<MatchCandidate>> =
        combine(profileFlow, allProfiles, connections, _skippedUids) { me, profiles, links, skipped ->
            if (me == null) return@combine emptyList()
            val statusByUid = links.associate { it.otherParty(me.uid) to it.status }

            profiles
                .asSequence()
                .filter { it.uid != me.uid }
                .filter { it.onboardingComplete }
                .filter { it.uid !in skipped }
                .filter { statusByUid[it.uid] != ConnectionStatus.ACCEPTED }
                .filter { statusByUid[it.uid] != ConnectionStatus.PENDING }
                .map { other ->
                    MatchCandidate(
                        profile = other,
                        compatibility = Compatibility.calculate(me, other),
                        connectionStatus = statusByUid[other.uid]
                    )
                }
                .sortedByDescending { it.score }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingOutgoingCount: StateFlow<Int> =
        combine(connections, profileFlow) { links, me ->
            if (me == null) 0
            else links.count { it.status == ConnectionStatus.PENDING && it.requesterId == me.uid }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun kotlinx.coroutines.flow.Flow<List<Connection>>.combineWithProfiles() =
        combine(this, allProfiles) { list, profiles ->
            val byUid = profiles.associateBy { it.uid }
            list.mapNotNull { connection -> byUid[connection.requesterId]?.let { connection to it } }
        }

    fun clearMessage() {
        _message.value = null
    }

    fun skip(uid: String) {
        _skippedUids.value = _skippedUids.value + uid
    }

    /** Puts every skipped candidate back in the deck. */
    fun resetSkipped() {
        _skippedUids.value = emptySet()
    }

    fun connect(candidate: MatchCandidate) {
        val me = profileFlow.value ?: return
        viewModelScope.launch {
            container.connectionRepository.sendRequest(me.uid, candidate.profile.uid)
                .onSuccess { _message.value = "Connection request sent to ${candidate.profile.displayName}." }
                .onFailure { _message.value = it.message ?: "Could not send that request." }
        }
    }

    fun respond(connection: Connection, accept: Boolean) {
        viewModelScope.launch {
            val status = if (accept) ConnectionStatus.ACCEPTED else ConnectionStatus.REJECTED
            container.connectionRepository.respond(connection.id, status)
                .onSuccess {
                    _message.value = if (accept) "You're now connected." else "Request declined."
                }
                .onFailure { _message.value = it.message ?: "Could not respond to that request." }
        }
    }

    fun removeConnection(otherUid: String) {
        val me = profileFlow.value ?: return
        viewModelScope.launch {
            container.connectionRepository.remove(Connection.connectionIdFor(me.uid, otherUid))
                .onSuccess { _message.value = "Connection removed." }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val me = profileFlow.value
            _searchResults.value = if (query.isBlank() || me == null) {
                emptyList()
            } else {
                container.profileRepository.searchByDisplayName(query)
                    .filter { it.uid != me.uid && it.onboardingComplete }
            }
        }
    }

    fun viewProfile(profile: UserProfile) {
        val me = profileFlow.value ?: return
        viewModelScope.launch {
            _viewedProfile.value = MatchCandidate(
                profile = profile,
                compatibility = Compatibility.calculate(me, profile),
                connectionStatus = container.connectionRepository.statusBetween(me.uid, profile.uid)
            )
        }
    }

    fun closeProfile() {
        _viewedProfile.value = null
    }

    /** Opens (or creates) a direct conversation, returning its id for navigation. */
    fun startConversation(other: UserProfile, onReady: (String) -> Unit) {
        val me = profileFlow.value ?: return
        viewModelScope.launch {
            container.messagingRepository.ensureDirectConversation(me, other)
                .onSuccess(onReady)
                .onFailure { _message.value = it.message ?: "Could not open that conversation." }
        }
    }
}
