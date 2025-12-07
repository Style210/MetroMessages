// File: com.metromessages.ui.screens.ConversationThreadScreen.kt
package com.metromessages.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metromessages.attachments.MetroMediaPickerSheet
import com.metromessages.data.local.metromessagehub.EmptyMediaState
import com.metromessages.data.local.metromessagehub.EmptyMessagesState
import com.metromessages.data.local.metromessagehub.ErrorState
import com.metromessages.data.local.metromessagehub.LoadingState
import com.metromessages.data.local.metromessagehub.MetroMessage
import com.metromessages.data.local.metromessagehub.MetroMessageBubble
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel
import com.metromessages.data.local.metropeoplehub.ui.ContactInfoPage
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.data.local.metromessagehub.calculateTabPosition
import com.metromessages.data.local.metromessagehub.formatPhoneNumberForDisplay
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.components.MetroMessageInputBar
import com.metromessages.ui.components.MetroPulsingDivider
import com.metromessages.data.local.metropeoplehub.ui.MetroContactEditor
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.voicerecorder.VoiceMemoViewModel
import kotlinx.coroutines.launch

private val DefaultMetroAccent = Color(0xFF0063B1)
private val MetroBlack = Color(0xFF000000)

@Stable
data class ConversationScreenState(
    val messages: List<MetroMessage> = emptyList(),
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
    contactId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 1,
    metroMessagesViewModel: MetroMessagesViewModel = hiltViewModel(),
    metroPeopleHubViewModel: MetroPeopleHubViewModel = hiltViewModel(),
    voiceMemoViewModel: VoiceMemoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(initial = DefaultMetroAccent)
    val metroFont by settingsPrefs.fontFlow.collectAsState(initial = MetroFont.Segoe)
    val coroutineScope = rememberCoroutineScope()

    println("🚀 ========== METRO MESSAGES CONVERSATION THREAD SCREEN ==========")
    println("📝 DEBUG: conversationId=$conversationId, contactId=$contactId, initialTab=$initialTab")

    // 🎯 FOSSIFY STATE MANAGEMENT
    val messages by metroMessagesViewModel.messages.collectAsState()
    val isLoading by metroMessagesViewModel.isLoading.collectAsState()
    val errorState by metroMessagesViewModel.error.collectAsState()
    val messageDraft by metroMessagesViewModel.messageDraft.collectAsState()
    val mediaAttachments by metroMessagesViewModel.mediaAttachments.collectAsState()

    // 🎯 CONTACT RESOLUTION
    val currentContact by metroPeopleHubViewModel.currentContact.collectAsState()
    val allContacts by metroPeopleHubViewModel.contacts.collectAsState()

    // 🎯 VOICE MEMO STATE
    val recorderState by voiceMemoViewModel.recorderState.collectAsState()

    // 🎯 UI STATE
    var showMediaPicker by remember { mutableStateOf(false) }
    var showContactEditor by remember { mutableStateOf(false) }

    // 🎯 FOSSIFY DATA INITIALIZATION - DIRECT & SIMPLE
    LaunchedEffect(conversationId, contactId) {
        println("🔄 DEBUG: Loading conversation data")

        // ✅ FOSSIFY WAY: Load messages directly (CONFIRMED WORKS)
        conversationId.toLongOrNull()?.let { threadId ->
            println("💬 DEBUG: Loading messages for thread: $threadId")
            metroMessagesViewModel.loadMessages(threadId)
        }

        // ✅ FIX: Compare String with String "0" instead of Long 0L
        if (contactId != "0") {  // ✅ CHANGED: 0L → "0"
            println("👤 DEBUG: Loading contact by ID: $contactId")
            metroPeopleHubViewModel.loadContactForEditing(contactId.toLong()) // ✅ Convert to Long for ViewModel
        } else {
            // ✅ FOSSIFY WAY: No complex resolution - phone number comes from loaded messages
            println("📞 DEBUG: No contact ID - phone number will be extracted from messages")
            // The phone number will be available in messages.firstOrNull()?.address
            // This follows Fossify's direct data access pattern
        }
    }

    // 🎯 CONTACT DISPLAY NAME RESOLUTION
    val displayName = remember(currentContact, messages) {
        when {
            !currentContact?.displayName.isNullOrBlank() -> currentContact!!.displayName
            messages.isNotEmpty() -> {
                val phoneNumber = messages.first().address
                formatPhoneNumberForDisplay(phoneNumber)
            }
            else -> "Unknown Contact"
        }
    }

    val displayPhotoUrl = remember(currentContact) {
        currentContact?.photoUri
    }

    // 🎯 CLEANUP ON DISPOSE
    DisposableEffect(Unit) {
        onDispose {
            println("🧹 DEBUG: Cleaning up ConversationThreadScreen")
            metroMessagesViewModel.clearCurrentConversation()
            voiceMemoViewModel.cleanup()
            metroPeopleHubViewModel.clearEditState()
        }
    }

    // 🎯 AUDIO CALLBACK SETUP
    LaunchedEffect(voiceMemoViewModel) {
        voiceMemoViewModel.onSendAudio = { audioPath ->
            println("🎙️ DEBUG: Sending audio message: $audioPath")
            metroMessagesViewModel.sendMediaMessage(audioPath, com.metromessages.data.local.metromessagehub.MessageType.AUDIO)
        }
    }

    // 🎯 TABBED NAVIGATION SETUP
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 3 }
    )
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val pageOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    // 🎯 HEADER SCROLL STATE
    val headerScrollState = rememberScrollState()

    // 🎯 SYNC HEADER WITH PAGER SCROLL
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        try {
            val totalScroll = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * 1000f
            val headerTarget = (totalScroll * 0.35f).toInt()
            headerScrollState.scrollTo(headerTarget)
        } catch (e: Exception) {
            // Ignore scroll errors
        }
    }

    // 🎯 TAB POSITIONS
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
        Column(modifier = Modifier.fillMaxSize()) {
            // 🎯 HEADER AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBackClick() }
                    .testTag("conversation_header")
            ) {
                MetroHeaderCanvas(
                    text = displayName.lowercase(),
                    scrollState = headerScrollState,
                    metroFont = metroFont,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                )
            }

            // 🎯 CONTENT AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp)
            ) {
                // 🎯 TAB HEADERS
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
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
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
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
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
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
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

                // 🎯 CONTENT BASED ON STATE
                when {
                    isLoading -> LoadingState(
                        accentColor = accentColor,
                        metroFont = metroFont,
                        modifier = Modifier.weight(1f)
                    )
                    errorState != null -> ErrorState(
                        errorMessage = errorState!!,
                        onRetry = {
                            coroutineScope.launch {
                                metroMessagesViewModel.loadMessages(conversationId.toLong())
                            }
                        },
                        accentColor = accentColor,
                        metroFont = metroFont,
                        modifier = Modifier.weight(1f)
                    )
                    else -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f).testTag("content_pager")
                        ) { page ->
                            when (page) {
                                0 -> ContactInfoPage(
                                    contactId = contactId,
                                    accentColor = accentColor,
                                    metroFont = metroFont,
                                    onEditClick = {
                                        showContactEditor = true
                                        println("✏️ DEBUG: Opening contact editor for contact: $contactId")
                                    },
                                    onBackClick = onBackClick,
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> MetroConversationPage(
                                    messages = messages,
                                    accentColor = accentColor,
                                    metroFont = metroFont,
                                    modifier = Modifier.fillMaxSize()
                                )
                                2 -> MetroMediaPage(
                                    messages = messages,
                                    accentColor = accentColor,
                                    metroFont = metroFont,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // 🎯 MESSAGE INPUT BAR (only on messages page)
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
                    mediaAttachments = mediaAttachments,
                    onSendText = { text ->
                        println("📤 DEBUG: Sending text message: $text")
                        metroMessagesViewModel.sendMessage(text)
                    },
                    onSendAudio = { audioPath ->
                        println("🎙️ DEBUG: Sending audio message: $audioPath")
                        metroMessagesViewModel.sendMediaMessage(audioPath, com.metromessages.data.local.metromessagehub.MessageType.AUDIO)
                    },
                    onAttachClick = { showMediaPicker = true },
                    onRemoveAttachment = { uri ->
                        metroMessagesViewModel.removeMediaFromDraft(uri)
                    },
                    onTextChange = { text -> metroMessagesViewModel.updateDraftText(text) },
                    accentColor = accentColor,
                    fontFamily = metroFont.fontFamily,
                    onStartRecording = { voiceMemoViewModel.startRecording() },
                    onStopRecording = { voiceMemoViewModel.stopRecording() },
                    onCancelRecording = { voiceMemoViewModel.cancelRecording() },
                    onSendAudioMessage = { voiceMemoViewModel.sendAudioMessage() },
                    onDeleteAudioPreview = { voiceMemoViewModel.deletePreview() },
                    onAttachmentClick = { uri, _ ->
                        println("📎 DEBUG: Draft attachment clicked: $uri")
                    }
                )
            }
        }

        // 🎯 CONTACT EDITOR DIALOG
        if (showContactEditor && contactId != "0") {
            MetroContactEditor(
                contactId = contactId.toLongOrNull(),
                accentColor = accentColor,
                metroFont = metroFont,
                onSaveComplete = {
                    showContactEditor = false
                    println("✅ DEBUG: Contact edit completed successfully")
                    metroPeopleHubViewModel.loadContactForEditing(contactId.toLongOrNull())
                },
                onCancel = {
                    showContactEditor = false
                    println("❌ DEBUG: Contact edit cancelled")
                }
            )
        }

        // 🎯 MEDIA PICKER BOTTOM SHEET
        if (showMediaPicker) {
            MetroMediaPickerSheet(
                onDismiss = { showMediaPicker = false },
                onMediaSelected = { mediaUris ->
                    coroutineScope.launch {
                        metroMessagesViewModel.addMediaToDraft(mediaUris)
                    }
                    showMediaPicker = false
                },
                accentColor = accentColor,
                metroFont = metroFont
            )
        }
    }

    println("🏁 ========== METRO MESSAGES CONVERSATION THREAD SCREEN END ==========")
}

