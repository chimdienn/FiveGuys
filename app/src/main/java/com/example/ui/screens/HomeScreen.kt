package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Trip
import com.example.domain.model.Weather
import com.example.domain.repository.DailyChallengeView
import com.example.domain.repository.LeaderboardEntry
import com.example.domain.weather.TrailRanking
import com.example.domain.weather.TrailRecommendation
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.BiomateProgressBar
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.ErrorState
import com.example.ui.components.SafetyNotice
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrailHeroArt
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.WeatherUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The app's landing screen.
 *
 * Ordered by what a user opening Biomate actually wants to know, in order: who they are
 * and how they are doing, what the weather is, where they might go, what they can earn,
 * and what is already booked in. Everything below the fold is discovery rather than
 * necessity.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    onOpenTrail: (String) -> Unit,
    onOpenTrip: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val profile by sessionViewModel.currentProfile.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val challenges by viewModel.dailyChallenges.collectAsStateWithLifecycle()
    val coins by viewModel.bioCoins.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val recommendation by viewModel.recommendation.collectAsStateWithLifecycle()
    val rankedTrails by viewModel.rankedTrails.collectAsStateWithLifecycle()
    val upcomingTrip by viewModel.upcomingTrip.collectAsStateWithLifecycle()

    // Re-check the calendar day on every entry so an app left open overnight rolls its
    // challenges over rather than showing yesterday's.
    LaunchedEffect(Unit) { viewModel.refreshDay() }

    val displayName = profile?.displayName?.substringBefore(' ') ?: "there"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HomeHeader(
                greeting = HomeViewModel.greetingFor(HomeViewModel.currentHour()),
                name = displayName,
                weather = weather,
                initials = profile?.initials ?: "?",
                avatarColour = profile?.avatarColorHex ?: 0xFFCD744C,
                onOpenProfile = onOpenProfile
            )
        }

        item {
            BiomateCharacter(
                userId = profile?.uid.orEmpty(),
                bioCoins = coins,
                challengesCompleted = challenges.count { it.daily.isComplete },
                challengeTotal = challenges.size
            )
        }

        item {
            WeatherCard(state = weather, onRetry = viewModel::refreshWeather)
        }

        if (upcomingTrip != null) {
            item {
                UpcomingTripCard(trip = upcomingTrip!!, onOpen = { onOpenTrip(upcomingTrip!!.id) })
            }
        }

        item {
            SectionHeader("Today's challenges")
        }
        item {
            DailyChallengesCard(
                challenges = challenges,
                onOpenScan = onOpenScan
            )
        }

        if (recommendation != null) {
            item { SectionHeader("Suggested for today") }
            item {
                RecommendedTrailCard(
                    recommendation = recommendation!!,
                    onOpen = { onOpenTrail(recommendation!!.trail.id) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Trails for you",
                actionLabel = "See all",
                onAction = onOpenDiscover
            )
        }
        item {
            TrailCarousel(
                recommendations = rankedTrails.take(8),
                onOpenTrail = onOpenTrail
            )
        }

        item { SectionHeader("Outdoor updates") }
        item { OutdoorUpdatesCard(weather = (weather as? WeatherUiState.Loaded)?.weather) }

        item { SectionHeader("BioCoin leaderboard") }
        item {
            LeaderboardCard(
                entries = leaderboard,
                currentUid = profile?.uid
            )
        }

        item { SafetyNotice(TrailRanking.SAFETY_DISCLAIMER) }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    name: String,
    weather: WeatherUiState,
    initials: String,
    avatarColour: Long,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "$greeting, $name 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            val subtitle = when (weather) {
                is WeatherUiState.Loaded ->
                    "${weather.locationLabel} · ${weather.weather.temperatureC.toInt()}°C"
                WeatherUiState.Loading -> "Checking conditions…"
                is WeatherUiState.Failed -> "Conditions unavailable"
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AdventurerAvatar(
            initials = initials,
            colorHex = avatarColour,
            sizeDp = 48,
            contentDescription = "Open your profile",
            onClick = onOpenProfile
        )
    }
}

/**
 * Placeholder for the customisable 3D companion.
 *
 * The real product intends a rigged chibi character here. Building that is explicitly out
 * of MVP scope (spec section 10), so this draws an animated illustrated stand-in — but it
 * takes the same inputs the eventual renderer will (`userId`, mood derived from progress,
 * level), so swapping the body of this composable for a 3D surface later touches nothing
 * around it.
 */
