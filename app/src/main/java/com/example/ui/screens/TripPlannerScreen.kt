package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SharedGearItem
import com.example.data.model.TripMeal
import com.example.data.model.TripParticipant
import com.example.data.model.TripPlan
import com.example.ui.BiomateViewModel
import com.example.ui.components.AdventurerAvatar
import com.example.ui.components.KataTjutaGraphic
import com.example.ui.components.StatusBadge
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SandBackground
import com.example.ui.theme.SurfaceVariantSand
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

data class EssentialGearCheckItem(
    val id: Int,
    val title: String,
    val description: String
)

@Composable
fun TripPlannerScreen(
    viewModel: BiomateViewModel,
    modifier: Modifier = Modifier
) {
    val activeTrip by viewModel.activeTrip.collectAsState()
    val participants by viewModel.tripParticipants.collectAsState()
    val gearList by viewModel.tripGear.collectAsState()
    val mealList by viewModel.tripMeals.collectAsState()
    val allTrails by viewModel.allTrails.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddGearDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }

    // Essentials verification checklist state
    val verifiedItemIds = remember { mutableStateListOf<Int>(1) }

    val defaultEssentials = remember {
        listOf(
            EssentialGearCheckItem(1, "High-ankle hiking boots", "Required for loose red scree & extreme heat"),
            EssentialGearCheckItem(2, "3L Water Hydration Bladder", "Minimum 3L required for desert climate"),
            EssentialGearCheckItem(3, "Broad-brim sun hat & UV Buff", "UV index forecast 11+ Extreme"),
            EssentialGearCheckItem(4, "Snake bite bandage (10cm)", "Pressure immobilization bandage ready"),
            EssentialGearCheckItem(5, "Electrolyte replacement salts", "Prevent hyponatremia & heat cramps"),
            EssentialGearCheckItem(6, "Offline topo map & power bank", "Offline GPS route downloaded to device")
        )
    }

    val tabs = listOf("Essentials", "Carpool & Team", "Shared Gear", "Food Prep")

    if (activeTrip == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active trip found. Select a trail from Discover to plan!", color = TextMuted)
        }
        return
    }

    val trip = activeTrip!!
    val currentTrail = allTrails.firstOrNull { it.id == trip.trailId } ?: allTrails.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SandBackground)
    ) {
        // Sub-tabs Row with warm sand aesthetic
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SandBackground,
            contentColor = TerracottaPrimary,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = TerracottaPrimary,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) TerracottaPrimary else TextMuted
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> EssentialsGearCheckTab(
                    trip = trip,
                    essentials = defaultEssentials,
                    verifiedIds = verifiedItemIds,
                    onToggleVerify = { id ->
                        if (verifiedItemIds.contains(id)) {
                            verifiedItemIds.remove(id)
                        } else {
                            verifiedItemIds.add(id)
                        }
                    },
                    onStartJourney = {
                        currentTrail?.let { viewModel.startOnTrailNavigation(it) }
                    }
                )
                1 -> ParticipantsAndCarpoolTab(
                    participants = participants,
                    trip = trip
                )
                2 -> SharedGearTab(
                    gearList = gearList,
                    onTogglePacked = { viewModel.toggleGearPacked(it) },
                    onAddGearClick = { showAddGearDialog = true }
                )
                3 -> FoodPrepTab(
                    mealList = mealList,
                    onAddMealClick = { showAddMealDialog = true }
                )
            }
        }
    }

    if (showAddGearDialog) {
        AddGearDialog(
            participants = participants.map { it.name },
            onDismiss = { showAddGearDialog = false },
            onAdd = { name, cat, assigned, essential ->
                viewModel.addSharedGearItem(name, cat, assigned, essential)
                showAddGearDialog = false
            }
        )
    }

    if (showAddMealDialog) {
        AddMealDialog(
            participants = participants.map { it.name },
            onDismiss = { showAddMealDialog = false },
            onAdd = { name, type, assigned, notes ->
                viewModel.addTripMeal(name, type, assigned, notes)
                showAddMealDialog = false
            }
        )
    }
}

