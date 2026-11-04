package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MemoryRecord
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BorrowTrackerScreen
import com.example.ui.screens.EditMemoryDialog
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProfileAndSpacesScreen
import com.example.ui.screens.QuickCaptureScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DataUiState
import com.example.ui.viewmodel.MemoryViewModel
import com.example.util.authStateFlow
import com.example.util.configureAppCheck
import com.example.util.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    MEMORIES("Memories", Icons.Filled.History, Icons.Outlined.History, "nav_memories"),
    ADD("Add", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_add"),
    BORROWED("Borrowed", Icons.Filled.Handshake, Icons.Outlined.Handshake, "nav_borrowed"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhereDidItGoRoot()
                }
            }
        }
    }
}

@Composable
fun WhereDidItGoRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        AuthenticatedAppShell(
            userId = user.uid,
            userName = user.displayName ?: "Memory Explorer",
            userEmail = user.email ?: "",
            userPhotoUrl = user.photoUrl?.toString() ?: ""
        )
    }
}

@Composable
fun AuthenticatedAppShell(
    userId: String,
    userName: String,
    userEmail: String,
    userPhotoUrl: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MemoryViewModel = viewModel(
        key = userId,
        factory = MemoryViewModel.factory(
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userPhotoUrl = userPhotoUrl
        )
    )

    val userProfile by viewModel.userProfileState.collectAsStateWithLifecycle()
    val allMemories by viewModel.combinedMemoriesState.collectAsStateWithLifecycle()
    val borrowedState by viewModel.borrowedItemsState.collectAsStateWithLifecycle()
    val sharedSpaces by viewModel.sharedSpacesState.collectAsStateWithLifecycle()
    val pendingInvites by viewModel.pendingInvitedSpacesState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val aiSearchState by viewModel.aiSearchState.collectAsStateWithLifecycle()
    val aiExtractionState by viewModel.aiExtractionState.collectAsStateWithLifecycle()
    val extractedDraft by viewModel.extractedDraft.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val borrowedItems = (borrowedState as? DataUiState.Success)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var openCameraInitially by rememberSaveable { mutableStateOf(false) }
    var memoryBeingEdited by remember { mutableStateOf<MemoryRecord?>(null) }

    if (memoryBeingEdited != null) {
        val target = memoryBeingEdited!!
        EditMemoryDialog(
            memory = target,
            onDismiss = { memoryBeingEdited = null },
            onConfirm = { newName, newLoc, newCat, newDesc, newPhotoBmp ->
                viewModel.updateExistingMemory(
                    memory = target,
                    newItemName = newName,
                    newLocation = newLoc,
                    newCategory = newCat,
                    newDescription = newDesc,
                    newPhotoBitmap = newPhotoBmp,
                    onComplete = { memoryBeingEdited = null }
                )
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = DeepCharcoal,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ElevatedSlate,
                        tonalElevation = 8.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SoftViolet,
                                    selectedTextColor = SoftViolet,
                                    indicatorColor = DeepVioletContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ElevatedSlate,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SoftViolet,
                                    selectedTextColor = SoftViolet,
                                    indicatorColor = DeepVioletContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(CardBorderSlate)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
                        Surface(
                            color = DeepVioletContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusBanner ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                    Text("Dismiss", color = ElectricBlue)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            AppTab.HOME -> HomeDashboardScreen(
                                userName = userProfile?.displayName ?: userName,
                                allMemories = allMemories,
                                borrowedItems = borrowedItems,
                                sharedSpaces = sharedSpaces,
                                frequentSearches = userProfile?.frequentSearches ?: emptyList(),
                                searchHistory = searchHistory,
                                aiSearchState = aiSearchState,
                                onAskQuestion = { viewModel.askWhereIsIt(it) },
                                onClearConversation = { viewModel.clearSearchConversation() },
                                onRememberSomethingClick = {
                                    openCameraInitially = false
                                    currentTab = AppTab.ADD
                                },
                                onClickPhotoQuickAction = {
                                    openCameraInitially = true
                                    currentTab = AppTab.ADD
                                },
                                onViewAllMemoriesClick = { currentTab = AppTab.MEMORIES },
                                onViewBorrowedClick = { currentTab = AppTab.BORROWED },
                                onEditMemory = { memoryBeingEdited = it },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onMarkBorrowReturned = { viewModel.toggleBorrowedItemReturned(it) }
                            )
                            AppTab.MEMORIES -> TimelineScreen(
                                allMemories = allMemories,
                                sharedSpaces = sharedSpaces,
                                onEditMemory = { memoryBeingEdited = it },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onNavigateBack = { currentTab = AppTab.HOME }
                            )
                            AppTab.ADD -> QuickCaptureScreen(
                                sharedSpaces = sharedSpaces,
                                extractedDraft = extractedDraft,
                                aiExtractionState = aiExtractionState,
                                openCameraInitially = openCameraInitially,
                                onCameraInitialConsumed = { openCameraInitially = false },
                                onRunAiExtraction = { text, bmp ->
                                    viewModel.runAiMemoryExtraction(text, bmp)
                                },
                                onSaveMemory = { item, loc, cat, desc, bmp, photoDesc, spaceId ->
                                    viewModel.saveMemoryRecord(
                                        itemName = item,
                                        location = loc,
                                        category = cat,
                                        description = desc,
                                        photoBitmap = bmp,
                                        photoAiDescription = photoDesc,
                                        targetSpaceId = spaceId,
                                        onComplete = { currentTab = AppTab.HOME }
                                    )
                                },
                                onNavigateBack = { currentTab = AppTab.HOME }
                            )
                            AppTab.BORROWED -> BorrowTrackerScreen(
                                borrowedItems = borrowedItems,
                                onAddBorrowedItem = { item, person, contact, notes, bMillis, dueMillis ->
                                    viewModel.addBorrowedItem(
                                        itemName = item,
                                        borrowerName = person,
                                        borrowerContact = contact,
                                        notes = notes,
                                        borrowDateMillis = bMillis,
                                        expectedReturnMillis = dueMillis
                                    )
                                },
                                onToggleReturned = { viewModel.toggleBorrowedItemReturned(it) },
                                onDeleteBorrowedItem = { viewModel.deleteBorrowedItem(it) },
                                onShowStatus = { msg ->
                                    viewModel.showStatusMessage(msg)
                                },
                                onNavigateBack = { currentTab = AppTab.HOME }
                            )
                            AppTab.PROFILE -> ProfileAndSpacesScreen(
                                userName = userProfile?.displayName ?: userName,
                                userEmail = userEmail,
                                userProfile = userProfile,
                                sharedSpaces = sharedSpaces,
                                pendingInvites = pendingInvites,
                                totalMemoriesCount = allMemories.size,
                                activeBorrowedCount = borrowedItems.count { !it.isReturned },
                                onCreateSpace = { name, email ->
                                    viewModel.createSharedSpace(name, email)
                                },
                                onJoinByCode = { code ->
                                    viewModel.joinSpaceByCode(code)
                                },
                                onInviteEmail = { space, email ->
                                    viewModel.inviteEmailToSpace(space, email)
                                },
                                onAcceptEmailInvite = { space ->
                                    viewModel.acceptEmailInvite(space)
                                },
                                onUpdateNotificationPrefs = { borrow, daily, shared ->
                                    viewModel.updateNotificationPrefs(borrow, daily, shared)
                                },
                                onShowStatus = { msg ->
                                    viewModel.showStatusMessage(msg)
                                },
                                onSignOut = {
                                    signOutUser(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {},
                                        scope = coroutineScope
                                    )
                                },
                                onNavigateBack = { currentTab = AppTab.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
