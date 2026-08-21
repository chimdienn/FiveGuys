package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ActivityType
import com.example.domain.model.Difficulty
import com.example.domain.model.Trail
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.TrailHeroArt
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.DiscoverViewModel

/**
 * Trail browsing with search and filters.
 *
 * Filters live in a horizontally scrolling strip that stays visible while the list
 * scrolls, so it is always obvious *why* a list is short — the commonest confusion in a
 * filtered list is a user who has forgotten a filter is on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onOpenTrail: (String) -> Unit
) {
    val trails by viewModel.visibleTrails.collectAsStateWithLifecycle()
    val allTrails by viewModel.allTrails.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val saved by viewModel.savedTrailIds.collectAsStateWithLifecycle()
    val regions by viewModel.regions.collectAsStateWithLifecycle()

    var showAllFilters by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                "Discover trails",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            VSpace(12)

            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                placeholder = { Text("Search trails, regions or tags") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setQuery("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .semantics { contentDescription = "Search trails" }
            )

            VSpace(12)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ToggleChip(
                        label = "Saved",
                        selected = filters.savedOnly,
                        onClick = { viewModel.setSavedOnly(!filters.savedOnly) }
                    )
                }
                items(ActivityType.mvpActivities, key = { it.name }) { activity ->
                    ToggleChip(
                        label = activity.label,
                        selected = filters.activity == activity,
                        onClick = {
                            viewModel.setActivity(if (filters.activity == activity) null else activity)
                        }
                    )
                }
                item {
                    ToggleChip(
                        label = if (showAllFilters) "Fewer filters" else "More filters",
                        selected = false,
                        onClick = { showAllFilters = !showAllFilters }
                    )
                }
            }

            if (showAllFilters) {
                VSpace(12)
                FilterSection("Difficulty") {
                    Difficulty.entries.forEach { difficulty ->
                        ToggleChip(
                            label = difficulty.label,
                            selected = filters.difficulty == difficulty,
                            onClick = {
                                viewModel.setDifficulty(
                                    if (filters.difficulty == difficulty) null else difficulty
                                )
                            }
                        )
                    }
                }
                VSpace(12)
                FilterSection("Distance") {
                    DISTANCE_OPTIONS.forEach { (label, km) ->
                        ToggleChip(
                            label = label,
                            selected = filters.maxDistanceKm == km,
                            onClick = {
                                viewModel.setMaxDistance(if (filters.maxDistanceKm == km) null else km)
                            }
                        )
                    }
                }
                VSpace(12)
                FilterSection("Duration") {
                    DURATION_OPTIONS.forEach { (label, minutes) ->
                        ToggleChip(
                            label = label,
                            selected = filters.maxDurationMinutes == minutes,
                            onClick = {
                                viewModel.setMaxDuration(
                                    if (filters.maxDurationMinutes == minutes) null else minutes
                                )
                            }
                        )
                    }
                }
                if (regions.isNotEmpty()) {
                    VSpace(12)
                    FilterSection("Region") {
                        regions.forEach { region ->
                            ToggleChip(
                                label = region,
                                selected = filters.region == region,
                                onClick = {
                                    viewModel.setRegion(if (filters.region == region) null else region)
                                }
                            )
                        }
                    }
                }
            }

            if (filters.activeCount > 0) {
                VSpace(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${trails.size} of ${allTrails.size} trails",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearFilters) {
                        Text("Clear ${filters.activeCount} filter${if (filters.activeCount == 1) "" else "s"}")
                    }
                }
            }
        }

        when {
            allTrails.isEmpty() -> EmptyState(
                emoji = "🗺️",
                title = "No trails yet",
                body = "The trail catalogue is still loading. Pull down or try again in a moment."
            )

            trails.isEmpty() -> EmptyState(
                emoji = "🔍",
                title = "Nothing matches those filters",
                body = "Try widening the distance or difficulty, or clear the filters to see everything.",
                actionLabel = "Clear filters",
                onAction = viewModel::clearFilters
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(trails, key = { it.id }) { trail ->
                    TrailListCard(
                        trail = trail,
                        isSaved = trail.id in saved,
                        onOpen = { onOpenTrail(trail.id) },
                        onToggleSaved = { viewModel.toggleSaved(trail.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VSpace(6)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = if (selected) {
            { Text("✓", style = MaterialTheme.typography.bodyMedium) }
        } else null,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun TrailListCard(
    trail: Trail,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column {
            Box {
                TrailHeroArt(
                    seed = trail.id.hashCode(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                IconButton(
                    onClick = onToggleSaved,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                ) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) {
                                "Remove ${trail.name} from saved"
                            } else {
                                "Save ${trail.name}"
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {
                Text(
                    trail.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${trail.region} · ${trail.stateOrCountry}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(12)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(trail.difficulty)
                    Text(
                        "${"%.1f".format(trail.distanceKm)} km · ↑${trail.elevationGainM} m · ${trail.estimatedDurationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val DISTANCE_OPTIONS = listOf(
    "Under 5 km" to 5.0,
    "Under 10 km" to 10.0,
    "Under 20 km" to 20.0
)

private val DURATION_OPTIONS = listOf(
    "Under 2 h" to 120,
    "Under 4 h" to 240,
    "Under 8 h" to 480
)
