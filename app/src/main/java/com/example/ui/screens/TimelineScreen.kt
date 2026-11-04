package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MEMORY_CATEGORIES
import com.example.data.model.MemoryRecord
import com.example.data.model.SharedSpace
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextSecondary

@Composable
fun TimelineScreen(
    allMemories: List<MemoryRecord>,
    sharedSpaces: List<SharedSpace>,
    onEditMemory: (MemoryRecord) -> Unit,
    onDeleteMemory: (MemoryRecord) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    var filterText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedScope by remember { mutableStateOf("ALL") } // ALL, PRIVATE, or spaceId

    val spaceMap = remember(sharedSpaces) {
        sharedSpaces.associate { it.id to it.name }
    }

    val filteredMemories = remember(allMemories, filterText, selectedCategory, selectedScope) {
        allMemories.filter { m ->
            val matchesScope = when (selectedScope) {
                "ALL" -> true
                "PRIVATE" -> m.spaceId.isBlank()
                else -> m.spaceId == selectedScope
            }
            val matchesCat = selectedCategory == "All" || m.category.equals(selectedCategory, ignoreCase = true)
            val q = filterText.trim()
            val matchesQuery = q.isEmpty() ||
                m.itemName.contains(q, ignoreCase = true) ||
                m.location.contains(q, ignoreCase = true) ||
                m.description.contains(q, ignoreCase = true) ||
                m.photoAiDescription.contains(q, ignoreCase = true) ||
                m.previousLocations.any { it.contains(q, ignoreCase = true) }
            matchesScope && matchesCat && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("memory_timeline_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Memory Timeline",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chronological history of your recorded belongings, photos, and previous locations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Filter by item, location, or photo content...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SoftViolet
                    )
                },
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { filterText = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear filter")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_filter_input")
            )
        }

        // Scope filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedScope == "ALL",
                    onClick = { selectedScope = "ALL" },
                    label = { Text("All Spaces (${allMemories.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                        selectedLabelColor = SoftViolet
                    )
                )
                FilterChip(
                    selected = selectedScope == "PRIVATE",
                    onClick = { selectedScope = "PRIVATE" },
                    label = { Text("Private Vault") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                        selectedLabelColor = SoftViolet
                    )
                )
                sharedSpaces.forEach { space ->
                    FilterChip(
                        selected = selectedScope == space.id,
                        onClick = { selectedScope = space.id },
                        label = { Text(space.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = ElectricBlue
                        )
                    )
                }
            }
        }

        // Category filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("All") + MEMORY_CATEGORIES).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                            selectedLabelColor = SoftViolet
                        )
                    )
                }
            }
        }

        if (filteredMemories.isEmpty()) {
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Matching Memories Found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your filter or tap 'Add' to record a new item location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredMemories, key = { "${it.spaceId}_${it.id}" }) { memory ->
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
