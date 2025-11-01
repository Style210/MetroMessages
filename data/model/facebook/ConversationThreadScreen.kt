package com.metromessages.data.model.facebook

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metromessages.attachments.MediaPreviewOverlay
import com.metromessages.attachments.MetroMediaPickerSheet
import com.metromessages.data.local.peoplescreen.ContactEdits
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.components.MetroContactCard
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.components.MetroMessageInputBar
import com.metromessages.ui.components.MetroPulsingDivider
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.viewmodel.ConversationViewModel
import com.metromessages.viewmodel.FacebookViewModel
import com.metromessages.voicerecorder.VoiceMemoViewModel
import kotlinx.coroutines.launch

private val DefaultMetroAccent = Color(0xFF0063B1)
private val MetroBlack = Color(0xFF000000)

@Stable
data class ConversationScreenState(
    val messages: List<FacebookUiMessage> = emptyList(),
    val currentTab: Int = 1,
    val showMediaPicker: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val displayName: String = "Unknown Contact",
    val displayPhotoUrl: String? = null
)

@SuppressLint("UseOfNonLambdaOffsetOverload", "RememberReturnType")
@Composable
fun ConversationThreadScreen(
    conversationId: String,
    contactId: Long,
    conversationViewModel: ConversationViewModel,
    facebookViewModel: FacebookViewModel,
    voiceMemoViewModel: VoiceMemoViewModel,
    onSendText: (String) -> Unit,
    onSendAudio: (String) -> Unit,
    onSendMedia: (List<android.net.Uri>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 1
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(initial = DefaultMetroAccent)
    val metroFont by settingsPrefs.fontFlow.collectAsState(initial = MetroFont.Segoe)
    val coroutineScope = rememberCoroutineScope()

    // State management
    var isLoading by remember { mutableStateOf(false) }
    var errorState by remember { mutableStateOf<String?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }

    // ✅ SINGLE SOURCE OF TRUTH: Use ConversationViewModel for contact data
    val conversationData by conversationViewModel
        .getConversationWithData(conversationId, contactId)
        .collectAsState(initial = null)

    // ✅ Use FacebookViewModel for messages (the actual message source)
    val messages by facebookViewModel.getMessagesForConversation(conversationId)
        .collectAsState(initial = emptyList())

    // ✅ ENHANCED: Get conversation details for unknown contacts
    val conversationDetails by facebookViewModel.facebookConversations.collectAsState()
    val currentConversation = remember(conversationId, conversationDetails) {
        conversationDetails.find { it.id == conversationId }
    }

    // ✅ PROPER UNKNOWN CONTACT HANDLING: Use phone number as display name
    val displayName = remember(conversationData, currentConversation) {
        when {
            conversationData?.contact?.displayName != null -> conversationData!!.contact?.displayName
            currentConversation?.displayName != null -> currentConversation.displayName
            currentConversation?.address != null -> formatPhoneNumberForDisplay(currentConversation.address)
            currentConversation?.facebookId != null -> formatPhoneNumberForDisplay(
                currentConversation.facebookId
            )
            else -> "Unknown Contact"
        }
    }

    val displayPhotoUrl = remember(conversationData, currentConversation) {
        conversationData?.contact?.photoUri ?: currentConversation?.avatarUrl
    }

    // ✅ Force refresh on composition to ensure data consistency
    LaunchedEffect(conversationId, contactId) {
        try {
            isLoading = true
            errorState = null

            // Refresh both data sources when screen loads
            conversationViewModel.refreshUnifiedData()
            facebookViewModel.refreshConversation(conversationId)

        } catch (e: Exception) {
            errorState = "Failed to load conversation: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // ✅ PERFORMANCE: Filter messages only when needed
    val visibleMessages by remember(messages) {
        derivedStateOf {
            messages.filter { it.shouldDisplay }
        }
    }

    val mediaMessages by remember(messages) {
        derivedStateOf {
            messages.filter { it.mediaUri != null }
        }
    }

    val messageDraft by facebookViewModel.messageDraft.collectAsState()
    val mediaPreviewState by facebookViewModel.mediaPreviewState.collectAsState()

    // ✅ MEMORY MANAGEMENT: Clear current conversation when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            facebookViewModel.currentConversationId.value = null
            voiceMemoViewModel.cleanup()
        }
    }

    // Set the audio callback for the ViewModel
    LaunchedEffect(voiceMemoViewModel) {
        voiceMemoViewModel.onSendAudio = { audioPath ->
            try {
                onSendAudio(audioPath)
            } catch (e: Exception) {
                errorState = "Failed to send audio: ${e.message}"
            }
        }
    }

    // Get the recorder state from ViewModel
    val recorderState by voiceMemoViewModel.recorderState.collectAsState()

    val listState = rememberLazyListState()
    LaunchedEffect(visibleMessages.size) {
        try {
            if (visibleMessages.isNotEmpty()) {
                listState.animateScrollToItem(visibleMessages.size - 1)
            }
        } catch (e: Exception) {
            // Ignore scroll errors, they're not critical
        }
    }

    // TABBED NAVIGATION SETUP - USE initialTab
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 3 }
    )
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val pageOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    // INDEPENDENT SCROLL STATES FOR METRO HEADER
    val headerScrollState = rememberScrollState()

    // Sync header with pager scroll (35% parallax effect)
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        try {
            val totalScroll = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * 1000f
            val headerTarget = (totalScroll * 0.35f).toInt()
            headerScrollState.scrollTo(headerTarget)
        } catch (e: Exception) {
            // Ignore scroll errors
        }
    }

    // ✅ PERFORMANCE: Calculate tab positions efficiently
    val tabTravel = 200.dp
    val contactX by remember(pageOffset, currentPage) {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 0, 3, tabTravel.value)
        }
    }

    val messagesX by remember(pageOffset, currentPage) {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 1, 3, tabTravel.value)
        }
    }

    val mediaX by remember(pageOffset, currentPage) {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 2, 3, tabTravel.value)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent).testTag("conversation_screen")) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header area (fixed height) - UPDATED: Make entire header clickable for back
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onBackClick()
                    }
                    .testTag("conversation_header")
            ) {
                if (displayName != null) {
                    MetroHeaderCanvas(
                        text = displayName.lowercase(),
                        scrollState = headerScrollState,
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxSize(),
                        textColor = Color.White,
                        opacity = 1f,
                    )
                }
            }

            // Content area (takes remaining space)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp)
            ) {
                // Tab headers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("conversation_tabs")
                ) {
                    // Contact tab
                    Text(
                        text = "contact",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = contactX)
                            .alpha(if (currentPage == 0) 1f else 0.6f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            .testTag("contact_tab"),
                        style = MetroTypography.MetroSubhead(metroFont).copy(
                            fontSize = 44.sp,
                            lineHeight = 53.sp,
                            letterSpacing = (-1.5).sp,
                            color = Color.White
                        )
                    )

                    // Messages tab
                    Text(
                        text = "thread",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = messagesX)
                            .alpha(if (currentPage == 1) 1f else 0.6f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                            .testTag("messages_tab"),
                        style = MetroTypography.MetroSubhead(metroFont).copy(
                            fontSize = 44.sp,
                            lineHeight = 53.sp,
                            letterSpacing = (-1.5).sp,
                            color = Color.White
                        )
                    )

                    // Media tab
                    Text(
                        text = "shared",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = mediaX)
                            .alpha(if (currentPage == 2) 1f else 0.6f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                            .testTag("media_tab"),
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
                            .offset(y = (-4).dp)
                            .testTag("tab_indicator"),
                        initialWidth = 100.dp,
                        maxTravel = 8.dp
                    )
                }

                // ✅ ERROR HANDLING: Show error state if needed
                when {
                    isLoading -> {
                        LoadingState(
                            accentColor = accentColor,
                            metroFont = metroFont,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    errorState != null -> {
                        ErrorState(
                            errorMessage = errorState!!,
                            onRetry = {
                                errorState = null
                                // Trigger reload
                                coroutineScope.launch {
                                    facebookViewModel.refreshConversation(conversationId)
                                }
                            },
                            accentColor = accentColor,
                            metroFont = metroFont,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        // Content pager - takes remaining space after tabs
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("content_pager")
                        ) { page ->
                            when (page) {
                                0 -> { // CONTACT PAGE
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Transparent) // ✅ ADDED: Transparent wrapper
                                    ) {
                                        ContactInfoPage(
                                            unifiedContact = conversationData?.contact,
                                            accentColor = accentColor,
                                            metroFont = metroFont,
                                            onContactUpdated = { contactEdits ->
                                                try {
                                                    conversationViewModel.updateContact(contactId, contactEdits)
                                                } catch (e: Exception) {
                                                    errorState = "Failed to update contact: ${e.message}"
                                                }
                                            },
                                            onFavoriteToggle = { contactId, starred ->
                                                try {
                                                    conversationViewModel.toggleFavorite(contactId, starred)
                                                } catch (e: Exception) {
                                                    errorState = "Failed to toggle favorite: ${e.message}"
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                1 -> { // MESSAGES PAGE
                                    ConversationPage(
                                        messages = visibleMessages,
                                        listState = listState,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        onMediaClick = { uri, bounds ->
                                            try {
                                                val allMediaUris = visibleMessages
                                                    .filter { it.mediaUri != null }
                                                    .map { it.mediaUri!! }

                                                if (allMediaUris.isNotEmpty()) {
                                                    val initialIndex = allMediaUris.indexOfFirst { it == uri }
                                                    if (initialIndex >= 0) {
                                                        val composeBounds = facebookViewModel.androidRectToComposeRect(bounds, density)
                                                        facebookViewModel.showMediaPreview(allMediaUris, initialIndex, composeBounds)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                errorState = "Failed to show media: ${e.message}"
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                2 -> { // MEDIA PAGE
                                    MediaPage(
                                        messages = mediaMessages,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        onMediaClick = { uri, bounds ->
                                            try {
                                                val allMediaUris = mediaMessages
                                                    .filter { it.mediaUri != null }
                                                    .map { it.mediaUri!! }

                                                if (allMediaUris.isNotEmpty()) {
                                                    val initialIndex = allMediaUris.indexOfFirst { it == uri }
                                                    if (initialIndex >= 0) {
                                                        val composeBounds = facebookViewModel.androidRectToComposeRect(bounds, density)
                                                        facebookViewModel.showMediaPreview(allMediaUris, initialIndex, composeBounds)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                errorState = "Failed to show media: ${e.message}"
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MESSAGE INPUT BAR (only show on messages page)
            if (currentPage == 1 && errorState == null) {
                MetroMessageInputBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MetroBlack)
                        .padding(bottom = 8.dp)
                        .testTag("message_input_bar"),
                    voiceRecorder = voiceMemoViewModel.voiceRecorder,
                    recorderState = recorderState,
                    messageDraft = messageDraft,
                    onSendText = { text ->
                        try {
                            onSendText(text)
                        } catch (e: Exception) {
                            errorState = "Failed to send message: ${e.message}"
                        }
                    },
                    onSendAudio = { audioPath ->
                        try {
                            onSendAudio(audioPath)
                        } catch (e: Exception) {
                            errorState = "Failed to send audio: ${e.message}"
                        }
                    },
                    onAttachClick = { showMediaPicker = true },
                    onRemoveAttachment = { uri ->
                        try {
                            facebookViewModel.removeMediaAttachment(uri)
                        } catch (e: Exception) {
                            errorState = "Failed to remove attachment: ${e.message}"
                        }
                    },
                    onTextChange = { text ->
                        facebookViewModel.updateDraftText(text)
                    },
                    accentColor = accentColor,
                    fontFamily = metroFont.fontFamily,
                    onStartRecording = {
                        try {
                            voiceMemoViewModel.startRecording()
                        } catch (e: Exception) {
                            errorState = "Failed to start recording: ${e.message}"
                        }
                    },
                    onStopRecording = {
                        try {
                            voiceMemoViewModel.stopRecording()
                        } catch (e: Exception) {
                            errorState = "Failed to stop recording: ${e.message}"
                        }
                    },
                    onCancelRecording = {
                        try {
                            voiceMemoViewModel.cancelRecording()
                        } catch (e: Exception) {
                            errorState = "Failed to cancel recording: ${e.message}"
                        }
                    },
                    onSendAudioMessage = {
                        try {
                            voiceMemoViewModel.sendAudioMessage()
                        } catch (e: Exception) {
                            errorState = "Failed to send audio message: ${e.message}"
                        }
                    },
                    onDeleteAudioPreview = {
                        try {
                            voiceMemoViewModel.deletePreview()
                        } catch (e: Exception) {
                            errorState = "Failed to delete audio preview: ${e.message}"
                        }
                    },
                    onAttachmentClick = { uri, bounds ->
                        try {
                            val draftMediaUris = messageDraft.attachments.map { it.uri }
                            if (draftMediaUris.isNotEmpty()) {
                                val initialIndex = draftMediaUris.indexOfFirst { it == uri }
                                if (initialIndex >= 0) {
                                    val composeBounds = facebookViewModel.androidRectToComposeRect(bounds, density)
                                    facebookViewModel.showMediaPreview(draftMediaUris, initialIndex, composeBounds)
                                }
                            }
                        } catch (e: Exception) {
                            errorState = "Failed to preview attachment: ${e.message}"
                        }
                    }
                )
            }
        }

        // Media Picker Bottom Sheet
        if (showMediaPicker) {
            MetroMediaPickerSheet(
                onDismiss = { showMediaPicker = false },
                onMediaSelected = { mediaUris ->
                    coroutineScope.launch {
                        try {
                            facebookViewModel.addMediaAttachments(mediaUris)
                        } catch (e: Exception) {
                            errorState = "Failed to add media: ${e.message}"
                        }
                    }
                    showMediaPicker = false
                },
                accentColor = accentColor,
                metroFont = metroFont
            )
        }

        // Media Preview Overlay
        MediaPreviewOverlay(
            isVisible = mediaPreviewState.isVisible,
            mediaUris = mediaPreviewState.mediaUris,
            initialIndex = mediaPreviewState.initialIndex,
            onDismiss = { facebookViewModel.hideMediaPreview() },
            onShare = { uri ->
                try {
                    facebookViewModel.shareMedia(uri)
                } catch (e: Exception) {
                    errorState = "Failed to share media: ${e.message}"
                }
            },
            onDownload = { uri ->
                try {
                    facebookViewModel.downloadMedia(uri)
                } catch (e: Exception) {
                    errorState = "Failed to download media: ${e.message}"
                }
            }
        )
    }
}

@Composable
fun ContactInfoPage(
    unifiedContact: com.metromessages.data.repository.UnifiedContact?,
    accentColor: Color,
    metroFont: MetroFont,
    onContactUpdated: (ContactEdits) -> Unit = {},
    onFavoriteToggle: (Long, Boolean) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    // Create a fallback contact if unifiedContact is null
    val contact = unifiedContact ?: com.metromessages.data.repository.UnifiedContact(
        id = 0,
        displayName = "Unknown Contact",
        photoUri = null,
        starred = false,
        smsConversationId = null,
        phoneNumber = null,
        hasUnreadMessages = false,
        hasMissedCalls = false,
        lastActivity = System.currentTimeMillis(),
        lastMessage = null,
        lastActivityType = com.metromessages.data.repository.ActivityType.SMS_MESSAGE
    )

    val detailedContact: com.metromessages.data.local.peoplescreen.PersonWithDetails? = null

    MetroContactCard(
        contact = contact,
        detailedContact = detailedContact,
        accentColor = accentColor,
        metroFont = metroFont,
        onFavoriteToggle = onFavoriteToggle,
        onContactUpdated = onContactUpdated,
        modifier = modifier.testTag("contact_card")
    )
}

@Composable
fun ConversationPage(
    messages: List<FacebookUiMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    accentColor: Color,
    metroFont: MetroFont,
    onMediaClick: (android.net.Uri, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        MetroEmptyState(
            accentColor = accentColor,
            metroFont = metroFont,
            modifier = modifier.testTag("empty_messages_state")
        )
    } else {
        LazyColumn(
            modifier = modifier.testTag("message_list"),
            state = listState,
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(messages, key = { it.id }) { facebookMessage ->
                FacebookMessageBubble(
                    message = facebookMessage,
                    accentColor = accentColor,
                    metroFont = metroFont,
                    onMediaClick = onMediaClick,
                    modifier = Modifier.testTag("message_bubble_${facebookMessage.id}")
                )
            }
        }
    }
}

@Composable
fun MediaPage(
    messages: List<FacebookUiMessage>,
    accentColor: Color,
    metroFont: MetroFont,
    onMediaClick: (android.net.Uri, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaMessages = messages.filter { it.mediaUri != null }

    if (mediaMessages.isEmpty()) {
        Box(
            modifier = modifier.testTag("empty_media_state"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No shared media yet",
                color = accentColor.copy(alpha = 0.6f),
                style = MetroTypography.MetroBody2(metroFont)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.testTag("media_grid"),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(mediaMessages, key = { it.id }) { message ->
                MetroMediaGridItem(
                    message = message,
                    onMediaClick = onMediaClick,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun MetroEmptyState(
    accentColor: Color = DefaultMetroAccent,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    metroFont: MetroFont
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No messages yet",
            color = accentColor.copy(alpha = 0.6f),
            style = MetroTypography.MetroBody2(metroFont)
        )
    }
}

@Composable
private fun LoadingState(
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading conversation...",
            color = accentColor.copy(alpha = 0.6f),
            style = MetroTypography.MetroBody2(metroFont)
        )
    }
}

@Composable
private fun ErrorState(
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
            Text(
                text = "Something went wrong",
                color = accentColor,
                style = MetroTypography.MetroBody1(metroFont),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = errorMessage,
                color = accentColor.copy(alpha = 0.6f),
                style = MetroTypography.MetroBody2(metroFont),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
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

// Helper function for tab positioning
private fun calculateTabPosition(offset: Float, tabIndex: Int, totalTabs: Int, tabTravel: Float): Dp {
    return when {
        offset < tabIndex -> ((tabIndex - offset) * tabTravel).dp
        offset > tabIndex + 1 -> ((totalTabs - offset + tabIndex) * -tabTravel).dp
        else -> ((tabIndex - offset) * tabTravel).dp
    }
}

// Extension property for message filtering
private val FacebookUiMessage.shouldDisplay: Boolean
    get() = body.isNotBlank() || mediaUri != null

@Composable
fun MetroMediaGridItem(
    message: FacebookUiMessage,
    onMediaClick: (android.net.Uri, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaUri = message.mediaUri ?: return

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val bounds = Rect(0, 0, 100, 100) // Placeholder
                onMediaClick(mediaUri, bounds)
            }
            .testTag("media_grid_item_${message.id}")
    ) {
        AsyncImage(
            model = mediaUri,
            contentDescription = "Shared media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = remember {
                ColorPainter(Color(0xFF1A1A1A))
            },
            error = remember {
                ColorPainter(Color(0xFF2A2A2A))
            }
        )
    }
}

// ✅ ADD: Phone number formatting helper for unknown contacts
private fun formatPhoneNumberForDisplay(phoneNumber: String): String {
    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

    return when {
        digitsOnly.length == 10 -> {
            "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
        }
        digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
            "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
        }
        else -> {
            // Return the original with basic formatting
            if (phoneNumber.contains("-")) phoneNumber else {
                val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
                if (cleaned.length <= 3) cleaned
                else if (cleaned.length <= 6) "${cleaned.substring(0, 3)}-${cleaned.substring(3)}"
                else "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
        }
    }
}
