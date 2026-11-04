package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SharedSpace
import com.example.data.model.UserProfile
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepBlueContainer
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary
import com.example.util.MemoryNotificationHelper

@Composable
fun ProfileAndSpacesScreen(
    userName: String,
    userEmail: String,
    userProfile: UserProfile?,
    sharedSpaces: List<SharedSpace>,
    pendingInvites: List<SharedSpace>,
    totalMemoriesCount: Int,
    activeBorrowedCount: Int,
    onCreateSpace: (name: String, initialEmail: String) -> Unit,
    onJoinByCode: (inviteCode: String) -> Unit,
    onInviteEmail: (space: SharedSpace, email: String) -> Unit,
    onAcceptEmailInvite: (SharedSpace) -> Unit,
    onUpdateNotificationPrefs: (borrowReminders: Boolean, dailySummary: Boolean, sharedUpdates: Boolean) -> Unit,
    onShowStatus: (String) -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var spaceForEmailInvite by remember { mutableStateOf<SharedSpace?>(null) }

    val borrowReminders = userProfile?.borrowRemindersEnabled ?: true
    val dailySummary = userProfile?.dailySummaryEnabled ?: true
    val sharedUpdates = userProfile?.sharedUpdatesEnabled ?: true

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MemoryNotificationHelper.sendDailySummaryNotification(
                context,
                totalMemoriesCount,
                activeBorrowedCount
            )
            onShowStatus("Daily memory summary notification sent!")
        }
    }

    if (showCreateSpaceDialog) {
        var spaceName by remember { mutableStateOf("") }
        var inviteEmail by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateSpaceDialog = false },
            containerColor = ElevatedSlate,
            title = { Text("Create Shared Memory Space", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Invite family members or roommates via a 6-character code or their email address.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = spaceName,
                        onValueChange = { spaceName = it },
                        label = { Text("Space Name *") },
                        placeholder = { Text("e.g. Apt 4B Roommates, Family Home") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_space_name_input")
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Invite Member Email (Optional)") },
                        placeholder = { Text("roommate@gmail.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_space_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (spaceName.isNotBlank()) {
                            onCreateSpace(spaceName, inviteEmail)
                            showCreateSpaceDialog = false
                        }
                    },
                    enabled = spaceName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier.testTag("confirm_create_space_button")
                ) {
                    Text("Create Space")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSpaceDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showJoinByCodeDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinByCodeDialog = false },
            containerColor = ElevatedSlate,
            title = { Text("Join Space via Invite Code", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the 6-character invite code shared by your family member or roommate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("6-Character Invite Code") },
                        placeholder = { Text("e.g. HOME99") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_space_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            onJoinByCode(code)
                            showJoinByCodeDialog = false
                        }
                    },
                    enabled = code.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color(0xFF0B0D13)),
                    modifier = Modifier.testTag("confirm_join_space_button")
                ) {
                    Text("Join Space", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinByCodeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (spaceForEmailInvite != null) {
        var email by remember { mutableStateOf("") }
        val target = spaceForEmailInvite!!
        AlertDialog(
            onDismissRequest = { spaceForEmailInvite = null },
            containerColor = ElevatedSlate,
            title = { Text("Invite to ${target.name}", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Member Email Address") },
                    placeholder = { Text("family@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.contains("@")) {
                            onInviteEmail(target, email)
                            spaceForEmailInvite = null
                        }
                    },
                    enabled = email.contains("@"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { spaceForEmailInvite = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_and_spaces_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. User Profile Card
        item {
            Surface(
                color = CardSlate,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DeepVioletContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SoftViolet,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userName.ifBlank { "Memory Explorer" },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = CoralError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", color = CoralError)
                    }
                }
            }
        }

        // 2. Shared Memory Spaces Header & Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Shared Household Memory Spaces",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Share household items with family or roommates using a 6-character invite code or email lookup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCreateSpaceDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("create_shared_space_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Space", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showJoinByCodeDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("join_shared_space_button")
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join with Code", color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Pending Email Invitations
        if (pendingInvites.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pending Email Invitations (${pendingInvites.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MintSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    pendingInvites.forEach { invitedSpace ->
                        Surface(
                            color = CardSlate,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintSuccess.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = invitedSpace.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Invited by ${invitedSpace.ownerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { onAcceptEmailInvite(invitedSpace) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintSuccess,
                                        contentColor = Color(0xFF0B0D13)
                                    )
                                ) {
                                    Text("Accept", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Shared Spaces List
        if (sharedSpaces.isEmpty()) {
            item {
                Surface(
                    color = CardSlate,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Shared Spaces Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a household space or enter an invite code above to collaborate on shared items.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(sharedSpaces, key = { it.id }) { space ->
                Surface(
                    color = CardSlate,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = space.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Members (${space.memberNames.size}): ${space.memberNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Surface(
                                color = DeepBlueContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Code: ${space.inviteCode}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(space.inviteCode))
                                            onShowStatus("Copied invite code ${space.inviteCode}")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy invite code",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (space.invitedEmails.isNotEmpty()) {
                            Text(
                                text = "Invited emails: ${space.invitedEmails.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftViolet
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { spaceForEmailInvite = space },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = null,
                                    tint = SoftViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Invite by Email", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. Notification Preferences
        item {
            Surface(
                color = CardSlate,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    PreferenceSwitchRow(
                        title = "Borrowed-Item Return Reminders",
                        subtitle = "Alert when lent items reach or pass their expected return date",
                        checked = borrowReminders,
                        onCheckedChange = {
                            onUpdateNotificationPrefs(it, dailySummary, sharedUpdates)
                        }
                    )

                    PreferenceSwitchRow(
                        title = "Daily Memory Summary",
                        subtitle = "Receive a helpful digest of recently recorded items and active borrows",
                        checked = dailySummary,
                        onCheckedChange = {
                            onUpdateNotificationPrefs(borrowReminders, it, sharedUpdates)
                        }
                    )

                    PreferenceSwitchRow(
                        title = "Shared-Space Updates",
                        subtitle = "Notify when family members or roommates add or move shared items",
                        checked = sharedUpdates,
                        onCheckedChange = {
                            onUpdateNotificationPrefs(borrowReminders, dailySummary, it)
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            if (MemoryNotificationHelper.hasNotificationPermission(context)) {
                                MemoryNotificationHelper.sendDailySummaryNotification(
                                    context,
                                    totalMemoriesCount,
                                    activeBorrowedCount
                                )
                                onShowStatus("Sent daily memory summary notification!")
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_summary_notification_button")
                    ) {
                        Text("Send Memory Summary Notification Now", color = ElectricBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricViolet
            )
        )
    }
}
