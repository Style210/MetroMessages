// File: ui/tab/MessagesTab.kt - FIXED VERSION
package com.metromessages.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metromessages.data.local.metromessagehub.MetroConversation
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel
import com.metromessages.ui.components.ContactAvatar
import com.metromessages.ui.components.FadingEdgesContent
import com.metromessages.ui.theme.formatFriendlyTimestamp

@Composable
fun MessagesTab(
    metroMessagesViewModel: MetroMessagesViewModel,
    modifier: Modifier = Modifier,
    onConversationClick: (String, String?, String?) -> Unit,
    filterPredicate: ((MetroConversation) -> Boolean)? = null,
    emptyStateTitle: String = "No conversations",
    emptyStateMessage: String = "Your conversations will appear here"
) {
    // ✅ FOSSIFY PATTERN: Single source of truth
    val conversations by metroMessagesViewModel.conversations.collectAsState()
    val isLoading by metroMessagesViewModel.isLoading.collectAsState()
    val error by metroMessagesViewModel.error.collectAsState()

    // ✅ UI-SIDE FILTERING: Pure Fossify compliance
    // ✅ FIXED: Removed threadId filtering - already handled at source
    val filteredConversations by remember(conversations, filterPredicate) {
        derivedStateOf {
            filterPredicate?.let { conversations.filter(it) } ?: conversations
        }
    }

    // Auto-refresh on first load
    LaunchedEffect(Unit) {
        if (conversations.isEmpty() && !isLoading) {
            metroMessagesViewModel.refreshAllConversations()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingState()
            error != null -> ErrorState(error!!) { metroMessagesViewModel.refreshAllConversations() }
            filteredConversations.isEmpty() -> EmptyState(emptyStateTitle, emptyStateMessage)
            else -> ConversationList(
                conversations = filteredConversations,
                onConversationClick = onConversationClick
            )
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<MetroConversation>,
    onConversationClick: (String, String?, String?) -> Unit
) {
    val lazyListState = rememberLazyListState()

    FadingEdgesContent(scrollState = lazyListState, topEdgeHeight = 80.dp) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(
                items = conversations,
                key = { conversation ->
                    // ✅ NOW GUARANTEED TO BE UNIQUE AND VALID
                    // threadId is > 0 because invalid conversations were filtered at source
                    conversation.threadId
                }
            ) { conversation ->
                ConversationListItem(
                    conversation = conversation,
                    onClick = {
                        onConversationClick(
                            conversation.threadId.toString(),
                            conversation.displayName,
                            conversation.contactPhotoUri
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: MetroConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ CONTACT AVATAR WITH PRIORITY PHOTOS
        ContactAvatar(
            name = conversation.displayName,
            photoUrl = conversation.contactPhotoUri,
            size = 56.dp,
            isWindowsPhoneStyle = true,
            isSquare = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            // Header row with name and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatFriendlyTimestamp(conversation.lastActivity),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            // Message preview row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage ?: "No messages",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Status indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Unread indicator
                    if (conversation.hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF0063B1))
                        )
                    }

                    // OTP badge
                    if (conversation.otpMessageCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF6B6B))
                        )
                    }

                    // Group indicator
                    if (conversation.isGroup) {
                        Text("👥", fontSize = 12.sp)
                    }

                    // Archived indicator
                    if (conversation.isArchived) {
                        Text("📁", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Loading conversations...",
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading conversations",
            color = Color.Red.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = errorMessage,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap to retry",
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable { onRetry() }
        )
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}