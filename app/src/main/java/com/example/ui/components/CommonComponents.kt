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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrailPoint
import com.example.ui.BiomateScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiomateTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onSosClick: () -> Unit = {},
    isSosActive: Boolean = false,
    userInitials: String = "ME",
    userColor: Color = TerracottaContainer,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        color = SandBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 24.sp,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, OutlineSubtle, CircleShape)
                            .clickable { onBackClick() }
                            .testTag("top_bar_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            actions = {
                // Emergency SOS pill
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSosActive) DangerRed else Color(0xFFFFEBEE))
                        .clickable { onSosClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("sos_beacon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency SOS",
                            tint = if (isSosActive) Color.White else DangerRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSosActive) "SOS ACTIVE" else "SOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSosActive) Color.White else DangerRed
                        )
                    }
                }

                // Profile Avatar badge with terracotta ring
                AdventurerAvatar(
                    initials = userInitials,
                    sizeDp = 38,
                    borderColor = TerracottaPrimary,
                    backgroundColor = userColor,
                    onClick = onProfileClick,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SandBackground
            )
        )
    }
}

@Composable
fun AdventurerAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    borderColor: Color = TerracottaPrimary,
    backgroundColor: Color = TerracottaContainer,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (sizeDp * 0.36).sp,
            fontWeight = FontWeight.Bold,
            color = TerracottaDark
        )
    }
}

@Composable
fun BiomateBottomNavigation(
    currentScreen: BiomateScreen,
    onNavigate: (BiomateScreen) -> Unit,
    isOnTrailActive: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = Color(0x33A04218),
                    ambientColor = Color(0x1AA04218)
                )
                .clip(CircleShape)
                .background(Color(0xFFFBF6F0))
                .border(1.dp, Color(0xFFECDCCF), CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Folded Map (Map / OnTrail)
                val isMapSelected = currentScreen == BiomateScreen.ON_TRAIL
                TaskbarIconButton(
                    isSelected = isMapSelected,
                    onClick = { onNavigate(BiomateScreen.ON_TRAIL) },
                    testTag = "nav_item_map",
                    contentDescription = "Trail Map & Live Tracking"
                ) { tint ->
                    FoldedMapNavIcon(color = tint)
                }

                // 2. Camera (PhotoScan AI)
                val isCameraSelected = currentScreen == BiomateScreen.PHOTO_SCAN
                TaskbarIconButton(
                    isSelected = isCameraSelected,
                    onClick = { onNavigate(BiomateScreen.PHOTO_SCAN) },
                    testTag = "nav_item_camera",
                    contentDescription = "PhotoScan AI"
                ) { tint ->
                    CameraNavIcon(color = tint)
                }

                // 3. Cabin / Home (Discover / Expedition Gear)
                val isHomeSelected = currentScreen == BiomateScreen.DISCOVER || currentScreen == BiomateScreen.TRIP_PLAN
                TaskbarIconButton(
                    isSelected = isHomeSelected,
                    onClick = { onNavigate(BiomateScreen.DISCOVER) },
                    testTag = "nav_item_home",
                    contentDescription = "Home & Trails"
                ) { tint ->
                    HomeCabinNavIcon(color = tint)
                }

                // 4. Chat Bubbles (Messages & Community)
                val isChatSelected = currentScreen == BiomateScreen.MESSAGES || currentScreen == BiomateScreen.COMMUNITY
                TaskbarIconButton(
                    isSelected = isChatSelected,
                    onClick = { onNavigate(BiomateScreen.MESSAGES) },
                    testTag = "nav_item_messages",
                    contentDescription = "Messages"
                ) { tint ->
                    ChatBubblesNavIcon(color = tint)
                }

                // 5. Stacked Cards (HikeMatch)
                val isCardsSelected = currentScreen == BiomateScreen.HIKE_MATCH
                TaskbarIconButton(
                    isSelected = isCardsSelected,
                    onClick = { onNavigate(BiomateScreen.HIKE_MATCH) },
                    testTag = "nav_item_cards",
                    contentDescription = "HikeMatch Buddies"
                ) { tint ->
                    StackedCardsNavIcon(color = tint)
                }
            }
        }
    }
}