@Composable
private fun BiomateCharacter(
    userId: String,
    bioCoins: Int,
    challengesCompleted: Int,
    challengeTotal: Int
) {
    val level = remember(bioCoins) { levelForCoins(bioCoins) }
    val mood = when {
        challengeTotal > 0 && challengesCompleted == challengeTotal -> CharacterMood.THRILLED
        challengesCompleted > 0 -> CharacterMood.HAPPY
        else -> CharacterMood.RESTING
    }

    // A slow breathing scale. Subtle on purpose: this sits above content people read.
    val transition = rememberInfiniteTransition(label = "character")
    val breathe by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "character_breathe"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(breathe)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mood.emoji,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    "Level $level",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    mood.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "🪙 $bioCoins",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics {
                        contentDescription = "$bioCoins BioCoins"
                    }
                )
                Text(
                    "BioCoins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private enum class CharacterMood(val emoji: String, val caption: String) {
    RESTING("🙂", "Ready when you are."),
    HAPPY("😄", "Nice work today."),
    THRILLED("🤩", "Every challenge done!")
}

/**
 * Level from lifetime BioCoins.
 *
 * A flat 250 coins per level — a curve would be more engaging but also more arbitrary,
 * and nothing in the MVP depends on the shape.
 */
private fun levelForCoins(coins: Int): Int = 1 + (coins / 250)

@Composable
private fun WeatherCard(state: WeatherUiState, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        when (state) {
            WeatherUiState.Loading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Checking conditions…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is WeatherUiState.Failed -> ErrorState(
                title = "Weather unavailable",
                message = state.message,
                retryLabel = "Retry",
                onRetry = onRetry
            )

            is WeatherUiState.Loaded -> WeatherContent(state.weather, state.locationLabel)
        }
    }
}

@Composable
private fun WeatherContent(weather: Weather, locationLabel: String) {
    Column(Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${weather.temperatureC.toInt()}°",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    weather.condition.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Feels like ${weather.apparentTemperatureC.toInt()}° · $locationLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        VSpace(16)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherFact("💧", "${"%.1f".format(weather.precipitationMm)} mm", "Rain")
            weather.precipitationProbabilityPercent?.let {
                WeatherFact("☔", "$it%", "Chance")
            }
            WeatherFact("💨", "${weather.windSpeedKmh.toInt()} km/h", "Wind")
        }

        // Only rendered when the conditions warrant it — a permanent banner becomes
        // wallpaper and stops being read.
        weather.advisory?.let { advisory ->
            VSpace(16)
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚠ $advisory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun WeatherFact(emoji: String, value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, modifier = Modifier.clearAndSetSemantics {})
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyChallengesCard(
    challenges: List<DailyChallengeView>,
    onOpenScan: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp)) {
            if (challenges.isEmpty()) {
                Text(
                    "Setting up today's challenges…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            challenges.forEachIndexed { index, view ->
                if (index > 0) VSpace(20)
                ChallengeRow(view, onOpenScan)
            }
        }
    }
}

@Composable
private fun ChallengeRow(view: DailyChallengeView, onOpenScan: () -> Unit) {
    val daily = view.daily
    val challenge = view.challenge
    val needsPhoto = challenge.photoSubject != null && !daily.isComplete

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // A tick, not just a colour: completion must survive greyscale.
                    if (daily.isComplete) {
                        Text("✓ ", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    if (daily.isComplete) {
                        "Complete"
                    } else {
                        "${daily.progress} / ${daily.target} ${challenge.unit}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "+${challenge.rewardCoins}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Worth ${challenge.rewardCoins} BioCoins"
                }
            )
        }

        VSpace(8)
        BiomateProgressBar(
            progress = daily.progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentDescription = "${(daily.progressFraction * 100).toInt()} percent complete"
        )

        if (needsPhoto) {
            VSpace(8)
            TextButton(onClick = onOpenScan) { Text("Take the photo") }
        }
    }
}

