package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.AppContainer
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Builds ViewModels with the container and the current profile.
 *
 * Feature ViewModels take the signed-in [UserProfile] as a flow rather than fetching it,
 * so there is exactly one authority on identity ([SessionViewModel]) and a sign-out
 * propagates everywhere at once instead of leaving stale screens holding a dead uid.
 */
class BiomateViewModelFactory(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(DiscoverViewModel::class.java) ->
            DiscoverViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(MatchViewModel::class.java) ->
            MatchViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(TripViewModel::class.java) ->
            TripViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(MessagesViewModel::class.java) ->
            MessagesViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(OnTrailViewModel::class.java) ->
            OnTrailViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(container, profileFlow) as T
        modelClass.isAssignableFrom(ScanViewModel::class.java) ->
            ScanViewModel(container, profileFlow) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

/** Separate factory because the session ViewModel is what *produces* the profile flow. */
class SessionViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return SessionViewModel(container) as T
    }
}
