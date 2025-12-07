package com.metromessages.ui.navigation

// Navigation destinations - UPDATED FOR MIGRATION
sealed class MetroDestinations(val route: String) {
    object Home : MetroDestinations("home")
    object Settings : MetroDestinations("settings")
    object People : MetroDestinations("people")
    object Social : MetroDestinations("social")
    object Messages : MetroDestinations("messages")

    // ✅ FIXED: SMS Conversation route - ADDED contactId parameter
    object SmsConversation : MetroDestinations("sms_conversation/{conversationId}/{contactId}/{contactName}/{contactPhotoUrl}/{initialTab}") {
        fun createRoute(
            conversationId: String,
            contactId: Long, // ✅ ADDED: contactId parameter
            contactName: String,
            contactPhotoUrl: String? = null,
            initialTab: Int = 1
        ): String {
            val encodedContactName = contactName.replace("/", "-")
            // ✅ FIXED: Use "null" string instead of empty string for null values
            val encodedPhotoUrl = contactPhotoUrl?.replace("/", "-") ?: "null"
            // ✅ ADDED: Include contactId in the route
            return "sms_conversation/$conversationId/$contactId/$encodedContactName/$encodedPhotoUrl/$initialTab"
        }
    }

    // ✅ KEEP: For future Facebook implementation
    object FacebookConversation : MetroDestinations("facebook_conversation/{conversationId}/{contactName}/{contactPhotoUrl}/{initialTab}") {
        fun createRoute(
            conversationId: String,
            contactName: String,
            contactPhotoUrl: String? = null,
            initialTab: Int = 1
        ): String {
            val encodedContactName = contactName.replace("/", "-")
            // ✅ FIXED: Use "null" string instead of empty string for null values
            val encodedPhotoUrl = contactPhotoUrl?.replace("/", "-") ?: "null"
            return "facebook_conversation/$conversationId/$encodedContactName/$encodedPhotoUrl/$initialTab"
        }
    }

    // ✅ ADD: Companion object for the parse function
    companion object {
        // ✅ FIXED: Move parse function to companion object - UPDATED to include contactId
        fun parseConversationParams(backStackEntry: androidx.navigation.NavBackStackEntry): ConversationParams {
            val photoUrl = backStackEntry.arguments?.getString("contactPhotoUrl")
                ?.takeIf { it != "null" && it.isNotEmpty() }
                ?.replace("-", "/")

            // ✅ ADDED: Parse contactId from route parameters
            val contactId = backStackEntry.arguments?.getString("contactId")?.toLongOrNull() ?: 0L

            println("🧭 DEBUG MetroDestinations.parseConversationParams:")
            println("   - conversationId: ${backStackEntry.arguments?.getString("conversationId")}")
            println("   - contactId: $contactId")
            println("   - contactName: ${backStackEntry.arguments?.getString("contactName")}")
            println("   - contactPhotoUrl: $photoUrl")
            println("   - initialTab: ${backStackEntry.arguments?.getString("initialTab")}")

            return ConversationParams(
                conversationId = backStackEntry.arguments?.getString("conversationId") ?: "",
                contactId = contactId, // ✅ ADDED: contactId parameter
                contactName = backStackEntry.arguments?.getString("contactName")?.replace("-", "/") ?: "Unknown Contact",
                contactPhotoUrl = photoUrl,
                initialTab = backStackEntry.arguments?.getString("initialTab")?.toIntOrNull() ?: 1
            )
        }
    }
}

// ✅ UPDATED: Added contactId parameter
data class ConversationParams(
    val conversationId: String,
    val contactId: Long, // ✅ ADDED: contactId parameter
    val contactName: String,
    val contactPhotoUrl: String?,
    val initialTab: Int
)
