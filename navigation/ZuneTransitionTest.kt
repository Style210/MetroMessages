package com.metromessages.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZuneStyleConversationScreen() {
    // Track drill-in state
    var isDrilledIn by remember { mutableStateOf(false) }
    var selectedConversation by remember { mutableStateOf("") }

    // Zune-style spring animation
    val springSpec = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Master animation container
        AnimatedContent(
            targetState = isDrilledIn,
            transitionSpec = {
                if (targetState) {
                    // DRILL-IN: Expand from tapped item
                    (fadeIn(animationSpec = springSpec) +
                            scaleIn(initialScale = 0.9f, animationSpec = springSpec))
                        .togetherWith(
                            fadeOut(animationSpec = springSpec) +
                                    scaleOut(targetScale = 1.1f, animationSpec = springSpec)
                        )
                } else {
                    // DRILL-OUT: Collapse back to origin
                    (fadeIn(animationSpec = springSpec) +
                            scaleIn(initialScale = 0.9f, animationSpec = springSpec))
                        .togetherWith(
                            fadeOut(animationSpec = springSpec) +
                                    scaleOut(targetScale = 1.1f, animationSpec = springSpec)
                        )
                }
            }
        ) { drilledIn ->
            if (drilledIn) {
                // DRILLED-IN VIEW (Chat Screen)
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header acts as back button
                    Text(
                        text = selectedConversation,
                        modifier = Modifier
                            .clickable { isDrilledIn = false }
                            .padding(16.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontSize = 32.sp
                    )

                    // Chat content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        repeat(20) { i ->
                            Text(
                                text = "Message $i",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // DRILLED-OUT VIEW (Conversation List)
                Column {
                    // List header
                    Text(
                        text = "Conversations",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontSize = 24.sp
                    )

                    // Conversation items
                    listOf("Mom", "Work Group", "Alex", "Sarah").forEach { convo ->
                        Text(
                            text = convo,
                            modifier = Modifier
                                .clickable {
                                    selectedConversation = convo
                                    isDrilledIn = true
                                }
                                .padding(16.dp),
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
