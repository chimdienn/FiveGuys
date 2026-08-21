package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.HikeBuddy
import com.example.data.model.TripPlan
import com.example.ui.BiomateViewModel
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.ScenicMountainGraphic
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSunrise
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SandBackground
import com.example.ui.theme.SurfaceVariantSand
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

enum class HikeMatchViewMode {
    CARDS,
    DIRECTORY,
    MY_MATCHES
}

@Composable
fun HikeMatchScreen(
    viewModel: BiomateViewModel,
    modifier: Modifier = Modifier
) {
    val buddies by viewModel.allBuddies.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()

    // Filter states from ViewModel
    val fitnessFilter by viewModel.hikeMatchFitnessFilter.collectAsState()
    val experienceFilter by viewModel.hikeMatchExperienceFilter.collectAsState()
    val socialVibeFilter by viewModel.hikeMatchSocialVibeFilter.collectAsState()
    val paceFilter by viewModel.hikeMatchPaceFilter.collectAsState()
    val searchQuery by viewModel.hikeMatchSearchQuery.collectAsState()

    // Local UI state
    var viewMode by remember { mutableStateOf(HikeMatchViewMode.CARDS) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var previousCardIndices by remember { mutableStateOf(listOf<Int>()) }

    // Dialog states
    var showFilterSheet by remember { mutableStateOf(false) }
    var matchedBuddyModal by remember { mutableStateOf<HikeBuddy?>(null) }
    var selectedBuddyForProfile by remember { mutableStateOf<HikeBuddy?>(null) }
    var selectedBuddyForInvite by remember { mutableStateOf<HikeBuddy?>(null) }

    // Filtered list calculation
    val filteredBuddies by remember(buddies, fitnessFilter, experienceFilter, socialVibeFilter, paceFilter, searchQuery) {
        derivedStateOf {
            buddies.filter { buddy ->
                val matchesFitness = fitnessFilter == "All" || buddy.fitnessLevel.contains(fitnessFilter, ignoreCase = true)
                val matchesExperience = when (experienceFilter) {
                    "All" -> true
                    "Novice (<2y)" -> buddy.experienceYears < 2
                    "Experienced (2-4y)" -> buddy.experienceYears in 2..4
                    "Veteran (5y+)" -> buddy.experienceYears >= 5
                    else -> true
                }
                val matchesVibe = socialVibeFilter == "All" || buddy.activityVibe.contains(socialVibeFilter, ignoreCase = true)
                val matchesPace = paceFilter == "All" || buddy.preferredPace.contains(paceFilter, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        buddy.name.contains(searchQuery, ignoreCase = true) ||
                        buddy.location.contains(searchQuery, ignoreCase = true) ||
                        buddy.bio.contains(searchQuery, ignoreCase = true) ||
                        buddy.verifiedSkills.any { it.contains(searchQuery, ignoreCase = true) } ||
                        buddy.preferredTrails.any { it.contains(searchQuery, ignoreCase = true) }

                matchesFitness && matchesExperience && matchesVibe && matchesPace && matchesSearch
            }
        }
    }

    val activeMatches = remember(buddies) {
        buddies.filter { it.matchStatus == "MATCHED" || it.matchStatus == "CHATTING" || it.matchStatus == "INVITED" }
    }

    val activeFiltersCount = remember(fitnessFilter, experienceFilter, socialVibeFilter, paceFilter) {
        listOf(fitnessFilter, experienceFilter, socialVibeFilter, paceFilter).count { it != "All" }
    }

    val currentCardBuddy = if (filteredBuddies.isNotEmpty()) {
        filteredBuddies.getOrNull(currentCardIndex % filteredBuddies.size)
    } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SandBackground)
    ) {
        // Navigation Tabs (Cards vs Directory vs My Matches)
        TabRow(
            selectedTabIndex = viewMode.ordinal,
            containerColor = Color.White,
            contentColor = TerracottaPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[viewMode.ordinal]),
                    color = TerracottaPrimary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = viewMode == HikeMatchViewMode.CARDS,
                onClick = { viewMode = HikeMatchViewMode.CARDS },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Card Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.testTag("tab_cards_mode")
            )
            Tab(
                selected = viewMode == HikeMatchViewMode.DIRECTORY,
                onClick = { viewMode = HikeMatchViewMode.DIRECTORY },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ranked (${filteredBuddies.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.testTag("tab_directory_mode")
            )
            Tab(
                selected = viewMode == HikeMatchViewMode.MY_MATCHES,
                onClick = { viewMode = HikeMatchViewMode.MY_MATCHES },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Matches (${activeMatches.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                modifier = Modifier.testTag("tab_my_matches_mode")
            )
        }

        // Search & Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setMatchSearchQuery(it) },
                placeholder = { Text("Search by name, skill, trail...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setMatchSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerracottaPrimary,
                    unfocusedBorderColor = OutlineSubtle,
                    unfocusedContainerColor = SurfaceVariantSand,
                    focusedContainerColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("hike_match_search_input")
            )

            // Filter button with active count badge
            Button(
                onClick = { showFilterSheet = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFiltersCount > 0) TerracottaPrimary else TerracottaContainer,
                    contentColor = if (activeFiltersCount > 0) Color.White else TerracottaDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("filter_preferences_button")
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (activeFiltersCount > 0) "Filters ($activeFiltersCount)" else "Filters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Filter Chips Row (Fitness, Pace, Vibe)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            val chips = listOf(
                "All" to (fitnessFilter == "All" && socialVibeFilter == "All" && paceFilter == "All"),
                "Beginner" to (fitnessFilter == "Beginner"),
                "Moderate" to (fitnessFilter == "Moderate"),
                "Advanced" to (fitnessFilter == "Advanced"),
                "Endurance" to (fitnessFilter == "Endurance"),
                "Photography" to (socialVibeFilter == "Photography"),
                "Fast Pace" to (paceFilter.contains("Fast")),
                "Campfire" to (socialVibeFilter == "Campfire")
            )

            items(chips) { (label, isSelected) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) TerracottaPrimary else SurfaceVariantSand)
                        .border(1.dp, if (isSelected) TerracottaPrimary else OutlineSubtle, RoundedCornerShape(16.dp))
                        .clickable {
                            when (label) {
                                "All" -> viewModel.resetMatchFilters()
                                "Beginner", "Moderate", "Advanced", "Endurance" -> {
                                    viewModel.setFitnessFilter(if (fitnessFilter == label) "All" else label)
                                }
                                "Photography" -> {
                                    viewModel.setSocialVibeFilter(if (socialVibeFilter == "Photography") "All" else "Photography")
                                }
                                "Fast Pace" -> {
                                    viewModel.setPaceFilter(if (paceFilter.contains("Fast")) "All" else "Fast")
                                }
                                "Campfire" -> {
                                    viewModel.setSocialVibeFilter(if (socialVibeFilter == "Campfire") "All" else "Campfire")
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("quick_filter_${label.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextDark
                    )
                }
            }
        }

        // Main Content Area depending on View Mode
        when (viewMode) {
            HikeMatchViewMode.CARDS -> {
                CardDiscoveryView(
                    buddy = currentCardBuddy,
                    totalCount = filteredBuddies.size,
                    currentIndex = currentCardIndex,
                    canUndo = previousCardIndices.isNotEmpty(),
                    onSkip = {
                        previousCardIndices = previousCardIndices + currentCardIndex
                        currentCardIndex++
                    },
                    onUndo = {
                        if (previousCardIndices.isNotEmpty()) {
                            currentCardIndex = previousCardIndices.last()
                            previousCardIndices = previousCardIndices.dropLast(1)
                        }
                    },
                    onSuperEndorse = { buddy ->
                        viewModel.superEndorseBuddy(buddy)
                        matchedBuddyModal = buddy
                        previousCardIndices = previousCardIndices + currentCardIndex
                        currentCardIndex++
                    },
                    onMatch = { buddy ->
                        viewModel.connectWithBuddy(buddy)
                        matchedBuddyModal = buddy
                        previousCardIndices = previousCardIndices + currentCardIndex
                        currentCardIndex++
                    },
                    onViewProfile = { buddy ->
                        selectedBuddyForProfile = buddy
                    },
                    onProposeHike = { buddy ->
                        selectedBuddyForInvite = buddy
                    },
                    onResetDeck = {
                        currentCardIndex = 0
                        previousCardIndices = emptyList()
                    },
                    onOpenFilters = { showFilterSheet = true }
                )
            }

            HikeMatchViewMode.DIRECTORY -> {
                CompatibilityDirectoryView(
                    buddies = filteredBuddies,
                    onViewProfile = { selectedBuddyForProfile = it },
                    onConnect = { buddy ->
                        viewModel.connectWithBuddy(buddy)
                        matchedBuddyModal = buddy
                    },
                    onProposeHike = { selectedBuddyForInvite = it },
                    onOpenFilters = { showFilterSheet = true }
                )
            }

            HikeMatchViewMode.MY_MATCHES -> {
                MyMatchesView(
                    matches = activeMatches,
                    onViewProfile = { selectedBuddyForProfile = it },
                    onSendMessage = { buddy ->
                        viewModel.startChatWithBuddy(buddy)
                    },
                    onProposeHike = { selectedBuddyForInvite = it },
                    onDiscoverMore = { viewMode = HikeMatchViewMode.CARDS }
                )
            }
        }
    }

    // Modal: Filter & Preferences Customization
    if (showFilterSheet) {
        FilterPreferencesDialog(
            currentFitness = fitnessFilter,
            currentExperience = experienceFilter,
            currentVibe = socialVibeFilter,
            currentPace = paceFilter,
            onApply = { fit, exp, vibe, pace ->
                viewModel.setFitnessFilter(fit)
                viewModel.setExperienceFilter(exp)
                viewModel.setSocialVibeFilter(vibe)
                viewModel.setPaceFilter(pace)
                showFilterSheet = false
            },
            onReset = {
                viewModel.resetMatchFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Modal: "It's a Match!" Celebration
    matchedBuddyModal?.let { buddy ->
        MatchCelebrationDialog(
            buddy = buddy,
            onDismiss = { matchedBuddyModal = null },
            onMessage = {
                matchedBuddyModal = null
                viewModel.startChatWithBuddy(buddy)
            },
            onInviteToTrip = {
                val toInvite = matchedBuddyModal
                matchedBuddyModal = null
                selectedBuddyForInvite = toInvite
            }
        )
    }

    // Modal: Detailed Partner Profile Inspector
    selectedBuddyForProfile?.let { buddy ->
        HikeBuddyProfileDialog(
            buddy = buddy,
            onDismiss = { selectedBuddyForProfile = null },
            onConnect = {
                viewModel.connectWithBuddy(buddy)
                selectedBuddyForProfile = null
                matchedBuddyModal = buddy
            },
            onProposeHike = {
                selectedBuddyForProfile = null
                selectedBuddyForInvite = buddy
            },
            onMessage = {
                selectedBuddyForProfile = null
                viewModel.startChatWithBuddy(buddy)
            }
        )
    }

    // Modal: Propose a Hike / Invite Partner to Trip
    selectedBuddyForInvite?.let { buddy ->
        ProposeHikeDialog(
            buddy = buddy,
            availableTrips = allTrips,
            onDismiss = { selectedBuddyForInvite = null },
            onSendInvite = { tripId, note ->
                viewModel.sendBuddyInvite(buddy, tripId, note)
                selectedBuddyForInvite = null
            }
        )
    }
}

// -------------------------------------------------------------
// 1. CARD DISCOVERY VIEW (Tinder-style Card Deck)
// -------------------------------------------------------------
@Composable
fun CardDiscoveryView(
    buddy: HikeBuddy?,
    totalCount: Int,
    currentIndex: Int,
    canUndo: Boolean,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onSuperEndorse: (HikeBuddy) -> Unit,
    onMatch: (HikeBuddy) -> Unit,
    onViewProfile: (HikeBuddy) -> Unit,
    onProposeHike: (HikeBuddy) -> Unit,
    onResetDeck: () -> Unit,
    onOpenFilters: () -> Unit
) {
    if (buddy == null || totalCount == 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TerracottaContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "You've Explored All Matches!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Try adjusting your fitness, pace, or social preferences to discover more outdoor partners nearby.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = onResetDeck,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle & Review Again", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenFilters,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust Compatibility Filters", color = TextDark)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Deck Progress Counter & Undo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPLORER ${(currentIndex % totalCount) + 1} OF $totalCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.1.sp
                )

                if (canUndo) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUndo() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TerracottaPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary)
                    }
                }
            }
        }

        // Hero Card
        item {
            HeroHikerDiscoveryCard(
                buddy = buddy,
                onViewProfile = { onViewProfile(buddy) },
                onProposeHike = { onProposeHike(buddy) }
            )
        }

        // 3-Action Match Controls (Skip, Super Endorse, Match)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pass / Skip Button (White circular with Red cross)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0x22000000))
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, DangerRed.copy(alpha = 0.6f), CircleShape)
                        .clickable { onSkip() }
                        .testTag("match_skip_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pass / Next",
                        tint = DangerRed,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Super Endorse / Star Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(4.dp, CircleShape, spotColor = AmberSunrise)
                        .clip(CircleShape)
                        .background(AmberContainer)
                        .border(1.5.dp, AmberSunrise, CircleShape)
                        .clickable { onSuperEndorse(buddy) }
                        .testTag("match_super_endorse_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Super Endorse",
                        tint = AmberSunrise,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Like / Match Button (Terracotta circular with White checkmark)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(6.dp, CircleShape, spotColor = TerracottaPrimary)
                        .clip(CircleShape)
                        .background(TerracottaPrimary)
                        .clickable { onMatch(buddy) }
                        .testTag("match_accept_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Match & Connect",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. HERO HIKER DISCOVERY CARD
// -------------------------------------------------------------
@Composable
fun HeroHikerDiscoveryCard(
    buddy: HikeBuddy,
    onViewProfile: () -> Unit,
    onProposeHike: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_hiker_card")
    ) {
        Column {
            // Scenic Header Graphic with Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                ScenicMountainGraphic(modifier = Modifier.fillMaxSize())

                // Match Percentage Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(1.dp, TerracottaPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberSunrise,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${buddy.matchScore}% MATCH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TerracottaDark
                        )
                    }
                }

                // Avatar, Name & Distance Overlay
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdventurerAvatar(
                        initials = buddy.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        sizeDp = 52,
                        borderColor = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.95f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${buddy.name}, ${buddy.age}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "📍 ${buddy.location}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Card Body
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bio quote
                Text(
                    text = "\"${buddy.bio}\"",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp,
                    color = TextCharcoal
                )

                // Compatibility Sync Grid (Fitness, Pace, Vibe)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompatibilitySyncPill(
                        icon = Icons.Default.FitnessCenter,
                        label = "Fitness",
                        value = buddy.fitnessLevel,
                        modifier = Modifier.weight(1f)
                    )
                    CompatibilitySyncPill(
                        icon = Icons.Default.Speed,
                        label = "Pace",
                        value = buddy.preferredPace.substringBefore(" ("),
                        modifier = Modifier.weight(1f)
                    )
                    CompatibilitySyncPill(
                        icon = Icons.Default.VolunteerActivism,
                        label = "Social Vibe",
                        value = buddy.activityVibe.substringBefore(" &"),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Why You Match (Match Reasons)
                if (buddy.matchReasons.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceVariantSand)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WHY YOU'RE COMPATIBLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                        }

                        buddy.matchReasons.take(3).forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", fontSize = 12.sp, color = TerracottaPrimary, fontWeight = FontWeight.Bold)
                                Text(reason, fontSize = 12.sp, color = TextDark, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // Verified Skills & Badges
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VERIFIED OUTDOOR SKILLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(buddy.verifiedSkills) { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ForestGreenContainer)
                                    .border(1.dp, ForestGreenPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(skill, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnForestGreenContainer)
                                }
                            }
                        }
                    }
                }

                // Preferred Trails in Common
                if (buddy.preferredTrails.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("FAVORITE TRAILS & EXPEDITIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(buddy.preferredTrails) { trail ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TerracottaContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Map, contentDescription = null, tint = TerracottaDark, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(trail, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TerracottaDark)
                                    }
                                }
                            }
                        }
                    }
                }

                // Reliability Rating & Completed Hikes Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9F7F2))
                        .border(1.dp, OutlineSubtle, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberSunrise, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${buddy.completedHikesCount} Hikes Completed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForestGreenContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("${buddy.attendanceRate}% Reliability", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                    }
                }

                // Action Buttons inside Card (Deep Dive + Propose Hike)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewProfile,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Profile", fontSize = 12.sp, color = TextDark)
                    }

                    Button(
                        onClick = onProposeHike,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite to Hike", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CompatibilitySyncPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceVariantSand)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------
// 3. COMPATIBILITY DIRECTORY VIEW (Ranked List)
// -------------------------------------------------------------
@Composable
fun CompatibilityDirectoryView(
    buddies: List<HikeBuddy>,
    onViewProfile: (HikeBuddy) -> Unit,
    onConnect: (HikeBuddy) -> Unit,
    onProposeHike: (HikeBuddy) -> Unit,
    onOpenFilters: () -> Unit
) {
    if (buddies.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Text("No matching hiking partners found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text("Try loosening your fitness, pace, or vibe filters to see more adventurers.", fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center)
                Button(
                    onClick = onOpenFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Modify Filters")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPATIBLE PARTNERS (${buddies.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Ranked by AI Compatibility",
                    fontSize = 11.sp,
                    color = TerracottaDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(buddies) { buddy ->
            HikeBuddyDirectoryCard(
                buddy = buddy,
                onViewProfile = { onViewProfile(buddy) },
                onConnect = { onConnect(buddy) },
                onProposeHike = { onProposeHike(buddy) }
            )
        }
    }
}

@Composable
fun HikeBuddyDirectoryCard(
    buddy: HikeBuddy,
    onViewProfile: () -> Unit,
    onConnect: () -> Unit,
    onProposeHike: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() }
            .testTag("directory_card_${buddy.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Avatar, Name, Match Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdventurerAvatar(
                        initials = buddy.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        sizeDp = 46
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${buddy.name}, ${buddy.age}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "📍 ${buddy.location}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Match Score Progress Chip
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AmberSunrise, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${buddy.matchScore}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = TerracottaDark
                        )
                    }
                    Text(
                        text = "Match",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Compatibility Progress Bar
            LinearProgressIndicator(
                progress = { buddy.matchScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (buddy.matchScore >= 90) ForestGreenPrimary else if (buddy.matchScore >= 80) TerracottaPrimary else AmberSunrise,
                trackColor = SurfaceVariantSand
            )

            // Bio preview
            Text(
                text = buddy.bio,
                fontSize = 12.sp,
                color = TextCharcoal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            // Attribute Chips (Fitness, Pace, Vibe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerracottaContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(buddy.fitnessLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TerracottaDark)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantSand)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(buddy.preferredPace.substringBefore(" ("), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextDark)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForestGreenContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(buddy.activityVibe, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnForestGreenContainer)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isMatched = buddy.matchStatus == "MATCHED" || buddy.matchStatus == "CHATTING"
                val isInvited = buddy.matchStatus == "INVITED"

                Text(
                    text = "${buddy.completedHikesCount} trips • ${buddy.attendanceRate}% reliability",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onProposeHike,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Invite", fontSize = 11.sp, color = TextDark)
                    }

                    Button(
                        onClick = { if (!isMatched) onConnect() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMatched) ForestGreenPrimary else if (isInvited) AmberSunrise else TerracottaPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isMatched) "Matched ✓" else if (isInvited) "Invited ⌛" else "Connect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. MY MATCHES & CONNECTIONS VIEW
// -------------------------------------------------------------
@Composable
fun MyMatchesView(
    matches: List<HikeBuddy>,
    onViewProfile: (HikeBuddy) -> Unit,
    onSendMessage: (HikeBuddy) -> Unit,
    onProposeHike: (HikeBuddy) -> Unit,
    onDiscoverMore: () -> Unit
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(36.dp))
                }

                Text("No Active Matches Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text(
                    "Start swiping on the Card Deck or browsing the Ranked Directory to connect with like-minded hiking partners.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onDiscoverMore,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Discover Hiking Partners")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "YOUR TRAIL COMPANION NETWORK (${matches.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.1.sp
            )
        }

        items(matches) { buddy ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdventurerAvatar(
                                initials = buddy.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                sizeDp = 48
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${buddy.name}, ${buddy.age}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(14.dp))
                                }
                                Text("📍 ${buddy.location}", fontSize = 11.sp, color = TextMuted)
                                Text("Vibe: ${buddy.activityVibe} • ${buddy.fitnessLevel}", fontSize = 11.sp, color = TerracottaDark, fontWeight = FontWeight.Medium)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (buddy.matchStatus == "CHATTING" || buddy.matchStatus == "MATCHED") ForestGreenContainer
                                    else AmberContainer
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (buddy.matchStatus == "CHATTING") "CHATTING" else if (buddy.matchStatus == "MATCHED") "MUTUAL MATCH" else "INVITE PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (buddy.matchStatus == "CHATTING" || buddy.matchStatus == "MATCHED") OnForestGreenContainer else AmberSunrise
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onViewProfile(buddy) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Profile", fontSize = 12.sp, color = TextDark)
                        }

                        Button(
                            onClick = { onSendMessage(buddy) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onProposeHike(buddy) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. FILTER & PREFERENCES CUSTOMIZER DIALOG
// -------------------------------------------------------------
@Composable
fun FilterPreferencesDialog(
    currentFitness: String,
    currentExperience: String,
    currentVibe: String,
    currentPace: String,
    onApply: (fitness: String, experience: String, vibe: String, pace: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempFitness by remember { mutableStateOf(currentFitness) }
    var tempExperience by remember { mutableStateOf(currentExperience) }
    var tempVibe by remember { mutableStateOf(currentVibe) }
    var tempPace by remember { mutableStateOf(currentPace) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Match Preferences",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Refine compatibility criteria to discover outdoor partners who match your trail style and pace.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                // 1. Fitness Level Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FITNESS LEVEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    val fitnessOptions = listOf("All", "Beginner", "Moderate", "Advanced", "Endurance")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(fitnessOptions) { opt ->
                            val isSel = tempFitness == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TerracottaPrimary else SurfaceVariantSand)
                                    .clickable { tempFitness = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else TextDark)
                            }
                        }
                    }
                }

                // 2. Experience Level Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("EXPERIENCE LEVEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    val expOptions = listOf("All", "Novice (<2y)", "Experienced (2-4y)", "Veteran (5y+)")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(expOptions) { opt ->
                            val isSel = tempExperience == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TerracottaPrimary else SurfaceVariantSand)
                                    .clickable { tempExperience = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else TextDark)
                            }
                        }
                    }
                }

                // 3. Social Style & Vibe
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SOCIAL STYLE & VIBE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    val vibeOptions = listOf("All", "Photography", "Social", "Campfire", "Fast & Training", "Wildflowers")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(vibeOptions) { opt ->
                            val isSel = tempVibe == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TerracottaPrimary else SurfaceVariantSand)
                                    .clickable { tempVibe = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else TextDark)
                            }
                        }
                    }
                }

                // 4. Pace Preference
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PREFERRED PACE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    val paceOptions = listOf("All", "Leisurely (3 km/h)", "Moderate (4.5 km/h)", "Fast-paced (6 km/h)")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(paceOptions) { opt ->
                            val isSel = tempPace == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TerracottaPrimary else SurfaceVariantSand)
                                    .clickable { tempPace = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else TextDark)
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset All", color = TextDark)
                    }

                    Button(
                        onClick = { onApply(tempFitness, tempExperience, tempVibe, tempPace) },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. MATCH CELEBRATION DIALOG ("It's a Match!")
// -------------------------------------------------------------
@Composable
fun MatchCelebrationDialog(
    buddy: HikeBuddy,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onInviteToTrip: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "It's an Adventure Match!",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 22.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You and ${buddy.name} share ${buddy.matchScore}% outdoor compatibility based on fitness, pace, and shared trail ambitions.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Shared tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantSand)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("⚡ Pace: ${buddy.preferredPace.substringBefore(" (")}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForestGreenContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🌿 ${buddy.activityVibe}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnForestGreenContainer)
                    }
                }

                Button(
                    onClick = onMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Say Hello in Messages", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onInviteToTrip,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Propose a Weekend Hike", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep Exploring", color = TextDark)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. DETAILED PARTNER PROFILE INSPECTOR DIALOG
// -------------------------------------------------------------
@Composable
fun HikeBuddyProfileDialog(
    buddy: HikeBuddy,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onProposeHike: () -> Unit,
    onMessage: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explorer Dossier", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Profile Header Card
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdventurerAvatar(
                        initials = buddy.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        sizeDp = 56
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${buddy.name}, ${buddy.age}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text("📍 ${buddy.location}", fontSize = 12.sp, color = TextMuted)
                        Text("${buddy.experienceYears} years experience • ${buddy.fitnessLevel}", fontSize = 12.sp, color = TerracottaDark, fontWeight = FontWeight.Medium)
                    }
                }

                // Bio
                Text(
                    text = "\"${buddy.bio}\"",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextCharcoal,
                    lineHeight = 18.sp
                )

                // Compatibility Stats Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantSand)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MATCH SCORE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("${buddy.matchScore}%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TerracottaDark)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMPLETED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("${buddy.completedHikesCount} Trips", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RELIABILITY", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("${buddy.attendanceRate}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnForestGreenContainer)
                    }
                }

                // Verified Badges & Skills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VERIFIED SKILLS & CERTIFICATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    buddy.verifiedSkills.forEach { skill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ForestGreenContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(skill, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = OnForestGreenContainer)
                        }
                    }
                }

                // Preferred Trails
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GO-TO DESTINATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    buddy.preferredTrails.forEach { trail ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerracottaContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = TerracottaDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(trail, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TerracottaDark)
                        }
                    }
                }

                // Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMatched = buddy.matchStatus == "MATCHED" || buddy.matchStatus == "CHATTING"

                    Button(
                        onClick = { if (isMatched) onMessage() else onConnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isMatched) Icons.Default.Message else Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isMatched) "Chat with ${buddy.name}" else "Connect as Hiking Buddy", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onProposeHike,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Propose a Joint Expedition", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. PROPOSE A HIKE / INVITE DIALOG
