// File: ui/screens/TabbedConversationScreen.kt - FIXED FUNCTION SIGNATURE
package com.metromessages.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.components.CollapsibleHeaderScreen
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.components.MetroPulsingDivider
import com.metromessages.ui.tab.ArchiveTab
import com.metromessages.ui.tab.MessagesTab
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.launch

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun TabbedConversationScreen(
    metroMessagesViewModel: MetroMessagesViewModel = hiltViewModel(),
    navController: NavHostController,
    metroFont: MetroFont,
    // ✅ FIXED: Removed @Composable annotation - matches MessagesTab signature
    onNavigateToConversation: (String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    // INDEPENDENT SCROLL STATES FOR METRO HEADER
    val headerScrollState = rememberScrollState()

    // Sync header with pager scroll (35% parallax effect)
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val totalScroll = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * 1000f
        val headerTarget = (totalScroll * 0.35f).toInt()
        headerScrollState.scrollTo(headerTarget)
    }

    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val pageOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    val tabTravel = 200.dp
    val messagesX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 0, 3, tabTravel.value)
        }
    }
    remember { SettingsPreferences(context) }
    val togetherX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 1, 3, tabTravel.value)
        }
    }

    val archiveX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 2, 3, tabTravel.value)
        }
    }

    CollapsibleHeaderScreen(
        headerContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        navController.popBackStack()
                    }
            ) {
                MetroHeaderCanvas(
                    text = "messages",
                    scrollState = headerScrollState,
                    metroFont = metroFont,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                )
            }
        },
        tabContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                // Messages tab
                Text(
                    text = "messages",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = messagesX)
                        .alpha(if (currentPage == 0) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                // Together tab
                Text(
                    text = "together",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = togetherX)
                        .alpha(if (currentPage == 1) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                // Archive tab
                Text(
                    text = "archive",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = archiveX)
                        .alpha(if (currentPage == 2) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                MetroPulsingDivider(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-4).dp),
                    initialWidth = 100.dp,
                    maxTravel = 8.dp
                )
            }
        },
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> {
                            MessagesTab(
                                metroMessagesViewModel = metroMessagesViewModel,
                                modifier = Modifier.fillMaxWidth(),
                                // ✅ Now the signatures match perfectly
                                onConversationClick = onNavigateToConversation,
                                filterPredicate = { conversation ->
                                    conversation.isIndividual && !conversation.isArchived
                                },
                                emptyStateTitle = "No individual messages",
                                emptyStateMessage = "Start a new conversation to begin messaging"
                            )
                        }
                        1 -> {
                            MessagesTab(
                                metroMessagesViewModel = metroMessagesViewModel,
                                modifier = Modifier.fillMaxWidth(),
                                onConversationClick = onNavigateToConversation,
                                filterPredicate = { conversation ->
                                    conversation.isGroup && !conversation.isArchived
                                },
                                emptyStateTitle = "No group conversations",
                                emptyStateMessage = "Create a group to start chatting with multiple people"
                            )
                        }
                        2 -> {
                            ArchiveTab(
                                metroMessagesViewModel = metroMessagesViewModel,
                                modifier = Modifier.fillMaxWidth(),
                                onConversationClick = onNavigateToConversation,
                                isAuthenticated = true,
                                onRequestAuthentication = {
                                    // No action needed - archive is always accessible
                                }
                            )
                        }
                    }
                }
            }
        },
        optionsContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                QuickLaunchIcons(
                    onNewConversation = {
                        println("New Conversation clicked - page to be built")
                    },
                    onNewSmsGroup = {
                        println("New SMS Group clicked - page to be built")
                    },
                    onRefreshConversations = {
                        metroMessagesViewModel.refreshAllConversations()
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun QuickLaunchIcons(
    onNewConversation: () -> Unit,
    onNewSmsGroup: () -> Unit,
    onRefreshConversations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable { onNewConversation() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Conversation",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable { onNewSmsGroup() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "New SMS Group",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable { onRefreshConversations() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Conversations",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

private fun calculateTabPosition(offset: Float, tabIndex: Int, totalTabs: Int, tabTravel: Float): Dp {
    return when {
        offset < tabIndex -> ((tabIndex - offset) * tabTravel).dp
        offset > tabIndex + 1 -> ((totalTabs - offset + tabIndex) * -tabTravel).dp
        else -> ((tabIndex - offset) * tabTravel).dp
    }
}
