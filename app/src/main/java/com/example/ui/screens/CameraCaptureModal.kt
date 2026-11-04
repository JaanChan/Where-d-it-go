package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary
import com.example.util.MediaHelper

private const val TAG = "CameraCaptureDialog"

@Composable
fun CameraCaptureDialog(
    onPhotoCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isTakingPicture by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var pendingSystemCameraUri by remember { mutableStateOf<Uri?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val imageCapture = remember(flashMode) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
    }

    // Fallback 1: System camera thumbnail preview intent
    val systemPreviewCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            onPhotoCaptured(MediaHelper.scaleBitmap(bmp, 720))
        }
    }

    // Fallback 2: Full-resolution system camera via FileProvider
    val systemFullResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingSystemCameraUri
        if (success && uri != null) {
            val bmp = MediaHelper.loadAndCompressBitmap(context, uri, 720)
            if (bmp != null) {
                onPhotoCaptured(bmp)
            } else {
                cameraError = "Could not load captured photo. Try clicking the shutter button or picking from Gallery."
            }
        }
    }

    // Fallback 3: Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = MediaHelper.loadAndCompressBitmap(context, uri, 720)
            if (bmp != null) {
                onPhotoCaptured(bmp)
            }
        }
    }

    LaunchedEffect(lensFacing, flashMode) {
        cameraError = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                if (cameraProvider.hasCamera(selector)) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageCapture
                    )
                } else {
                    cameraError = "No hardware camera stream detected on this device. Use 'System Camera' or 'Gallery' below."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind CameraX use cases", e)
                cameraError = "Camera preview unavailable (${e.localizedMessage ?: "error"}). You can still use System Camera or Gallery below."
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_capture_modal"),
            color = DeepCharcoal
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElevatedSlate)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Click Photo of Item or Location",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gemini AI auto-detects items & storage context",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier.testTag("camera_flash_toggle")
                        ) {
                            val flashIcon = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            }
                            Icon(
                                imageVector = flashIcon,
                                contentDescription = "Toggle Flash",
                                tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) TextSecondary else SoftViolet
                            )
                        }

                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier.testTag("camera_switch_lens_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera Lens",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_camera_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Camera",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Live CameraX Viewfinder Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, CardBorderSlate, RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Framing Reticle Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.76f)
                            .border(
                                width = 1.5.dp,
                                color = ElectricBlue.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(20.dp)
                            )
                    )

                    Surface(
                        color = ElevatedSlate.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Point at your item, drawer, shelf, desk, or bag",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    if (!cameraError.isNullOrBlank()) {
                        Surface(
                            color = CardSlate.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = cameraError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Bottom Shutter & Fallback Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElevatedSlate)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Big Shutter Button
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .border(4.dp, ElectricBlue, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isTakingPicture) SoftViolet else ElectricViolet)
                            .clickable(enabled = !isTakingPicture) {
                                isTakingPicture = true
                                cameraError = null
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = MediaHelper.imageProxyToBitmap(image, 720)
                                            image.close()
                                            isTakingPicture = false
                                            if (bmp != null) {
                                                onPhotoCaptured(bmp)
                                            } else {
                                                cameraError = "Failed to decode camera frame. Try System Camera below."
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(TAG, "ImageCapture failed", exception)
                                            isTakingPicture = false
                                            // Automatically fall back to system camera preview if CameraX capture fails
                                            runCatching {
                                                systemPreviewCameraLauncher.launch(null)
                                            }.onFailure {
                                                cameraError = "Could not capture photo (${exception.localizedMessage}). Try Gallery or System Camera."
                                            }
                                        }
                                    }
                                )
                            }
                            .testTag("click_photo_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTakingPicture) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Click Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isTakingPicture) "Capturing photo..." else "Tap shutter to click photo",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val uri = MediaHelper.createTempImageUri(context)
                                if (uri != null) {
                                    pendingSystemCameraUri = uri
                                    runCatching {
                                        systemFullResCameraLauncher.launch(uri)
                                    }.onFailure {
                                        systemPreviewCameraLauncher.launch(null)
                                    }
                                } else {
                                    systemPreviewCameraLauncher.launch(null)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("system_camera_fallback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("System Camera", color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("camera_modal_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MintSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Gallery", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
