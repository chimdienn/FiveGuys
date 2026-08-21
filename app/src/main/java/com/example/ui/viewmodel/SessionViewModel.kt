package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.model.ActivityType
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FitnessLevel
import com.example.domain.model.PreferredPace
import com.example.domain.model.Skill
import com.example.domain.model.SocialStyle
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Where the app is in its sign-in lifecycle.
 *
 * Modelled as a closed set rather than a pile of booleans so the navigation graph can
 * switch on it exhaustively — there is no state in which the app is both "signed out" and
 * "needs onboarding", and the compiler enforces that.
 */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    /** Authenticated, but the profile is not complete enough to use the app. */
    data class NeedsOnboarding(val uid: String, val email: String) : SessionState
    data class Ready(val profile: UserProfile) : SessionState
}

/**
 * Owns authentication and the signed-in user's profile.
 *
 * Every other ViewModel takes the current [UserProfile] rather than reaching for auth
 * itself, so there is exactly one place that decides who the user is.
 */
class SessionViewModel(private val container: AppContainer) : ViewModel() {

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    val isFirebaseConfigured: Boolean get() = container.isFirebaseConfigured

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<SessionState> = container.authRepository.authState
        .flatMapLatest { auth ->
            when (auth) {
                AuthState.Loading -> flowOf(SessionState.Loading)
                AuthState.SignedOut -> flowOf(SessionState.SignedOut)
                is AuthState.SignedIn -> container.profileRepository.observeProfile(auth.uid)
                    .map { profile ->
                        if (profile == null || !profile.onboardingComplete) {
                            SessionState.NeedsOnboarding(auth.uid, auth.email)
                        } else {
                            SessionState.Ready(profile)
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    /** The signed-in profile, or null while loading, signed out or onboarding. */
    val currentProfile: StateFlow<UserProfile?> = sessionState
        .map { (it as? SessionState.Ready)?.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearFormError() {
        _formError.value = null
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun signIn(email: String, password: String) {
        submit {
            container.authRepository.signIn(email, password)
                .onFailure { _formError.value = it.message ?: "Could not sign in." }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        submit {
            container.authRepository.register(email, password, displayName)
                .onSuccess { uid ->
                    // A minimal profile exists from the moment the account does, so the
                    // onboarding screen edits a real record rather than holding state
                    // that would be lost if the user backgrounded the app mid-flow.
                    container.profileRepository.saveProfile(
                        UserProfile(
                            uid = uid,
                            displayName = displayName.trim(),
                            onboardingComplete = false
                        )
                    )
                }
                .onFailure { _formError.value = it.message ?: "Could not create that account." }
        }
    }

    fun sendPasswordReset(email: String) {
        submit {
            container.authRepository.sendPasswordReset(email)
                .onSuccess { _notice.value = "Check your inbox for a reset link." }
                .onFailure { _formError.value = it.message ?: "Could not send a reset email." }
        }
    }

    fun signOut() {
        viewModelScope.launch { container.authRepository.signOut() }
    }

    /** Completes onboarding, flipping the profile to usable. */
    fun completeOnboarding(
        uid: String,
        displayName: String,
        bio: String,
        birthYear: Int?,
        gender: String?,
        homeArea: String?,
        interests: Set<ActivityType>,
        fitnessLevel: FitnessLevel,
        experienceLevel: ExperienceLevel,
        preferredPace: PreferredPace,
        socialStyles: Set<SocialStyle>,
        skills: Set<Skill>,
        avatarColorHex: Long
    ) {
        submit {
            val existing = container.profileRepository.getProfile(uid)
            container.profileRepository.saveProfile(
                (existing ?: UserProfile(uid = uid, displayName = displayName)).copy(
                    displayName = displayName.trim(),
                    bio = bio.trim(),
                    birthYear = birthYear,
                    gender = gender?.takeIf { it.isNotBlank() },
                    homeArea = homeArea?.takeIf { it.isNotBlank() },
                    interests = interests,
                    fitnessLevel = fitnessLevel,
                    experienceLevel = experienceLevel,
                    preferredPace = preferredPace,
                    socialStyles = socialStyles,
                    skills = skills,
                    avatarColorHex = avatarColorHex,
                    onboardingComplete = true
                )
            ).onFailure { _formError.value = it.message ?: "Could not save your profile." }
        }
    }

    fun updateProfile(updated: UserProfile) {
        submit {
            container.profileRepository.saveProfile(updated)
                .onSuccess { _notice.value = "Profile updated." }
                .onFailure { _formError.value = it.message ?: "Could not save your profile." }
        }
    }

    private fun submit(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _formError.value = null
            try {
                block()
            } finally {
                // In a `finally` so a thrown exception cannot strand the UI with a
                // permanently disabled, permanently spinning submit button.
                _isSubmitting.value = false
            }
        }
    }
}
