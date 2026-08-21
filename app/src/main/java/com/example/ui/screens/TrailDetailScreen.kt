package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.MomentCategory
import com.example.domain.model.TrailMoment
import com.example.domain.weather.TrailRanking
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.ElevationProfile
import com.example.ui.components.LoadingState
import com.example.ui.components.SafetyNotice
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.WeatherUiState

/**
 * Everything about one trail, plus the three things you can do with it.
 *
 * The recent community moments section is the point of difference from a static trail
 * guide, so it sits above the fold-ish rather than buried at the bottom — a two-hour-old
 * "tree across the track" is the most valuable thing on this screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrailDetailScreen(
    trailId: String,
    viewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onCreateTrip: () -> Unit,
    onFindPeople: () -> Unit,
    onStartAdventure: () -> Unit
) {
    LaunchedEffect(trailId) { viewModel.openTrail(trailId) }

    val trail by viewModel.selectedTrail.collectAsStateWithLifecycle()
    val weather by viewModel.detailWeather.collectAsStateWithLifecycle()
    val moments by viewModel.selectedTrailMoments.collectAsStateWithLifecycle()
    val saved by viewModel.savedTrailIds.collectAsStateWithLifecycle()

    val currentTrail = trail
    if (currentTrail == null) {
        LoadingState("Loading trail…")
        return
    }

    val isSaved = currentTrail.id in saved
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box {
                com.example.ui.components.TrailHeroArt(
                    seed = currentTrail.id.hashCode(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleSaved(currentTrail.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save this trail",
                        tint = Color.White
                    )
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        currentTrail.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        "${currentTrail.region} · ${currentTrail.stateOrCountry}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("${"%.1f".format(currentTrail.distanceKm)} km", "Distance", Modifier.weight(1f))
                StatTile("${currentTrail.elevationGainM} m", "Climb", Modifier.weight(1f))
                StatTile(currentTrail.estimatedDurationLabel, "Typical time", Modifier.weight(1f))
            }
        }

        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DifficultyBadge(currentTrail.difficulty)
                currentTrail.activityTypes.forEach { activity ->
                    AssistChip(onClick = {}, label = { Text(activity.label) })
                }
                currentTrail.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        item {
            Text(
                currentTrail.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Conditions")
                VSpace(12)
                TrailWeatherPanel(weather)
            }
        }

        if (currentTrail.waypoints.size >= 2) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    ElevationProfile(
                        waypoints = currentTrail.waypoints,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (currentTrail.highlights.isNotEmpty() || currentTrail.recommendedGear.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    if (currentTrail.highlights.isNotEmpty()) {
                        SectionHeader("Highlights")
                        VSpace(8)
                        currentTrail.highlights.forEach {
                            Text(
                                "• $it",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VSpace(16)
                    }
                    if (currentTrail.recommendedGear.isNotEmpty()) {
                        SectionHeader("Worth packing")
                        VSpace(8)
                        currentTrail.recommendedGear.forEach {
                            Text(
                                "• $it",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Recent community reports")
                VSpace(12)
                if (moments.isEmpty()) {
                    Text(
                        "No reports yet. Be the first to leave one while you are out there.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    moments.take(5).forEachIndexed { index, moment ->
                        if (index > 0) VSpace(12)
                        MomentSummaryRow(moment = moment, nowMillis = now)
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Button(
                    onClick = onCreateTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Create a trip here", style = MaterialTheme.typography.titleMedium) }

                VSpace(10)
                OutlinedButton(
                    onClick = onFindPeople,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Find people to go with", style = MaterialTheme.typography.titleMedium) }

                VSpace(10)
                OutlinedButton(
                    onClick = onStartAdventure,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Start walking now", style = MaterialTheme.typography.titleMedium) }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SafetyNotice(TrailRanking.SAFETY_DISCLAIMER)
                VSpace(8)
                // States plainly what the mapped route is and is not, so nobody mistakes
                // a demonstration polyline for navigation data.
                SafetyNotice(
                    "Route lines in this build are simplified demonstration data, not survey-grade " +
                        "navigation. Carry a proper map."
                )
            }
        }
    }
}

@Composable
private fun TrailWeatherPanel(state: WeatherUiState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            WeatherUiState.Loading -> Text(
                "Checking conditions at the trailhead…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            is WeatherUiState.Failed -> Text(
                "Conditions unavailable: ${state.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            is WeatherUiState.Loaded -> Column(Modifier.padding(16.dp)) {
                Text(
                    "${state.weather.temperatureC.toInt()}° · ${state.weather.condition.label}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Feels like ${state.weather.apparentTemperatureC.toInt()}° · " +
                        "wind ${state.weather.windSpeedKmh.toInt()} km/h · " +
                        "${"%.1f".format(state.weather.precipitationMm)} mm rain",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.weather.advisory?.let {
                    VSpace(10)
                    Text(
                        "⚠ $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * One community report.
 *
 * The age is always shown, and time-sensitive categories older than a week are explicitly
 * flagged as possibly out of date rather than merely dimmed (spec section 37).
 */
@Composable
fun MomentSummaryRow(moment: TrailMoment, nowMillis: Long) {
    val ageMs = nowMillis - moment.createdAt
    val isStale = moment.isTimeSensitive && ageMs > STALE_AFTER_MS

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (moment.category == MomentCategory.HAZARD) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${categoryEmoji(moment.category)} ${moment.category.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    moment.ageLabel(nowMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                if (moment.upvotes > 0) {
                    Text(
                        "▲ ${moment.upvotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            VSpace(6)
            Text(
                moment.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            VSpace(4)
            Text(
                "Reported by ${moment.creatorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isStale) {
                VSpace(6)
                Text(
                    "⏳ This report is old and may no longer be accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun categoryEmoji(category: MomentCategory): String = when (category) {
    MomentCategory.HAZARD -> "⚠️"
    MomentCategory.NOTE -> "📝"
    MomentCategory.PHOTO -> "📷"
    MomentCategory.WILDLIFE -> "🦎"
    MomentCategory.VIEWPOINT -> "🏔️"
    MomentCategory.WATER -> "💧"
    MomentCategory.TRAIL_CONDITION -> "🥾"
}

/** A week. Past this, a hazard or condition report is presented as possibly stale. */
private const val STALE_AFTER_MS = 7L * 24 * 60 * 60 * 1000
