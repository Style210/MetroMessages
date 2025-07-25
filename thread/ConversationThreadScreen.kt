package com.metromessages.ui.thread

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metromessages.data.model.UiMessage
import com.metromessages.util.MockMessageGenerator
import com.metromessages.viewmodel.FacebookViewModel
import com.metromessages.viewmodel.SmsViewModel

private val DefaultMetroAccent = Color(0xFF0063B1)

@Composable
fun ConversationThreadScreen(
    conversationId: String,
    contactName: String?,
    contactPhotoUrl: String?,
    smsViewModel: SmsViewModel,
    facebookViewModel: FacebookViewModel,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Message collection
    val messagesFlow = if (conversationId.startsWith("fb_")) {
        facebookViewModel.getMessagesForConversation(conversationId)
    } else {
        smsViewModel.getMessagesForConversation(conversationId)
    }
    val realMessages by messagesFlow.collectAsState(initial = emptyList())

    // Debug mode handling
    val context = LocalContext.current
    val displayMessages = if (context.isDebug()) {
        remember(conversationId) {
            MockMessageGenerator.generateMockThread(
                conversationId = conversationId,
                senderName = "You",
                recipientName = contactName ?: "Contact"
            )
        }
    } else {
        realMessages
    }

    // Contact name formatting
    val displayName = when {
        !contactName.isNullOrBlank() -> contactName
        conversationId.startsWith("fb_") -> "Facebook Contact"
        else -> "Unknown Number"
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Metro-styled header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Contact photo
                if (!contactPhotoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(contactPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clickable { onPhotoClick() }
                        )
                    }
                }

                // Contact name with panoramic bleed
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 65.sp,
                        lineHeight = 70.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-4).dp),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Windows Phone accent line
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(DefaultMetroAccent)
            )
        }

        // Message list with softened transition
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                displayMessages.isEmpty() -> MetroEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    accentColor = DefaultMetroAccent
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    reverseLayout = true,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(displayMessages) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: UiMessage) {
    val isOwnMessage = message.isSentByUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            color = if (isOwnMessage) DefaultMetroAccent
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.body,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOwnMessage) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Context.isDebug(): Boolean {
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}