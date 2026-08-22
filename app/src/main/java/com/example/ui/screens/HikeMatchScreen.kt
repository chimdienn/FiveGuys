package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UserProfile
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.ChibiAvatar
import com.example.ui.components.ChibiMotion
import com.example.ui.components.BiomateProgressBar
import com.example.ui.components.EmptyState
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.MatchCandidate
import com.example.ui.viewmodel.MatchViewModel
import java.util.Calendar

/**
 * HikeMatch: finding compatible people for outdoor activities.
 *
 * Explicitly not a dating deck — there is no photo-first swipe. The card leads with the
 * compatibility score *and the reasons for it*, because "84%" on its own is a number
 * someone has to take on trust, while "you both hike and camp, similar pace" is a claim
 * they can check against the profile below it.
 */
@Composable
fun HikeMatchScreen(
    viewModel: MatchViewModel,
    onOpenConnections: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val incoming by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val pendingOutgoing by viewModel.pendingOutgoingCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val viewedProfile by viewModel.viewedProfile.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Find your people",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Compatible outdoor companions, not dates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showSearch = !showSearch },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (showSearch) "Close search" else "Search people by name"
                )
            }
        }

        AnimatedVisibility(visible = showSearch) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::search,
                    singleLine = true,
                    placeholder = { Text("Search by name") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .semantics { contentDescription = "Search people by name" }
                )
                if (searchQuery.isNotBlank()) {
                    VSpace(12)
                    if (searchResults.isEmpty()) {
                        Text(
                            "Nobody matches that name.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        searchResults.forEach { profile ->
                            PersonRow(
                                profile = profile,
                                onClick = { viewModel.viewProfile(profile) }
                            )
                            VSpace(8)
                        }
                    }
                }
            }
        }

        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Suggested (${candidates.size})") }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Requests (${incoming.size})") }
            )
        }

        when (tab) {
            0 -> SuggestedTab(
                candidates = candidates,
                pendingOutgoing = pendingOutgoing,
                onConnect = viewModel::connect,
                onSkip = { viewModel.skip(it.profile.uid) },
                onView = { viewModel.viewProfile(it.profile) },
                onReset = viewModel::resetSkipped,
                onOpenConnections = onOpenConnections
            )

            else -> RequestsTab(
                requests = incoming,
                onAccept = { viewModel.respond(it, accept = true) },
                onDecline = { viewModel.respond(it, accept = false) },
                onOpenConnections = onOpenConnections
            )
        }
    }

    viewedProfile?.let { candidate ->
        ProfileDetailSheet(
            candidate = candidate,
            onDismiss = viewModel::closeProfile,
            onConnect = {
                viewModel.connect(candidate)
                viewModel.closeProfile()
            },
            onMessage = {
                viewModel.startConversation(candidate.profile) { conversationId ->
                    viewModel.closeProfile()
                    onOpenConversation(conversationId)
                }
            }
        )
    }
}

