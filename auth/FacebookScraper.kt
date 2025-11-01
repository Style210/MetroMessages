// File: FacebookScraper.kt
package com.metromessages.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FacebookScraper(private val tokenManager: FacebookTokenManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("MMM d 'at' h:mma", Locale.US)
    private val shortDateFormat = SimpleDateFormat("EEE h:mma", Locale.US)

    // Data classes for internal use only
    data class ScrapedConversation(
        val id: String,
        val name: String,
        val lastMessage: String,
        val timestamp: Long,
        val profilePicUrl: String? = null,
        val isPinned: Boolean = false,
        val unreadCount: Int = 0
    )

    data class ScrapedMessage(
        val messageId: String,
        val conversationId: String,
        val sender: String,
        val content: String,
        val timestamp: Long,
        val isFromCurrentUser: Boolean,
        val messageType: String
    )

    data class ScrapedReaction(
        val userId: String,
        val emoji: String,
        val timestamp: Long
    )

    suspend fun loadConversations(): List<ScrapedConversation> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) throw Exception("Not authenticated")

            val html = fetchMessengerPage(token)
            val document = Jsoup.parse(html)
            parseConversationList(document)

        } catch (e: Exception) {
            Log.e("FacebookScraper", "Failed to load conversations: ${e.message}")
            emptyList()
        }
    }

    suspend fun loadMessages(conversationId: String): List<ScrapedMessage> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) throw Exception("Not authenticated")

            val html = fetchConversationPage(token, conversationId)
            val document = Jsoup.parse(html)
            parseMessageThread(document, conversationId)

        } catch (e: Exception) {
            Log.e("FacebookScraper", "Failed to load messages: ${e.message}")
            emptyList()
        }
    }

    private fun fetchMessengerPage(token: String): String {
        val request = Request.Builder()
            .url("https://www.messenger.com/")
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        return response.body?.string() ?: throw IOException("Empty response")
    }

    private fun fetchConversationPage(token: String, conversationId: String): String {
        val request = Request.Builder()
            .url("https://www.messenger.com/t/$conversationId")
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        return response.body?.string() ?: throw IOException("Empty response")
    }

    private fun parseConversationList(document: Document): List<ScrapedConversation> {
        val conversations = mutableListOf<ScrapedConversation>()

        val conversationElements = document.select("div[role=listitem]").ifEmpty {
            document.select("div.x9f619.x1ja2u2z.x78zum5.x2lah0s.x1n2onr6.x1qughib")
        }

        conversationElements.forEach { element ->
            try {
                val name = element.select("span[dir=auto]").text()
                val lastMessage = element.select("span[style*='webkit-line-clamp']").text()
                val timestampText = element.select("span.x186z157.xk50ysn").text()
                val timestamp = parseTimestamp(timestampText)
                val profilePicUrl = element.select("img[referrerpolicy='origin-when-cross-origin']")
                    .attr("src")
                    .takeIf { it.isNotBlank() }
                val id = element.attr("data-thread-id")

                conversations.add(
                    ScrapedConversation(
                        id = id,
                        name = name,
                        lastMessage = lastMessage,
                        timestamp = timestamp,
                        profilePicUrl = profilePicUrl,
                        isPinned = element.attr("aria-pressed") == "true",
                        unreadCount = calculateUnreadCount(element)
                    )
                )
            } catch (e: Exception) {
                Log.w("FacebookScraper", "Failed to parse conversation: ${e.message}")
            }
        }

        return conversations.sortedByDescending { it.timestamp }
    }

    private fun parseMessageThread(document: Document, conversationId: String): List<ScrapedMessage> {
        val messages = mutableListOf<ScrapedMessage>()

        val messageElements = document.select("div[role=log] > div").ifEmpty {
            document.select("div[data-pagelet=Message]")
        }

        messageElements.forEachIndexed { index, element ->
            try {
                val content = element.select("div[dir=auto]").text()
                val senderName = element.select("span.xxymvpz.x1dyh7pn").text()
                val timestampText = element.select("span.x186z157.xk50ysn").text()
                val timestamp = parseTimestamp(timestampText)
                val messageType = detectMessageType(content)

                messages.add(
                    ScrapedMessage(
                        messageId = "${conversationId}_msg_$index",
                        conversationId = conversationId,
                        sender = senderName,
                        content = content,
                        timestamp = timestamp,
                        isFromCurrentUser = isMessageFromCurrentUser(senderName),
                        messageType = messageType
                    )
                )
            } catch (e: Exception) {
                Log.w("FacebookScraper", "Failed to parse message: ${e.message}")
            }
        }

        return messages.sortedBy { it.timestamp }
    }

    private fun detectMessageType(text: String): String {
        return when {
            text == "Media, files and links" -> "MEDIA"
            text.startsWith("http://") || text.startsWith("https://") -> "LINK"
            text.endsWith(".jpg") || text.endsWith(".jpeg") || text.endsWith(".png") ||
                    text.endsWith(".gif") || text.endsWith(".webp") -> "IMAGE"
            text.endsWith(".mp4") || text.endsWith(".mov") || text.endsWith(".avi") -> "VIDEO"
            text.endsWith(".pdf") || text.endsWith(".doc") || text.endsWith(".docx") ||
                    text.endsWith(".xls") || text.endsWith(".xlsx") || text.endsWith(".zip") -> "FILE"
            text.endsWith(".mp3") || text.endsWith(".wav") || text.endsWith(".m4a") -> "AUDIO"
            text.contains("🎵") || text.contains("🎶") || text.contains("audio", ignoreCase = true) -> "AUDIO"
            text.contains("📷") || text.contains("photo", ignoreCase = true) || text.contains("image", ignoreCase = true) -> "IMAGE"
            text.contains("🎥") || text.contains("video", ignoreCase = true) -> "VIDEO"
            else -> "TEXT"
        }
    }

    private fun parseTimestamp(timestampText: String?): Long {
        if (timestampText.isNullOrBlank()) return System.currentTimeMillis()

        return try {
            when {
                timestampText.contains("at") -> dateFormat.parse(timestampText)?.time ?: System.currentTimeMillis()
                else -> shortDateFormat.parse(timestampText)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun isMessageFromCurrentUser(senderName: String): Boolean {
        return senderName.equals("You", ignoreCase = true) ||
                senderName.equals("Current User", ignoreCase = true) ||
                !senderName.contains(" ") // Simple heuristic
    }

    private fun calculateUnreadCount(element: Element): Int {
        return when {
            element.select("[data-testid=unread]").isNotEmpty() -> 1
            element.select(".x1iyjqo2.x1pi30zi").isNotEmpty() -> 1
            element.attr("aria-label").contains("unread") -> 1
            else -> 0
        }
    }

    private fun parseReactions(element: Element): List<ScrapedReaction> {
        val reactions = mutableListOf<ScrapedReaction>()

        element.select("[aria-label*='reaction']").forEach { reactionElement ->
            try {
                val emoji = reactionElement.text().takeIf { it.isNotBlank() } ?: "❤️"
                val userId = "user_${reactionElement.hashCode()}"

                reactions.add(
                    ScrapedReaction(
                        userId = userId,
                        emoji = emoji,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Skip invalid reactions
            }
        }

        return reactions
    }

    // Utility methods for future integration
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) return@withContext false

            val html = fetchMessengerPage(token)
            html.contains("messenger", ignoreCase = true)
        } catch (e: Exception) {
            Log.e("FacebookScraper", "Connection test failed: ${e.message}")
            false
        }
    }

    suspend fun getProfileInfo(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) return@withContext emptyMap()

            val html = fetchMessengerPage(token)
            val document = Jsoup.parse(html)

            val profileInfo = mutableMapOf<String, String>()

            // Extract profile name
            document.select("[aria-label='Facebook']").first()?.let { element ->
                profileInfo["name"] = element.attr("aria-label")
            }

            // Extract profile picture
            document.select("img[referrerpolicy='origin-when-cross-origin']").first()?.let { element ->
                profileInfo["profilePic"] = element.attr("src")
            }

            profileInfo
        } catch (e: Exception) {
            Log.e("FacebookScraper", "Failed to get profile info: ${e.message}")
            emptyMap()
        }
    }
}