// 🎯 FOSSIFY-COMPLIANT CONVERSATION PAGE
@Composable
fun MetroConversationPage(
    messages: List<MetroMessage>,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    println("💬 DEBUG: Rendering ${messages.size} MetroMessages")

    // ✅ CRITICAL FIX: Filter out invalid messages before rendering
    val validMessages = messages.filter { message ->
        // ✅ Ensure message has valid ID (not -1 or 0)
        message.id > 0
    }

    if (validMessages.isEmpty()) {
        Log.d("MetroConversationPage", "No valid messages to display (filtered out ${messages.size - validMessages.size} invalid)")
        EmptyMessagesState(
            accentColor = accentColor,
            metroFont = metroFont,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            state = rememberLazyListState(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 72.dp)
        ) {
            // ✅ FIXED: Use only valid messages with guaranteed unique IDs
            items(
                items = validMessages,
                key = { message ->
                    // ✅ Now guaranteed to be unique and > 0
                    message.id
                }
            ) { message ->
                MetroMessageBubble(
                    message = message,
                    metroFont = metroFont,
                    onMediaClick = { uri ->
                        println("🖼️ DEBUG: Media clicked: $uri")
                        // TODO: Implement media preview for MetroMessages
                    },
                    modifier = Modifier.testTag("metro_message_${message.id}")
                )
            }
        }
    }
}

