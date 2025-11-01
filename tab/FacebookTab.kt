// File: ui/tab/FacebookTab.kt
package com.metromessages.ui.tab

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metromessages.data.model.facebook.FacebookConversationEntity
import com.metromessages.ui.components.ContactAvatar
import com.metromessages.ui.components.FadingEdgesContent
import com.metromessages.ui.theme.formatFriendlyTimestamp
import com.metromessages.viewmodel.FacebookViewModel

@Composable
fun FacebookTab(
    facebookViewModel: FacebookViewModel,
    modifier: Modifier = Modifier,
    onConversationClick: (String, String?, String?) -> Unit,
    filterPredicate: ((FacebookConversationEntity) -> Boolean)? = null,
    emptyStateTitle: String = "No conversations",
    emptyStateMessage: String = "Your conversations will appear here"
) {
    val conversations by facebookViewModel.facebookConversations.collectAsState()
    val isLoading by facebookViewModel.isLoading.collectAsState()
    val error by facebookViewModel.error.collectAsState()
    val isAuthenticated by facebookViewModel.isAuthenticated.collectAsState()

    LaunchedEffect(Unit) {
        facebookViewModel.loadFacebookConversations()
    }

    val filteredConversations = if (filterPredicate != null) {
        conversations.filter(filterPredicate)
    } else {
        conversations
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAuthenticated) "Messages" else "Messages (Demo)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { facebookViewModel.loadFacebookConversations() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_rotate),
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isAuthenticated) "Loading conversations..." else "Loading demo data...",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            error != null -> {
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
                        text = error!!,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap to retry",
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable { facebookViewModel.loadFacebookConversations() }
                    )
                }
            }

            filteredConversations.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = emptyStateTitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = emptyStateMessage,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val lazyListState = rememberLazyListState()

                FadingEdgesContent(
                    scrollState = lazyListState,
                    topEdgeHeight = 80.dp
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredConversations) { conversation ->
                            conversation.lastMessage?.let {
                                ConversationItem(
                                    name = conversation.displayName,
                                    lastMessage = it,
                                    profilePictureUrl = conversation.profilePictureUrl,
                                    timestamp = conversation.timestamp,
                                    isGroup = conversation.isSmsGroup,
                                    isUnknownContact = conversation.isUnknownContact,
                                    onClick = {
                                        onConversationClick(
                                            conversation.id,
                                            conversation.name,
                                            conversation.profilePictureUrl
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    name: String,
    lastMessage: String,
    profilePictureUrl: String?,
    timestamp: Long,
    isGroup: Boolean = false,
    isUnknownContact: Boolean = false,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            name = name,
            photoUrl = profilePictureUrl,
            size = 50.dp,
            isWindowsPhoneStyle = true,
            isSquare = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isGroup) {
                    Text(
                        text = "👥",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (isUnknownContact) {
                    Text(
                        text = "📞",
                        modifier = Modifier.padding(start = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = lastMessage,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = formatFriendlyTimestamp(timestamp),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}