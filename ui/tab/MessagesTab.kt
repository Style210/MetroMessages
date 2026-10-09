// File: ui/tab/MessagesTab.kt - UPDATED with 1:1 filtering
package com.metromessages.ui.tab

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metromessages.data.local.metromessagehub.MetroConversation
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel
import com.metromessages.ui.components.FadingEdgesContent
import com.metromessages.ui.theme.formatFriendlyTimestamp
import kotlinx.coroutines.delay

@Composable
fun MessagesTab(
    metroMessagesViewModel: MetroMessagesViewModel,
    modifier: Modifier = Modifier,
    onConversationClick: (String, String?, String?) -> Unit,
    filterPredicate: ((MetroConversation) -> Boolean)? = null,
    emptyStateTitle: String = "no chats",
    emptyStateMessage: String = "your 1:1 conversations will appear here"
) {
    val conversations by metroMessagesViewModel.conversations.collectAsState()

    // DEFAULT FILTER: Show only 1:1 conversations (exactly 2 participants)
    val defaultFilter: (MetroConversation) -> Boolean = { conversation ->
        conversation.isOneToOne
    }

    val filteredConversations = remember(conversations, filterPredicate) {
        val finalFilter = filterPredicate ?: defaultFilter
        conversations.filter(finalFilter)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            filteredConversations.isEmpty() -> {
                EmptyConversationState(
                    title = emptyStateTitle,
                    message = emptyStateMessage
                )
            }
            else -> {
                ConversationList(
                    conversations = filteredConversations,
                    onConversationClick = onConversationClick,
                    // archive is app-owned (Android has no archive flag): press-and-hold toggles it
                    onLongPress = { metroMessagesViewModel.setArchived(it.threadId, !it.isArchived) }
                )
            }
        }
    }
}

@Composable
private fun EmptyConversationState(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = message,
            color = Color.White.copy(0.6f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<MetroConversation>,
    onConversationClick: (String, String?, String?) -> Unit,
    onLongPress: (MetroConversation) -> Unit
) {
    val lazyListState = rememberLazyListState()

    FadingEdgesContent(
        scrollState = lazyListState,
        topEdgeHeight = 80.dp
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = conversations,
                key = { _, conversation -> conversation.threadId }
            ) { index, conversation ->
                AnimatedConversationListItem(
                    conversation = conversation,
                    index = index,
                    onClick = {
                        onConversationClick(
                            conversation.threadId.toString(),
                            conversation.displayName,
                            conversation.contactPhotoUri
                        )
                    },
                    onLongClick = { onLongPress(conversation) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedConversationListItem(
    conversation: MetroConversation,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val density = LocalDensity.current
    var entranceStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(5L)
        entranceStarted = true
    }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (entranceStarted) 1f else 0f,
        animationSpec = tween(600),
        label = "ConversationEntranceAlpha"
    )

    val entranceSlide by animateFloatAsState(
        targetValue = if (entranceStarted) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ConversationEntranceSlide"
    )

    val entranceRotation by animateFloatAsState(
        targetValue = if (entranceStarted) 0f else -10f,
        animationSpec = spring(
            dampingRatio = DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ConversationEntranceRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp)
            .graphicsLayer {
                alpha = entranceAlpha
                translationX = entranceSlide
                rotationY = entranceRotation
                transformOrigin = TransformOrigin(0f, 0.5f)
                cameraDistance = 8f * density.density
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .graphicsLayer {
                    scaleX = if (entranceStarted) 1f else 0.9f
                    scaleY = scaleX
                    rotationY = if (entranceStarted) 0f else -15f
                },
            contentAlignment = Alignment.Center
        ) {
            if (conversation.contactPhotoUri != null) {
                AsyncImage(
                    model = conversation.contactPhotoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Contact",
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
                .graphicsLayer {
                    val infoProgress = if (entranceStarted) 1f else 0f
                    alpha = infoProgress
                    translationX = if (entranceStarted) 0f else -10f
                    scaleX = if (entranceStarted) 1f else 0.95f
                    scaleY = scaleX
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFriendlyTimestamp(conversation.lastActivity),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = conversation.lastMessage ?: "",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}