package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BorrowedItem
import com.example.data.model.MemoryRecord
import com.example.data.model.SharedSpace
import com.example.data.model.SpaceInviteCode
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MemoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun formatHistoryTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: Date()
        val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        return formatter.format(date)
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        photoUrl: String
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val current = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (current != null) return@runCatching current
            }
            val newProfile = UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.ifBlank { "Memory Explorer" },
                email = email,
                photoUrl = photoUrl,
                frequentSearches = emptyList(),
                borrowRemindersEnabled = true,
                dailySummaryEnabled = true,
                sharedUpdatesEnabled = true
            )
            docRef.set(newProfile.toCreateMap()).await()
            newProfile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateNotificationPreferences(
        borrowRemindersEnabled: Boolean,
        dailySummaryEnabled: Boolean,
        sharedUpdatesEnabled: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "borrowRemindersEnabled" to borrowRemindersEnabled,
                    "dailySummaryEnabled" to dailySummaryEnabled,
                    "sharedUpdatesEnabled" to sharedUpdatesEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordFrequentSearch(query: String, existingSearches: List<String>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleaned = query.trim().take(120)
        if (cleaned.isBlank()) return@runCatching
        val updated = (listOf(cleaned) + existingSearches.filterNot { it.equals(cleaned, ignoreCase = true) }).take(10)
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "frequentSearches" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- PRIVATE MEMORIES ---

    fun observePrivateMemories(userId: String = requireUserId()): Flow<List<MemoryRecord>> = flow {
        val path = "users/$userId/memories"
        emitAll(
            db.collection("users").document(userId).collection("memories")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MemoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getPrivateMemoryById(ownerUserId: String, memoryId: String): Result<MemoryRecord?> = runCatching {
        val docRef = db.collection("users").document(ownerUserId).collection("memories").document(memoryId)
        try {
            val snap = docRef.get().await()
            snap.toObject(MemoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveMemory(
        itemName: String,
        location: String,
        category: String,
        description: String,
        imageBase64: String = "",
        photoAiDescription: String = "",
        spaceId: String = "",
        spaceMemberUids: List<String> = emptyList(),
        addedByName: String = "",
        existingMemoriesInScope: List<MemoryRecord> = emptyList()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val normalizedItem = itemName.trim()
        val normalizedLocation = location.trim()
        require(normalizedItem.isNotEmpty()) { "Item name cannot be empty" }
        require(normalizedLocation.isNotEmpty()) { "Location cannot be empty" }

        val resolvedAddedBy = addedByName.ifBlank {
            auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Me"
        }

        // Check if an item with the same name already exists in this scope to preserve location history
        val existingMatch = existingMemoriesInScope.firstOrNull {
            it.itemName.trim().equals(normalizedItem, ignoreCase = true) && it.spaceId == spaceId
        }

        val collectionRef = if (spaceId.isBlank()) {
            db.collection("users").document(uid).collection("memories")
        } else {
            db.collection("shared_spaces").document(spaceId).collection("memories")
        }

        if (existingMatch != null && existingMatch.id.isNotBlank()) {
            val docRef = collectionRef.document(existingMatch.id)
            val historyEntry = "${existingMatch.location} (${formatHistoryTimestamp(existingMatch.updatedAt ?: existingMatch.createdAt)})"
            val updatedHistory = if (!existingMatch.location.equals(normalizedLocation, ignoreCase = true)) {
                (listOf(historyEntry) + existingMatch.previousLocations).take(20)
            } else {
                existingMatch.previousLocations
            }

            val updatePayload = mutableMapOf<String, Any>(
                "addedByName" to resolvedAddedBy.take(100),
                "itemName" to normalizedItem.take(120),
                "location" to normalizedLocation.take(200),
                "category" to category.ifBlank { existingMatch.category }.take(60),
                "description" to description.ifBlank { existingMatch.description }.take(1000),
                "previousLocations" to updatedHistory.map { it.take(250) },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (imageBase64.isNotBlank()) {
                updatePayload["imageBase64"] = imageBase64.take(350000)
            }
            if (photoAiDescription.isNotBlank()) {
                updatePayload["photoAiDescription"] = photoAiDescription.take(1000)
            }
            if (spaceId.isNotBlank() && spaceMemberUids.isNotEmpty()) {
                updatePayload["memberUids"] = spaceMemberUids.take(20)
            }
            try {
                docRef.update(updatePayload).await()
                return@runCatching existingMatch.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val docRef = collectionRef.document()
            val members = if (spaceId.isBlank()) listOf(uid) else (spaceMemberUids + uid).distinct()
            val record = MemoryRecord(
                id = docRef.id,
                userId = uid,
                addedByName = resolvedAddedBy,
                itemName = normalizedItem,
                location = normalizedLocation,
                category = category.ifBlank { "Other" },
                description = description,
                imageBase64 = imageBase64,
                photoAiDescription = photoAiDescription,
                previousLocations = emptyList(),
                searchCount = 0L,
                spaceId = spaceId,
                memberUids = members
            )
            try {
                docRef.set(record.toCreateMap()).await()
                return@runCatching docRef.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun updateMemory(
        memory: MemoryRecord,
        newItemName: String,
        newLocation: String,
        newCategory: String,
        newDescription: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(memory.id.isNotBlank()) { "Memory ID must not be blank" }

        val collectionRef = if (memory.spaceId.isBlank()) {
            db.collection("users").document(uid).collection("memories")
        } else {
            db.collection("shared_spaces").document(memory.spaceId).collection("memories")
        }
        val docRef = collectionRef.document(memory.id)

        val updatedHistory = if (!memory.location.equals(newLocation.trim(), ignoreCase = true)) {
            val entry = "${memory.location} (${formatHistoryTimestamp(memory.updatedAt ?: memory.createdAt)})"
            (listOf(entry) + memory.previousLocations).take(20)
        } else {
            memory.previousLocations
        }

        try {
            docRef.update(
                mapOf(
                    "itemName" to newItemName.trim().take(120),
                    "location" to newLocation.trim().take(200),
                    "category" to newCategory.ifBlank { "Other" }.take(60),
                    "description" to newDescription.trim().take(1000),
                    "previousLocations" to updatedHistory.map { it.take(250) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementMemorySearchCount(memory: MemoryRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (memory.id.isBlank()) return@runCatching
        val collectionRef = if (memory.spaceId.isBlank()) {
            db.collection("users").document(uid).collection("memories")
        } else {
            db.collection("shared_spaces").document(memory.spaceId).collection("memories")
        }
        val docRef = collectionRef.document(memory.id)
        try {
            docRef.update(
                mapOf(
                    "searchCount" to (memory.searchCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMemory(memory: MemoryRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(memory.id.isNotBlank()) { "Memory ID must not be blank" }
        val collectionRef = if (memory.spaceId.isBlank()) {
            db.collection("users").document(uid).collection("memories")
        } else {
            db.collection("shared_spaces").document(memory.spaceId).collection("memories")
        }
        val docRef = collectionRef.document(memory.id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- BORROWED ITEMS ---

    fun observeBorrowedItems(userId: String = requireUserId()): Flow<List<BorrowedItem>> = flow {
        val path = "users/$userId/borrowed_items"
        emitAll(
            db.collection("users").document(userId).collection("borrowed_items")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BorrowedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<BorrowedItem> { it.isReturned }
                                .thenBy { it.expectedReturnMillis }
                        )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addBorrowedItem(
        itemName: String,
        borrowerName: String,
        borrowerContact: String,
        notes: String,
        borrowDateMillis: Long,
        expectedReturnMillis: Long
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("borrowed_items").document()
        val item = BorrowedItem(
            id = docRef.id,
            userId = uid,
            itemName = itemName.trim(),
            borrowerName = borrowerName.trim(),
            borrowerContact = borrowerContact.trim(),
            notes = notes.trim(),
            borrowDateMillis = borrowDateMillis,
            expectedReturnMillis = expectedReturnMillis,
            isReturned = false,
            returnedAtMillis = 0L
        )
        try {
            docRef.set(item.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markBorrowedItemReturned(borrowId: String, returned: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("borrowed_items").document(borrowId)
        try {
            docRef.update(
                mapOf(
                    "isReturned" to returned,
                    "returnedAtMillis" to if (returned) System.currentTimeMillis() else 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBorrowedItem(borrowId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("borrowed_items").document(borrowId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- SHARED SPACES ---

    fun observeSharedSpaces(userId: String = requireUserId()): Flow<List<SharedSpace>> = flow {
        val path = "shared_spaces"
        emitAll(
            db.collection("shared_spaces")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SharedSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeInvitedSharedSpaces(userEmail: String): Flow<List<SharedSpace>> = flow {
        val cleanEmail = userEmail.trim().lowercase(Locale.US)
        if (cleanEmail.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "shared_spaces"
        emitAll(
            db.collection("shared_spaces")
                .whereArrayContains("invitedEmails", cleanEmail)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SharedSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSharedSpaceMemories(spaceId: String, userId: String = requireUserId()): Flow<List<MemoryRecord>> = flow {
        val path = "shared_spaces/$spaceId/memories"
        emitAll(
            db.collection("shared_spaces").document(spaceId).collection("memories")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MemoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSharedSpace(name: String, ownerName: String, initialInvitedEmail: String = ""): Result<SharedSpace> = runCatching {
        val uid = requireUserId()
        val resolvedOwnerName = ownerName.ifBlank { auth.currentUser?.displayName ?: "Owner" }.take(100)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val inviteCode = (1..6).map { chars.random() }.joinToString("")
        val spaceDoc = db.collection("shared_spaces").document()
        val codeDoc = db.collection("space_codes").document(inviteCode)

        val invitedList = if (initialInvitedEmail.isNotBlank()) {
            listOf(initialInvitedEmail.trim().lowercase(Locale.US).take(150))
        } else {
            emptyList()
        }

        val spacePayload = mapOf(
            "name" to name.trim().take(100),
            "ownerId" to uid,
            "ownerName" to resolvedOwnerName,
            "inviteCode" to inviteCode,
            "memberUids" to listOf(uid),
            "memberNames" to listOf(resolvedOwnerName),
            "invitedEmails" to invitedList,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val codePayload = mapOf(
            "inviteCode" to inviteCode,
            "spaceId" to spaceDoc.id,
            "spaceName" to name.trim().take(100),
            "ownerId" to uid,
            "ownerName" to resolvedOwnerName,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(spaceDoc, spacePayload)
            batch.set(codeDoc, codePayload)
            batch.commit().await()
            SharedSpace(
                id = spaceDoc.id,
                name = name.trim().take(100),
                ownerId = uid,
                ownerName = resolvedOwnerName,
                inviteCode = inviteCode,
                memberUids = listOf(uid),
                memberNames = listOf(resolvedOwnerName),
                invitedEmails = invitedList
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, spaceDoc.path)
            throw e
        }
    }

    suspend fun inviteMemberByEmail(space: SharedSpace, email: String): Result<Unit> = runCatching {
        requireUserId()
        val cleanEmail = email.trim().lowercase(Locale.US).take(150)
        require(cleanEmail.contains("@")) { "Please enter a valid email address" }
        val updatedEmails = (space.invitedEmails + cleanEmail).distinct().take(20)
        val docRef = db.collection("shared_spaces").document(space.id)
        try {
            docRef.update(
                mapOf(
                    "invitedEmails" to updatedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun joinSharedSpaceByCode(rawCode: String, memberName: String): Result<String> = runCatching {
        val uid = requireUserId()
        val code = rawCode.trim().uppercase(Locale.US)
        require(code.length in 4..16) { "Enter a valid 6-character invite code" }
        val codeRef = db.collection("space_codes").document(code)
        val codeSnap = try {
            codeRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, codeRef.path)
            throw e
        }

        if (!codeSnap.exists()) {
            throw IllegalArgumentException("No shared space found with invite code $code")
        }
        val codeObj = codeSnap.toObject(SpaceInviteCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalArgumentException("Invalid invite code record")

        val spaceRef = db.collection("shared_spaces").document(codeObj.spaceId)
        val resolvedName = memberName.ifBlank { auth.currentUser?.displayName ?: "Member" }.take(100)
        try {
            spaceRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberNames" to FieldValue.arrayUnion(resolvedName),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            codeObj.spaceName
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, spaceRef.path)
            throw e
        }
    }

    suspend fun acceptEmailInvite(space: SharedSpace, memberName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val resolvedName = memberName.ifBlank { auth.currentUser?.displayName ?: "Member" }.take(100)
        val spaceRef = db.collection("shared_spaces").document(space.id)
        try {
            spaceRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberNames" to FieldValue.arrayUnion(resolvedName),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, spaceRef.path)
            throw e
        }
    }
}
