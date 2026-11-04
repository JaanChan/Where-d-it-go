package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiSearchTurn
import com.example.data.model.BorrowedItem
import com.example.data.model.ExtractedMemoryDraft
import com.example.data.model.MEMORY_CATEGORIES
import com.example.data.model.MemoryRecord
import com.example.data.model.SearchConfidence
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MemoryAiService"

class MemoryAiService {

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "itemName" to Schema.string(
                description = "The primary object or items placed (e.g. 'Black headphones', 'School ID', 'Spare keys')"
            ),
            "location" to Schema.string(
                description = "The specific place, surface, or container where the item is stored (e.g. 'Blue school bag', 'Second desk drawer', 'Wooden desk')"
            ),
            "category" to Schema.enumeration(
                values = MEMORY_CATEGORIES,
                description = "Best matching category for the object"
            ),
            "description" to Schema.string(
                description = "Helpful contextual details, landmarks, or notes extracted from the user's statement or photo"
            ),
            "photoAiDescription" to Schema.string(
                description = "Detailed visual inventory of visible objects and containers if an image was provided, or empty string if text only"
            )
        ),
        optionalProperties = listOf("description", "photoAiDescription")
    )

    private val searchAnswerSchema = Schema.obj(
        properties = mapOf(
            "answer" to Schema.string(
                description = "Conversational, direct, and honest response grounded strictly in the user's saved memories and borrow records. Never invent locations."
            ),
            "confidence" to Schema.enumeration(
                values = listOf("CONFIRMED", "HISTORICAL", "BORROWED", "NOT_FOUND"),
                description = "CONFIRMED if found in current saved memories, HISTORICAL if referring to previous locations, BORROWED if lent out, NOT_FOUND if no record exists"
            ),
            "matchedMemoryIds" to Schema.array(
                items = Schema.string(),
                description = "Exact IDs of the saved memories that support this answer"
            ),
            "matchedBorrowedIds" to Schema.array(
                items = Schema.string(),
                description = "Exact IDs of the borrowed item records that support this answer"
            )
        )
    )

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
            }
        )
    }

    private val searchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = searchAnswerSchema
            }
        )
    }

    suspend fun extractMemoryFromInput(
        userText: String,
        photoBitmap: Bitmap? = null
    ): Result<ExtractedMemoryDraft> {
        return try {
            val promptText = buildString {
                appendLine("You are the AI memory extraction engine for 'Where'd It Go?'.")
                appendLine("Extract the object's name, its exact storage location, category, and useful contextual notes.")
                if (photoBitmap != null) {
                    appendLine("A photo of the item or storage space (drawer, shelf, desk, bag, cupboard) is attached.")
                    appendLine("Automatically analyze the photo to detect visible items, containers, surfaces, and landmarks.")
                    appendLine("Include a rich visual inventory in 'photoAiDescription' so the user can search for any item visible in this photo later.")
                }
                if (userText.isNotBlank()) {
                    appendLine("User statement: \"$userText\"")
                } else {
                    appendLine("The user clicked a photo without typing text. Identify the main item(s) for 'itemName' and the visible container, surface, drawer, shelf, bag, or room for 'location'. Always provide non-empty values for both 'itemName' and 'location'.")
                }
            }

            val response = withTimeout(60_000L) {
                if (photoBitmap != null) {
                    extractionModel.generateContent(
                        content {
                            image(photoBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    extractionModel.generateContent(promptText)
                }
            }

            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty extraction response.")
            val json = JSONObject(rawJson)
            Result.success(
                ExtractedMemoryDraft(
                    itemName = json.optString("itemName", "").trim(),
                    location = json.optString("location", "").trim(),
                    category = json.optString("category", "Other").trim().ifBlank { "Other" },
                    description = json.optString("description", "").trim(),
                    photoAiDescription = json.optString("photoAiDescription", "").trim()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI memory extraction timed out", e)
            Result.failure(Exception("AI extraction timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI memory extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI extraction failed."))
        }
    }

    suspend fun answerMemoryQuestion(
        question: String,
        memories: List<MemoryRecord>,
        borrowedItems: List<BorrowedItem>,
        chatHistory: List<AiSearchTurn>
    ): Result<AiSearchTurn> {
        return try {
            val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)

            val memoriesContext = if (memories.isEmpty()) {
                "NO SAVED MEMORIES."
            } else {
                memories.joinToString("\n") { m ->
                    val ts = (m.updatedAt ?: m.createdAt)?.toDate()?.let { dateFormat.format(it) } ?: "Recently"
                    val prev = if (m.previousLocations.isNotEmpty()) {
                        " | Previous locations: ${m.previousLocations.joinToString(" -> ")}"
                    } else ""
                    val photoDesc = if (m.photoAiDescription.isNotBlank()) {
                        " | Photo contents: ${m.photoAiDescription}"
                    } else ""
                    val spaceTag = if (m.spaceId.isNotBlank()) " [Shared Space Item added by ${m.addedByName}]" else " [Private Item]"
                    "- ID=${m.id} | Item=\"${m.itemName}\" | Current Location=\"${m.location}\" | Category=${m.category} | Recorded=$ts | Notes=\"${m.description}\"$prev$photoDesc$spaceTag"
                }
            }

            val borrowedContext = if (borrowedItems.isEmpty()) {
                "NO BORROWED ITEMS."
            } else {
                borrowedItems.joinToString("\n") { b ->
                    val borrowedOn = dateFormat.format(Date(b.borrowDateMillis))
                    val dueOn = dateFormat.format(Date(b.expectedReturnMillis))
                    val status = if (b.isReturned) "Returned" else if (b.isOverdue()) "OVERDUE (Not returned)" else "Currently borrowed (Not returned)"
                    "- ID=${b.id} | Item=\"${b.itemName}\" | Borrower=\"${b.borrowerName}\" (${b.borrowerContact}) | Borrowed=$borrowedOn | Expected Return=$dueOn | Status=$status | Notes=\"${b.notes}\""
                }
            }

            val recentConversation = if (chatHistory.isEmpty()) {
                "No prior conversation."
            } else {
                chatHistory.takeLast(4).joinToString("\n") { turn ->
                    "User: ${turn.question}\nAssistant: ${turn.answer}"
                }
            }

            val prompt = """
                You are the personal memory assistant inside the app "Where'd It Go?".
                CRITICAL RULES:
                1. Search ONLY the user's actual SAVED MEMORIES and BORROWED ITEMS provided below.
                2. NEVER fabricate, guess, or invent a location that is not supported by the saved records below.
                3. Understand synonyms, related terms, and photo descriptions (e.g. "earphones" vs "headphones", "ID badge" vs "School ID", or items detected inside a photographed drawer).
                4. If multiple records or previous locations exist for an item, state the most recent confirmed location clearly AND mention its previous recorded locations.
                5. If the user asks who borrowed an item (or if an item is lent out), check the BORROWED ITEMS list.
                6. Use the RECENT CONVERSATION context to resolve follow-up questions (like "When did I put it there?" or "What else is in that bag?").
                7. If no matching record exists anywhere in SAVED MEMORIES or BORROWED ITEMS, respond honestly that you don't have a saved memory for that item yet and set confidence to "NOT_FOUND".

                SAVED MEMORIES:
                $memoriesContext

                BORROWED ITEMS:
                $borrowedContext

                RECENT CONVERSATION:
                $recentConversation

                USER QUESTION: "$question"
            """.trimIndent()

            val response = withTimeout(60_000L) {
                searchModel.generateContent(prompt)
            }

            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty search answer.")
            val json = JSONObject(rawJson)
            val answer = json.optString("answer", "I couldn't find any saved memory for that item.")
            val confidenceStr = json.optString("confidence", "NOT_FOUND")
            val confidence = runCatching { SearchConfidence.valueOf(confidenceStr) }.getOrDefault(SearchConfidence.NOT_FOUND)

            val matchedMemIds = mutableListOf<String>()
            val memArr = json.optJSONArray("matchedMemoryIds")
            if (memArr != null) {
                for (i in 0 until memArr.length()) {
                    matchedMemIds.add(memArr.optString(i))
                }
            }

            val matchedBorrowIds = mutableListOf<String>()
            val borrowArr = json.optJSONArray("matchedBorrowedIds")
            if (borrowArr != null) {
                for (i in 0 until borrowArr.length()) {
                    matchedBorrowIds.add(borrowArr.optString(i))
                }
            }

            val matchedMemories = memories.filter { it.id in matchedMemIds }
            val matchedBorrows = borrowedItems.filter { it.id in matchedBorrowIds }

            Result.success(
                AiSearchTurn(
                    id = System.currentTimeMillis().toString(),
                    question = question,
                    answer = answer,
                    confidence = confidence,
                    matchedMemories = matchedMemories,
                    matchedBorrowedItems = matchedBorrows
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI search timed out", e)
            Result.failure(Exception("AI search timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI search failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to run AI search."))
        }
    }
}
