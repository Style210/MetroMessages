// File: ui/tab/TogetherTab.kt
package com.metromessages.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel

/**
 * TogetherTab - Group conversations view
 *
 * ARCHITECTURAL RULES:
 * 1. Group detection lives in MetroConversation model (isGroup property)
 * 2. This tab is ONLY a filter over existing conversations
 * 3. No new queries, no message inspection, no backend logic
 * 4. Exactly like MessagesTab, just with filterPredicate = { it.isGroup }
 *
 * DESIGN CONTRACT:
 * - MetroConversation.isGroup determines group-ness
 * - Groups are not hidden/private (no auth needed)
 * - Mirror of MessagesTab pattern - boring and correct
 */
@Composable
fun TogetherTab(
    metroMessagesViewModel: MetroMessagesViewModel,
    modifier: Modifier = Modifier,
    onConversationClick: (String, String?, String?) -> Unit,
    emptyStateTitle: String = "No group conversations",
    emptyStateMessage: String = "Group messages will appear here"
) {
    // ✅ CORRECT: Just MessagesTab with group filter
    MessagesTab(
        metroMessagesViewModel = metroMessagesViewModel,
        modifier = modifier,
        onConversationClick = onConversationClick,
        filterPredicate = { it.isGroup && !it.isArchived }, // Only show group conversations
        emptyStateTitle = emptyStateTitle,
        emptyStateMessage = emptyStateMessage
    )
}
