package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val frequentSearches: List<String> = emptyList(),
    val borrowRemindersEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val sharedUpdatesEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "Memory Explorer" }.take(100),
        "email" to email.take(150),
        "photoUrl" to photoUrl.take(500),
        "frequentSearches" to frequentSearches.take(15).map { it.take(120) },
        "borrowRemindersEnabled" to borrowRemindersEnabled,
        "dailySummaryEnabled" to dailySummaryEnabled,
        "sharedUpdatesEnabled" to sharedUpdatesEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MemoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val addedByName: String = "",
    val itemName: String = "",
    val location: String = "",
    val category: String = "Other",
    val description: String = "",
    val imageBase64: String = "",
    val photoAiDescription: String = "",
    val previousLocations: List<String> = emptyList(),
    val searchCount: Long = 0L,
    val spaceId: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "addedByName" to addedByName.ifBlank { "Me" }.take(100),
        "itemName" to itemName.trim().take(120),
        "location" to location.trim().take(200),
        "category" to category.ifBlank { "Other" }.take(60),
        "description" to description.take(1000),
        "imageBase64" to imageBase64.take(350000),
        "photoAiDescription" to photoAiDescription.take(1000),
        "previousLocations" to previousLocations.take(20).map { it.take(250) },
        "searchCount" to searchCount,
        "spaceId" to spaceId.take(128),
        "memberUids" to memberUids.take(20).map { it.take(128) },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class BorrowedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemName: String = "",
    val borrowerName: String = "",
    val borrowerContact: String = "",
    val notes: String = "",
    val borrowDateMillis: Long = 0L,
    val expectedReturnMillis: Long = 0L,
    @get:PropertyName("isReturned")
    @set:PropertyName("isReturned")
    var isReturned: Boolean = false,
    val returnedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return !isReturned && expectedReturnMillis > 0L && nowMillis > expectedReturnMillis
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "itemName" to itemName.trim().take(120),
        "borrowerName" to borrowerName.trim().take(100),
        "borrowerContact" to borrowerContact.trim().take(150),
        "notes" to notes.trim().take(500),
        "borrowDateMillis" to borrowDateMillis.coerceAtLeast(0L),
        "expectedReturnMillis" to expectedReturnMillis.coerceAtLeast(0L),
        "isReturned" to isReturned,
        "returnedAtMillis" to returnedAtMillis.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SharedSpace(
    @DocumentId val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val invitedEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SpaceInviteCode(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val spaceId: String = "",
    val spaceName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ExtractedMemoryDraft(
    val itemName: String = "",
    val location: String = "",
    val category: String = "Other",
    val description: String = "",
    val photoAiDescription: String = ""
)

enum class SearchConfidence {
    CONFIRMED,
    HISTORICAL,
    BORROWED,
    NOT_FOUND
}

data class AiSearchTurn(
    val id: String,
    val question: String,
    val answer: String,
    val confidence: SearchConfidence,
    val matchedMemories: List<MemoryRecord> = emptyList(),
    val matchedBorrowedItems: List<BorrowedItem> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)

val MEMORY_CATEGORIES = listOf(
    "Keys",
    "ID & Cards",
    "Electronics",
    "Documents",
    "Bag & Clothes",
    "Books",
    "Household",
    "Other"
)
