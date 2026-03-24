// Camera-based pantry scanning screen: captures photos, runs on-device detection, and lets users add detected items.
package com.example.pantrychef.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.core.CandidateItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPantryScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddItems: (List<CandidateItem>) -> Unit
) {
    val candidates by viewModel.candidates.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCamera) requestPermission.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Pantry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasCamera) {
                    Button(
                        onClick = {
                            imageCapture?.let { ic ->
                                val file = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
                                val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                                ic.takePicture(
                                    opts,
                                    mainExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val uri: Uri = output.savedUri ?: Uri.fromFile(file)
                                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                            viewModel.handleCapturedUri(uri, bmp)
                                        }
                                        override fun onError(ex: ImageCaptureException) {}
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Capture") }
                } else {
                    OutlinedButton(
                        onClick = { requestPermission.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Enable Camera") }
                }

                OutlinedButton(
                    onClick = { viewModel.clear() },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }

                Button(
                    onClick = { onAddItems(candidates) },
                    enabled = candidates.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Add selected") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCamera) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()

                                val preview = Preview.Builder().build().apply {
                                    setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, selector, preview, capture
                                    )
                                    imageCapture = capture
                                } catch (_: Exception) {}
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Camera permission needed to preview.")
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (candidates.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Point at your shelf and tap Capture.\nDetected items will appear here.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(candidates, key = { it.name.lowercase() }) { item ->
                            CandidateRow(
                                item = item,
                                onInc = { viewModel.increment(item.name) },
                                onDec = { viewModel.decrement(item.name) },
                                onUnit = { viewModel.setUnit(item.name, it) },
                                onRemove = { viewModel.removeCandidate(item.name) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(10f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Looking for yummy food",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    item: CandidateItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onUnit: (String) -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(item.confidence * 100).toInt()}% confidence",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(onClick = onDec) { Text("−") }
                Text(
                    text = "x${item.count}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                FilledTonalButton(onClick = onInc) { Text("+") }

                Spacer(Modifier.width(16.dp))
                var unitText by remember { mutableStateOf(TextFieldValue(item.unit)) }
                OutlinedTextField(
                    value = unitText,
                    onValueChange = {
                        unitText = it
                        onUnit(it.text.trim())
                    },
                    singleLine = true,
                    label = { Text("Unit") },
                    modifier = Modifier
                        .widthIn(min = 100.dp)
                        .weight(1f, fill = false)
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text(
                text = "Source: ${item.source.name.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
