package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Difficulty
import com.example.domain.model.TrailWaypoint
import kotlin.math.max
import kotlin.math.min

/**
 * Shared Biomate visual language.
 *
 * These carry over the illustrated, warm-terracotta identity from the original scaffold —
 * hand-drawn scenery rather than stock photography — while taking their data from the new
 * domain model.
 */

/**
 * A user's avatar: their photo when they have one, otherwise their initials on the colour
 * assigned at sign-up.
 *
 * Initials rather than a generic silhouette, because in a list of five participants a row
 * of identical grey heads tells the reader nothing.
 */
@Composable
fun AdventurerAvatar(
    initials: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val base = Color(colorHex)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(base.copy(alpha = 0.18f))
            .border(1.5.dp, base, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics {}
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (sizeDp * 0.36).sp,
            fontWeight = FontWeight.Bold,
            color = base
        )
    }
}

/**
 * A small status pill.
 *
 * Always carries text. Status is never communicated by the pill's colour alone, so it
 * still reads correctly in greyscale and to a colour-blind user.
 */
@Composable
fun StatusBadge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

/** Difficulty as a labelled pill, colour-coded but never colour-only. */
@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val (container, content) = when (difficulty) {
        Difficulty.EASY -> Color(0xFFDCEEE4) to Color(0xFF0F3625)
        Difficulty.MODERATE -> Color(0xFFFFECCC) to Color(0xFF7A4A08)
        Difficulty.HARD -> Color(0xFFFBDCC8) to Color(0xFF8A3A12)
        Difficulty.CHALLENGING -> Color(0xFFF9D2D2) to Color(0xFF7F1D1D)
    }
    StatusBadge(difficulty.label, container, content, modifier)
}

/**
 * Illustrated trail scenery.
 *
 * Stands in for a hero photograph. Drawing it means no image licensing, no network fetch
 * and no empty grey box while a picture loads — and it varies by [seed] so two trails in
 * a list do not look identical.
 */
@Composable
fun TrailHeroArt(
    seed: Int,
    modifier: Modifier = Modifier
) {
    val warm = (seed % 3)
    val skyColours = when (warm) {
        0 -> listOf(Color(0xFFEAA058), Color(0xFFECC8B4), Color(0xFFF9EDE4))
        1 -> listOf(Color(0xFF7FB3D5), Color(0xFFCFE3EF), Color(0xFFF6ECE4))
        else -> listOf(Color(0xFFB98CC0), Color(0xFFEBC8D8), Color(0xFFFBF1E8))
    }
    val ridgeColour = when (warm) {
        0 -> Color(0xFF9E4928)
        1 -> Color(0xFF3D6B57)
        else -> Color(0xFF6B4A7A)
    }

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height

        drawRect(brush = Brush.verticalGradient(skyColours))

        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.85f),
            radius = w * 0.09f,
            center = Offset(w * (0.2f + (seed % 5) * 0.13f), h * 0.28f)
        )

        // Far ridge.
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.72f)
                lineTo(w * (0.28f + (seed % 3) * 0.07f), h * 0.36f)
                lineTo(w * 0.62f, h * 0.66f)
                lineTo(w, h * 0.42f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            color = ridgeColour.copy(alpha = 0.45f)
        )

        // Near ridge.
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.82f)
                lineTo(w * (0.45f + (seed % 4) * 0.06f), h * 0.47f)
                lineTo(w, h * 0.78f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            color = ridgeColour
        )

        // Tree line.
        val treeCount = 12
        for (i in 0..treeCount) {
            val treeX = w * (i.toFloat() / treeCount)
            val treeY = h * 0.9f
            val treeHeight = h * (0.08f + ((i + seed) % 3) * 0.02f)
            drawPath(
                Path().apply {
                    moveTo(treeX, treeY - treeHeight)
                    lineTo(treeX - w * 0.022f, treeY)
                    lineTo(treeX + w * 0.022f, treeY)
                    close()
                },
                color = Color(0xFF2B1B14).copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * The elevation profile along a trail.
 *
 * Renders the climb as a filled area with the named waypoints marked, and — when the user
 * is walking — a line showing where they currently are. Falls back to nothing rather than
 * an empty axis when a trail has no waypoint data.
 */
@Composable
fun ElevationProfile(
    waypoints: List<TrailWaypoint>,
    modifier: Modifier = Modifier,
    currentKm: Double? = null
) {
    if (waypoints.size < 2) return

    val minElevation = waypoints.minOf { it.elevationM }
    val maxElevation = waypoints.maxOf { it.elevationM }
    val totalKm = waypoints.maxOf { it.kmMarker }.takeIf { it > 0 } ?: return
    val span = max(1, maxElevation - minElevation)

    val lineColour = MaterialTheme.colorScheme.primary
    val fillColour = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val markerColour = MaterialTheme.colorScheme.secondary

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Elevation profile",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${minElevation}m – ${maxElevation}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics {
                    contentDescription =
                        "Elevation profile from ${minElevation} to ${maxElevation} metres " +
                            "over ${"%.1f".format(totalKm)} kilometres"
                }
        ) {
            val w = size.width
            val h = size.height
            val padding = h * 0.12f

            fun xFor(km: Double) = (km / totalKm).toFloat() * w
            fun yFor(elevation: Int): Float {
                val ratio = (elevation - minElevation).toFloat() / span
                return h - padding - ratio * (h - padding * 2)
            }

            val line = Path().apply {
                waypoints.forEachIndexed { index, point ->
                    val x = xFor(point.kmMarker)
                    val y = yFor(point.elevationM)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(xFor(totalKm), h)
                lineTo(xFor(0.0), h)
                close()
            }

            drawPath(area, color = fillColour)
            drawPath(line, color = lineColour, style = Stroke(width = 3f))

            waypoints.forEach { point ->
                drawCircle(
                    color = lineColour,
                    radius = 4f,
                    center = Offset(xFor(point.kmMarker), yFor(point.elevationM))
                )
            }

            if (currentKm != null) {
                val clamped = min(max(currentKm, 0.0), totalKm)
                val x = xFor(clamped)
                drawLine(
                    color = markerColour,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3f
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                waypoints.first().name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                waypoints.last().name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A labelled statistic. The unit is always shown — a bare "84" is not information. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (emoji != null) {
                Text(emoji, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clearAndSetSemantics {})
                Spacer(Modifier.height(4.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
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

/** A section heading with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

/** Horizontal spacer sized to the app's rhythm. */
@Composable
fun HSpace(dp: Int) = Spacer(Modifier.width(dp.dp))

/** Vertical spacer sized to the app's rhythm. */
@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))