@Composable
private fun SuggestedTab(
    candidates: List<MatchCandidate>,
    pendingOutgoing: Int,
    onConnect: (MatchCandidate) -> Unit,
    onSkip: (MatchCandidate) -> Unit,
    onView: (MatchCandidate) -> Unit,
    onReset: () -> Unit,
    onOpenConnections: () -> Unit
) {
    if (candidates.isEmpty()) {
        EmptyState(
            emoji = "🧭",
            title = "No strong matches right now",
            body = "Try changing your preferences or check again later. Anyone you skipped can be brought back.",
            actionLabel = "Bring back skipped people",
            onAction = onReset
        )
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pendingOutgoing > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenConnections)
                ) {
                    Text(
                        "$pendingOutgoing request${if (pendingOutgoing == 1) "" else "s"} waiting on a reply.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        items(candidates, key = { it.profile.uid }) { candidate ->
            MatchCard(
                candidate = candidate,
                onConnect = { onConnect(candidate) },
                onSkip = { onSkip(candidate) },
                onView = { onView(candidate) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCard(
    candidate: MatchCandidate,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    onView: () -> Unit
) {
    val profile = candidate.profile
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdventurerAvatar(
                    initials = profile.initials,
                    colorHex = profile.avatarColorHex,
                    sizeDp = 56
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        buildString {
                            profile.approximateAge(year)?.let { append("$it · ") }
                            append(profile.homeArea ?: "Location not shared")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${candidate.score}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics {
                            contentDescription = "${candidate.score} percent compatible"
                        }
                    )
                    Text(
                        "match",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VSpace(14)
            BiomateProgressBar(
                progress = candidate.score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            VSpace(14)
            candidate.compatibility.reasons.forEach { reason ->
                Text(
                    "• $reason",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VSpace(14)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AssistChip(onClick = onView, label = { Text(profile.fitnessLevel.label) })
                AssistChip(onClick = onView, label = { Text(profile.experienceLevel.label) })
                AssistChip(onClick = onView, label = { Text("${profile.preferredPace.label} pace") })
            }

            VSpace(18)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Skip") }
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Profile") }
                Button(
                    onClick = onConnect,
                    enabled = candidate.canConnect,
                    modifier = Modifier
                        .weight(1.2f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Connect") }
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<Pair<com.example.domain.model.Connection, UserProfile>>,
    onAccept: (com.example.domain.model.Connection) -> Unit,
    onDecline: (com.example.domain.model.Connection) -> Unit,
    onOpenConnections: () -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(
            emoji = "📮",
            title = "No pending requests",
            body = "When someone asks to connect, it will show up here.",
            actionLabel = "See your connections",
            onAction = onOpenConnections
        )
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.first.id }) { (connection, profile) ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdventurerAvatar(
                            initials = profile.initials,
                            colorHex = profile.avatarColorHex,
                            sizeDp = 48
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                profile.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                profile.homeArea ?: "Location not shared",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    VSpace(14)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onDecline(connection) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Decline") }
                        Button(
                            onClick = { onAccept(connection) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Accept") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(profile: UserProfile, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdventurerAvatar(
                initials = profile.initials,
                colorHex = profile.avatarColorHex,
                sizeDp = 40
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    profile.homeArea ?: "Location not shared",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The full profile view.
 *
 * Shows the compatibility breakdown component by component. A user who disagrees with an
 * 84% can see exactly which facet earned what, which is only possible because the scoring
 * function is deterministic and explainable.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDetailSheet(
    candidate: MatchCandidate,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onMessage: () -> Unit
) {
    val profile = candidate.profile
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            ChibiAvatar(
                userId = profile.uid,
                displayName = profile.displayName,
                motion = ChibiMotion.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
            VSpace(16)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdventurerAvatar(
                    initials = profile.initials,
                    colorHex = profile.avatarColorHex,
                    sizeDp = 64
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        buildString {
                            profile.approximateAge(year)?.let { append("$it · ") }
                            append(profile.homeArea ?: "Location not shared")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (profile.bio.isNotBlank()) {
                VSpace(16)
                Text(
                    profile.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VSpace(20)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "${candidate.score}% compatible",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    VSpace(10)
                    candidate.compatibility.reasons.forEach {
                        Text(
                            "• $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    VSpace(14)
                    candidate.compatibility.components.forEach { component ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                component.facet.name.lowercase().replaceFirstChar(Char::uppercase),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${component.points} / ${component.weight}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            VSpace(20)
            DetailBlock("Activities", profile.interests.map { it.label })
            DetailBlock(
                "Style",
                listOf(
                    profile.fitnessLevel.label,
                    profile.experienceLevel.label,
                    "${profile.preferredPace.label} pace"
                ) + profile.socialStyles.map { it.label }
            )
            if (profile.skills.isNotEmpty()) {
                DetailBlock("Skills", profile.skills.map { it.label })
            }

            VSpace(24)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Message") }
                Button(
                    onClick = onConnect,
                    enabled = candidate.canConnect,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        when (candidate.connectionStatus) {
                            com.example.domain.model.ConnectionStatus.ACCEPTED -> "Connected"
                            com.example.domain.model.ConnectionStatus.PENDING -> "Request sent"
                            else -> "Connect"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBlock(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VSpace(8)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            values.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
    }
}