@Composable
private fun UpcomingTripCard(trip: Trip, onOpen: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM · h:mm a", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "NEXT ADVENTURE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            VSpace(6)
            Text(
                trip.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                trip.trailName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            VSpace(10)
            Text(
                dateFormat.format(Date(trip.startsAt)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (trip.meetingPoint.isNotBlank()) {
                Text(
                    "📍 ${trip.meetingPoint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecommendedTrailCard(
    recommendation: TrailRecommendation,
    onOpen: () -> Unit
) {
    val trail = recommendation.trail
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box {
                TrailHeroArt(
                    seed = trail.id.hashCode(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                // Scrim so the overlaid title keeps its contrast whatever the art does.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        trail.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        trail.region,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(trail.difficulty)
                    Text(
                        "${"%.1f".format(trail.distanceKm)} km · ${trail.estimatedDurationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (recommendation.reasons.isNotEmpty()) {
                    VSpace(12)
                    recommendation.reasons.forEach { reason ->
                        Text(
                            "• $reason",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailCarousel(
    recommendations: List<TrailRecommendation>,
    onOpenTrail: (String) -> Unit
) {
    if (recommendations.isEmpty()) {
        Text(
            "No trails loaded yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Cards sized against the viewport rather than a fixed width, so they stay readable
    // from a 360dp phone up to a tablet.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = (screenWidth * 0.62f).coerceIn(220.dp, 300.dp)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(recommendations, key = { it.trail.id }) { recommendation ->
            val trail = recommendation.trail
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(cardWidth)
                    .clickable { onOpenTrail(trail.id) }
            ) {
                Column {
                    TrailHeroArt(
                        seed = trail.id.hashCode(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            trail.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        Text(
                            trail.region,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VSpace(10)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyBadge(trail.difficulty)
                            Text(
                                "${"%.1f".format(trail.distanceKm)} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Short, derived conditions notes.
 *
 * Generated from the weather already fetched rather than from a news feed — the spec is
 * explicit that a full aggregation service is out of scope (spec section 11), and inventing
 * headlines would mean presenting fiction as trail information.
 */
@Composable
private fun OutdoorUpdatesCard(weather: Weather?) {
    val updates = remember(weather) { buildUpdates(weather) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp)) {
            updates.forEachIndexed { index, update ->
                if (index > 0) VSpace(14)
                Row(verticalAlignment = Alignment.Top) {
                    Text(update.first, modifier = Modifier.clearAndSetSemantics {})
                    Text(
                        update.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

private fun buildUpdates(weather: Weather?): List<Pair<String, String>> {
    if (weather == null) {
        return listOf("📡" to "Conditions unavailable right now. Check an official forecast before heading out.")
    }
    val updates = mutableListOf<Pair<String, String>>()

    updates += when {
        weather.isWet -> "🌧️" to "Wet underfoot. Rock, boardwalk and clay all get slippery — allow extra time."
        weather.isHot -> "🥵" to "Warm day. Start early, carry more water than you think you need."
        weather.isCold -> "🥶" to "Cold. Wind chill on exposed ground will feel considerably worse than the number."
        else -> "🥾" to "Good walking conditions today."
    }

    if (weather.isWindy) {
        updates += "💨" to "Wind above 30 km/h. Ridgelines and cliff edges are harder work and less forgiving."
    }
    if (weather.precipitationMm >= 5.0) {
        updates += "🌊" to "Heavy rain can raise creek levels quickly. Do not attempt a crossing you are unsure of."
    }
    updates += "🏞️" to "Check the Parks Victoria site for closures and fire restrictions before you travel."

    return updates
}

@Composable
private fun LeaderboardCard(
    entries: List<LeaderboardEntry>,
    currentUid: String?
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp)) {
            if (entries.isEmpty()) {
                Text(
                    "No BioCoins earned yet. Finish a challenge to get on the board.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            entries.take(5).forEachIndexed { index, entry ->
                if (index > 0) VSpace(12)
                val isMe = entry.uid == currentUid
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.rank}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp)
                    )
                    AdventurerAvatar(
                        initials = entry.displayName.take(2).uppercase(),
                        colorHex = entry.avatarColorHex,
                        sizeDp = 36
                    )
                    Text(
                        // "(you)" rather than a highlight colour alone.
                        if (isMe) "${entry.displayName} (you)" else entry.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                    Text(
                        "🪙 ${entry.bioCoins}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
