package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.BadgeStatus
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The user's own profile.
 *
 * Every number here is derived from persisted activity — completed sessions, the coin
 * ledger, moments actually created. The prototype displayed seeded constants
 * (`totalHikes = 42`); those are gone, which means a new account honestly shows zeros
 * until it earns otherwise.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    sessionViewModel: SessionViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenMemories: () -> Unit
) {
    val profile by sessionViewModel.currentProfile.collectAsStateWithLifecycle()
    val stats by profileViewModel.stats.collectAsStateWithLifecycle()
    val coins by profileViewModel.bioCoins.collectAsStateWithLifecycle()
    val badges by profileViewModel.badges.collectAsStateWithLifecycle()
    val history by profileViewModel.history.collectAsStateWithLifecycle()
    val transactions by profileViewModel.transactions.collectAsStateWithLifecycle()

    var showSignOut by remember { mutableStateOf(false) }
    val currentProfile = profile ?: return
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Your profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEditProfile) { Text("Edit") }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdventurerAvatar(
                    initials = currentProfile.initials,
                    colorHex = currentProfile.avatarColorHex,
                    sizeDp = 96
                )
                VSpace(14)
                Text(
                    currentProfile.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    buildString {
                        currentProfile.approximateAge(year)?.let { append("$it · ") }
                        append(currentProfile.homeArea ?: "Location not shared")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentProfile.bio.isNotBlank()) {
                    VSpace(12)
                    Text(
                        currentProfile.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Your activity")
                VSpace(12)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${stats?.trailsCompleted ?: 0}", "Trails", Modifier.weight(1f))
                    StatTile(
                        "${"%.0f".format(stats?.totalDistanceKm ?: 0.0)} km",
                        "Explored",
                        Modifier.weight(1f)
                    )
                    StatTile("${stats?.tripsCompleted ?: 0}", "Trips", Modifier.weight(1f))
                }
                VSpace(10)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("$coins", "BioCoins", Modifier.weight(1f))
                    StatTile(
                        "${stats?.trailMomentsCreated ?: 0}",
                        "Moments",
                        Modifier.weight(1f)
                    )
                    StatTile(
                        // "—" rather than "0%" when there is no group history: never
                        // having joined a group trip is not a 0% attendance record.
                        stats?.attendanceRatePercent?.let { "$it%" } ?: "—",
                        "Attendance",
                        Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Outdoor preferences")
                VSpace(12)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    currentProfile.interests.forEach {
                        AssistChip(onClick = {}, label = { Text(it.label) })
                    }
                    AssistChip(onClick = {}, label = { Text(currentProfile.fitnessLevel.label) })
                    AssistChip(onClick = {}, label = { Text(currentProfile.experienceLevel.label) })
                    AssistChip(onClick = {}, label = { Text("${currentProfile.preferredPace.label} pace") })
                    currentProfile.socialStyles.forEach {
                        AssistChip(onClick = {}, label = { Text(it.label) })
                    }
                }
            }
        }

        if (currentProfile.skills.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader("Skills")
                    VSpace(12)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        currentProfile.skills.forEach {
                            AssistChip(onClick = {}, label = { Text(it.label) })
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Badges (${badges.count { it.isEarned }}/${badges.size})")
                VSpace(12)
                badges.forEach { status ->
                    BadgeRow(status)
                    VSpace(8)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "Adventure history",
                    actionLabel = if (history.isNotEmpty()) "Memories" else null,
                    onAction = if (history.isNotEmpty()) onOpenMemories else null
                )
                VSpace(12)
                if (history.isEmpty()) {
                    Text(
                        "No completed adventures yet. Start one from a trail and finish it to build your history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    history.take(5).forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    entry.trail?.name ?: entry.trip?.trailName ?: "Adventure",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    dateFormat.format(
                                        Date(entry.session.completedAt ?: entry.session.startedAt)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VSpace(8)
                                Text(
                                    "${"%.2f".format(entry.session.distanceKm)} km · " +
                                        "${entry.session.durationMinutes} min" +
                                        if (entry.session.companionCount > 0) {
                                            " · ${entry.session.companionCount} companions"
                                        } else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        VSpace(8)
                    }
                }
            }
        }

        if (transactions.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader("BioCoin history")
                    VSpace(12)
                    transactions.take(8).forEach { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    transaction.reason,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    dateFormat.format(Date(transaction.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "+${transaction.amount}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                OutlinedButton(
                    onClick = onOpenConnections,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Your connections") }
                VSpace(10)
                TextButton(
                    onClick = { showSignOut = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You can sign back in at any time. Your trips and history stay where they are.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOut = false
                    sessionViewModel.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { showSignOut = false }) { Text("Stay") } }
        )
    }
}

/**
 * One badge row.
 *
 * Locked badges are shown greyed with their criterion, rather than hidden — knowing that
 * "Explorer" needs five trails is what makes it worth chasing.
 */
@Composable
private fun BadgeRow(status: BadgeStatus) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (status.isEarned) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (status.isEarned) status.badge.emoji else "🔒",
                style = MaterialTheme.typography.titleLarge
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    status.badge.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    status.badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (status.isEarned) {
                Text(
                    "Earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
