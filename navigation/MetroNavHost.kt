package com.metromessages.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import com.metromessages.ui.screens.TabbedConversationScreen
import com.metromessages.ui.thread.ConversationThreadScreen
import com.metromessages.viewmodel.SmsViewModel
import com.metromessages.viewmodel.FacebookViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.fillMaxSize

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MetroNavHost(
    modifier: Modifier = Modifier,
    smsViewModel: SmsViewModel = hiltViewModel(),
    facebookViewModel: FacebookViewModel = hiltViewModel()
) {
    var transitionOrigin by remember { mutableStateOf(TransformOrigin.Center) }
    var currentTarget by remember { mutableStateOf<NavTarget>(NavTarget.TabbedConversations) }

    AnimatedContent(
        targetState = currentTarget,
        modifier = modifier,
        transitionSpec = {
            scaleIn(initialScale = 0.92f, transformOrigin = transitionOrigin) +
                    fadeIn() togetherWith
                    scaleOut(targetScale = 0.92f, transformOrigin = transitionOrigin) +
                    fadeOut()
        },
        contentKey = { it::class.simpleName }
    ) { target ->
        when (target) {
            is NavTarget.TabbedConversations -> {
                TabbedConversationScreen(
                    smsViewModel = smsViewModel,
                    facebookViewModel = facebookViewModel,
                    onConversationClick = { conversationId, isFacebook, contactName, contactPhotoUrl ->
                        transitionOrigin = TransformOrigin.Center
                        currentTarget = NavTarget.ConversationThread(
                            conversationId = conversationId,
                            isFacebook = isFacebook,
                            contactName = contactName,
                            contactPhotoUrl = contactPhotoUrl
                        )
                    }
                )
            }

            is NavTarget.ConversationThread -> {
                ConversationThreadScreen(
                    conversationId = target.conversationId,
                    contactName = target.contactName as? String,
                    contactPhotoUrl = target.contactPhotoUrl as? String,
                    smsViewModel = smsViewModel,
                    facebookViewModel = facebookViewModel,
                    onPhotoClick = {
                        // Handle photo click (expand to full screen, etc.)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    BackHandler(enabled = currentTarget != NavTarget.TabbedConversations) {
        currentTarget = NavTarget.TabbedConversations
    }
}