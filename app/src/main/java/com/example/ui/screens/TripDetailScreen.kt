package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Conversation
import com.example.domain.model.GearItem
import com.example.domain.model.Readiness
import com.example.domain.model.ReadinessItem
import com.example.domain.model.TripMember
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.BiomateProgressBar
import com.example.ui.components.LoadingState
import com.example.ui.components.SectionHeader
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.MatchViewModel
import com.example.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single trip: who is coming, what everyone is carrying, and whether they are ready.
 *
 * The organiser sees management controls; participants see only what they can actually
 * change. Hiding an action nobody is allowed to take is kinder than showing it and
 * failing on tap.
 */
@Composable
fun TripDetailScreen(
    tripId: String,
    tripViewModel: TripViewModel,
    matchViewModel: MatchViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onStartAdventure: (String) -> Unit
) {
    LaunchedEffect(tripId) { tripViewModel.selectTrip(tripId) }

    val trip by tripViewModel.selectedTrip.collectAsStateWithLifecycle()
    val joined by tripViewModel.joinedMembers.collectAsStateWithLifecycle()
    val invited by tripViewModel.invitedMembers.collectAsStateWithLifecycle()
    val gear by tripViewModel.gear.collectAsStateWithLifecycle()
    val allReadiness by tripViewModel.allReadiness.collectAsStateWithLifecycle()
    val myReadiness by tripViewModel.myReadiness.collectAsStateWithLifecycle()
    val isOrganiser by tripViewModel.isOrganiser.collectAsStateWithLifecycle()
    val myMembership by tripViewModel.myMembership.collectAsStateWithLifecycle()
    val connections by matchViewModel.connectedProfiles.collectAsStateWithLifecycle()

    var showInvite by remember { mutableStateOf(false) }
    var showAddGear by remember { mutableStateOf(false) }
    var showReadiness by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var showLeave by remember { mutableStateOf(false) }

    val currentTrip = trip
    if (currentTrip == null) {
        LoadingState("Loading trip…")
        return
    }

    val dateFormat = remember { SimpleDateFormat("EEEE d MMMM yyyy · h:mm a", Locale.getDefault()) }
    val isInvitee = myMembership?.status == TripMemberStatus.INVITED
    val packedCount = gear.count { it.isPacked }

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
                Column(Modifier.weight(1f)) {
                    Text(
                        currentTrip.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        currentTrip.trailName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TripStatusBadge(currentTrip.status)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        dateFormat.format(Date(currentTrip.startsAt)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentTrip.meetingPoint.isNotBlank()) {
                        VSpace(6)
                        Text(
                            "📍 ${currentTrip.meetingPoint}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    currentTrip.participantLimit?.let {
                        VSpace(6)
                        Text(
                            "${joined.size} of $it spots taken",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (isInvitee) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "You've been invited",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        VSpace(12)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { tripViewModel.declineInvite(tripId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Decline") }
                            Button(
                                onClick = { tripViewModel.joinTrip(tripId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Join trip") }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "Who's coming (${joined.size})",
                    actionLabel = if (isOrganiser) "Invite" else null,
                    onAction = if (isOrganiser) ({ showInvite = true }) else null
                )
                VSpace(12)
                joined.forEach { member ->
                    MemberRow(
                        member = member,
                        readiness = allReadiness.firstOrNull { it.uid == member.uid }
                    )
                    VSpace(8)
                }
                if (invited.isNotEmpty()) {
                    VSpace(6)
                    Text(
                        "Invited: ${invited.joinToString(", ") { it.displayName }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "Shared gear ($packedCount/${gear.size} packed)",
                    actionLabel = "Add",
                    onAction = { showAddGear = true }
                )
                VSpace(12)
                if (gear.isEmpty()) {
                    Text(
                        "Nothing on the shared list yet. Add the things only one person needs to carry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    gear.forEach { item ->
                        GearRow(
                            item = item,
                            members = joined,
                            isOrganiser = isOrganiser,
                            onToggleAssignment = { tripViewModel.toggleGearAssignment(item) },
                            onTogglePacked = { tripViewModel.toggleGearPacked(item) },
                            onAssignTo = { member -> tripViewModel.assignGearTo(item, member) },
                            onRemove = { tripViewModel.removeGear(item) }
                        )
                        VSpace(8)
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Your readiness")
                VSpace(12)
                ReadinessSummaryCard(
                    readiness = myReadiness,
                    onOpen = { showReadiness = true }
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Trip notes")
                VSpace(12)
                NotesBlock("🚗 Carpool", currentTrip.carpoolNotes)
                NotesBlock("🍎 Food", currentTrip.foodNotes)
                NotesBlock("📋 General", currentTrip.generalNotes)
                NotesBlock("🚨 Emergency", currentTrip.emergencyNotes)
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                OutlinedButton(
                    onClick = { onOpenChat(Conversation.tripIdFor(tripId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Open group chat", style = MaterialTheme.typography.titleMedium) }

                if (currentTrip.status == TripStatus.PLANNING || currentTrip.status == TripStatus.ACTIVE) {
                    VSpace(10)
                    Button(
                        onClick = { onStartAdventure(currentTrip.trailId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (currentTrip.status == TripStatus.ACTIVE) "Rejoin adventure" else "Start adventure",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                VSpace(10)
                if (isOrganiser) {
                    if (currentTrip.status != TripStatus.CANCELLED && currentTrip.status != TripStatus.COMPLETED) {
                        TextButton(
                            onClick = { showCancel = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) { Text("Cancel trip", color = MaterialTheme.colorScheme.error) }
                    }
                } else if (myMembership?.status == TripMemberStatus.JOINED) {
                    TextButton(
                        onClick = { showLeave = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) { Text("Leave trip", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showInvite) {
        InviteDialog(
            connections = connections,
            alreadyOnTrip = (joined + invited).map { it.uid }.toSet(),
            onInvite = { profile ->
                tripViewModel.invite(profile)
                showInvite = false
            },
            onDismiss = { showInvite = false }
        )
    }

    if (showAddGear) {
        AddGearDialog(
            onAdd = { name, category, quantity, essential ->
                tripViewModel.addGear(name, category, quantity, essential)
                showAddGear = false
            },
            onDismiss = { showAddGear = false }
        )
    }

    if (showReadiness) {
        ReadinessDialog(
            initial = myReadiness ?: Readiness(tripId, ""),
            onSave = { items, confidence, notes ->
                tripViewModel.saveReadiness(items, confidence, notes)
                showReadiness = false
            },
            onDismiss = { showReadiness = false }
        )
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Cancel this trip?") },
            text = { Text("Everyone on the trip will see it as cancelled. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    tripViewModel.cancelTrip()
                    showCancel = false
                    onBack()
                }) { Text("Cancel trip") }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep it") } }
        )
    }

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text("Leave this trip?") },
            text = { Text("You'll be removed from the group and the chat. The organiser can invite you again.") },
            confirmButton = {
                TextButton(onClick = {
                    tripViewModel.leaveTrip()
                    showLeave = false
                    onBack()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { showLeave = false }) { Text("Stay") } }
        )
    }
}

@Composable
private fun NotesBlock(title: String, body: String) {
    if (body.isBlank()) return
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VSpace(4)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MemberRow(member: TripMember, readiness: Readiness?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdventurerAvatar(
                initials = member.displayName.take(2).uppercase(),
                colorHex = 0xFFCD744C,
                sizeDp = 40
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    TripViewModel.roleLabel(member.role),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Shows how prepared each person says they are, without allowing anyone to
            // change anyone else's answer.
            Text(
                when {
                    readiness == null -> "Not started"
                    readiness.isComplete -> "✓ Ready"
                    else -> "${readiness.completedCount}/${readiness.totalCount}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GearRow(
    item: GearItem,
    members: List<TripMember>,
    isOrganiser: Boolean,
    onToggleAssignment: () -> Unit,
    onTogglePacked: () -> Unit,
    onAssignTo: (TripMember?) -> Unit,
    onRemove: () -> Unit
) {
    var showAssign by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPacked,
                onCheckedChange = { onTogglePacked() },
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "${item.name} packed"
                    }
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isEssential) {
                        Text(
                            " · essential",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    item.assignedToName ?: "Unassigned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = { if (isOrganiser) showAssign = true else onToggleAssignment() },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(if (item.assignedToUid == null) "Take it" else "Change")
            }
        }
    }

    if (showAssign) {
        AlertDialog(
            onDismissRequest = { showAssign = false },
            title = { Text("Who's carrying ${item.name}?") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            onAssignTo(null)
                            showAssign = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) { Text("Nobody yet") }
                    members.forEach { member ->
                        TextButton(
                            onClick = {
                                onAssignTo(member)
                                showAssign = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) { Text(member.displayName) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showAssign = false
                }) { Text("Remove item", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showAssign = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ReadinessSummaryCard(readiness: Readiness?, onOpen: () -> Unit) {
    val completed = readiness?.completedCount ?: 0
    val total = ReadinessItem.all.size
    val fraction = if (total == 0) 0f else completed.toFloat() / total

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (readiness?.isComplete == true) "You're ready" else "Preparation checklist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$completed / $total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            VSpace(10)
            BiomateProgressBar(
                progress = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentDescription = "$completed of $total preparation items done"
            )
            VSpace(10)
            Text(
                "Only you can fill this in, and it is guidance rather than a safety guarantee.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadinessDialog(
    initial: Readiness,
    onSave: (Set<ReadinessItem>, Int?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var checked by remember { mutableStateOf(initial.checkedItems) }
    var confidence by remember { mutableStateOf(initial.confidence) }
    var notes by remember { mutableStateOf(initial.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your readiness") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ReadinessItem.all.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                checked = if (item in checked) checked - item else checked + item
                            }
                            .defaultMinSize(minHeight = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item in checked,
                            onCheckedChange = {
                                checked = if (item in checked) checked - item else checked + item
                            }
                        )
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                VSpace(16)
                Text(
                    "How prepared do you feel?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { value ->
                        AssistChip(
                            onClick = { confidence = value },
                            label = { Text(value.toString()) },
                            modifier = Modifier.semantics {
                                contentDescription = "Confidence $value out of 5"
                            }
                        )
                    }
                }
                confidence?.let {
                    VSpace(4)
                    Text(
                        "Selected: $it out of 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VSpace(16)
                Text(
                    "Any issues or injuries the group should know about? (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(8)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Readiness notes" }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(checked, confidence, notes) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun InviteDialog(
    connections: List<com.example.domain.model.UserProfile>,
    alreadyOnTrip: Set<String>,
    onInvite: (com.example.domain.model.UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val available = connections.filter { it.uid !in alreadyOnTrip }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite people") },
        text = {
            if (available.isEmpty()) {
                Text(
                    if (connections.isEmpty()) {
                        "You have no connections yet. Connect with people in HikeMatch first."
                    } else {
                        "Everyone you're connected with is already on this trip."
                    }
                )
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    available.forEach { profile ->
                        TextButton(
                            onClick = { onInvite(profile) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AdventurerAvatar(
                                    initials = profile.initials,
                                    colorHex = profile.avatarColorHex,
                                    sizeDp = 32
                                )
                                Text(
                                    profile.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun AddGearDialog(
    onAdd: (String, String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var quantityText by remember { mutableStateOf("1") }
    var essential by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add shared gear") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Item") },
                    placeholder = { Text("Satellite communicator") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                VSpace(12)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        singleLine = true,
                        label = { Text("Category") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter(Char::isDigit).take(2) },
                        singleLine = true,
                        label = { Text("Qty") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                VSpace(12)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { essential = !essential }
                        .defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = essential, onCheckedChange = { essential = it })
                    Text("Essential — the trip should not go without it")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, category, quantityText.toIntOrNull() ?: 1, essential) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
