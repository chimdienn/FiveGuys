package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.AppContainer
import com.example.ui.components.LoadingState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.CreateTripScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.HikeMatchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnTrailScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhotoScanScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TrailDetailScreen
import com.example.ui.screens.TripDetailScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.viewmodel.BiomateViewModelFactory
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MatchViewModel
import com.example.ui.viewmodel.MessagesViewModel
import com.example.ui.viewmodel.OnTrailViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.ScanViewModel
import com.example.ui.viewmodel.SessionState
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.TripViewModel

/**
 * The root of the app.
 *
 * Which graph is shown is derived from [SessionState] rather than from navigation calls,
 * so a sign-out cannot leave a signed-in screen on the back stack and an incomplete
 * profile cannot slip past onboarding by pressing back.
 */
@Composable
fun BiomateApp(
    container: AppContainer,
    sessionViewModel: SessionViewModel,
    sessionState: SessionState
) {
    when (sessionState) {
        SessionState.Loading -> Box(Modifier.fillMaxSize()) { LoadingState("Starting Biomate…") }

        SessionState.SignedOut -> AuthScreen(viewModel = sessionViewModel)

        is SessionState.NeedsOnboarding -> OnboardingScreen(
            uid = sessionState.uid,
            initialDisplayName = "",
            viewModel = sessionViewModel
        )

        is SessionState.Ready -> SignedInApp(
            container = container,
            sessionViewModel = sessionViewModel
        )
    }
}

