package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Trail
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Creating a trip.
 *
 * The required fields are only a title, a trail and a time; everything else — carpool,
 * food, notes, emergency information — is optional and can be filled in later from the
 * trip itself. A twelve-field mandatory form is how a group ends up with no trip at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    presetTrailId: String?,
    tripViewModel: TripViewModel,
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit
) {
    val trails by discoverViewModel.allTrails.collectAsStateWithLifecycle()
    val isSaving by tripViewModel.isSaving.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var selectedTrail by remember { mutableStateOf<Trail?>(null) }
    var trailMenuOpen by remember { mutableStateOf(false) }
    var meetingPoint by remember { mutableStateOf("") }
    var participantLimitText by remember { mutableStateOf("") }
    var carpoolNotes by remember { mutableStateOf("") }
    var foodNotes by remember { mutableStateOf("") }
    var generalNotes by remember { mutableStateOf("") }
    var emergencyNotes by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    // Defaults to 7am a week out — a plausible starting point that still has to be
    // confirmed, rather than "now", which is never right for a hike.
    var startsAt by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        )
    }

    // Preselects the trail the user came from, so arriving here from a trail page does
    // not make them find it again in a list.
    androidx.compose.runtime.LaunchedEffect(trails, presetTrailId) {
        if (selectedTrail == null && presetTrailId != null) {
            selectedTrail = trails.firstOrNull { it.id == presetTrailId }
            if (title.isBlank()) {
                selectedTrail?.let { title = it.name }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val titleError = if (touched && title.isBlank()) "Give the trip a name." else null
    val trailError = if (touched && selectedTrail == null) "Choose a trail." else null
    val limit = participantLimitText.toIntOrNull()
    val limitError = when {
        participantLimitText.isBlank() -> null
        limit == null || limit < 2 -> "Enter a number of 2 or more, or leave it blank."
        else -> null
    }

    val canSubmit = title.isNotBlank() && selectedTrail != null && limitError == null && !isSaving

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
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
                "New trip",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(Modifier.padding(20.dp)) {
            FormLabel("Trip name")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                isError = titleError != null,
                placeholder = { Text("Wilsons Prom Weekend") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Trip name" }
            )
            FieldError(titleError)

            VSpace(16)
            FormLabel("Trail")
            ExposedDropdownMenuBox(
                expanded = trailMenuOpen,
                onExpandedChange = { trailMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = selectedTrail?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    isError = trailError != null,
                    placeholder = { Text("Choose a trail") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trailMenuOpen) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        .semantics { contentDescription = "Trail" }
                )
                ExposedDropdownMenu(
                    expanded = trailMenuOpen,
                    onDismissRequest = { trailMenuOpen = false }
                ) {
                    trails.forEach { trail ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(trail.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${trail.region} · ${"%.1f".format(trail.distanceKm)} km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedTrail = trail
                                if (title.isBlank()) title = trail.name
                                trailMenuOpen = false
                            }
                        )
                    }
                }
            }
            FieldError(trailError)

            VSpace(16)
            FormLabel("When")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerButton(
                    text = dateFormat.format(Date(startsAt)),
                    contentDescription = "Change date",
                    modifier = Modifier.weight(1f)
                ) {
                    val calendar = Calendar.getInstance().apply { timeInMillis = startsAt }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            startsAt = Calendar.getInstance().apply {
                                timeInMillis = startsAt
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                PickerButton(
                    text = timeFormat.format(Date(startsAt)),
                    contentDescription = "Change departure time",
                    modifier = Modifier.weight(1f)
                ) {
                    val calendar = Calendar.getInstance().apply { timeInMillis = startsAt }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            startsAt = Calendar.getInstance().apply {
                                timeInMillis = startsAt
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }.timeInMillis
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                }
            }

            VSpace(16)
            FormLabel("Meeting point")
            OutlinedTextField(
                value = meetingPoint,
                onValueChange = { meetingPoint = it },
                singleLine = true,
                placeholder = { Text("Telegraph Saddle car park") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Meeting point" }
            )

            VSpace(16)
            FormLabel("Participant limit (optional)")
            OutlinedTextField(
                value = participantLimitText,
                onValueChange = { participantLimitText = it.filter(Char::isDigit).take(2) },
                singleLine = true,
                isError = limitError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Leave blank for no limit") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Participant limit" }
            )
            FieldError(limitError)

            VSpace(16)
            FormLabel("Carpool notes (optional)")
            NotesField(carpoolNotes, { carpoolNotes = it }, "Alex — 3 seats, leaving 05:30 from UniMelb", "Carpool notes")

            VSpace(16)
            FormLabel("Food notes (optional)")
            NotesField(foodNotes, { foodNotes = it }, "Everyone brings their own lunch", "Food notes")

            VSpace(16)
            FormLabel("General notes (optional)")
            NotesField(generalNotes, { generalNotes = it }, "Tide-dependent crossing — check before we go", "General notes")

            VSpace(16)
            FormLabel("Emergency information (optional)")
            NotesField(
                emergencyNotes,
                { emergencyNotes = it },
                "Parks Victoria 13 19 63 · Emergency 000 · nearest hospital",
                "Emergency information"
            )
            VSpace(6)
            Text(
                "Worth filling in. Everyone on the trip can see it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VSpace(28)
            Button(
                onClick = {
                    touched = true
                    val trail = selectedTrail ?: return@Button
                    tripViewModel.createTrip(
                        title = title,
                        trail = trail,
                        startsAt = startsAt,
                        meetingPoint = meetingPoint,
                        participantLimit = limit,
                        carpoolNotes = carpoolNotes,
                        foodNotes = foodNotes,
                        generalNotes = generalNotes,
                        emergencyNotes = emergencyNotes,
                        onCreated = onCreated
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isSaving) "Creating…" else "Create trip",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            VSpace(32)
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    VSpace(6)
}

@Composable
private fun FieldError(error: String?) {
    if (error == null) return
    VSpace(4)
    Text(
        "⚠ $error",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun NotesField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        minLines = 2,
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
    )
}

@Composable
private fun PickerButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .semantics { this.contentDescription = "$contentDescription, currently $text" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
