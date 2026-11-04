package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BorrowedItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.util.MemoryNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BorrowTrackerScreen(
    borrowedItems: List<BorrowedItem>,
    onAddBorrowedItem: (
        itemName: String,
        borrowerName: String,
        borrowerContact: String,
        notes: String,
        borrowDateMillis: Long,
        expectedReturnMillis: Long
    ) -> Unit,
    onToggleReturned: (BorrowedItem) -> Unit,
    onDeleteBorrowedItem: (BorrowedItem) -> Unit,
    onShowStatus: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var filterTab by remember { mutableStateOf("ACTIVE") } // ACTIVE, OVERDUE, RETURNED
    var pendingNotificationItem by remember { mutableStateOf<BorrowedItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val item = pendingNotificationItem
        if (granted && item != null) {
            MemoryNotificationHelper.sendOverdueBorrowedNotification(context, item)
            onShowStatus("Sent return reminder notification for ${item.itemName}")
        }
        pendingNotificationItem = null
    }

    val activeCount = remember(borrowedItems) { borrowedItems.count { !it.isReturned } }
    val overdueCount = remember(borrowedItems) { borrowedItems.count { it.isOverdue() } }
    val returnedCount = remember(borrowedItems) { borrowedItems.count { it.isReturned } }

    val displayedItems = remember(borrowedItems, filterTab) {
        when (filterTab) {
            "ACTIVE" -> borrowedItems.filter { !it.isReturned }
            "OVERDUE" -> borrowedItems.filter { it.isOverdue() }
            "RETURNED" -> borrowedItems.filter { it.isReturned }
            else -> borrowedItems
        }
    }

    if (showAddDialog) {
        AddBorrowedItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { itemName, borrower, contact, notes, dueDays ->
                val now = System.currentTimeMillis()
                val dueMillis = now + dueDays * 24L * 60L * 60L * 1000L
                onAddBorrowedItem(itemName, borrower, contact, notes, now, dueMillis)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("borrow_tracker_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Borrow Tracker",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keep track of belongings lent to friends, classmates, or roommates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("open_add_borrow_dialog_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lend Item")
                }
            }
        }

        // Summary Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChipCard(
                    title = "Active Lent",
                    count = activeCount,
                    accent = ElectricBlue,
                    selected = filterTab == "ACTIVE",
                    onClick = { filterTab = "ACTIVE" },
                    modifier = Modifier.weight(1f)
                )
                StatChipCard(
                    title = "Overdue",
                    count = overdueCount,
                    accent = CoralError,
                    selected = filterTab == "OVERDUE",
                    onClick = { filterTab = "OVERDUE" },
                    modifier = Modifier.weight(1f)
                )
                StatChipCard(
                    title = "Returned",
                    count = returnedCount,
                    accent = MintSuccess,
                    selected = filterTab == "RETURNED",
                    onClick = { filterTab = "RETURNED" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (displayedItems.isEmpty()) {
            item {
                Surface(
                    color = CardSlate,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Borrowed Items in This View",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Lend Item' whenever someone borrows your calculator, charger, book, or tools.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(displayedItems, key = { it.id }) { item ->
                BorrowedItemCard(
                    item = item,
                    onToggleReturned = { onToggleReturned(item) },
                    onSendReminder = {
                        if (MemoryNotificationHelper.hasNotificationPermission(context)) {
                            MemoryNotificationHelper.sendOverdueBorrowedNotification(context, item)
                            onShowStatus("Reminder notification sent for ${item.itemName}")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingNotificationItem = item
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onDelete = { onDeleteBorrowedItem(item) }
                )
            }
        }
    }
}

@Composable
private fun StatChipCard(
    title: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.16f) else CardSlate,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accent else CardBorderSlate
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun BorrowedItemCard(
    item: BorrowedItem,
    onToggleReturned: () -> Unit,
    onSendReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val isOverdue = item.isOverdue()

    Surface(
        color = CardSlate,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOverdue) CoralError.copy(alpha = 0.65f) else CardBorderSlate
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("borrowed_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val statusColor = when {
                            item.isReturned -> MintSuccess
                            isOverdue -> CoralError
                            else -> AmberWarning
                        }
                        val statusLabel = when {
                            item.isReturned -> "RETURNED"
                            isOverdue -> "OVERDUE"
                            else -> "BORROWED"
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Borrower: ${item.borrowerName}" +
                            if (item.borrowerContact.isNotBlank()) " (${item.borrowerContact})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricBlue
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete borrow record",
                        tint = CoralError.copy(alpha = 0.85f)
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lent: ${dateFormat.format(Date(item.borrowDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Expected Return: ${dateFormat.format(Date(item.expectedReturnMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) CoralError else TextSecondary,
                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleReturned,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isReturned) ElevatedSlate else MintSuccess,
                        contentColor = if (item.isReturned) TextSecondary else Color(0xFF0B0D13)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_returned_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.isReturned) "Mark Unreturned" else "Mark Returned",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!item.isReturned) {
                    OutlinedButton(
                        onClick = onSendReminder,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("remind_borrow_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Send Reminder",
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remind", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBorrowedItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (itemName: String, borrowerName: String, contact: String, notes: String, dueDays: Int) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var borrowerName by remember { mutableStateOf("") }
    var borrowerContact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueInDays by remember { mutableIntStateOf(7) }

    val dayPresets = listOf(
        -1 to "Overdue (Test)",
        1 to "Tomorrow",
        3 to "3 Days",
        7 to "1 Week",
        14 to "2 Weeks"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSlate,
        title = {
            Text(
                text = "Record Lent Item",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Lent *") },
                    placeholder = { Text("e.g. Graphing Calculator, USB-C Charger") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrow_item_name_input")
                )
                OutlinedTextField(
                    value = borrowerName,
                    onValueChange = { borrowerName = it },
                    label = { Text("Who Borrowed It? *") },
                    placeholder = { Text("e.g. Jordan, Roommate Alex") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrow_person_input")
                )
                OutlinedTextField(
                    value = borrowerContact,
                    onValueChange = { borrowerContact = it },
                    label = { Text("Borrower Contact (Optional)") },
                    placeholder = { Text("Phone or email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Expected Return",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayPresets.forEach { (days, label) ->
                        FilterChip(
                            selected = dueInDays == days,
                            onClick = { dueInDays = days },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                selectedLabelColor = SoftViolet
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank() && borrowerName.isNotBlank()) {
                        onConfirm(itemName, borrowerName, borrowerContact, notes, dueInDays)
                    }
                },
                enabled = itemName.isNotBlank() && borrowerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("save_borrowed_item_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