@Composable
private fun TaskbarIconButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    contentDescription: String,
    icon: @Composable (tint: Color) -> Unit
) {
    val activeColor = Color(0xFFBA4E26)
    val inactiveColor = Color(0xFFC76C48)
    val tint = if (isSelected) activeColor else inactiveColor

    Box(
        modifier = Modifier
            .size(48.dp, 44.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(tint)
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}

@Composable
fun FoldedMapNavIcon(
    color: Color,
    modifier: Modifier = Modifier.size(25.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        val p1Top = Offset(w * 0.12f, h * 0.22f)
        val p2Top = Offset(w * 0.38f, h * 0.12f)
        val p3Top = Offset(w * 0.62f, h * 0.22f)
        val p4Top = Offset(w * 0.88f, h * 0.12f)

        val p1Bot = Offset(w * 0.12f, h * 0.88f)
        val p2Bot = Offset(w * 0.38f, h * 0.78f)
        val p3Bot = Offset(w * 0.62f, h * 0.88f)
        val p4Bot = Offset(w * 0.88f, h * 0.78f)

        val outerMap = Path().apply {
            moveTo(p1Top.x, p1Top.y)
            lineTo(p2Top.x, p2Top.y)
            lineTo(p3Top.x, p3Top.y)
            lineTo(p4Top.x, p4Top.y)
            lineTo(p4Bot.x, p4Bot.y)
            lineTo(p3Bot.x, p3Bot.y)
            lineTo(p2Bot.x, p2Bot.y)
            lineTo(p1Bot.x, p1Bot.y)
            close()
        }
        drawPath(
            path = outerMap,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Inner vertical fold lines
        drawLine(color = color, start = p2Top, end = p2Bot, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color = color, start = p3Top, end = p3Bot, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CameraNavIcon(
    color: Color,
    modifier: Modifier = Modifier.size(25.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        // Rounded camera body
        val bodyRect = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.10f,
                    top = h * 0.28f,
                    right = w * 0.90f,
                    bottom = h * 0.84f,
                    radiusX = 5.dp.toPx(),
                    radiusY = 5.dp.toPx()
                )
            )
        }
        drawPath(
            path = bodyRect,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Top hump/shutter bump
        val topHump = Path().apply {
            moveTo(w * 0.32f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.15f)
            lineTo(w * 0.68f, h * 0.28f)
        }
        drawPath(
            path = topHump,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Center lens
        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.56f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun HomeCabinNavIcon(
    color: Color,
    modifier: Modifier = Modifier.size(25.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        // House outline
        val housePath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.14f, h * 0.42f)
            lineTo(w * 0.14f, h * 0.86f)
            lineTo(w * 0.86f, h * 0.86f)
            lineTo(w * 0.86f, h * 0.42f)
            close()
        }
        drawPath(
            path = housePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Rounded arch door
        val doorPath = Path().apply {
            moveTo(w * 0.38f, h * 0.86f)
            lineTo(w * 0.38f, h * 0.58f)
            cubicTo(w * 0.38f, h * 0.48f, w * 0.62f, h * 0.48f, w * 0.62f, h * 0.58f)
            lineTo(w * 0.62f, h * 0.86f)
        }
        drawPath(
            path = doorPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun ChatBubblesNavIcon(
    color: Color,
    modifier: Modifier = Modifier.size(25.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        // Main left bubble
        val b1 = Path().apply {
            moveTo(w * 0.22f, h * 0.66f)
            lineTo(w * 0.10f, h * 0.74f)
            lineTo(w * 0.26f, h * 0.58f)
        }
        drawCircle(
            color = color,
            radius = w * 0.22f,
            center = Offset(w * 0.36f, h * 0.44f),
            style = Stroke(width = strokeWidth)
        )
        drawPath(
            path = b1,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Secondary right bubble
        val b2 = Path().apply {
            moveTo(w * 0.76f, h * 0.70f)
            lineTo(w * 0.88f, h * 0.78f)
            lineTo(w * 0.78f, h * 0.56f)
        }
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.64f, h * 0.56f),
            style = Stroke(width = strokeWidth)
        )
        drawPath(
            path = b2,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun StackedCardsNavIcon(
    color: Color,
    modifier: Modifier = Modifier.size(25.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.2.dp.toPx()

        // Back card tilted counter-clockwise
        rotate(degrees = -14f, pivot = Offset(w * 0.44f, h * 0.5f)) {
            val backCard = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = w * 0.22f,
                        top = h * 0.16f,
                        right = w * 0.68f,
                        bottom = h * 0.84f,
                        radiusX = 4.dp.toPx(),
                        radiusY = 4.dp.toPx()
                    )
                )
            }
            drawPath(
                path = backCard,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Front card tilted clockwise
        rotate(degrees = 14f, pivot = Offset(w * 0.56f, h * 0.5f)) {
            val frontCard = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = w * 0.32f,
                        top = h * 0.16f,
                        right = w * 0.78f,
                        bottom = h * 0.84f,
                        radiusX = 4.dp.toPx(),
                        radiusY = 4.dp.toPx()
                    )
                )
            }
            drawPath(
                path = frontCard,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun KataTjutaGraphic(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky background gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF87CEEB), Color(0xFFFDE8D0), Color(0xFFE89B72))
            )
        )

        // Golden Sun
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = w * 0.18f,
            center = Offset(w * 0.75f, h * 0.35f)
        )

        // Background Kata Tjuta Domes (Distant)
        val distantDome = Path().apply {
            moveTo(0f, h)
            cubicTo(w * 0.1f, h * 0.35f, w * 0.35f, h * 0.3f, w * 0.5f, h)
            close()
        }
        drawPath(distantDome, color = Color(0xFFBF4F26))

        // Midground Kata Tjuta Dome (Center-Right)
        val midDome = Path().apply {
            moveTo(w * 0.3f, h)
            cubicTo(w * 0.45f, h * 0.25f, w * 0.75f, h * 0.2f, w * 0.9f, h)
            close()
        }
        drawPath(midDome, color = Color(0xFFC75F2A))

        // Foreground Dome (Left)
        val fgDome = Path().apply {
            moveTo(0f, h)
            cubicTo(w * 0.05f, h * 0.45f, w * 0.25f, h * 0.4f, w * 0.45f, h)
            close()
        }
        drawPath(fgDome, color = Color(0xFFD67448))

        // Desert Red Sand & Spinifex Grass Foreground
        val sandPath = Path().apply {
            moveTo(0f, h * 0.85f)
            quadraticTo(w * 0.5f, h * 0.8f, w, h * 0.9f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(sandPath, color = Color(0xFFB85324))

        // Spinifex tufts
        for (i in 1..5) {
            val tuftX = w * (i * 0.18f)
            val tuftY = h * 0.88f
            drawLine(
                color = Color(0xFFD2B55B),
                start = Offset(tuftX, tuftY),
                end = Offset(tuftX - 6f, tuftY - 14f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFFE5C158),
                start = Offset(tuftX, tuftY),
                end = Offset(tuftX + 6f, tuftY - 14f),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
fun ScenicMountainGraphic(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky sunset gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEAA058), Color(0xFFECC8B4), Color(0xFFF9EDE4))
            )
        )

        // Distant mountain
        val mountain1 = Path().apply {
            moveTo(0f, h * 0.7f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.65f)
            lineTo(w, h * 0.4f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(mountain1, color = Color(0xFFCD744C).copy(alpha = 0.5f))

        // Near mountain
        val mountain2 = Path().apply {
            moveTo(0f, h * 0.8f)
            lineTo(w * 0.55f, h * 0.45f)
            lineTo(w, h * 0.75f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(mountain2, color = Color(0xFF9E4928))

        // Pine trees silhouette at bottom
        for (i in 0..10) {
            val treeX = w * (i * 0.1f)
            val treeY = h * 0.82f
            val treeH = 30f
            val treePath = Path().apply {
                moveTo(treeX, treeY - treeH)
                lineTo(treeX - 12f, treeY)
                lineTo(treeX + 12f, treeY)
                close()
            }
            drawPath(treePath, color = Color(0xFF331D16))
        }
    }
}

@Composable
fun ElevationProfileView(
    waypoints: List<TrailPoint>,
    currentProgressKm: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (waypoints.isEmpty()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantSand),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Elevation Profile",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextCharcoal)
                )
                val maxElev = waypoints.maxOfOrNull { it.elevationM } ?: 0
                val minElev = waypoints.minOfOrNull { it.elevationM } ?: 0
                Text(
                    text = "Min ${minElev}m • Max ${maxElev}m",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                val width = size.width
                val height = size.height
                val totalKm = waypoints.last().kmMarker.coerceAtLeast(1.0)
                val minEl = (waypoints.minOfOrNull { it.elevationM } ?: 0) - 20
                val maxEl = (waypoints.maxOfOrNull { it.elevationM } ?: 500) + 50
                val elRange = (maxEl - minEl).coerceAtLeast(100)

                val points = waypoints.map { wp ->
                    val x = ((wp.kmMarker / totalKm) * width).toFloat()
                    val y = height - (((wp.elevationM - minEl).toFloat() / elRange) * height)
                    Offset(x, y)
                }

                // Draw filled gradient under path
                if (points.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TerracottaPrimary.copy(alpha = 0.4f),
                                TerracottaPrimary.copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Draw line
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = TerracottaPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Waypoint markers
                    points.forEachIndexed { index, pt ->
                        val wp = waypoints[index]
                        val markerColor = when (wp.type) {
                            "SUMMIT" -> TerracottaPrimary
                            "WATER" -> Color(0xFF0288D1)
                            "CAMPSITE" -> AmberSunrise
                            else -> ForestGreenPrimary
                        }
                        drawCircle(
                            color = markerColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }

                    // Progress marker indicator
                    if (currentProgressKm > 0.0) {
                        val progX = ((currentProgressKm / totalKm) * width).coerceIn(0.0, width.toDouble()).toFloat()
                        drawLine(
                            color = TerracottaDark,
                            start = Offset(progX, 0f),
                            end = Offset(progX, height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waypoint key legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                waypoints.take(4).forEach { wp ->
                    Text(
                        text = "${wp.name.take(12)} (${wp.elevationM}m)",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun TopographicMapSimulation(
    trailPoints: List<TrailPoint>,
    progressPercent: Int,
    isNavigating: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7ECE4))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val w = size.width
            val h = size.height

            // Draw Topographic contour lines
            for (i in 1..6) {
                val radius = (h * 0.2f * i)
                drawCircle(
                    color = Color(0xFFE2D0C5),
                    radius = radius,
                    center = Offset(w * 0.45f, h * 0.5f),
                    style = Stroke(width = 1.2f)
                )
            }

            // Draw Trail Path
            val pathPoints = listOf(
                Offset(w * 0.1f, h * 0.85f),
                Offset(w * 0.25f, h * 0.65f),
                Offset(w * 0.42f, h * 0.5f),
                Offset(w * 0.6f, h * 0.35f),
                Offset(w * 0.75f, h * 0.45f),
                Offset(w * 0.9f, h * 0.2f)
            )

            val trailPath = Path().apply {
                moveTo(pathPoints.first().x, pathPoints.first().y)
                for (i in 1 until pathPoints.size) {
                    lineTo(pathPoints[i].x, pathPoints[i].y)
                }
            }

            // Background dashed trail
            drawPath(
                path = trailPath,
                color = Color(0xFFB59382),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active completed path
            val activeFrac = (progressPercent / 100f).coerceIn(0f, 1f)
            val currentPos = Offset(
                x = pathPoints.first().x + (pathPoints.last().x - pathPoints.first().x) * activeFrac,
                y = pathPoints.first().y + (pathPoints.last().y - pathPoints.first().y) * activeFrac
            )

            // Draw hiker avatar marker
            drawCircle(
                color = TerracottaPrimary.copy(alpha = 0.3f),
                radius = 14.dp.toPx(),
                center = currentPos
            )
            drawCircle(
                color = TerracottaPrimary,
                radius = 8.dp.toPx(),
                center = currentPos
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = currentPos
            )

            // Start and end markers
            drawCircle(color = ForestGreenPrimary, radius = 5.dp.toPx(), center = pathPoints.first())
            drawCircle(color = AmberSunrise, radius = 5.dp.toPx(), center = pathPoints.last())
        }

        // Live badge overlay
        Row(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isNavigating) Color(0xFF43A047) else AmberSunrise)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isNavigating) "GPS LOCK • HIGH ACCURACY (±3m)" else "OFFLINE TOPOGRAPHIC CACHE ACTIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

