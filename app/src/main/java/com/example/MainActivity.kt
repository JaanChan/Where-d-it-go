package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.screens.ThemeCustomizerDialog
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.buildAccentGradientBrush
import com.example.ui.theme.buildAppBackgroundBrush
import com.example.ui.viewmodel.DataUiState
import com.example.ui.viewmodel.GoogleAuthViewModel
import com.example.ui.viewmodel.MemoryViewModel
import com.example.util.configureAppCheck
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0E0F14")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#161821"))
        )
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = DeepCharcoal
                ) {
                    WhereDidItGoRoot()
                }
            }
        }
    }
}

@Composable
fun WhereDidItGoRoot(
    auth: FirebaseAuth = Firebase.auth,
    authViewModel: GoogleAuthViewModel = viewModel(factory = GoogleAuthViewModel.factory(auth))
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val user = authUiState.currentUser

    if (user == null) {
        AuthScreen(authViewModel = authViewModel)
    } else {
        AuthenticatedAppShell(
            userId = user.uid,
            userName = user.displayName ?: "Memory Explorer",
            userEmail = user.email ?: "",
            userPhotoUrl = user.photoUrl?.toString() ?: "",
            authViewModel = authViewModel
        )
    }
}

@Composable
fun AuthenticatedAppShell(
    userId: String,
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    authViewModel: GoogleAuthViewModel = viewModel(factory = GoogleAuthViewModel.factory())
) {
    val context = LocalContext.current
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
    var showThemeStudio by rememberSaveable { mutableStateOf(false) }

    if (showThemeStudio) {
        ThemeCustomizerDialog(onDismiss = { showThemeStudio = false })
    }

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(buildAppBackgroundBrush())
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.Transparent,
            topBar = {
                AppTopHeaderBar(
                    userName = userProfile?.displayName ?: userName,
                    onOpenThemeStudio = { showThemeStudio = true },
                    onAvatarClick = { currentTab = AppTab.PROFILE }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    Surface(
                        color = ElevatedSlate.copy(alpha = 0.96f),
                        tonalElevation = 10.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
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
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ElevatedSlate.copy(alpha = 0.92f),
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
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, buildAccentGradientBrush()),
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
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                    Text("Dismiss", color = ElectricBlue, fontWeight = FontWeight.Bold)
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
                                onOpenThemeStudio = { showThemeStudio = true },
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
                                    authViewModel.signOut(
                                        context = context,
                                        credentialManager = credentialManager
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

@Composable
private fun AppTopHeaderBar(
    userName: String,
    onOpenThemeStudio: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Surface(
        color = ElevatedSlate.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorderSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(buildAccentGradientBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Where'd It Go?",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI Memory Vault",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftViolet
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Theme & Gradient Customizer Button
                Surface(
                    onClick = onOpenThemeStudio,
                    color = DeepVioletContainer,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, buildAccentGradientBrush()),
                    modifier = Modifier.testTag("open_theme_studio_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(buildAccentGradientBrush())
                        )
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize Theme & Gradients",
                            tint = SoftViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // User Avatar Badge
                val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(buildAccentGradientBrush())
                        .border(1.5.dp, CardBorderSlate, CircleShape)
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
