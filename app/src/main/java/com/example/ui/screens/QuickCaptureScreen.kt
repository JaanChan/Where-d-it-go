package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ExtractedMemoryDraft
import com.example.data.model.MEMORY_CATEGORIES
import com.example.data.model.SharedSpace
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepBlueContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiTaskState
import com.example.util.MediaHelper
import java.util.Locale

@Composable
fun QuickCaptureScreen(
    sharedSpaces: List<SharedSpace>,
    extractedDraft: ExtractedMemoryDraft?,
    aiExtractionState: AiTaskState,
    openCameraInitially: Boolean = false,
    onCameraInitialConsumed: () -> Unit = {},
    onRunAiExtraction: (rawText: String, bitmap: Bitmap?) -> Unit,
    onSaveMemory: (
        itemName: String,
        location: String,
        category: String,
        description: String,
        photoBitmap: Bitmap?,
        photoAiDescription: String,
        targetSpaceId: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    var naturalSentence by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var description by remember { mutableStateOf("") }
    var photoAiDescription by remember { mutableStateOf("") }
    var selectedSpaceId by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var showCameraModal by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Populate structured fields when AI extraction succeeds
    LaunchedEffect(extractedDraft) {
        if (extractedDraft != null) {
            if (extractedDraft.itemName.isNotBlank()) itemName = extractedDraft.itemName
            if (extractedDraft.location.isNotBlank()) location = extractedDraft.location
            if (extractedDraft.category.isNotBlank()) category = extractedDraft.category
            if (extractedDraft.description.isNotBlank()) description = extractedDraft.description
            if (extractedDraft.photoAiDescription.isNotBlank()) photoAiDescription = extractedDraft.photoAiDescription
            validationError = null
        }
    }

    // Fallback system speech recognizer activity launcher
    val speechIntentFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                naturalSentence = spoken
                onRunAiExtraction(spoken, attachedBitmap)
            }
        }
    }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningVoice = false
            }
            override fun onError(error: Int) {
                isListeningVoice = false
            }
            override fun onResults(results: Bundle?) {
                isListeningVoice = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    naturalSentence = spoken
                    onRunAiExtraction(spoken, attachedBitmap)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    naturalSentence = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val startVoiceCapture = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak where you placed the item...")
        }
        if (speechRecognizer != null) {
            isListeningVoice = true
            speechRecognizer.startListening(intent)
        } else {
            runCatching { speechIntentFallbackLauncher.launch(intent) }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceCapture()
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = MediaHelper.loadAndCompressBitmap(context, uri)
            if (bmp != null) {
                attachedBitmap = bmp
                onRunAiExtraction(naturalSentence, bmp)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraModal = true
        }
    }

    val triggerCameraOpen = {
        val hasCam = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            showCameraModal = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(openCameraInitially) {
        if (openCameraInitially) {
            onCameraInitialConsumed()
            triggerCameraOpen()
        }
    }

    if (showCameraModal) {
        CameraCaptureDialog(
            onPhotoCaptured = { bmp ->
                attachedBitmap = bmp
                showCameraModal = false
                // Auto-analyze photo with Gemini AI to detect visible items and storage context
                onRunAiExtraction(naturalSentence, bmp)
            },
            onDismiss = { showCameraModal = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("quick_capture_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Remember Something",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Click a photo of your item, drawer, bag, or shelf—or speak/type where you put it. Gemini AI extracts the item and location automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        // Prominent Camera Action Banner to Click Photos of Things
        Surface(
            color = DeepBlueContainer,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(ElectricBlue, ElectricViolet))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Click Photo of Item or Drawer",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Snap keys, IDs, desks, shelves, or bags for instant AI memory",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = triggerCameraOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color(0xFF0B0D13)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("hero_click_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Click Photo", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Natural Language + Voice + Photo Capture Box
        Surface(
            color = CardSlate,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Natural Capture",
                    style = MaterialTheme.typography.titleSmall,
                    color = SoftViolet,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = naturalSentence,
                    onValueChange = { naturalSentence = it },
                    placeholder = {
                        Text(
                            if (isListeningVoice) "Listening... Speak where you placed the item"
                            else "Example: \"I put my black headphones inside my blue school bag.\""
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("natural_memory_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                if (isListeningVoice) {
                                    speechRecognizer?.stopListening()
                                    isListeningVoice = false
                                } else {
                                    startVoiceCapture()
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListeningVoice) CoralError else SoftViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListeningVoice) "Stop" else "Voice",
                            color = if (isListeningVoice) CoralError else Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = triggerCameraOpen,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("camera_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Click Photo",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gallery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick Photo",
                            tint = MintSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", color = Color.White)
                    }
                }

                // Attached Photo Preview with Retake & Remove
                if (attachedBitmap != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                bitmap = attachedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured memory photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, ElectricBlue.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = triggerCameraOpen,
                                    modifier = Modifier
                                        .background(ElevatedSlate.copy(alpha = 0.88f), RoundedCornerShape(50))
                                        .testTag("retake_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retake photo",
                                        tint = ElectricBlue
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        attachedBitmap = null
                                        photoAiDescription = ""
                                    },
                                    modifier = Modifier
                                        .background(ElevatedSlate.copy(alpha = 0.88f), RoundedCornerShape(50))
                                        .testTag("remove_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Photo attached • Gemini AI analyzes visible items & storage details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintSuccess
                        )
                    }
                }

                Button(
                    onClick = { onRunAiExtraction(naturalSentence, attachedBitmap) },
                    enabled = (naturalSentence.isNotBlank() || attachedBitmap != null) && aiExtractionState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color(0xFF0B0D13)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("extract_with_ai_button")
                ) {
                    if (aiExtractionState is AiTaskState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF0B0D13),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiExtractionState.statusMessage,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extract Item & Location with AI",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (aiExtractionState is AiTaskState.Error) {
                    Text(
                        text = aiExtractionState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError
                    )
                }
            }
        }

        // 2. Structured Item & Location Details
        Surface(
            color = CardSlate,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Memory Details",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        validationError = null
                    },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Black headphones, School ID, Car keys") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_item_name_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        validationError = null
                    },
                    label = { Text("Where is it located? *") },
                    placeholder = { Text("e.g. Blue school bag, Second drawer") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_location_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MEMORY_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                selectedLabelColor = SoftViolet
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Optional Notes / Context") },
                    placeholder = { Text("Any helpful details or landmarks...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_notes_input")
                )

                AnimatedVisibility(visible = photoAiDescription.isNotBlank()) {
                    OutlinedTextField(
                        value = photoAiDescription,
                        onValueChange = { photoAiDescription = it },
                        label = { Text("AI Photo Visual Inventory (Searchable)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Target Memory Space Selector (Private vs Shared Spaces)
                Text(
                    text = "Save To Memory Space",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSpaceId.isEmpty(),
                        onClick = { selectedSpaceId = "" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("My Private Vault") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                            selectedLabelColor = SoftViolet
                        )
                    )
                    sharedSpaces.forEach { space ->
                        FilterChip(
                            selected = selectedSpaceId == space.id,
                            onClick = { selectedSpaceId = space.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(space.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                                selectedLabelColor = ElectricBlue
                            )
                        )
                    }
                }

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError
                    )
                }

                Button(
                    onClick = {
                        if (itemName.isBlank() || location.isBlank()) {
                            validationError = "Please enter both an item name and its location (or click 'Extract Item & Location with AI' above)."
                        } else {
                            onSaveMemory(
                                itemName,
                                location,
                                category,
                                description.ifBlank { naturalSentence },
                                attachedBitmap,
                                photoAiDescription,
                                selectedSpaceId
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
