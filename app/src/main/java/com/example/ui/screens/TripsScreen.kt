package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.ui.components.EmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Upcoming and past trips, split so the next thing to prepare for is always at the top. */
@Composable
fun TripsScreen(
    viewModel: TripViewModel,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenMemories: () -> Unit
) {
    val upcoming by viewModel.upcomingTrips.collectAsStateWithLifecycle()
    val past by viewModel.pastTrips.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The app-level Scaffold has already consumed the system bar insets; consuming
        // them again here would pad the top twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTrip,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New trip") }
            )
        }
    ) { padding ->
        if (upcoming.isEmpty() && past.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyState(
                    emoji = "🏕️",
                    title = "No upcoming adventures",
                    body = "Find a trail and create your first trip. Invite the people you've connected with and sort out gear together.",
                    actionLabel = "Create a trip",
                    onAction = onCreateTrip
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your trips",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onOpenMessages) { Text("Messages") }
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming") }
                items(upcoming, key = { it.id }) { trip ->
                    TripCard(trip = trip, onOpen = { onOpenTrip(trip.id) })
                }
            }

            if (past.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Past",
                        actionLabel = "Memories",
                        onAction = onOpenMemories
                    )
                }
                items(past, key = { it.id }) { trip ->
                    TripCard(trip = trip, onOpen = { onOpenTrip(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onOpen: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM yyyy · h:mm a", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        trip.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        trip.trailName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TripStatusBadge(trip.status)
            }
            VSpace(12)
            Text(
                dateFormat.format(Date(trip.startsAt)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (trip.meetingPoint.isNotBlank()) {
                Text(
                    "📍 ${trip.meetingPoint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TripStatusBadge(status: TripStatus, modifier: Modifier = Modifier) {
    val (label, container, content) = when (status) {
        TripStatus.PLANNING -> Triple(
            "Planning",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        TripStatus.ACTIVE -> Triple(
            "On trail",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TripStatus.COMPLETED -> Triple(
            "Completed",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TripStatus.CANCELLED -> Triple(
            "Cancelled",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    StatusBadge(label, container, content, modifier)
}
