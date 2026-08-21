package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import com.example.domain.model.UserProfile
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.EmptyState
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.MatchViewModel

/** The list of accepted connections, with the ability to message or remove each one. */
@Composable
fun ConnectionsScreen(
    viewModel: MatchViewModel,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val connections by viewModel.connectedProfiles.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<UserProfile?>(null) }

    Column(Modifier.fillMaxSize()) {
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
                "Your connections",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (connections.isEmpty()) {
            EmptyState(
                emoji = "🤝",
                title = "No connections yet",
                body = "Connect with someone from HikeMatch and they will appear here once they accept."
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(connections, key = { it.uid }) { profile ->
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
                                    buildString {
                                        append(profile.homeArea ?: "Location not shared")
                                        if (profile.interests.isNotEmpty()) {
                                            append(" · ")
                                            append(profile.interests.joinToString(", ") { it.label })
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        VSpace(12)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.startConversation(profile, onOpenConversation)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Message") }
                            TextButton(
                                onClick = { pendingRemoval = profile },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }

    // Removing a connection is not obviously reversible from the other person's side, so
    // it asks first.
    pendingRemoval?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${profile.displayName}?") },
            text = {
                Text(
                    "You will no longer be connected. You can send a new request later if you change your mind."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeConnection(profile.uid)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }
}
