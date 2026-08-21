package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.model.GearItem
import com.example.domain.model.Readiness
import com.example.domain.model.ReadinessItem
import com.example.domain.model.Trail
import com.example.domain.model.Trip
import com.example.domain.model.TripMember
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TripViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val trips: StateFlow<List<Trip>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.tripRepository.observeTripsForUser(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingTrips: StateFlow<List<Trip>> = trips
        .map { list ->
            list.filter { it.status == TripStatus.PLANNING || it.status == TripStatus.ACTIVE }
                .sortedBy { it.startsAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pastTrips: StateFlow<List<Trip>> = trips
        .map { list ->
            list.filter { it.status == TripStatus.COMPLETED || it.status == TripStatus.CANCELLED }
                .sortedByDescending { it.completedAt ?: it.startsAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTrip: StateFlow<Trip?> = _selectedTripId
        .flatMapLatest { id -> if (id == null) flowOf(null) else container.tripRepository.observeTrip(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<TripMember>> = _selectedTripId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else container.tripRepository.observeMembers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Members who have actually joined — invitees are shown separately. */
    val joinedMembers: StateFlow<List<TripMember>> = members
        .map { list -> list.filter { it.status == TripMemberStatus.JOINED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val invitedMembers: StateFlow<List<TripMember>> = members
        .map { list -> list.filter { it.status == TripMemberStatus.INVITED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val gear: StateFlow<List<GearItem>> = _selectedTripId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else container.tripRepository.observeGear(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allReadiness: StateFlow<List<Readiness>> = _selectedTripId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else container.tripRepository.observeReadiness(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val myReadiness: StateFlow<Readiness?> =
        combine(_selectedTripId, profileFlow) { id, profile -> id to profile?.uid }
            .flatMapLatest { (tripId, uid) ->
                if (tripId == null || uid == null) flowOf(null)
                else container.tripRepository.observeMyReadiness(tripId, uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True when the signed-in user organised the open trip. Gates every management action. */
    val isOrganiser: StateFlow<Boolean> =
        combine(selectedTrip, profileFlow) { trip, profile ->
            trip != null && profile != null && trip.creatorId == profile.uid
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val myMembership: StateFlow<TripMember?> =
        combine(members, profileFlow) { list, profile ->
            profile?.let { p -> list.firstOrNull { it.uid == p.uid } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearMessage() {
        _message.value = null
    }

    fun selectTrip(tripId: String?) {
        _selectedTripId.value = tripId
    }

    fun createTrip(
        title: String,
        trail: Trail,
        startsAt: Long,
        meetingPoint: String,
        participantLimit: Int?,
        carpoolNotes: String,
        foodNotes: String,
        generalNotes: String,
        emergencyNotes: String,
        onCreated: (String) -> Unit
    ) {
        val me = profileFlow.value ?: return
        run(saving = true) {
            container.tripRepository.createTrip(
                trip = Trip(
                    id = "",
                    creatorId = me.uid,
                    trailId = trail.id,
                    trailName = trail.name,
                    title = title.trim(),
                    startsAt = startsAt,
                    meetingPoint = meetingPoint.trim(),
                    participantLimit = participantLimit,
                    carpoolNotes = carpoolNotes.trim(),
                    foodNotes = foodNotes.trim(),
                    generalNotes = generalNotes.trim(),
                    emergencyNotes = emergencyNotes.trim()
                ),
                organiser = me
            ).onSuccess { tripId ->
                // The group chat is created with the trip, so there is never a trip whose
                // members have no way to talk to each other.
                container.messagingRepository.ensureTripConversation(
                    tripId = tripId,
                    title = title.trim(),
                    memberIds = listOf(me.uid)
                )
                _selectedTripId.value = tripId
                _message.value = "Trip created."
                onCreated(tripId)
            }.onFailure { _message.value = it.message ?: "Could not create that trip." }
        }
    }

    fun invite(invitee: UserProfile) {
        val tripId = _selectedTripId.value ?: return
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.invite(tripId, invitee, me.uid)
                .onSuccess {
                    syncConversationMembers(tripId)
                    _message.value = "${invitee.displayName} has been invited."
                }
                .onFailure { _message.value = it.message ?: "Could not send that invite." }
        }
    }

    fun joinTrip(tripId: String) {
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.join(tripId, me)
                .onSuccess {
                    syncConversationMembers(tripId)
                    _message.value = "You're on the trip."
                }
                .onFailure { _message.value = it.message ?: "Could not join that trip." }
        }
    }

    fun declineInvite(tripId: String) {
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.leave(tripId, me.uid)
                .onSuccess { _message.value = "Invite declined." }
                .onFailure { _message.value = it.message ?: "Could not decline that invite." }
        }
    }

    fun leaveTrip() {
        val tripId = _selectedTripId.value ?: return
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.leave(tripId, me.uid)
                .onSuccess {
                    syncConversationMembers(tripId)
                    _selectedTripId.value = null
                    _message.value = "You've left the trip."
                }
                .onFailure { _message.value = it.message ?: "Could not leave that trip." }
        }
    }

    fun cancelTrip() {
        val tripId = _selectedTripId.value ?: return
        run {
            if (!isOrganiser.value) {
                _message.value = "Only the organiser can cancel this trip."
                return@run
            }
            container.tripRepository.setStatus(tripId, TripStatus.CANCELLED)
                .onSuccess { _message.value = "Trip cancelled." }
        }
    }

    fun updateNotes(carpool: String, food: String, general: String, emergency: String) {
        val trip = selectedTrip.value ?: return
        run(saving = true) {
            if (!isOrganiser.value) {
                _message.value = "Only the organiser can edit trip notes."
                return@run
            }
            container.tripRepository.updateTrip(
                trip.copy(
                    carpoolNotes = carpool.trim(),
                    foodNotes = food.trim(),
                    generalNotes = general.trim(),
                    emergencyNotes = emergency.trim()
                )
            ).onSuccess { _message.value = "Trip details saved." }
        }
    }

    fun addGear(name: String, category: String, quantity: Int, essential: Boolean) {
        val tripId = _selectedTripId.value ?: return
        run {
            container.tripRepository.addGear(
                GearItem(
                    id = "gear_" + UUID.randomUUID().toString().replace("-", "").take(12),
                    tripId = tripId,
                    name = name.trim(),
                    category = category,
                    quantity = quantity.coerceAtLeast(1),
                    isEssential = essential
                )
            ).onSuccess { _message.value = "Added to the shared list." }
                .onFailure { _message.value = it.message ?: "Could not add that item." }
        }
    }

    /** Claims an unassigned item, or hands it back. */
    fun toggleGearAssignment(item: GearItem) {
        val me = profileFlow.value ?: return
        run {
            val takingIt = item.assignedToUid != me.uid
            if (!takingIt && item.assignedToUid != me.uid && !isOrganiser.value) {
                _message.value = "Only ${item.assignedToName} can hand that back."
                return@run
            }
            container.tripRepository.assignGear(
                itemId = item.id,
                uid = if (takingIt) me.uid else null,
                displayName = if (takingIt) me.displayName else null
            ).onFailure { _message.value = it.message ?: "Could not update that item." }
        }
    }

    fun assignGearTo(item: GearItem, member: TripMember?) {
        run {
            if (!isOrganiser.value) {
                _message.value = "Only the organiser can assign gear to other people."
                return@run
            }
            container.tripRepository.assignGear(item.id, member?.uid, member?.displayName)
                .onFailure { _message.value = it.message ?: "Could not assign that item." }
        }
    }

    fun toggleGearPacked(item: GearItem) {
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.setGearPacked(item.id, !item.isPacked, me.uid)
                .onFailure { _message.value = it.message ?: "Could not update that item." }
        }
    }

    fun removeGear(item: GearItem) {
        val me = profileFlow.value ?: return
        run {
            container.tripRepository.removeGear(item.id, me.uid)
                .onFailure { _message.value = it.message ?: "Could not remove that item." }
        }
    }

    /** Saves the signed-in user's own readiness. There is no path to edit anyone else's. */
    fun saveReadiness(items: Set<ReadinessItem>, confidence: Int?, notes: String) {
        val tripId = _selectedTripId.value ?: return
        val me = profileFlow.value ?: return
        run(saving = true) {
            container.tripRepository.setReadiness(tripId, me.uid, items, confidence, notes)
                .onSuccess { _message.value = "Readiness saved." }
                .onFailure { _message.value = it.message ?: "Could not save your readiness." }
        }
    }

    /** Keeps the trip chat membership in step with the trip roster. */
    private suspend fun syncConversationMembers(tripId: String) {
        val currentMembers = container.tripRepository.observeMembers(tripId).first()
        container.messagingRepository.syncTripConversationMembers(
            tripId = tripId,
            memberIds = currentMembers
                .filter { it.status == TripMemberStatus.JOINED || it.status == TripMemberStatus.INVITED }
                .map { it.uid }
        )
    }

    private fun run(saving: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (saving) _isSaving.value = true
            try {
                block()
            } finally {
                if (saving) _isSaving.value = false
            }
        }
    }

    companion object {
        fun roleLabel(role: TripRole): String = when (role) {
            TripRole.ORGANISER -> "Organiser"
            TripRole.PARTICIPANT -> "Participant"
        }
    }
}
