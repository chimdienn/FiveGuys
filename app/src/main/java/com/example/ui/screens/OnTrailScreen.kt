package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.GeoPoint
import com.example.domain.model.MomentCategory
import com.example.domain.weather.TrailRanking
import com.example.ui.components.BiomateProgressBar
import com.example.ui.components.EmptyState
import com.example.ui.components.SafetyNotice
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.LocationUiState
import com.example.ui.viewmodel.OnTrailViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * OnTrail: the live map, tracking and Trail Moments.
 *
 * This is the screen that replaced the prototype's fake GPS — a coroutine that added a
 * random elevation every two seconds. Everything here comes from the fused location
 * provider: the marker, the distance, and the position a Trail Moment is pinned to.
 *
 * Tracking is foreground only. Leaving the app stops the updates, which is stated plainly
 * rather than implied.
 */
@Composable
fun OnTrailScreen(
    trailId: String?,
    tripId: String?,
    viewModel: OnTrailViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val trail by viewModel.trail.collectAsStateWithLifecycle()
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val distanceKm by viewModel.distanceKm.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val progress by viewModel.routeProgress.collectAsStateWithLifecycle()
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()

    var showAddMoment by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showTrailInfo by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        viewModel.onPermissionResult(
            granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
    }

    LaunchedEffect(trailId) {
        trailId?.let(viewModel::previewTrail)
    }

    // Asks on entry rather than on the first "Add Moment" tap, so the map can show the
    // user's position from the outset.
    LaunchedEffect(Unit) {
        if (locationState is LocationUiState.Idle) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val currentTrail = trail
    val isTracking = session != null

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (locationState) {
                    LocationUiState.PermissionRequired -> LocationPermissionExplainer(
                        onGrant = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )

                    else -> TrailMap(
                        routePoints = currentTrail?.route.orEmpty(),
                        currentPoint = (locationState as? LocationUiState.Available)?.fix?.point,
                        moments = moments,
                        onMomentSelected = {}
                    )
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (locationState is LocationUiState.WaitingForFix) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            "Waiting for GPS…",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Category filters sit over the map so they are reachable without
                // scrolling away from what they filter.
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MapFilterChip("All", categoryFilter == null) {
                            viewModel.setCategoryFilter(null)
                        }
                    }
                    items(MomentCategory.entries) { category ->
                        MapFilterChip(
                            "${categoryEmoji(category)} ${category.label}",
                            categoryFilter == category
                        ) {
                            viewModel.setCategoryFilter(
                                if (categoryFilter == category) null else category
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        currentTrail?.name ?: "Choose a trail",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    VSpace(14)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AdventureStat(formatDuration(elapsedSeconds), "Elapsed")
                        AdventureStat("${"%.2f".format(distanceKm)} km", "Travelled")
                        AdventureStat(
                            // Shows "—" rather than a fabricated number when the route is
                            // unmapped or the walker is far from it.
                            progress?.let { "${it.percent}%" } ?: "—",
                            "Along route"
                        )
                    }

                    progress?.let {
                        VSpace(12)
                        BiomateProgressBar(
                            progress = it.fraction.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentDescription = "${it.percent} percent along the route"
                        )
                        if (it.offRouteMeters > 100) {
                            VSpace(6)
                            Text(
                                "You are about ${it.offRouteMeters.toInt()} m from the mapped route.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    VSpace(18)
                    if (isTracking) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showAddMoment = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Add moment") }
                            Button(
                                onClick = { showFinishConfirm = true },
                                enabled = !isFinishing,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(if (isFinishing) "Finishing…" else "Finish") }
                        }
                        VSpace(8)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(
                                onClick = { showTrailInfo = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Trail info") }
                            TextButton(
                                onClick = viewModel::abandonAdventure,
                                modifier = Modifier.weight(1f)
                            ) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                        }
                    } else {
                        Button(
                            onClick = { trailId?.let { viewModel.startAdventure(it, tripId) } },
                            enabled = trailId != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Start adventure", style = MaterialTheme.typography.titleMedium)
                        }
                        VSpace(8)
                        Text(
                            "Tracking runs while Biomate is open. It stops if you close the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddMoment) {
        AddMomentDialog(
            hasLocation = locationState is LocationUiState.Available,
            onAdd = { category, description ->
                viewModel.addMoment(category, description)
                showAddMoment = false
            },
            onDismiss = { showAddMoment = false }
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Finish this adventure?") },
            text = {
                Text(
                    "We'll record ${"%.2f".format(distanceKm)} km over " +
                        "${formatDuration(elapsedSeconds)}, stop tracking your location, " +
                        "and work out any challenges you completed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirm = false
                    viewModel.finishAdventure()
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("Keep walking") }
            }
        )
    }

    if (showTrailInfo && currentTrail != null) {
        AlertDialog(
            onDismissRequest = { showTrailInfo = false },
            title = { Text(currentTrail.name) },
            text = {
                Column {
                    Text(currentTrail.description, style = MaterialTheme.typography.bodyMedium)
                    VSpace(12)
                    Text(
                        "${"%.1f".format(currentTrail.distanceKm)} km · " +
                            "↑${currentTrail.elevationGainM} m · " +
                            "${currentTrail.estimatedDurationLabel} · " +
                            currentTrail.difficulty.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    VSpace(12)
                    SafetyNotice(TrailRanking.SAFETY_DISCLAIMER)
                }
            },
            confirmButton = { TextButton(onClick = { showTrailInfo = false }) { Text("Close") } }
        )
    }

    summary?.let { adventureSummary ->
        AdventureCompleteDialog(
            summary = adventureSummary,
            onDismiss = {
                viewModel.dismissSummary()
                onFinished()
            }
        )
    }
}

@Composable
private fun AdventureStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats elapsed time for the live stats panel.
 *
 * Seconds are shown below an hour so the timer visibly moves from the moment tracking
 * starts — a readout stuck on "0m" for the first minute looks broken.
 */
private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}

/**
 * The map itself.
 *
 * Draws the trail polyline, the community moments, and the user's own position. The
 * camera follows the walker only until they touch the map — hijacking the camera while
 * someone is looking ahead at the route is infuriating, so `hasMovedCamera` latches once
 * an initial position is framed.
 */
@Composable
private fun TrailMap(
    routePoints: List<GeoPoint>,
    currentPoint: GeoPoint?,
    moments: List<com.example.domain.model.TrailMoment>,
    onMomentSelected: (com.example.domain.model.TrailMoment) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    var hasFramed by remember { mutableStateOf(false) }

    val focus = currentPoint ?: routePoints.firstOrNull()
    LaunchedEffect(focus) {
        if (!hasFramed && focus != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(focus.latitude, focus.longitude),
                14f
            )
            hasFramed = true
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            // The blue dot is drawn by the SDK only when permission is held; the app's own
            // marker below is what the tracking logic actually uses.
            isMyLocationEnabled = false,
            mapType = com.google.maps.android.compose.MapType.TERRAIN
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        if (routePoints.size >= 2) {
            Polyline(
                points = routePoints.map { LatLng(it.latitude, it.longitude) },
                color = Color(0xFFCD744C),
                width = 12f
            )
            Marker(
                state = MarkerState(
                    LatLng(routePoints.first().latitude, routePoints.first().longitude)
                ),
                title = "Start"
            )
            Marker(
                state = MarkerState(
                    LatLng(routePoints.last().latitude, routePoints.last().longitude)
                ),
                title = "End"
            )
        }

        moments.forEach { moment ->
            Marker(
                state = MarkerState(LatLng(moment.latitude, moment.longitude)),
                title = "${categoryEmoji(moment.category)} ${moment.category.label}",
                snippet = moment.description.take(80),
                onClick = {
                    onMomentSelected(moment)
                    false
                }
            )
        }

        currentPoint?.let { point ->
            Marker(
                state = MarkerState(LatLng(point.latitude, point.longitude)),
                title = "You are here"
            )
        }
    }
}

@Composable
private fun MapFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = if (selected) {
            { Text("✓", style = MaterialTheme.typography.bodyMedium) }
        } else null,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
    )
}

/**
 * Explains why the app wants location before asking again.
 *
 * A bare second permission prompt after a denial is usually denied again. Saying what the
 * permission buys — and, just as importantly, that the location is not broadcast — is the
 * only honest way to ask twice (spec section 72).
 */
@Composable
private fun LocationPermissionExplainer(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📍", style = MaterialTheme.typography.displayLarge)
        VSpace(16)
        Text(
            "Biomate needs your location",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        VSpace(12)
        Text(
            "Biomate uses your location during an adventure to show where you are relative " +
                "to the trail and to create Trail Moments.\n\n" +
                "Your location is not automatically shared publicly, and tracking stops when " +
                "you leave the app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        VSpace(28)
        Button(
            onClick = onGrant,
            modifier = Modifier.defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Allow location", style = MaterialTheme.typography.titleMedium) }
        VSpace(10)
        Text(
            "If you previously chose \"Don't allow\", you'll need to enable location for " +
                "Biomate in Android Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Creating a Trail Moment.
 *
 * Disabled without a location fix, with the reason stated. A moment whose coordinates are
 * a guess is worse than no moment (spec section 34).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddMomentDialog(
    hasLocation: Boolean,
    onAdd: (MomentCategory, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(MomentCategory.NOTE) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave a Trail Moment") },
        text = {
            Column {
                if (!hasLocation) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Waiting for a GPS fix. A moment is pinned to where you actually are, " +
                                "so it can't be created yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    VSpace(12)
                }

                Text(
                    "What kind of thing?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(8)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MomentCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text("${categoryEmoji(option)} ${option.label}") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        )
                    }
                }

                VSpace(16)
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    minLines = 2,
                    label = { Text("What did you see?") },
                    placeholder = { Text("Tree down across the track just past the saddle") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Trail moment description" }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(category, description) },
                enabled = hasLocation && description.isNotBlank()
            ) { Text("Share") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The post-adventure summary: distance, time, company, and what it earned. */
@Composable
private fun AdventureCompleteDialog(
    summary: com.example.ui.viewmodel.AdventureSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adventure complete 🎉") },
        text = {
            Column {
                Text(
                    summary.trailName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                VSpace(14)
                Text(
                    "${"%.2f".format(summary.session.distanceKm)} km",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    formatDuration(summary.session.durationMinutes * 60),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (summary.companionCount > 0) {
                    Text(
                        "${summary.companionCount} ${if (summary.companionCount == 1) "companion" else "companions"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (summary.momentCount > 0) {
                    Text(
                        "${summary.momentCount} trail moments",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (summary.coinsAwarded > 0) {
                    VSpace(16)
                    Text(
                        "+${summary.coinsAwarded} BioCoins",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    summary.challengesCompleted.forEach {
                        Text(
                            "• ${it.challenge.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.badgesEarned.isNotEmpty()) {
                    VSpace(16)
                    Text(
                        "New badges",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    summary.badgesEarned.forEach { badgeId ->
                        val badge = com.example.domain.badge.BadgeRules.byId(badgeId)
                        Text(
                            "${badge.emoji} ${badge.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
