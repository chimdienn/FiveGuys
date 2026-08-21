package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.BiomateProgressBar
import com.example.ui.viewmodel.SessionViewModel

/**
 * The post-registration profile setup.
 *
 * Split into short steps rather than one long form: the compatibility algorithm needs six
 * different signals, and asking for all of them at once is how you get people mashing
 * "next" with defaults. Only the display name and one interest are required — everything
 * else can be skipped and filled in later from Profile, because a half-finished profile
 * that lets someone into the app beats a complete one they abandoned.
 */
@Composable
fun OnboardingScreen(
    uid: String,
    initialDisplayName: String,
    viewModel: SessionViewModel
) {
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(0) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var bio by remember { mutableStateOf("") }
    var birthYearText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var homeArea by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(setOf<ActivityType>()) }
    var fitness by remember { mutableStateOf(FitnessLevel.MODERATE) }
    var experience by remember { mutableStateOf(ExperienceLevel.BEGINNER) }
    var pace by remember { mutableStateOf(PreferredPace.MODERATE) }
    var socialStyles by remember { mutableStateOf(setOf<SocialStyle>()) }
    var skills by remember { mutableStateOf(setOf<Skill>()) }
    val avatarColour = remember { AVATAR_COLOURS.random() }

    val totalSteps = 5
    val progress by animateFloatAsState(
        targetValue = (step + 1f) / totalSteps,
        label = "onboarding_progress"
    )

    val birthYear = birthYearText.toIntOrNull()
    val birthYearError = when {
        birthYearText.isBlank() -> null
        birthYear == null -> "Enter a four-digit year."
        birthYear !in 1900..2020 -> "That does not look like a birth year."
        else -> null
    }

    val canAdvance = when (step) {
        0 -> displayName.isNotBlank() && birthYearError == null
        1 -> interests.isNotEmpty()
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        BiomateProgressBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            contentDescription = "Step ${step + 1} of $totalSteps"
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Step ${step + 1} of $totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedContent(targetState = step, label = "onboarding_step") { current ->
                when (current) {
                    0 -> StepBasics(
                        displayName = displayName,
                        onDisplayName = { displayName = it },
                        bio = bio,
                        onBio = { bio = it },
                        birthYearText = birthYearText,
                        onBirthYear = { birthYearText = it.filter(Char::isDigit).take(4) },
                        birthYearError = birthYearError,
                        gender = gender,
                        onGender = { gender = it },
                        homeArea = homeArea,
                        onHomeArea = { homeArea = it }
                    )
                    1 -> StepInterests(interests) { interests = it }
                    2 -> StepLevels(
                        fitness = fitness, onFitness = { fitness = it },
                        experience = experience, onExperience = { experience = it }
                    )
                    3 -> StepPaceAndStyle(
                        pace = pace, onPace = { pace = it },
                        styles = socialStyles, onStyles = { socialStyles = it }
                    )
                    else -> StepSkills(skills) { skills = it }
                }
            }
        }

        if (formError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    formError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Back") }
            }
            Button(
                onClick = {
                    if (step < totalSteps - 1) {
                        step++
                    } else {
                        viewModel.completeOnboarding(
                            uid = uid,
                            displayName = displayName,
                            bio = bio,
                            birthYear = birthYear,
                            gender = gender.takeIf { it.isNotBlank() },
                            homeArea = homeArea.takeIf { it.isNotBlank() },
                            interests = interests,
                            fitnessLevel = fitness,
                            experienceLevel = experience,
                            preferredPace = pace,
                            socialStyles = socialStyles,
                            skills = skills,
                            avatarColorHex = avatarColour
                        )
                    }
                },
                enabled = canAdvance && !isSubmitting,
                modifier = Modifier
                    .weight(if (step > 0) 1f else 2f)
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    when {
                        isSubmitting -> "Saving…"
                        step < totalSteps - 1 -> "Continue"
                        else -> "Start exploring"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StepBasics(
    displayName: String,
    onDisplayName: (String) -> Unit,
    bio: String,
    onBio: (String) -> Unit,
    birthYearText: String,
    onBirthYear: (String) -> Unit,
    birthYearError: String?,
    gender: String,
    onGender: (String) -> Unit,
    homeArea: String,
    onHomeArea: (String) -> Unit
) {
    Column {
        StepHeader("About you", "This is what other explorers will see.")

        FieldLabel("Display name")
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayName,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Display name" },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("Short bio (optional)")
        OutlinedTextField(
            value = bio,
            onValueChange = onBio,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Short bio" },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("Birth year (optional)")
        OutlinedTextField(
            value = birthYearText,
            onValueChange = onBirthYear,
            singleLine = true,
            isError = birthYearError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Birth year" },
            shape = RoundedCornerShape(14.dp)
        )
        if (birthYearError != null) {
            Text(
                "⚠ $birthYearError",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))
        FieldLabel("Gender (optional)")
        OutlinedTextField(
            value = gender,
            onValueChange = onGender,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Gender" },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("Approximate area (optional)")
        OutlinedTextField(
            value = homeArea,
            onValueChange = onHomeArea,
            singleLine = true,
            placeholder = { Text("Melbourne, Victoria") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Approximate area" },
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(6.dp))
        // States the privacy rule at the point of collection, where it can actually
        // influence what someone types (spec section 64).
        Text(
            "A suburb or city is enough. Biomate never asks for or stores your exact address.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepInterests(selected: Set<ActivityType>, onChange: (Set<ActivityType>) -> Unit) {
    Column {
        StepHeader("What do you want to do?", "Pick everything that appeals. This drives your matches and recommendations.")
        ChipGroup(
            options = ActivityType.mvpActivities,
            isSelected = { it in selected },
            label = { it.label },
            onToggle = { option ->
                onChange(if (option in selected) selected - option else selected + option)
            }
        )
        if (selected.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Choose at least one to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepLevels(
    fitness: FitnessLevel,
    onFitness: (FitnessLevel) -> Unit,
    experience: ExperienceLevel,
    onExperience: (ExperienceLevel) -> Unit
) {
    Column {
        StepHeader("Where are you at?", "Be honest — this is how Biomate finds people you will actually enjoy walking with.")
        FieldLabel("Fitness level")
        ChipGroup(
            options = FitnessLevel.entries,
            isSelected = { it == fitness },
            label = { it.label },
            onToggle = onFitness
        )
        Spacer(Modifier.height(24.dp))
        FieldLabel("Outdoor experience")
        ChipGroup(
            options = ExperienceLevel.entries,
            isSelected = { it == experience },
            label = { it.label },
            onToggle = onExperience
        )
    }
}

@Composable
private fun StepPaceAndStyle(
    pace: PreferredPace,
    onPace: (PreferredPace) -> Unit,
    styles: Set<SocialStyle>,
    onStyles: (Set<SocialStyle>) -> Unit
) {
    Column {
        StepHeader("How do you like to walk?", "Pace and vibe matter more than fitness for a good day out.")
        FieldLabel("Preferred pace")
        ChipGroup(
            options = PreferredPace.entries,
            isSelected = { it == pace },
            label = { "${it.label} · ~${it.approxKmh} km/h" },
            onToggle = onPace
        )
        Spacer(Modifier.height(24.dp))
        FieldLabel("Your style (pick any)")
        ChipGroup(
            options = SocialStyle.entries,
            isSelected = { it in styles },
            label = { it.label },
            onToggle = { option -> onStyles(if (option in styles) styles - option else styles + option) }
        )
    }
}

@Composable
private fun StepSkills(skills: Set<Skill>, onChange: (Set<Skill>) -> Unit) {
    Column {
        StepHeader("Anything you bring to a group?", "Optional — useful when a trip is sorting out who carries what.")
        ChipGroup(
            options = Skill.entries,
            isSelected = { it in skills },
            label = { it.label },
            onToggle = { option -> onChange(if (option in skills) skills - option else skills + option) }
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "You can change any of this later from your profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * A wrapping row of selectable chips.
 *
 * Each chip carries a check mark when selected as well as the colour change, so selection
 * is never conveyed by colour alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

private val AVATAR_COLOURS = listOf(
    0xFFCD744C, 0xFF1B4938, 0xFFD97706, 0xFF2563EB,
    0xFF7C3AED, 0xFF0D9488, 0xFFBE185D, 0xFF65A30D
)