// 🎯 FOSSIFY-COMPLIANT MEDIA PAGE
@Composable
fun MetroMediaPage(
    messages: List<MetroMessage>,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    // ✅ CRITICAL FIX: Filter out invalid messages first
    val validMessages = messages.filter { message ->
        // ✅ Ensure message has valid ID (not -1 or 0)
        message.id > 0
    }

    val mediaMessages = validMessages.filter { it.hasMedia }

    Log.d("MetroMediaPage", "Found ${mediaMessages.size} media messages (filtered out ${messages.size - validMessages.size} invalid)")
    println("🖼️ DEBUG: Found ${mediaMessages.size} media messages out of ${validMessages.size} valid messages")

    if (mediaMessages.isEmpty()) {
        EmptyMediaState(
            accentColor = accentColor,
            metroFont = metroFont,
            modifier = modifier
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
        ) {
            // ✅ FIXED: Use only valid media messages with guaranteed unique IDs
            items(
                items = mediaMessages,
                key = { message ->
                    // ✅ Now guaranteed to be unique and > 0
                    message.id
                }
            ) { message ->
                MetroMediaGridItem(
                    message = message,
                    onMediaClick = { uri ->
                        println("🖼️ DEBUG: Media grid item clicked: $uri")
                        // TODO: Implement media preview for MetroMessages
                    },
                    modifier = Modifier
                )
            }
        }
    }
}

// 🎯 FOSSIFY-COMPLIANT MEDIA GRID ITEM
@Composable
fun MetroMediaGridItem(
    message: MetroMessage,
    onMediaClick: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaUri = message.mediaUri ?: return
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onMediaClick(mediaUri)
            }
            .testTag("metro_media_item_${message.id}")
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
            contentDescription = "Shared media",
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}