// -------------------------------------------------------------
@Composable
fun ProposeHikeDialog(
    buddy: HikeBuddy,
    availableTrips: List<TripPlan>,
    onDismiss: () -> Unit,
    onSendInvite: (tripId: String?, note: String) -> Unit
) {
    var selectedTripId by remember { mutableStateOf(availableTrips.firstOrNull()?.id) }
    var customNote by remember { mutableStateOf("Hey ${buddy.name}! Saw we share a love for scenic trails and similar pace. Would you like to join our upcoming hike?") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invite to Expedition", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Invite ${buddy.name} to join one of your upcoming collaborative trips or propose a weekend trail.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                // Select Existing Trip
                if (availableTrips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SELECT TRIP PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                        availableTrips.forEach { trip ->
                            val isSel = selectedTripId == trip.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) TerracottaContainer else SurfaceVariantSand)
                                    .border(1.dp, if (isSel) TerracottaPrimary else OutlineSubtle, RoundedCornerShape(12.dp))
                                    .clickable { selectedTripId = trip.id }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trip.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    Text("📅 ${trip.departureDate} • ${trip.trailName}", fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Custom Note
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PERSONAL INVITATION NOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = OutlineSubtle
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextDark)
                    }

                    Button(
                        onClick = { onSendInvite(selectedTripId, customNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Invite", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
