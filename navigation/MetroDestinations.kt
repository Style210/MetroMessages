package com.metromessages.ui.navigation
// Navigation destinations - UPDATED FOR MIGRATION
sealed class MetroDestinations(val route: String) {
    object Home : MetroDestinations("home")
    object Settings : MetroDestinations("settings")
    object People : MetroDestinations("people")
    object Social : MetroDestinations("social")
    object Messages : MetroDestinations("messages")

    // ✅ FIXED: SMS Conversation route
    object SmsConversation : MetroDestinations("sms_conversation/{conversationId}/{contactName}/{contactPhotoUrl}/{initialTab}") {
        fun createRoute(
            conversationId: String,
            contactName: String,
            contactPhotoUrl: String? = null,
            initialTab: Int = 1
        ): String {
            val encodedContactName = contactName.replace("/", "-")
            // ✅ FIXED: Use "null" string instead of empty string for null values
            val encodedPhotoUrl = contactPhotoUrl?.replace("/", "-") ?: "null"
            return "sms_conversation/$conversationId/$encodedContactName/$encodedPhotoUrl/$initialTab"
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
        // ✅ FIXED: Move parse function to companion object
        fun parseConversationParams(backStackEntry: androidx.navigation.NavBackStackEntry): ConversationParams {
            val photoUrl = backStackEntry.arguments?.getString("contactPhotoUrl")
                ?.takeIf { it != "null" && it.isNotEmpty() }
                ?.replace("-", "/")

            return ConversationParams(
                conversationId = backStackEntry.arguments?.getString("conversationId") ?: "",
                contactName = backStackEntry.arguments?.getString("contactName")?.replace("-", "/") ?: "Unknown Contact",
                contactPhotoUrl = photoUrl,
                initialTab = backStackEntry.arguments?.getString("initialTab")?.toIntOrNull() ?: 1
            )
        }
    }
}

data class ConversationParams(
    val conversationId: String,
    val contactName: String,
    val contactPhotoUrl: String?,
    val initialTab: Int
)
