package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.MomentCategory
import com.example.domain.model.SubmissionState
import com.example.ui.components.EmptyState
import com.example.ui.components.VSpace
import com.example.ui.viewmodel.CameraMode
import com.example.ui.viewmodel.CaptureState
import com.example.ui.viewmodel.OnTrailViewModel
import com.example.ui.viewmodel.ScanViewModel
import java.io.ByteArrayOutputStream

/**
 * The camera, in two modes.
 *
 * **Challenge** submits a photo against a daily challenge. That submission is final — the
 * user can retake as often as they like beforehand, but the confirm dialog is the point of
 * no return, and it says so.
 *
 * **Explore** identifies whatever is in frame. Every result carries an uncertainty note
 * and a safety caution, and neither can be dismissed: an image classifier telling someone
 * a mushroom is fine to eat is the one failure in this app that could actually hurt them.
 */
@Composable
fun PhotoScanScreen(
    viewModel: ScanViewModel,
    onTrailViewModel: OnTrailViewModel
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val photoChallenges by viewModel.photoChallenges.collectAsStateWithLifecycle()
    val targetChallengeId by viewModel.targetDailyChallengeId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var showFinalSubmitConfirm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (mode == CameraMode.EXPLORE) 0 else 1) {
            Tab(
                selected = mode == CameraMode.EXPLORE,
                onClick = { viewModel.setMode(CameraMode.EXPLORE) },
                text = { Text("Identify") }
            )
            Tab(
                selected = mode == CameraMode.CHALLENGE,
                onClick = { viewModel.setMode(CameraMode.CHALLENGE) },
                text = { Text("Challenge") }
            )
        }

        if (mode == CameraMode.CHALLENGE) {
            if (photoChallenges.isEmpty()) {
                EmptyState(
                    emoji = "📸",
                    title = "No photo challenges today",
                    body = "Today's challenges don't need a photo. Switch to Identify to look something up instead.",
                    actionLabel = "Switch to Identify",
                    onAction = { viewModel.setMode(CameraMode.EXPLORE) }
                )
                return@Column
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoChallenges, key = { it.daily.id }) { view ->
                    FilterChip(
                        selected = targetChallengeId == view.daily.id,
                        onClick = { viewModel.selectChallenge(view.daily.id) },
                        label = { Text(view.challenge.title) },
                        enabled = !view.daily.isComplete,
                        leadingIcon = if (view.daily.isComplete) {
                            { Text("✓") }
                        } else null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            targetChallengeId?.let { id ->
                photoChallenges.firstOrNull { it.daily.id == id }?.let { view ->
                    Text(
                        "Photograph ${view.challenge.photoSubject}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = captureState) {
                CaptureState.Ready -> {
                    if (hasCameraPermission) {
                        CameraPreview(
                            onCaptured = viewModel::onPhotoCaptured,
                            onError = viewModel::onCaptureFailed
                        )
                    } else {
                        CameraPermissionExplainer {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }

                is CaptureState.Captured -> CapturedPreview(
                    bytes = state.bytes,
                    mode = mode,
                    canSubmit = mode == CameraMode.EXPLORE || targetChallengeId != null,
                    onRetake = viewModel::retake,
                    onIdentify = { viewModel.identifyPhoto(locationLabel = null) },
                    onSubmit = { showFinalSubmitConfirm = true }
                )

                CaptureState.Submitting -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        VSpace(16)
                        Text(
                            if (mode == CameraMode.CHALLENGE) "Checking your photo…" else "Identifying…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is CaptureState.ChallengeVerdict -> ChallengeVerdictPanel(
                    state = state,
                    onDone = viewModel::reset
                )

                is CaptureState.Identified -> IdentificationPanel(
                    identification = state.result,
                    onAddMoment = {
                        onTrailViewModel.addMoment(
                            category = viewModel.momentCategoryFor(state.result),
                            description = "${state.result.commonName} — ${state.result.description}"
                        )
                        viewModel.reset()
                    },
                    onDone = viewModel::reset
                )

                is CaptureState.Failed -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("😕", style = MaterialTheme.typography.displayLarge)
                    VSpace(16)
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    VSpace(24)
                    Button(
                        onClick = viewModel::retake,
                        modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Try again") }
                }
            }
        }
    }

    // The confirm step exists because the submission cannot be undone. Saying so here is
    // the difference between an informed action and a trap.
    if (showFinalSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showFinalSubmitConfirm = false },
            title = { Text("Submit this photo?") },
            text = {
                Text(
                    "Once you submit, this photo is final for this challenge — you won't be " +
                        "able to replace it. Retake it now if you're not happy with it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinalSubmitConfirm = false
                    viewModel.submitChallengePhoto()
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showFinalSubmitConfirm = false }) { Text("Retake") }
            }
        )
    }
}

/**
 * The live camera.
 *
 * Uses CameraX bound to the composable's lifecycle, so the camera is released when the
 * screen leaves composition rather than being held open in the background.
 */
@Composable
private fun CameraPreview(
    onCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isCapturing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).also { previewView ->
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }.onFailure { error ->
                            Log.e(TAG, "Could not start the camera", error)
                            onError("Could not start the camera on this device.")
                        }
                    }, ContextCompat.getMainExecutor(viewContext))
                }
            }
        )

        // Shutter. Disabled while a capture is in flight so a rapid double tap cannot
        // queue two frames.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(76.dp)
                    .semantics { contentDescription = "Take photo" }
            ) {
                TextButton(
                    onClick = {
                        if (isCapturing) return@TextButton
                        isCapturing = true
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onSuccess = {
                                isCapturing = false
                                onCaptured(it)
                            },
                            onError = { message ->
                                isCapturing = false
                                onError(message)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(if (isCapturing) "…" else "📷", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private const val TAG = "PhotoScanScreen"

/**
 * Grabs one frame from CameraX and returns a normal, correctly-oriented JPEG.
 *
 * Reading only `image.planes[0]` assumes every device returns an already encoded JPEG.
 * ImageProxy can legally be JPEG or YUV/RGBA on different CameraX/device pipelines, and
 * it also carries a rotation that must be applied. Converting through `toBitmap()` makes
 * the bytes sent to Gemini consistent across real phones. The image is also scaled down
 * before upload so a 48 MP camera does not create a needlessly large/slow API request.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val source = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    val oriented = if (rotation == 0) {
                        source
                    } else {
                        Bitmap.createBitmap(
                            source,
                            0,
                            0,
                            source.width,
                            source.height,
                            Matrix().apply { postRotate(rotation.toFloat()) },
                            true
                        )
                    }

                    val resized = scaleForGemini(oriented, maxDimension = 1600)
                    val output = ByteArrayOutputStream()
                    check(resized.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                        "JPEG compression failed"
                    }
                    val bytes = output.toByteArray()
                    check(bytes.isNotEmpty()) { "Captured JPEG was empty" }

                    if (resized !== oriented) resized.recycle()
                    if (oriented !== source) oriented.recycle()
                    source.recycle()

                    Log.d(TAG, "Camera image prepared for Gemini: ${bytes.size / 1024} KB")
                    onSuccess(bytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not prepare the captured frame", e)
                    onError("Could not prepare that photo for AI identification. Try again.")
                } finally {
                    // Always closed: a leaked ImageProxy stalls the whole capture pipeline.
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
                onError("The camera could not take that photo.")
            }
        }
    )
}

private fun scaleForGemini(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / largest.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
}

@Composable
private fun CapturedPreview(
    bytes: ByteArray,
    mode: CameraMode,
    canSubmit: Boolean,
    onRetake: () -> Unit,
    onIdentify: () -> Unit,
    onSubmit: () -> Unit
) {
    val bitmap = remember(bytes) {
        runCatching {
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "The photo you just took",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(Modifier.padding(20.dp)) {
            if (mode == CameraMode.CHALLENGE && !canSubmit) {
                Text(
                    "⚠ Choose a challenge above before submitting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                VSpace(12)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Retake") }
                Button(
                    onClick = if (mode == CameraMode.CHALLENGE) onSubmit else onIdentify,
                    enabled = canSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (mode == CameraMode.CHALLENGE) "Submit" else "Identify")
                }
            }
        }
    }
}

@Composable
private fun ChallengeVerdictPanel(
    state: CaptureState.ChallengeVerdict,
    onDone: () -> Unit
) {
    val passed = state.submission.state == SubmissionState.PASSED

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (passed) "✅" else "❌", style = MaterialTheme.typography.displayLarge)
        VSpace(16)
        Text(
            if (passed) "Challenge complete" else "Not quite",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        VSpace(12)
        state.submission.explanation?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        state.submission.confidence?.let {
            VSpace(8)
            Text(
                "Confidence: ${(it * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.coinsAwarded > 0) {
            VSpace(20)
            Text(
                "+${state.coinsAwarded} BioCoins",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (!passed) {
            VSpace(16)
            Text(
                "This submission is final for today's challenge, so it can't be replaced.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        VSpace(28)
        Button(
            onClick = onDone,
            modifier = Modifier.defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Done") }
    }
}

@Composable
private fun IdentificationPanel(
    identification: com.example.domain.ai.SpeciesIdentification,
    onAddMoment: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            identification.commonName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (identification.scientificName.isNotBlank()) {
            Text(
                identification.scientificName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VSpace(8)
        Text(
            "${identification.category} · ${identification.confidence}% confidence" +
                when (identification.isNative) {
                    true -> " · native"
                    false -> " · introduced"
                    null -> ""
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        VSpace(16)
        Text(
            identification.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (identification.habitat.isNotBlank()) {
            VSpace(16)
            Text(
                "Habitat",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                identification.habitat,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (identification.interestingFacts.isNotEmpty()) {
            VSpace(16)
            Text(
                "Worth knowing",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            identification.interestingFacts.forEach {
                Text(
                    "• $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Both notices are always rendered, in this order, and neither is dismissible.
        VSpace(20)
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "⚠ Safety",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                VSpace(6)
                Text(
                    identification.safetyNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                VSpace(10)
                Text(
                    identification.uncertaintyNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        VSpace(24)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Done") }
            Button(
                onClick = onAddMoment,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Add as moment") }
        }
        VSpace(8)
        Text(
            "Adding a moment pins this to your current location for other walkers to see.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VSpace(24)
    }
}

@Composable
private fun CameraPermissionExplainer(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📷", style = MaterialTheme.typography.displayLarge)
        VSpace(16)
        Text(
            "Biomate needs the camera",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        VSpace(12)
        Text(
            "The camera is used to identify plants and wildlife, and to complete photo " +
                "challenges. Photos stay on your device unless you choose to share them.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        VSpace(28)
        Button(
            onClick = onGrant,
            modifier = Modifier.defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Allow camera") }
    }
}
