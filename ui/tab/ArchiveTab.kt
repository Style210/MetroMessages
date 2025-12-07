// File: ui/tab/ArchiveTab.kt - COMPLETELY REBUILT
package com.metromessages.ui.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel

@Composable
fun ArchiveTab(
    metroMessagesViewModel: MetroMessagesViewModel,
    modifier: Modifier = Modifier,
    onConversationClick: (String, String?, String?) -> Unit,
    isAuthenticated: Boolean = false,
    onRequestAuthentication: () -> Unit
) {
    when {
        !isAuthenticated -> {
            LockedArchiveView(onRequestAuthentication)
        }
        else -> {
            // ✅ REUSE MESSAGES TAB WITH ARCHIVE FILTER
            MessagesTab(
                metroMessagesViewModel = metroMessagesViewModel,
                modifier = modifier,
                onConversationClick = onConversationClick,
                filterPredicate = { it.isArchived }, // Only show archived conversations
                emptyStateTitle = "No archived conversations",
                emptyStateMessage = "Swipe right on conversations to archive them"
            )
        }
    }
}

@Composable
private fun LockedArchiveView(onRequestAuthentication: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onRequestAuthentication() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked Archive",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Archive Locked",
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "Authenticate to view archived conversations",
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Text(
                    text = "Tap to unlock",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
