package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every route in the app.
 *
 * Kept as constants rather than scattered string literals so a typo is a compile error
 * and the bottom bar cannot drift out of step with the graph.
 */
object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"

    const val HOME = "home"
    const val DISCOVER = "discover"
    const val MATCH = "match"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"

    const val TRAIL_DETAIL = "trail/{trailId}"
    fun trailDetail(trailId: String) = "trail/$trailId"

    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip/{tripId}"
    fun tripDetail(tripId: String) = "trip/$tripId"

    const val CREATE_TRIP = "createTrip?trailId={trailId}"
    fun createTrip(trailId: String? = null) =
        if (trailId == null) "createTrip?trailId=" else "createTrip?trailId=$trailId"

    const val CONVERSATION = "conversation/{conversationId}"
    fun conversation(conversationId: String) = "conversation/$conversationId"

    const val ON_TRAIL = "onTrail?trailId={trailId}&tripId={tripId}"
    fun onTrail(trailId: String? = null, tripId: String? = null) =
        "onTrail?trailId=${trailId.orEmpty()}&tripId=${tripId.orEmpty()}"

    const val SCAN = "scan"
    const val CONNECTIONS = "connections"
    const val MEMORIES = "memories"
    const val EDIT_PROFILE = "editProfile"
}

/**
 * The five primary destinations.
 *
 * Five is the ceiling for a bottom bar before targets get too narrow to hit reliably;
 * everything else is reached from within a section.
 */
enum class BottomDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    DISCOVER(Routes.DISCOVER, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    MATCH(Routes.MATCH, "Buddies", Icons.Filled.Person, Icons.Outlined.Person),
    TRIPS(Routes.TRIPS, "Trips", Icons.Filled.Map, Icons.Outlined.Map),
    SCAN(Routes.SCAN, "Scan", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt);

    companion object {
        fun forRoute(route: String?): BottomDestination? =
            entries.firstOrNull { it.route == route }
    }
}
