package com.metromessages.ui.navigation

sealed class NavTarget {
    data object TabbedConversations : NavTarget()

    data class ConversationThread(
        val conversationId: String,  // Change to String for consistency
        val isFacebook: Boolean,
        val contactName: Any?,
        val contactPhotoUrl: Any?
    ) : NavTarget()
}
