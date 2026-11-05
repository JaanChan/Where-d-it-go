package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AiSearchTurn
import com.example.data.model.BorrowedItem
import com.example.data.model.MemoryRecord
import com.example.data.model.SearchConfidence
import com.example.data.model.SharedSpace
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepBlueContainer
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.DynamicThemeState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.buildAccentGradientBrush
import com.example.ui.viewmodel.AiTaskState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SAMPLE_QUESTIONS = listOf(
    "Where is my charger?",
    "Where did I put my school ID?",
    "Did I leave my headphones in my bag?",
    "Who borrowed my calculator?"
)

@Composable
fun HomeDashboardScreen(
    userName: String,
    allMemories: List<MemoryRecord>,
    borrowedItems: List<BorrowedItem>,
    sharedSpaces: List<SharedSpace>,
    frequentSearches: List<String>,
    searchHistory: List<AiSearchTurn>,
    aiSearchState: AiTaskState,
    onAskQuestion: (String) -> Unit,
    onClearConversation: () -> Unit,
    onRememberSomethingClick: () -> Unit,
    onClickPhotoQuickAction: () -> Unit = {},
    onOpenThemeStudio: () -> Unit = {},
    onViewAllMemoriesClick: () -> Unit,
    onViewBorrowedClick: () -> Unit,
    onEditMemory: (MemoryRecord) -> Unit,
    onDeleteMemory: (MemoryRecord) -> Unit,
    onMarkBorrowReturned: (BorrowedItem) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var isListeningVoiceSearch by remember { mutableStateOf(false) }

    val speechIntentFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoiceSearch = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                searchQuery = spoken
                onAskQuestion(spoken)
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
                isListeningVoiceSearch = false
            }
            override fun onError(error: Int) {
                isListeningVoiceSearch = false
            }
            override fun onResults(results: Bundle?) {
                isListeningVoiceSearch = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    searchQuery = spoken
                    onAskQuestion(spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    searchQuery = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val startVoiceSearch = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask where you placed an item...")
        }
        if (speechRecognizer != null) {
            isListeningVoiceSearch = true
            speechRecognizer.startListening(intent)
        } else {
            runCatching { speechIntentFallbackLauncher.launch(intent) }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceSearch()
    }

    val activeBorrowed = remember(borrowedItems) {
        borrowedItems.filter { !it.isReturned }
    }
    val frequentlySearchedMemories = remember(allMemories) {
        allMemories.filter { it.searchCount > 0 }.sortedByDescending { it.searchCount }.take(5)
    }
    val spaceMap = remember(sharedSpaces) {
        sharedSpaces.associate { it.id to it.name }
    }

    val firstName = remember(userName) {
        userName.trim().substringBefore(" ").ifBlank { "Friend" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Greeting, Theme Pill & Live Vault Overview Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = DeepVioletContainer,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SoftViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "EXTERNAL MEMORY VAULT",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftViolet,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Active Theme & Gradient Indicator Pill
                    Surface(
                        onClick = onOpenThemeStudio,
                        color = CardSlate,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, buildAccentGradientBrush())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = DynamicThemeState.gradientPreset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Hey $firstName! What are we looking for?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask naturally about any saved item, photo, or borrowed belonging.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Quick Metrics Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatPill(
                        value = allMemories.size.toString(),
                        label = "Memories",
                        accent = SoftViolet,
                        onClick = onViewAllMemoriesClick,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        value = sharedSpaces.size.toString(),
                        label = "Spaces",
                        accent = ElectricBlue,
                        onClick = onViewAllMemoriesClick,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        value = activeBorrowed.size.toString(),
                        label = "Lent Out",
                        accent = if (activeBorrowed.any { it.isOverdue() }) CoralError else MintSuccess,
                        onClick = onViewBorrowedClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Large AI Search Bar & Quick Prompts
        item {
            Surface(
                color = CardSlate,
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.8.dp,
                    brush = buildAccentGradientBrush()
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = if (isListeningVoiceSearch) "Listening to your question..." else "Ask e.g. \"Where did I put my school ID?\"",
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(buildAccentGradientBrush()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val hasMic = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) {
                                            if (isListeningVoiceSearch) {
                                                speechRecognizer?.stopListening()
                                                isListeningVoiceSearch = false
                                            } else {
                                                startVoiceSearch()
                                            }
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.testTag("voice_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice search",
                                        tint = if (isListeningVoiceSearch) CoralError else SoftViolet
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            keyboardController?.hide()
                                            onAskQuestion(searchQuery)
                                        }
                                    },
                                    enabled = searchQuery.isNotBlank(),
                                    modifier = Modifier.testTag("submit_ai_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Ask AI",
                                        tint = if (searchQuery.isNotBlank()) ElectricBlue else TextMuted
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    keyboardController?.hide()
                                    onAskQuestion(searchQuery)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CardBorderSlate,
                            focusedContainerColor = ElevatedSlate,
                            unfocusedContainerColor = ElevatedSlate,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val suggestions = if (frequentSearches.isNotEmpty()) {
                        (frequentSearches + SAMPLE_QUESTIONS).distinct().take(5)
                    } else {
                        SAMPLE_QUESTIONS
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { sample ->
                            Surface(
                                color = ElevatedSlate,
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                                modifier = Modifier.clickable {
                                    searchQuery = sample
                                    keyboardController?.hide()
                                    onAskQuestion(sample)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = SoftViolet,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = sample,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // AI Search Loading or Error state
                    when (aiSearchState) {
                        is AiTaskState.Loading -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ElectricBlue,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = aiSearchState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricBlue
                                )
                            }
                        }
                        is AiTaskState.Error -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = CoralError.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiSearchState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralError,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        AiTaskState.Idle -> {}
                    }

                    // Grounded Conversational Search Results
                    AnimatedVisibility(visible = searchHistory.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Memory Assistant",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onClearConversation) {
                                    Text("Clear Chat", color = TextSecondary)
                                }
                            }

                            searchHistory.takeLast(3).forEach { turn ->
                                AiSearchTurnCard(
                                    turn = turn,
                                    spaceMap = spaceMap,
                                    onEditMemory = onEditMemory,
                                    onDeleteMemory = onDeleteMemory
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Prominent "Remember Something" (Gradient Fill) & "Click Photo of Item" Hero Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(buildAccentGradientBrush())
                        .clickable { onRememberSomethingClick() }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .testTag("remember_something_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Remember Something",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Type, speak, or snap a photo of where you placed an item",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                // Direct Camera Action Button to Click Photos of Things
                Surface(
                    color = DeepBlueContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        buildAccentGradientBrush()
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClickPhotoQuickAction() }
                        .testTag("home_click_photo_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(buildAccentGradientBrush()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Click Photo of Item or Drawer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Open camera to photograph keys, IDs, bags, or shelves",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Open Camera",
                            tint = ElectricBlue
                        )
                    }
                }
            }
        }

        // 4. Borrowed Items Awaiting Return
        if (activeBorrowed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Borrowed Items Awaiting Return (${activeBorrowed.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onViewBorrowedClick) {
                            Text("Manage", color = SoftViolet, fontWeight = FontWeight.Bold)
                        }
                    }

                    activeBorrowed.take(3).forEach { borrowed ->
                        val isOverdue = borrowed.isOverdue()
                        val dateFormat = remember { SimpleDateFormat("MMM d", Locale.US) }
                        Surface(
                            color = CardSlate,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOverdue) CoralError.copy(alpha = 0.6f) else CardBorderSlate
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = borrowed.itemName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isOverdue) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = CoralError.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "OVERDUE",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = CoralError,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Borrowed by ${borrowed.borrowerName} • Due ${dateFormat.format(Date(borrowed.expectedReturnMillis))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                TextButton(onClick = { onMarkBorrowReturned(borrowed) }) {
                                    Text("Returned", color = MintSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Frequently Searched Items
        if (frequentlySearchedMemories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Frequently Searched Belongings",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        frequentlySearchedMemories.forEach { item ->
                            Surface(
                                color = CardSlate,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                                modifier = Modifier.clickable {
                                    onAskQuestion("Where is my ${item.itemName}?")
                                }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = item.itemName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recently Remembered Items & Recent Memory Timeline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Remembered Items (${allMemories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (allMemories.isNotEmpty()) {
                    TextButton(onClick = onViewAllMemoriesClick) {
                        Text("Full Timeline", color = SoftViolet, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (allMemories.isEmpty()) {
            item {
                Surface(
                    color = CardSlate,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, buildAccentGradientBrush()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(buildAccentGradientBrush()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Memories Recorded Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap \"Remember Something\" or \"Click Photo\" to record your keys, school ID, charger, or photograph a drawer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(allMemories.take(6), key = { "${it.spaceId}_${it.id}" }) { memory ->
                MemoryItemCard(
                    memory = memory,
                    sharedSpaceName = spaceMap[memory.spaceId],
                    onEdit = onEditMemory,
                    onDelete = onDeleteMemory
                )
            }
        }
    }
}

@Composable
private fun DashboardStatPill(
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSlate,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AiSearchTurnCard(
    turn: AiSearchTurn,
    spaceMap: Map<String, String>,
    onEditMemory: (MemoryRecord) -> Unit,
    onDeleteMemory: (MemoryRecord) -> Unit
) {
    val badgeColor = when (turn.confidence) {
        SearchConfidence.CONFIRMED -> MintSuccess
        SearchConfidence.HISTORICAL -> AmberWarning
        SearchConfidence.BORROWED -> ElectricBlue
        SearchConfidence.NOT_FOUND -> CoralError
    }
    val badgeLabel = when (turn.confidence) {
        SearchConfidence.CONFIRMED -> "CONFIRMED RECORD"
        SearchConfidence.HISTORICAL -> "HISTORICAL RECORD"
        SearchConfidence.BORROWED -> "LENT / BORROWED"
        SearchConfidence.NOT_FOUND -> "NO RECORD FOUND"
    }

    Surface(
        color = ElevatedSlate,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q: \"${turn.question}\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = SoftViolet,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = turn.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            if (turn.matchedMemories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    turn.matchedMemories.forEach { matched ->
                        MemoryItemCard(
                            memory = matched,
                            sharedSpaceName = spaceMap[matched.spaceId],
                            onEdit = onEditMemory,
                            onDelete = onDeleteMemory
                        )
                    }
                }
            }
        }
    }
}
