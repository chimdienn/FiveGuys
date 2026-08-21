package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.ai.SpeciesIdentification
import com.example.domain.challenge.ActivitySignal
import com.example.domain.challenge.ChallengeEngine
import com.example.domain.model.ChallengeSubmission
import com.example.domain.model.MomentCategory
import com.example.domain.model.SubmissionState
import com.example.domain.model.UserProfile
import com.example.domain.repository.DailyChallengeView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

/** The two things the camera is for. */
enum class CameraMode { CHALLENGE, EXPLORE }

/**
 * Where a capture is in its lifecycle.
 *
 * [Captured] is the only state from which a retake is possible. Once [Submitting] begins
 * the submission is final (spec section 46), which is why the states are separate rather
 * than a single `isLoading` boolean.
 */
sealed interface CaptureState {
    data object Ready : CaptureState
    data class Captured(val bytes: ByteArray) : CaptureState {
        override fun equals(other: Any?) = this === other
        override fun hashCode() = System.identityHashCode(this)
    }
    data object Submitting : CaptureState
    data class ChallengeVerdict(val submission: ChallengeSubmission, val coinsAwarded: Int) : CaptureState
    data class Identified(val result: SpeciesIdentification) : CaptureState
    data class Failed(val message: String) : CaptureState
}

class ScanViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _mode = MutableStateFlow(CameraMode.EXPLORE)
    val mode: StateFlow<CameraMode> = _mode.asStateFlow()

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Ready)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _targetDailyChallengeId = MutableStateFlow<String?>(null)
    val targetDailyChallengeId: StateFlow<String?> = _targetDailyChallengeId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val dateKey = MutableStateFlow(
        System.currentTimeMillis().let { now ->
            ChallengeEngine.dateKey(now, TimeZone.getDefault().getOffset(now))
        }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val photoChallenges: StateFlow<List<DailyChallengeView>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.challengeRepository.observeDailyChallenges(profile.uid, dateKey.value)
        }
        .map { list -> list.filter { it.challenge.photoSubject != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearMessage() {
        _message.value = null
    }

    fun setMode(mode: CameraMode) {
        _mode.value = mode
        _captureState.value = CaptureState.Ready
    }

    fun selectChallenge(dailyChallengeId: String?) {
        _targetDailyChallengeId.value = dailyChallengeId
        _mode.value = if (dailyChallengeId == null) CameraMode.EXPLORE else CameraMode.CHALLENGE
        _captureState.value = CaptureState.Ready
    }

    fun onPhotoCaptured(bytes: ByteArray) {
        _captureState.value = CaptureState.Captured(bytes)
    }

    /** Discards the capture. Only legal before a final submit. */
    fun retake() {
        _captureState.value = CaptureState.Ready
    }

    fun onCaptureFailed(message: String) {
        _captureState.value = CaptureState.Failed(message)
    }

    /**
     * Final submit for a challenge photo.
     *
     * Irreversible by design: the repository rejects a second submission for the same
     * daily challenge, so the UI must have already confirmed with the user.
     */
    fun submitChallengePhoto() {
        val me = profileFlow.value ?: return
        val bytes = (_captureState.value as? CaptureState.Captured)?.bytes ?: return
        val dailyId = _targetDailyChallengeId.value ?: run {
            _message.value = "Choose a challenge first."
            return
        }

        viewModelScope.launch {
            _captureState.value = CaptureState.Submitting
            container.challengeRepository.submitPhoto(me.uid, dailyId, bytes)
                .onSuccess { submission ->
                    val coins = if (submission.state == SubmissionState.PASSED) {
                        container.challengeRepository
                            .applyActivity(me.uid, dateKey.value, ActivitySignal(photosVerified = 1))
                            .sumOf { it.coins }
                    } else 0
                    _captureState.value = CaptureState.ChallengeVerdict(submission, coins)
                }
                .onFailure {
                    _captureState.value = CaptureState.Failed(
                        it.message ?: "Could not submit that photo."
                    )
                }
        }
    }

    /** Explore mode: identify what is in the photo. Non-destructive, so retakes are fine. */
    fun identifyPhoto(locationLabel: String?) {
        val bytes = (_captureState.value as? CaptureState.Captured)?.bytes ?: return
        viewModelScope.launch {
            _captureState.value = CaptureState.Submitting
            container.speciesIdentification.identify(bytes, textHint = null, locationLabel = locationLabel)
                .onSuccess { _captureState.value = CaptureState.Identified(it) }
                .onFailure {
                    _captureState.value = CaptureState.Failed(
                        it.message ?: "Could not identify that. Try a clearer photo."
                    )
                }
        }
    }

    /**
     * Suggests a Trail Moment category for an identification.
     *
     * Only ever a suggestion — the user confirms before anything is published.
     */
    fun momentCategoryFor(identification: SpeciesIdentification): MomentCategory =
        when (identification.category.uppercase()) {
            "BIRD", "ANIMAL", "REPTILE", "MUSHROOM" -> MomentCategory.WILDLIFE
            "PLANT" -> MomentCategory.PHOTO
            "GEOLOGY" -> MomentCategory.VIEWPOINT
            "TRACK" -> MomentCategory.WILDLIFE
            else -> MomentCategory.NOTE
        }

    fun reset() {
        _captureState.value = CaptureState.Ready
        _targetDailyChallengeId.value = null
    }
}
