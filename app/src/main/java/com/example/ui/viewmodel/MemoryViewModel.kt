package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.MemoryAiService
import com.example.data.model.AiSearchTurn
import com.example.data.model.BorrowedItem
import com.example.data.model.ExtractedMemoryDraft
import com.example.data.model.MemoryRecord
import com.example.data.model.SharedSpace
import com.example.data.model.UserProfile
import com.example.data.repository.MemoryRepository
import com.example.util.MediaHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MemoryViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Loading(val statusMessage: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(
    private val repository: MemoryRepository,
    private val aiService: MemoryAiService,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String,
    val currentUserPhotoUrl: String
) : ViewModel() {

    val userProfileState: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val privateMemoriesState: StateFlow<DataUiState<List<MemoryRecord>>> = repository.observePrivateMemories(currentUserId)
        .map<List<MemoryRecord>, DataUiState<List<MemoryRecord>>> { DataUiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing private memories", e)
            emit(DataUiState.Error(e.localizedMessage ?: "Failed to load memories"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val borrowedItemsState: StateFlow<DataUiState<List<BorrowedItem>>> = repository.observeBorrowedItems(currentUserId)
        .map<List<BorrowedItem>, DataUiState<List<BorrowedItem>>> { DataUiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing borrowed items", e)
            emit(DataUiState.Error(e.localizedMessage ?: "Failed to load borrowed items"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val sharedSpacesState: StateFlow<List<SharedSpace>> = repository.observeSharedSpaces(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing shared spaces", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingInvitedSpacesState: StateFlow<List<SharedSpace>> = repository.observeInvitedSharedSpaces(currentUserEmail)
        .combine(sharedSpacesState) { invited, joined ->
            val joinedIds = joined.map { it.id }.toSet()
            invited.filter { it.id !in joinedIds && currentUserId !in it.memberUids }
        }
        .catch { e ->
            Log.w(TAG, "Error observing invited shared spaces", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allSharedMemoriesState: StateFlow<List<MemoryRecord>> = sharedSpacesState
        .flatMapLatest { spaces ->
            if (spaces.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = spaces.map { space ->
                    repository.observeSharedSpaceMemories(space.id, currentUserId)
                        .catch { emit(emptyList()) }
                }
                combine(flows) { lists ->
                    lists.flatMap { it }.sortedByDescending { (it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Combined memories (Private + Shared Spaces) for AI Search & Timeline
    val combinedMemoriesState: StateFlow<List<MemoryRecord>> = combine(
        privateMemoriesState,
        allSharedMemoriesState
    ) { privateState, sharedList ->
        val privateList = (privateState as? DataUiState.Success)?.data ?: emptyList()
        (privateList + sharedList).sortedByDescending { (it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _searchHistory = MutableStateFlow<List<AiSearchTurn>>(emptyList())
    val searchHistory: StateFlow<List<AiSearchTurn>> = _searchHistory.asStateFlow()

    private val _aiSearchState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiSearchState: StateFlow<AiTaskState> = _aiSearchState.asStateFlow()

    private val _aiExtractionState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiExtractionState: StateFlow<AiTaskState> = _aiExtractionState.asStateFlow()

    private val _extractedDraft = MutableStateFlow<ExtractedMemoryDraft?>(null)
    val extractedDraft: StateFlow<ExtractedMemoryDraft?> = _extractedDraft.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = currentUserName,
                email = currentUserEmail,
                photoUrl = currentUserPhotoUrl
            )
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun clearExtractedDraft() {
        _extractedDraft.value = null
        _aiExtractionState.value = AiTaskState.Idle
    }

    fun clearSearchConversation() {
        _searchHistory.value = emptyList()
        _aiSearchState.value = AiTaskState.Idle
    }

    fun runAiMemoryExtraction(rawInput: String, photoBitmap: Bitmap?) {
        if (rawInput.isBlank() && photoBitmap == null) {
            _aiExtractionState.value = AiTaskState.Error("Type or speak where you put the item, or attach a photo first.")
            return
        }
        viewModelScope.launch {
            val statusMsg = if (photoBitmap != null) {
                "Analyzing photo & extracting item location with Gemini..."
            } else {
                "Extracting item & location with Gemini..."
            }
            _aiExtractionState.value = AiTaskState.Loading(statusMsg)
            val result = aiService.extractMemoryFromInput(rawInput, photoBitmap)
            result.fold(
                onSuccess = { draft ->
                    _extractedDraft.value = draft
                    _aiExtractionState.value = AiTaskState.Idle
                },
                onFailure = { error ->
                    _aiExtractionState.value = AiTaskState.Error(error.message ?: "AI extraction failed.")
                }
            )
        }
    }

    fun saveMemoryRecord(
        itemName: String,
        location: String,
        category: String,
        description: String,
        photoBitmap: Bitmap?,
        photoAiDescription: String,
        targetSpaceId: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val encodedImage = photoBitmap?.let { MediaHelper.encodeBitmapToBase64(it) } ?: ""
            val spaceMembers = if (targetSpaceId.isNotBlank()) {
                sharedSpacesState.value.firstOrNull { it.id == targetSpaceId }?.memberUids ?: listOf(currentUserId)
            } else {
                listOf(currentUserId)
            }
            val result = repository.saveMemory(
                itemName = itemName,
                location = location,
                category = category,
                description = description,
                imageBase64 = encodedImage,
                photoAiDescription = photoAiDescription,
                spaceId = targetSpaceId,
                spaceMemberUids = spaceMembers,
                addedByName = currentUserName.ifBlank { "Me" },
                existingMemoriesInScope = combinedMemoriesState.value
            )
            result.fold(
                onSuccess = {
                    _statusBannerMessage.value = "Remembered: $itemName → $location"
                    clearExtractedDraft()
                    onComplete()
                },
                onFailure = { error ->
                    _statusBannerMessage.value = error.localizedMessage ?: "Failed to save memory."
                }
            )
        }
    }

    fun updateExistingMemory(
        memory: MemoryRecord,
        newItemName: String,
        newLocation: String,
        newCategory: String,
        newDescription: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateMemory(
                memory = memory,
                newItemName = newItemName,
                newLocation = newLocation,
                newCategory = newCategory,
                newDescription = newDescription
            )
            result.fold(
                onSuccess = {
                    _statusBannerMessage.value = "Updated ${newItemName.trim()}"
                    onComplete()
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Failed to update memory"
                }
            )
        }
    }

    fun deleteMemory(memory: MemoryRecord) {
        viewModelScope.launch {
            repository.deleteMemory(memory).fold(
                onSuccess = { _statusBannerMessage.value = "Removed ${memory.itemName}" },
                onFailure = { e -> _statusBannerMessage.value = e.localizedMessage ?: "Failed to delete memory" }
            )
        }
    }

    fun askWhereIsIt(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) return

        viewModelScope.launch {
            _aiSearchState.value = AiTaskState.Loading("Searching your memories...")
            val existingFrequent = userProfileState.value?.frequentSearches ?: emptyList()
            repository.recordFrequentSearch(cleanQuestion, existingFrequent)

            val allMemories = combinedMemoriesState.value
            val borrowedList = (borrowedItemsState.value as? DataUiState.Success)?.data ?: emptyList()

            val result = aiService.answerMemoryQuestion(
                question = cleanQuestion,
                memories = allMemories,
                borrowedItems = borrowedList,
                chatHistory = _searchHistory.value
            )

            result.fold(
                onSuccess = { turn ->
                    _searchHistory.value = _searchHistory.value + turn
                    _aiSearchState.value = AiTaskState.Idle
                    turn.matchedMemories.forEach { matched ->
                        repository.incrementMemorySearchCount(matched)
                    }
                },
                onFailure = { e ->
                    _aiSearchState.value = AiTaskState.Error(e.message ?: "AI search failed.")
                }
            )
        }
    }

    // --- BORROW TRACKER ---

    fun addBorrowedItem(
        itemName: String,
        borrowerName: String,
        borrowerContact: String,
        notes: String,
        borrowDateMillis: Long,
        expectedReturnMillis: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addBorrowedItem(
                itemName = itemName,
                borrowerName = borrowerName,
                borrowerContact = borrowerContact,
                notes = notes,
                borrowDateMillis = borrowDateMillis,
                expectedReturnMillis = expectedReturnMillis
            ).fold(
                onSuccess = {
                    _statusBannerMessage.value = "Tracked lent item: $itemName to $borrowerName"
                    onComplete()
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Failed to save borrowed item"
                }
            )
        }
    }

    fun toggleBorrowedItemReturned(item: BorrowedItem) {
        viewModelScope.launch {
            val nextStatus = !item.isReturned
            repository.markBorrowedItemReturned(item.id, nextStatus).fold(
                onSuccess = {
                    _statusBannerMessage.value = if (nextStatus) {
                        "Marked ${item.itemName} as returned!"
                    } else {
                        "Marked ${item.itemName} as still borrowed."
                    }
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Failed to update status"
                }
            )
        }
    }

    fun deleteBorrowedItem(item: BorrowedItem) {
        viewModelScope.launch {
            repository.deleteBorrowedItem(item.id).fold(
                onSuccess = { _statusBannerMessage.value = "Deleted borrow record for ${item.itemName}" },
                onFailure = { e -> _statusBannerMessage.value = e.localizedMessage ?: "Failed to delete borrow record" }
            )
        }
    }

    // --- SHARED SPACES ---

    fun createSharedSpace(name: String, initialEmail: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createSharedSpace(
                name = name,
                ownerName = currentUserName.ifBlank { "Owner" },
                initialInvitedEmail = initialEmail
            ).fold(
                onSuccess = { space ->
                    _statusBannerMessage.value = "Created space '${space.name}' (Invite Code: ${space.inviteCode})"
                    onComplete()
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Failed to create shared space"
                }
            )
        }
    }

    fun inviteEmailToSpace(space: SharedSpace, email: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.inviteMemberByEmail(space, email).fold(
                onSuccess = {
                    _statusBannerMessage.value = "Invited $email to ${space.name}"
                    onComplete()
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Failed to invite email"
                }
            )
        }
    }

    fun joinSpaceByCode(inviteCode: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.joinSharedSpaceByCode(
                rawCode = inviteCode,
                memberName = currentUserName.ifBlank { "Member" }
            ).fold(
                onSuccess = { spaceName ->
                    _statusBannerMessage.value = "Joined shared space '$spaceName'!"
                    onComplete()
                },
                onFailure = { e ->
                    _statusBannerMessage.value = e.localizedMessage ?: "Could not join with code $inviteCode"
                }
            )
        }
    }

    fun acceptEmailInvite(space: SharedSpace) {
        viewModelScope.launch {
            repository.acceptEmailInvite(space, currentUserName.ifBlank { "Member" }).fold(
                onSuccess = { _statusBannerMessage.value = "Joined '${space.name}'!" },
                onFailure = { e -> _statusBannerMessage.value = e.localizedMessage ?: "Failed to join space" }
            )
        }
    }

    // --- NOTIFICATION PREFERENCES ---

    fun updateNotificationPrefs(
        borrowReminders: Boolean,
        dailySummary: Boolean,
        sharedUpdates: Boolean
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(
                borrowRemindersEnabled = borrowReminders,
                dailySummaryEnabled = dailySummary,
                sharedUpdatesEnabled = sharedUpdates
            ).fold(
                onSuccess = { _statusBannerMessage.value = "Notification preferences saved" },
                onFailure = { e -> _statusBannerMessage.value = e.localizedMessage ?: "Failed to update preferences" }
            )
        }
    }

    companion object {
        fun factory(
            userId: String,
            userName: String,
            userEmail: String,
            userPhotoUrl: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MemoryViewModel(
                    repository = MemoryRepository(db),
                    aiService = MemoryAiService(),
                    currentUserId = userId,
                    currentUserName = userName,
                    currentUserEmail = userEmail,
                    currentUserPhotoUrl = userPhotoUrl
                )
            }
        }
    }
}
