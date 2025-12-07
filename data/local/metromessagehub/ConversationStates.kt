// File: data/model/facebook/components/ConversationStates.kt
package com.metromessages.data.local.metromessagehub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun LoadingState(
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Loading conversation...",
            color = accentColor.copy(alpha = 0.6f),
            style = MetroTypography.MetroBody2(metroFont)
        )
    }
}

@Composable
fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("error_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Something went wrong",
                color = accentColor,
                style = MetroTypography.MetroBody1(metroFont),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            androidx.compose.material3.Text(
                text = errorMessage,
                color = accentColor.copy(alpha = 0.6f),
                style = MetroTypography.MetroBody2(metroFont),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            androidx.compose.material3.Text(
                text = "Tap to retry",
                color = accentColor,
                style = MetroTypography.MetroBody2(metroFont),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRetry() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun EmptyMessagesState(
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("empty_messages_state"),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "No messages yet",
            color = accentColor.copy(alpha = 0.6f),
            style = MetroTypography.MetroBody2(metroFont)
        )
    }
}

@Composable
fun EmptyMediaState(
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("empty_media_state"),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "No shared media yet",
            color = accentColor.copy(alpha = 0.6f),
            style = MetroTypography.MetroBody2(metroFont)
        )
    }
}