@Composable
private fun SignedInApp(
    container: AppContainer,
    sessionViewModel: SessionViewModel
) {
    val navController = rememberNavController()
    val factory = remember(container) {
        BiomateViewModelFactory(container, sessionViewModel.currentProfile)
    }

    // Hoisted to the app level so the same instances back both the tab and any detail
    // screen pushed on top of it — a trip opened from Home is the same trip object the
    // Trips tab is showing.
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val discoverViewModel: DiscoverViewModel = viewModel(factory = factory)
    val matchViewModel: MatchViewModel = viewModel(factory = factory)
    val tripViewModel: TripViewModel = viewModel(factory = factory)
    val messagesViewModel: MessagesViewModel = viewModel(factory = factory)
    val onTrailViewModel: OnTrailViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val scanViewModel: ScanViewModel = viewModel(factory = factory)

    val snackbarHostState = remember { SnackbarHostState() }
    val unreadCount by messagesViewModel.unreadCount.collectAsStateWithLifecycle()
    val pendingRequests by matchViewModel.incomingRequests.collectAsStateWithLifecycle()

    // One place where every ViewModel's transient message becomes a snackbar, so no
    // screen has to own a host of its own.
    CollectMessage(matchViewModel.message.collectAsStateWithLifecycle().value, snackbarHostState) {
        matchViewModel.clearMessage()
    }
    CollectMessage(tripViewModel.message.collectAsStateWithLifecycle().value, snackbarHostState) {
        tripViewModel.clearMessage()
    }
    CollectMessage(messagesViewModel.message.collectAsStateWithLifecycle().value, snackbarHostState) {
        messagesViewModel.clearMessage()
    }
    CollectMessage(onTrailViewModel.message.collectAsStateWithLifecycle().value, snackbarHostState) {
        onTrailViewModel.clearMessage()
    }
    CollectMessage(scanViewModel.message.collectAsStateWithLifecycle().value, snackbarHostState) {
        scanViewModel.clearMessage()
    }
    CollectMessage(sessionViewModel.notice.collectAsStateWithLifecycle().value, snackbarHostState) {
        sessionViewModel.clearNotice()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomDestination.forRoute(currentRoute) != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                BiomateBottomBar(
                    currentRoute = currentRoute,
                    unreadMessages = unreadCount,
                    pendingRequests = pendingRequests.size,
                    onNavigate = { destination -> navController.navigateToTab(destination.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    sessionViewModel = sessionViewModel,
                    onOpenTrail = { navController.navigate(Routes.trailDetail(it)) },
                    onOpenTrip = { navController.navigate(Routes.tripDetail(it)) },
                    onOpenDiscover = { navController.navigateToTab(Routes.DISCOVER) },
                    onOpenScan = { navController.navigateToTab(Routes.SCAN) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
                )
            }

            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    viewModel = discoverViewModel,
                    onOpenTrail = { navController.navigate(Routes.trailDetail(it)) }
                )
            }

            composable(
                route = Routes.TRAIL_DETAIL,
                arguments = listOf(navArgument("trailId") { type = NavType.StringType })
            ) { entry ->
                val trailId = entry.arguments?.getString("trailId").orEmpty()
                TrailDetailScreen(
                    trailId = trailId,
                    viewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onCreateTrip = { navController.navigate(Routes.createTrip(trailId)) },
                    onFindPeople = { navController.navigateToTab(Routes.MATCH) },
                    onStartAdventure = { navController.navigate(Routes.onTrail(trailId = trailId)) }
                )
            }

            composable(Routes.MATCH) {
                HikeMatchScreen(
                    viewModel = matchViewModel,
                    onOpenConnections = { navController.navigate(Routes.CONNECTIONS) },
                    onOpenConversation = { navController.navigate(Routes.conversation(it)) }
                )
            }

            composable(Routes.CONNECTIONS) {
                ConnectionsScreen(
                    viewModel = matchViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenConversation = { navController.navigate(Routes.conversation(it)) }
                )
            }

            composable(Routes.TRIPS) {
                TripsScreen(
                    viewModel = tripViewModel,
                    onOpenTrip = { navController.navigate(Routes.tripDetail(it)) },
                    onCreateTrip = { navController.navigate(Routes.createTrip()) },
                    onOpenMessages = { navController.navigate(Routes.MESSAGES) },
                    onOpenMemories = { navController.navigate(Routes.MEMORIES) }
                )
            }

            composable(
                route = Routes.TRIP_DETAIL,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { entry ->
                val tripId = entry.arguments?.getString("tripId").orEmpty()
                TripDetailScreen(
                    tripId = tripId,
                    tripViewModel = tripViewModel,
                    matchViewModel = matchViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { navController.navigate(Routes.conversation(it)) },
                    onStartAdventure = { trailId ->
                        navController.navigate(Routes.onTrail(trailId = trailId, tripId = tripId))
                    }
                )
            }

            composable(
                route = Routes.CREATE_TRIP,
                arguments = listOf(navArgument("trailId") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                CreateTripScreen(
                    presetTrailId = entry.arguments?.getString("trailId").orEmpty().ifBlank { null },
                    tripViewModel = tripViewModel,
                    discoverViewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onCreated = { tripId ->
                        navController.popBackStack()
                        navController.navigate(Routes.tripDetail(tripId))
                    }
                )
            }

            composable(Routes.MESSAGES) {
                MessagesScreen(
                    viewModel = messagesViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenConversation = { navController.navigate(Routes.conversation(it)) }
                )
            }

            composable(
                route = Routes.CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { entry ->
                ConversationScreen(
                    conversationId = entry.arguments?.getString("conversationId").orEmpty(),
                    viewModel = messagesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ON_TRAIL,
                arguments = listOf(
                    navArgument("trailId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("tripId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                OnTrailScreen(
                    trailId = entry.arguments?.getString("trailId").orEmpty().ifBlank { null },
                    tripId = entry.arguments?.getString("tripId").orEmpty().ifBlank { null },
                    viewModel = onTrailViewModel,
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }

            composable(Routes.SCAN) {
                PhotoScanScreen(
                    viewModel = scanViewModel,
                    onTrailViewModel = onTrailViewModel
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    sessionViewModel = sessionViewModel,
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenConnections = { navController.navigate(Routes.CONNECTIONS) },
                    onOpenMemories = { navController.navigate(Routes.MEMORIES) }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    sessionViewModel = sessionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MEMORIES) {
                MemoriesScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Switches primary tabs.
 *
 * Pops back to the graph's start destination and restores the tab's own state, so tabs
 * behave like tabs — moving between them does not stack an unbounded history.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun CollectMessage(
    message: String?,
    host: SnackbarHostState,
    onShown: () -> Unit
) {
    LaunchedEffect(message) {
        if (message != null) {
            host.showSnackbar(message)
            onShown()
        }
    }
}

@Composable
private fun BiomateBottomBar(
    currentRoute: String?,
    unreadMessages: Int,
    pendingRequests: Int,
    onNavigate: (BottomDestination) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        BottomDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val badgeCount = when (destination) {
                BottomDestination.MATCH -> pendingRequests
                BottomDestination.TRIPS -> unreadMessages
                else -> 0
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text(badgeCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            // Null because the always-visible label already names the tab;
                            // announcing both would read the name twice.
                            contentDescription = null
                        )
                    }
                },
                // Labels stay visible on unselected tabs: an icon-only bar makes people
                // guess, and the filled/outlined distinction is too subtle on its own.
                label = { Text(destination.label, style = MaterialTheme.typography.bodySmall) },
                alwaysShowLabel = true
            )
        }
    }
}
