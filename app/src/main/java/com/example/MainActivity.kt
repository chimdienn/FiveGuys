package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.BiomateDatabase
import com.example.data.repository.BiomateRepository
import com.example.ui.BiomateScreen
import com.example.ui.theme.SandBackground
import com.example.ui.BiomateViewModel
import com.example.ui.BiomateViewModelFactory
import com.example.ui.components.BiomateBottomNavigation
import com.example.ui.components.BiomateTopBar
import com.example.ui.screens.AdventureMemoriesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HikeMatchScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnTrailScreen
import com.example.ui.screens.PhotoScanScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TripPlannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerracottaContainer
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = BiomateDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = BiomateRepository(database.biomateDao())
        val viewModelFactory = BiomateViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val biomateViewModel: BiomateViewModel = viewModel(factory = viewModelFactory)
                BiomateApp(viewModel = biomateViewModel)
            }
        }
    }
}

@Composable
fun BiomateApp(viewModel: BiomateViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val isOnTrailActive by viewModel.isOnTrailActive.collectAsState()
    val isSosActive by viewModel.isSosActive.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val screenTitle = when (currentScreen) {
        BiomateScreen.DISCOVER -> "Discover Trails"
        BiomateScreen.HIKE_MATCH -> "Discover your friends"
        BiomateScreen.TRIP_PLAN -> "Gear Check"
        BiomateScreen.MESSAGES -> "All Messages"
        BiomateScreen.ON_TRAIL -> "OnTrail HUD"
        BiomateScreen.PHOTO_SCAN -> "PhotoScan AI"
        BiomateScreen.MEMORIES -> "Adventure Story"
        BiomateScreen.COMMUNITY -> "Adventure Club"
        BiomateScreen.PROFILE -> "My Reputation"
        BiomateScreen.AUTH -> "Explorer Account"
    }

    val screenSubtitle = when (currentScreen) {
        BiomateScreen.DISCOVER -> "Find hikes, weather & packing guides"
        BiomateScreen.HIKE_MATCH -> "Find compatible outdoor companions"
        BiomateScreen.TRIP_PLAN -> activeTrip?.trailName?.ifBlank { "Kata Tjuta" } ?: "Kata Tjuta"
        BiomateScreen.MESSAGES -> "Active activities and buddy chats"
        BiomateScreen.ON_TRAIL -> "Live GPS elevation & hazard stream"
        BiomateScreen.PHOTO_SCAN -> "Identify native flora, fauna & fungi"
        BiomateScreen.MEMORIES -> "Collective post-trip recap & chronicles"
        BiomateScreen.COMMUNITY -> "Shared maps, challenges & squads"
        BiomateScreen.PROFILE -> "Verified skills & reliability rating"
        BiomateScreen.AUTH -> "Sign in or register your account"
    }

    val isAuthScreen = currentScreen == BiomateScreen.AUTH

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            if (!isAuthScreen) {
                BiomateTopBar(
                    title = screenTitle,
                    subtitle = screenSubtitle,
                    onBackClick = if (currentScreen != BiomateScreen.DISCOVER) {
                        { viewModel.navigateTo(BiomateScreen.DISCOVER) }
                    } else null,
                    onSosClick = { viewModel.triggerSosBeacon() },
                    isSosActive = isSosActive,
                    userInitials = currentUser?.avatarInitials ?: "ME",
                    userColor = currentUser?.avatarColorHex?.let { Color(it) } ?: TerracottaContainer,
                    onProfileClick = { viewModel.navigateTo(BiomateScreen.PROFILE) }
                )
            }
        },
        bottomBar = {
            if (!isAuthScreen) {
                BiomateBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) },
                    isOnTrailActive = isOnTrailActive
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    BiomateScreen.DISCOVER -> DiscoverScreen(viewModel = viewModel)
                    BiomateScreen.HIKE_MATCH -> HikeMatchScreen(viewModel = viewModel)
                    BiomateScreen.TRIP_PLAN -> TripPlannerScreen(viewModel = viewModel)
                    BiomateScreen.MESSAGES -> MessagesScreen(viewModel = viewModel)
                    BiomateScreen.ON_TRAIL -> OnTrailScreen(viewModel = viewModel)
                    BiomateScreen.PHOTO_SCAN -> PhotoScanScreen(viewModel = viewModel)
                    BiomateScreen.MEMORIES -> AdventureMemoriesScreen(viewModel = viewModel)
                    BiomateScreen.COMMUNITY -> CommunityScreen(viewModel = viewModel)
                    BiomateScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                    BiomateScreen.AUTH -> AuthScreen(viewModel = viewModel)
                }
            }
        }
    }
}
