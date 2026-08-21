package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyState
import com.example.ui.components.StatTile
import com.example.ui.components.TrailHeroArt
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adventure memories: completed trips, as they actually happened.
 *
 * Built entirely from recorded session data — distance, duration, companions, moments.
 * There is deliberately no generated narrative: the spec rules out AI story generation
 * for the MVP (spec section 67), and a fabricated account of someone's day out is worse
 * than an honest set of numbers.
 */
@Composable
fun MemoriesScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault()) }

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
                "Adventure memories",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (history.isEmpty()) {
            EmptyState(
                emoji = "📔",
                title = "No memories yet",
                body = "Finish an adventure and it will be recorded here with the distance you covered and who was with you."
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(history, key = { it.session.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box {
                            TrailHeroArt(
                                seed = entry.session.trailId.hashCode(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                        )
                                    )
                            )
                            Column(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    entry.trip?.title
                                        ?: entry.trail?.name
                                        ?: "Adventure",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    dateFormat.format(
                                        Date(entry.session.completedAt ?: entry.session.startedAt)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Column(Modifier.padding(16.dp)) {
                            entry.trail?.let {
                                Text(
                                    "${it.name} · ${it.region}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VSpace(12)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile(
                                    "${"%.2f".format(entry.session.distanceKm)} km",
                                    "Distance",
                                    Modifier.weight(1f)
                                )
                                StatTile(
                                    "${entry.session.durationMinutes} min",
                                    "Time",
                                    Modifier.weight(1f)
                                )
                                StatTile(
                                    "${entry.session.companionCount}",
                                    "With you",
                                    Modifier.weight(1f)
                                )
                            }
                            if (entry.session.momentCount > 0) {
                                VSpace(12)
                                Text(
                                    "${entry.session.momentCount} trail moments recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
