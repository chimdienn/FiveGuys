package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ActivityType
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FitnessLevel
import com.example.domain.model.PreferredPace
import com.example.domain.model.Skill
import com.example.domain.model.SocialStyle
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.SessionViewModel

/**
 * Editing the profile after onboarding.
 *
 * Everything set during onboarding is editable here, so nothing chosen in a hurry on day
 * one is permanent — including the preferences the matching algorithm reads.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    sessionViewModel: SessionViewModel,
    onBack: () -> Unit
) {
    val profile by sessionViewModel.currentProfile.collectAsStateWithLifecycle()
    val isSubmitting by sessionViewModel.isSubmitting.collectAsStateWithLifecycle()
    val formError by sessionViewModel.formError.collectAsStateWithLifecycle()

    val currentProfile = profile ?: return

    var displayName by remember(currentProfile.uid) { mutableStateOf(currentProfile.displayName) }
    var bio by remember(currentProfile.uid) { mutableStateOf(currentProfile.bio) }
    var birthYearText by remember(currentProfile.uid) {
        mutableStateOf(currentProfile.birthYear?.toString().orEmpty())
    }
    var gender by remember(currentProfile.uid) { mutableStateOf(currentProfile.gender.orEmpty()) }
    var homeArea by remember(currentProfile.uid) { mutableStateOf(currentProfile.homeArea.orEmpty()) }
    var interests by remember(currentProfile.uid) { mutableStateOf(currentProfile.interests) }
    var fitness by remember(currentProfile.uid) { mutableStateOf(currentProfile.fitnessLevel) }
    var experience by remember(currentProfile.uid) { mutableStateOf(currentProfile.experienceLevel) }
    var pace by remember(currentProfile.uid) { mutableStateOf(currentProfile.preferredPace) }
    var styles by remember(currentProfile.uid) { mutableStateOf(currentProfile.socialStyles) }
    var skills by remember(currentProfile.uid) { mutableStateOf(currentProfile.skills) }

    val birthYear = birthYearText.toIntOrNull()
    val birthYearError = when {
        birthYearText.isBlank() -> null
        birthYear == null || birthYear !in 1900..2020 -> "Enter a valid four-digit year."
        else -> null
    }
    val nameError = if (displayName.isBlank()) "Enter a display name." else null
    val canSave = nameError == null && birthYearError == null && interests.isNotEmpty() && !isSubmitting

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
                "Edit profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(Modifier.padding(20.dp)) {
            EditLabel("Display name")
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                singleLine = true,
                isError = nameError != null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Display name" }
            )
            if (nameError != null) {
                Text(
                    "⚠ $nameError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            VSpace(16)
            EditLabel("Bio")
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Bio" }
            )

            VSpace(16)
            EditLabel("Birth year")
            OutlinedTextField(
                value = birthYearText,
                onValueChange = { birthYearText = it.filter(Char::isDigit).take(4) },
                singleLine = true,
                isError = birthYearError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Birth year" }
            )
            if (birthYearError != null) {
                Text(
                    "⚠ $birthYearError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            VSpace(16)
            EditLabel("Gender")
            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Gender" }
            )

            VSpace(16)
            EditLabel("Approximate area")
            OutlinedTextField(
                value = homeArea,
                onValueChange = { homeArea = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Approximate area" }
            )
            VSpace(4)
            Text(
                "Suburb or city only. Biomate never stores your exact address.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VSpace(20)
            EditLabel("Activities")
            EditChips(
                options = ActivityType.mvpActivities,
                isSelected = { it in interests },
                label = { it.label },
                onToggle = { interests = if (it in interests) interests - it else interests + it }
            )
            if (interests.isEmpty()) {
                Text(
                    "⚠ Choose at least one activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            VSpace(20)
            EditLabel("Fitness level")
            EditChips(
                options = FitnessLevel.entries,
                isSelected = { it == fitness },
                label = { it.label },
                onToggle = { fitness = it }
            )

            VSpace(20)
            EditLabel("Experience")
            EditChips(
                options = ExperienceLevel.entries,
                isSelected = { it == experience },
                label = { it.label },
                onToggle = { experience = it }
            )

            VSpace(20)
            EditLabel("Preferred pace")
            EditChips(
                options = PreferredPace.entries,
                isSelected = { it == pace },
                label = { it.label },
                onToggle = { pace = it }
            )

            VSpace(20)
            EditLabel("Social style")
            EditChips(
                options = SocialStyle.entries,
                isSelected = { it in styles },
                label = { it.label },
                onToggle = { styles = if (it in styles) styles - it else styles + it }
            )

            VSpace(20)
            EditLabel("Skills")
            EditChips(
                options = Skill.entries,
                isSelected = { it in skills },
                label = { it.label },
                onToggle = { skills = if (it in skills) skills - it else skills + it }
            )

            if (formError != null) {
                VSpace(16)
                Text(
                    "⚠ $formError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            VSpace(28)
            Button(
                onClick = {
                    sessionViewModel.updateProfile(
                        currentProfile.copy(
                            displayName = displayName.trim(),
                            bio = bio.trim(),
                            birthYear = birthYear,
                            gender = gender.trim().takeIf { it.isNotBlank() },
                            homeArea = homeArea.trim().takeIf { it.isNotBlank() },
                            interests = interests,
                            fitnessLevel = fitness,
                            experienceLevel = experience,
                            preferredPace = pace,
                            socialStyles = styles,
                            skills = skills
                        )
                    )
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isSubmitting) "Saving…" else "Save changes",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            VSpace(32)
        }
    }
}

@Composable
private fun EditLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    VSpace(8)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EditChips(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = isSelected(option)
            FilterChip(
                selected = selected,
                onClick = { onToggle(option) },
                label = { Text(label(option), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = if (selected) {
                    { Text("✓", style = MaterialTheme.typography.bodyMedium) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            )
        }
    }
}