@Composable
fun EssentialsGearCheckTab(
    trip: TripPlan,
    essentials: List<EssentialGearCheckItem>,
    verifiedIds: List<Int>,
    onToggleVerify: (Int) -> Unit,
    onStartJourney: () -> Unit
) {
    val totalCount = essentials.size
    val verifiedCount = verifiedIds.size
    val progressFraction = (verifiedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progressFraction * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Trail Card (Kata Tjuta) with Thumbnail
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_trail_gear_card")
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Square Kata Tjuta Thumbnail Graphic
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        KataTjutaGraphic(modifier = Modifier.fillMaxSize())
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trip.trailName.ifBlank { "Kata Tjuta" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tomorrow, 8:00 AM • 7.4 km",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFICATION IN PROGRESS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TerracottaPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Readiness Progress Card ("0 / 6 verified", "5%")
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$verifiedCount / $totalCount verified",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal
                            )
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TerracottaPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar matching Screenshot 1
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = TerracottaPrimary,
                        trackColor = SurfaceVariantSand
                    )
                }
            }
        }

        // Section Title: "ESSENTIALS"
        item {
            Text(
                text = "ESSENTIALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        // Numbered Checklist Cards (1, 2, 3, 4, 5, 6)
        items(essentials) { item ->
            val isVerified = verifiedIds.contains(item.id)
            val isFirstItem = item.id == 1

            // Numbered card styling directly from Screenshot 1
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFirstItem && isVerified) TerracottaPrimary else TerracottaContainer
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isFirstItem && isVerified) 3.dp else 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleVerify(item.id) }
                    .testTag("gear_essential_item_${item.id}")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Number Circle Badge (1, 2, 3...)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFirstItem && isVerified) Color(0xFF9E4928) else Color(0xFFD6A78F)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirstItem && isVerified) Color.White else Color(0xFF4A342B)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isFirstItem && isVerified) Color.White else TextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = if (isFirstItem && isVerified) Color.White.copy(alpha = 0.85f) else TextMuted
                            )
                        }
                    }

                    // Radio / Check Verification Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVerified) {
                                    if (isFirstItem) Color.White else TerracottaPrimary
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = if (isFirstItem) TerracottaPrimary else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Unverified",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom CTA: "START JOURNEY"
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStartJourney,
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(32.dp), spotColor = TerracottaPrimary)
                    .testTag("start_journey_button")
            ) {
                Text(
                    text = "START JOURNEY",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ParticipantsAndCarpoolTab(
    participants: List<TripParticipant>,
    trip: TripPlan
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Carpool & Vehicle Logistics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextCharcoal)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Meeting at ${trip.meetingPoint} on ${trip.departureDate} at ${trip.departureTime}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        items(participants) { p ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdventurerAvatar(
                            initials = p.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            sizeDp = 42
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text(text = "Role: ${p.role} • ${p.carpoolRole}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    if (p.carpoolRole.contains("Driver", ignoreCase = true)) {
                        StatusBadge(text = "🚗 DRIVER", color = TerracottaPrimary)
                    } else if (p.isReady) {
                        StatusBadge(text = "READY ✓", color = TerracottaDark)
                    }
                }
            }
        }
    }
}

@Composable
fun SharedGearTab(
    gearList: List<SharedGearItem>,
    onTogglePacked: (SharedGearItem) -> Unit,
    onAddGearClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shared Expedition Gear (${gearList.size} items)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextCharcoal)
                )
                Button(
                    onClick = onAddGearClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(gearList) { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isPacked) TerracottaContainer.copy(alpha = 0.5f) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Assigned to ${item.assignedTo} • ${item.category}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = { onTogglePacked(item) }) {
                        Icon(
                            imageVector = if (item.isPacked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Packed status",
                            tint = if (item.isPacked) TerracottaPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoodPrepTab(
    mealList: List<TripMeal>,
    onAddMealClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expedition Meals & Rations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextCharcoal)
                )
                Button(
                    onClick = onAddMealClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Meal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(mealList) { meal ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = meal.mealName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                        StatusBadge(text = meal.mealType, color = TerracottaContainer, textColor = TerracottaDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Cook/Chef: ${meal.assignedTo} • Notes: ${meal.dietaryInfo}", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun AddGearDialog(
    participants: List<String>,
    onDismiss: () -> Unit,
    onAdd: (name: String, cat: String, assigned: String, essential: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Safety") }
    var assignedTo by remember { mutableStateOf(participants.firstOrNull() ?: "Me") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Shared Expedition Gear", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Shelter, Cooking, Safety...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Assigned Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onAdd(name, category, assignedTo, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun AddMealDialog(
    participants: List<String>,
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, assigned: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Dinner") }
    var assignedTo by remember { mutableStateOf(participants.firstOrNull() ?: "Me") }
    var notes by remember { mutableStateOf("High-protein & dehydrated") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Expedition Meal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Meal Type (Breakfast, Lunch, Dinner, Snack)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Prepared By") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Dietary Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onAdd(name, type, assignedTo, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                    ) {
                        Text("Add Meal")
                    }
                }
            }
        }
    }
}
