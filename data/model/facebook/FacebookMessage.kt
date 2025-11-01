// File: FacebookMessage.kt
package com.metromessages.data.model.facebook

data class FacebookMessage(
    val id: String,              // String ID matching UiMessageConverters
    val conversationId: String,  // String ID matching UiMessageConverters
    val sender: String,          // Sender name or ID
    val body: String,            // Message content
    val timestamp: Long,         // Unix epoch millis
    val isSentByUser: Boolean    // True if current user sent it
)


