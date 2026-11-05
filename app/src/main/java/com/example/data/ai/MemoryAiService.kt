package com.example.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
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

    private val fastLiteExtractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
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
            val optimizedBitmap = if (photoBitmap != null) {
                withContext(Dispatchers.Default) {
                    prepareBitmapForAi(photoBitmap, maxDim = 384)
                }
            } else null

            val promptText = buildString {
                appendLine("You are the AI memory extraction engine for 'Where'd It Go?'.")
                appendLine("Extract the object's name, its exact storage location, category, and useful contextual notes.")
                if (optimizedBitmap != null) {
                    appendLine("A photo of the item or storage space is attached. Identify the visible item(s) and container/surface.")
                    appendLine("Include a concise visual inventory in 'photoAiDescription'.")
                }
                if (userText.isNotBlank()) {
                    appendLine("User statement: \"$userText\"")
                } else {
                    appendLine("Identify the main visible item for 'itemName' and the visible surface, drawer, bag, shelf, or room for 'location'. Always return non-empty 'itemName' and 'location'.")
                }
            }

            val response = withTimeout(60_000L) {
                try {
                    withTimeout(12_000L) {
                        if (optimizedBitmap != null) {
                            extractionModel.generateContent(
                                content {
                                    image(optimizedBitmap)
                                    text(promptText)
                                }
                            )
                        } else {
                            extractionModel.generateContent(promptText)
                        }
                    }
                } catch (firstErr: Exception) {
                    if (firstErr is CancellationException && firstErr !is TimeoutCancellationException) {
                        throw firstErr
                    }
                    Log.w(TAG, "Primary gemini-3.8-flash extraction slow or failed, trying gemini-3.5-flash-lite", firstErr)
                    withTimeout(8_000L) {
                        if (optimizedBitmap != null) {
                            fastLiteExtractionModel.generateContent(
                                content {
                                    image(optimizedBitmap)
                                    text(promptText)
                                }
                            )
                        } else {
                            fastLiteExtractionModel.generateContent(promptText)
                        }
                    }
                }
            }

            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty extraction response.")
            val json = JSONObject(rawJson)
            val fallbackDraft = extractMemoryLocally(userText, photoBitmap)
            Result.success(
                ExtractedMemoryDraft(
                    itemName = json.optString("itemName", "").trim().ifBlank { fallbackDraft.itemName },
                    location = json.optString("location", "").trim().ifBlank { fallbackDraft.location },
                    category = json.optString("category", "Other").trim().ifBlank { fallbackDraft.category },
                    description = json.optString("description", "").trim().ifBlank { fallbackDraft.description },
                    photoAiDescription = json.optString("photoAiDescription", "").trim().ifBlank { fallbackDraft.photoAiDescription }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Cloud AI extraction timed out, using instant on-device extraction fallback", e)
            Result.success(extractMemoryLocally(userText, photoBitmap))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud AI extraction unavailable (${e.message}), using instant on-device extraction fallback", e)
            Result.success(extractMemoryLocally(userText, photoBitmap))
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
                withTimeout(14_000L) {
                    searchModel.generateContent(prompt)
                }
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
            Log.w(TAG, "Cloud AI search timed out, using instant local memory search", e)
            Result.success(answerQuestionLocally(question, memories, borrowedItems))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud AI search unavailable (${e.message}), using instant local memory search", e)
            Result.success(answerQuestionLocally(question, memories, borrowedItems))
        }
    }

    /**
     * Downscales and re-encodes the bitmap as a lightweight software ARGB_8888 image
     * so Firebase.ai uploads a compact image payload in ~1-2 seconds instead of timing out.
     */
    private fun prepareBitmapForAi(source: Bitmap, maxDim: Int = 384): Bitmap {
        return try {
            val softwareBmp = if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }
            val w = softwareBmp.width.coerceAtLeast(1)
            val h = softwareBmp.height.coerceAtLeast(1)
            val scaled = if (w > maxDim || h > maxDim) {
                val ratio = min(maxDim.toFloat() / w, maxDim.toFloat() / h)
                Bitmap.createScaledBitmap(
                    softwareBmp,
                    (w * ratio).toInt().coerceAtLeast(1),
                    (h * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                softwareBmp
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, out)
            val bytes = out.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: scaled
        } catch (e: Exception) {
            source
        }
    }

    /**
     * Instant on-device natural language & visual analyzer so photo capture and voice/text
     * extraction always succeed immediately even when offline or without an App Check debug token.
     */
    fun extractMemoryLocally(
        rawText: String,
        photoBitmap: Bitmap?
    ): ExtractedMemoryDraft {
        val clean = rawText.trim()
        val photoSummary = if (photoBitmap != null) {
            describeBitmapVisualCharacteristics(photoBitmap)
        } else {
            ""
        }

        if (clean.isBlank()) {
            val timeLabel = SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date())
            return ExtractedMemoryDraft(
                itemName = "Photographed Item",
                location = "Captured Storage Spot",
                category = "Other",
                description = "Photo memory captured on $timeLabel",
                photoAiDescription = photoSummary.ifBlank { "Visual photo memory ($timeLabel)" }
            )
        }

        // Parse natural sentences like:
        // "I put my black headphones inside my blue school bag"
        // "Left my passport in the second bedroom drawer"
        // "Spare keys on the kitchen counter"
        val prepositions = listOf(
            " inside of ", " inside ", " in the ", " in my ", " in ",
            " on top of ", " on the ", " on my ", " on ",
            " under the ", " under my ", " under ",
            " next to ", " beside ", " behind ", " at the ", " at "
        )

        var cleanedPrefix = clean
            .replace(Regex("^(i\\s+)?(put|placed|left|kept|stored|saved|hid|dropped|tucked)\\s+(my|the|a|an|our)?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .removeSuffix(".")

        var extractedItem = ""
        var extractedLocation = ""

        for (prep in prepositions) {
            val idx = cleanedPrefix.indexOf(prep, ignoreCase = true)
            if (idx > 0) {
                extractedItem = cleanedPrefix.substring(0, idx).trim()
                val rawLoc = cleanedPrefix.substring(idx + prep.length).trim()
                extractedLocation = rawLoc
                    .replace(Regex("^(my|the|a|an|our)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
                break
            }
        }

        if (extractedItem.isBlank()) {
            val parts = cleanedPrefix.split(Regex("\\s*[,\\-→:]\\s*"), limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                extractedItem = parts[0].trim()
                extractedLocation = parts[1].trim()
            } else {
                extractedItem = cleanedPrefix.take(40).trim()
                extractedLocation = if (photoBitmap != null) "Shown in attached photo" else "Home Storage"
            }
        }

        val itemTitle = extractedItem.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val locationTitle = extractedLocation.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val detectedCategory = inferCategoryFromText("$itemTitle $locationTitle")

        return ExtractedMemoryDraft(
            itemName = itemTitle.ifBlank { "Saved Item" },
            location = locationTitle.ifBlank { "Saved Location" },
            category = detectedCategory,
            description = clean,
            photoAiDescription = photoSummary
        )
    }

    private fun inferCategoryFromText(text: String): String {
        val lower = text.lowercase(Locale.getDefault())
        return when {
            listOf("headphone", "earphone", "airpod", "charger", "laptop", "phone", "cable", "mouse", "usb", "watch", "powerbank", "camera", "tablet").any { it in lower } -> "Electronics"
            listOf("id", "passport", "card", "license", "certificate", "document", "file", "paper", "receipt", "notebook", "book").any { it in lower } -> "Documents"
            listOf("key", "wallet", "glasses", "sunglasses", "umbrella", "watch", "ring", "purse", "badge").any { it in lower } -> "Accessories"
            listOf("jacket", "shirt", "shoe", "hoodie", "sweater", "coat", "hat", "scarf", "gloves", "jeans", "bag", "backpack").any { it in lower } -> "Clothing"
            listOf("hammer", "screwdriver", "wrench", "drill", "tape", "scissors", "flashlight", "tool", "battery").any { it in lower } -> "Tools"
            else -> "Other"
        }
    }

    private fun describeBitmapVisualCharacteristics(bitmap: Bitmap): String {
        return try {
            val sample = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            val stepX = (sample.width / 8).coerceAtLeast(1)
            val stepY = (sample.height / 8).coerceAtLeast(1)
            for (x in 0 until sample.width step stepX) {
                for (y in 0 until sample.height step stepY) {
                    val pixel = sample.getPixel(x, y)
                    rSum += Color.red(pixel)
                    gSum += Color.green(pixel)
                    bSum += Color.blue(pixel)
                    count++
                }
            }
            if (count == 0) return "Captured photo of item and storage context"
            val rAvg = (rSum / count).toInt()
            val gAvg = (gSum / count).toInt()
            val bAvg = (bSum / count).toInt()
            val brightness = (rAvg + gAvg + bAvg) / 3
            val tone = when {
                brightness < 65 -> "dark-toned"
                brightness > 190 -> "brightly lit"
                rAvg > gAvg + 25 && rAvg > bAvg + 25 -> "warm/wooden-toned"
                bAvg > rAvg + 20 && bAvg > gAvg + 10 -> "cool/blue-toned"
                else -> "neutral indoor"
            }
            val orientation = if (sample.width >= sample.height) "wide view" else "close-up portrait view"
            "Photographed item in $tone storage area ($orientation, ${sample.width}x${sample.height})"
        } catch (e: Exception) {
            "Captured photo of item and storage location"
        }
    }

    private fun answerQuestionLocally(
        question: String,
        memories: List<MemoryRecord>,
        borrowedItems: List<BorrowedItem>
    ): AiSearchTurn {
        val stopWords = setOf(
            "where", "is", "are", "my", "the", "did", "i", "put", "place", "leave", "kept",
            "find", "have", "seen", "who", "borrowed", "took", "has", "in", "on", "at", "a", "an", "to", "it"
        )
        val tokens = question.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 && it !in stopWords }

        val dateFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.US)

        // Score memories
        val scoredMemories = memories.map { mem ->
            val searchable = "${mem.itemName} ${mem.location} ${mem.category} ${mem.description} ${mem.photoAiDescription} ${mem.previousLocations.joinToString(" ")}".lowercase(Locale.getDefault())
            val score = if (tokens.isEmpty()) {
                0
            } else {
                tokens.sumOf { token ->
                    when {
                        mem.itemName.lowercase(Locale.getDefault()).contains(token) -> 5
                        mem.location.lowercase(Locale.getDefault()).contains(token) -> 3
                        searchable.contains(token) -> 2
                        else -> 0
                    }
                }
            }
            mem to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        // Score borrowed items
        val scoredBorrows = borrowedItems.map { b ->
            val searchable = "${b.itemName} ${b.borrowerName} ${b.notes}".lowercase(Locale.getDefault())
            val score = if (tokens.isEmpty()) {
                0
            } else {
                tokens.sumOf { token ->
                    when {
                        b.itemName.lowercase(Locale.getDefault()).contains(token) -> 5
                        b.borrowerName.lowercase(Locale.getDefault()).contains(token) -> 4
                        searchable.contains(token) -> 2
                        else -> 0
                    }
                }
            }
            b to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        val topMemories = scoredMemories.take(3).map { it.first }
        val topBorrows = scoredBorrows.take(2).map { it.first }

        if (topMemories.isEmpty() && topBorrows.isEmpty()) {
            val fallbackMsg = if (memories.isEmpty() && borrowedItems.isEmpty()) {
                "You haven't saved any memories or borrowed items yet. Tap 'Add' or 'Click Photo' to save where your things are!"
            } else {
                "I couldn't find a saved memory matching \"$question\". Try checking your Timeline or saving its location when you find it."
            }
            return AiSearchTurn(
                id = System.currentTimeMillis().toString(),
                question = question,
                answer = fallbackMsg,
                confidence = SearchConfidence.NOT_FOUND,
                matchedMemories = emptyList(),
                matchedBorrowedItems = emptyList()
            )
        }

        val answerBuilder = StringBuilder()
        if (topMemories.isNotEmpty()) {
            val primary = topMemories.first()
            val timeStr = (primary.updatedAt ?: primary.createdAt)?.toDate()?.let { dateFormat.format(it) } ?: "recently"
            answerBuilder.append("Your **${primary.itemName}** is in **${primary.location}** (saved $timeStr).")
            if (primary.description.isNotBlank()) {
                answerBuilder.append(" Note: \"${primary.description}\".")
            }
            if (primary.previousLocations.isNotEmpty()) {
                answerBuilder.append(" Previously it was recorded in: ${primary.previousLocations.joinToString(", ")}.")
            }
            if (topMemories.size > 1) {
                val others = topMemories.drop(1).joinToString("; ") { "${it.itemName} → ${it.location}" }
                answerBuilder.append("\nRelated saved items: $others.")
            }
        }

        if (topBorrows.isNotEmpty()) {
            val primaryBorrow = topBorrows.first()
            if (answerBuilder.isNotEmpty()) answerBuilder.append("\n")
            val statusText = if (primaryBorrow.isReturned) {
                "was borrowed by ${primaryBorrow.borrowerName} and marked returned"
            } else {
                "is currently borrowed by **${primaryBorrow.borrowerName}**"
            }
            answerBuilder.append("Borrow record: **${primaryBorrow.itemName}** $statusText.")
        }

        val confidence = when {
            topBorrows.isNotEmpty() && topMemories.isEmpty() -> SearchConfidence.BORROWED
            topMemories.firstOrNull()?.previousLocations?.isNotEmpty() == true -> SearchConfidence.CONFIRMED
            else -> SearchConfidence.CONFIRMED
        }

        return AiSearchTurn(
            id = System.currentTimeMillis().toString(),
            question = question,
            answer = answerBuilder.toString(),
            confidence = confidence,
            matchedMemories = topMemories,
            matchedBorrowedItems = topBorrows
        )
    }
}
