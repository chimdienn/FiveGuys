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
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Trail
import com.example.data.model.TrailMoment
import com.example.ui.BiomateScreen
import com.example.ui.BiomateViewModel
import com.example.ui.components.ElevationProfileView
import com.example.ui.components.StatusBadge
import com.example.ui.components.TopographicMapSimulation
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSunrise
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextDark

@Composable
fun OnTrailScreen(
    viewModel: BiomateViewModel,
    modifier: Modifier = Modifier
) {
    val allTrails by viewModel.allTrails.collectAsState()
    val selectedTrailId by viewModel.selectedTrailId.collectAsState()
    val allMoments by viewModel.allMoments.collectAsState()

    val isOnTrailActive by viewModel.isOnTrailActive.collectAsState()
    val progressPercent by viewModel.trailProgressPercent.collectAsState()
    val currentElevation by viewModel.currentElevationM.collectAsState()
    val speedKmh by viewModel.currentSpeedKmh.collectAsState()
    val elapsedSeconds by viewModel.elapsedTimeSeconds.collectAsState()
    val isSosActive by viewModel.isSosActive.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    val currentTrail: Trail = allTrails.firstOrNull { it.id == selectedTrailId }
        ?: allTrails.firstOrNull()
        ?: return

    val currentDistanceCovered = (currentTrail.distanceKm * (progressPercent / 100.0)).let {
        String.format("%.1f", it)
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Trail Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnTrailActive) Color(0xFF43A047) else AmberSunrise)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnTrailActive) "LIVE ONTRAIL HUD" else "ONTRAIL PAUSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOnTrailActive) Color(0xFF2E7D32) else AmberSunrise
                            )
                        }

                        // Navigation pause/resume
                        IconButton(
                            onClick = { viewModel.pauseOrResumeOnTrail() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestGreenContainer)
                                .testTag("pause_resume_ontrail_button")
                        ) {
                            Icon(
                                imageVector = if (isOnTrailActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause or Resume",
                                tint = ForestGreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentTrail.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ForestGreenPrimary,
                        trackColor = ForestGreenContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$currentDistanceCovered km of ${currentTrail.distanceKm} km completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "$progressPercent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TerracottaPrimary
                        )
                    }
                }
            }
        }

        // Live Topographic Map Simulation
        item {
            TopographicMapSimulation(
                trailPoints = currentTrail.routeWaypoints,
                progressPercent = progressPercent,
                isNavigating = isOnTrailActive
            )
        }

        // HUD Metrics Grid (Speed, Elevation, Time, Compass)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudMetricCard(
                    title = "SPEED",
                    value = "$speedKmh",
                    unit = "km/h",
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    title = "ELEVATION",
                    value = "$currentElevation",
                    unit = "meters",
                    modifier = Modifier.weight(1f)
                )
                HudMetricCard(
                    title = "ELAPSED",
                    value = timeFormatted,
                    unit = "time",
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        // Action Quick Bar: Report Moment, PhotoScan, SOS Beacon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("report_moment_button")
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report Hazard / Moment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(BiomateScreen.PHOTO_SCAN) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PhotoScan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Trail Moments & Hazard Reports
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Trail Conditions & Discoveries",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Updated 2m ago",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(allMoments) { moment ->
            TrailMomentCard(
                moment = moment,
                onUpvote = { viewModel.upvoteMoment(moment) }
            )
        }

        // Post-trip story generator action
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Finished your hike? Generate Adventure Story!",
                        fontWeight = FontWeight.Bold,
                        color = OnForestGreenContainer,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Summarize today's highlights, species spotted, companion stats, and elevation milestones with AI.",
                        fontSize = 11.sp,
                        color = OnForestGreenContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.createAdventureStoryForActiveTrip() },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_story_button")
                    ) {
                        Text("Create Adventure Story & Recap", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportMomentDialog(
            trailId = currentTrail.id,
            onDismiss = { showReportDialog = false },
            onSubmit = { type, title, desc, km, warn ->
                viewModel.reportTrailMoment(type, title, desc, km, warn)
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun HudMetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary)
            Text(text = unit, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrailMomentCard(
    moment: TrailMoment,
    onUpvote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (moment.warningLevel == "CAUTION" || moment.warningLevel == "DANGER")
                Color(0xFFFFF3E0)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (moment.type) {
                    "HAZARD_SNAKE" -> Icons.Default.Warning
                    "HAZARD_BLOCKED" -> Icons.Default.Warning
                    "WATER_SOURCE" -> Icons.Default.WaterDrop
                    "WILDFLOWER" -> Icons.Default.LocalFlorist
                    else -> Icons.Default.WbSunny
                }
                val iconTint = when (moment.warningLevel) {
                    "CAUTION" -> Color(0xFFE65100)
                    "DANGER" -> Color(0xFFC62828)
                    else -> ForestGreenPrimary
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = moment.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "km ${moment.kmMarker}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = moment.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Reported by ${moment.reportedBy} • ${moment.timeAgo}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Upvote Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onUpvote() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(12.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${moment.upvotes}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                }
            }
        }
    }
}

@Composable
fun ReportMomentDialog(
    trailId: String,
    onDismiss: () -> Unit,
    onSubmit: (type: String, title: String, desc: String, km: Double, warn: String) -> Unit
) {
    var type by remember { mutableStateOf("HAZARD_SNAKE") }
    var title by remember { mutableStateOf("Snake sighted near rock path") }
    var desc by remember { mutableStateOf("Red-bellied black snake basking on warm granite rock. Watch your step.") }
    var kmMarker by remember { mutableStateOf("12.3") }
    var warningLevel by remember { mutableStateOf("CAUTION") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Report Trail Condition / Discovery",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description & Instructions") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kmMarker,
                        onValueChange = { kmMarker = it },
                        label = { Text("Km Marker") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = warningLevel,
                        onValueChange = { warningLevel = it },
                        label = { Text("Warning Level (INFO/CAUTION/DANGER)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSubmit(type, title, desc, kmMarker.toDoubleOrNull() ?: 5.0, warningLevel)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        modifier = Modifier.testTag("submit_report_button")
                    ) {
                        Text("Broadcast")
                    }
                }
            }
        }
    }
}
