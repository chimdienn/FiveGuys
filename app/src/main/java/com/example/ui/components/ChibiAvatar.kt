package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.location.LocationProvider
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.environment.Environment
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberViewNodeManager
import io.github.sceneview.node.ModelNode
import com.google.android.filament.Skybox
import kotlinx.coroutines.flow.catch

/** The movement vocabulary shared by GPS, profiles and trip participants. */
enum class ChibiMotion {
    IDLE,
    WALKING,
    RUNNING,
    JUMPING,
    LOOSE;

    companion object {
        /**
         * GPS walking speeds are noisy around zero. The dead-band prevents the avatar
         * flickering between Idle and Walk while the phone is sitting on a table.
         */
        fun fromSpeed(speedMetersPerSecond: Float?): ChibiMotion = when {
            speedMetersPerSecond == null || speedMetersPerSecond < 0.45f -> IDLE
            speedMetersPerSecond < 2.7f -> WALKING
            else -> RUNNING
        }
    }
}

data class ChibiParticipant(
    val userId: String,
    val displayName: String,
    val motion: ChibiMotion = ChibiMotion.IDLE
)

private data class ChibiAsset(
    val path: String,
    val animationSuffix: String
)

private val animatedChibis = listOf(
    ChibiAsset("mini_chibi_kid_free_demo.glb", " 01"),
    ChibiAsset("mini_simple_character_free_demo__animations.glb", ""),
    ChibiAsset("mini_simple_characters__skeleton_free_demo.glb", "")
)

private fun ChibiMotion.animationName(suffix: String): String = when (this) {
    ChibiMotion.IDLE -> "Idle$suffix"
    ChibiMotion.WALKING -> "Walk$suffix"
    ChibiMotion.RUNNING -> "Run$suffix"
    ChibiMotion.JUMPING -> "Jump$suffix"
    ChibiMotion.LOOSE -> "Loose$suffix"
}

/**
 * A native Filament/SceneView-backed GLB avatar with an accessible Compose name tag.
 * The user id deterministically selects one of the supplied animated characters, so a
 * person's chibi remains consistent on every screen without adding profile storage yet.
 */
@Composable
fun ChibiAvatar(
    userId: String,
    displayName: String,
    motion: ChibiMotion,
    modifier: Modifier = Modifier,
    showNameTag: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    val asset = remember(userId) {
        animatedChibis[Math.floorMod(userId.hashCode(), animatedChibis.size)]
    }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val scene = rememberScene(engine)
    val skybox = remember(engine, backgroundColor) {
        Skybox.Builder()
            .color(
                backgroundColor.red,
                backgroundColor.green,
                backgroundColor.blue,
                1f
            )
            .build(engine)
    }
    val environment = remember(skybox) { Environment(skybox = skybox) }
    DisposableEffect(scene, skybox) {
        onDispose {
            engine.destroySkybox(skybox)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            scene = scene,
            viewNodeWindowManager = rememberViewNodeManager(),
            environment = environment,
            cameraNode = rememberCameraNode(engine) {
                position = Position(z = 3.2f)
            },
            mainLightNode = rememberMainLightNode(engine)
        ) {
            rememberModelInstance(modelLoader, asset.path)?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.75f,
                    autoAnimate = false,
                    animationName = motion.animationName(asset.animationSuffix),
                    animationLoop = motion != ChibiMotion.JUMPING,
                    animationSpeed = when (motion) {
                        ChibiMotion.WALKING -> 1.1f
                        ChibiMotion.RUNNING -> 1.25f
                        else -> 1f
                    }
                )
            }
        }

        if (showNameTag) {
            Text(
                text = displayName,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/** Compact party presentation used only for trips of six joined users or fewer. */
@Composable
fun ChibiGroup(
    participants: List<ChibiParticipant>,
    modifier: Modifier = Modifier
) {
    val visible = participants.take(6)
    if (visible.isEmpty()) return

    Column(modifier) {
        Text(
            text = "Your adventure crew",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visible.forEach { participant ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChibiAvatar(
                        userId = participant.userId,
                        displayName = participant.displayName,
                        motion = participant.motion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        showNameTag = true
                    )
                }
            }
        }
    }
}

/** Collect the signed-in device's real GPS speed when permission has been granted. */
@Composable
fun rememberCurrentChibiMotion(locationProvider: LocationProvider): State<ChibiMotion> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(ChibiMotion.IDLE) }
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(locationProvider, hasPermission) {
        if (!hasPermission) {
            state.value = ChibiMotion.IDLE
            return@LaunchedEffect
        }
        locationProvider.locationUpdates()
            .catch { state.value = ChibiMotion.IDLE }
            .collect { fix -> state.value = ChibiMotion.fromSpeed(fix.speedMetersPerSecond) }
    }
    return state
}
