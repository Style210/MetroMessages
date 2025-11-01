package com.metromessages.viewmodel

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.net.Uri
import android.telephony.SmsManager
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.attachments.AttachmentProcessorRepository
import com.metromessages.data.local.MessageType
import com.metromessages.data.model.facebook.FacebookConversationEntity
import com.metromessages.data.model.facebook.FacebookDao
import com.metromessages.data.model.facebook.FacebookMessageEntity
import com.metromessages.data.model.facebook.FacebookUiMessage
import com.metromessages.data.model.facebook.MessageStatus
import com.metromessages.data.model.facebook.PhoneNumberId
import com.metromessages.data.model.facebook.SimCard
import com.metromessages.data.model.facebook.SmsType
import com.metromessages.data.model.facebook.SyncState
import com.metromessages.data.repository.UnifiedContactRepository
import com.metromessages.mockdata.MockDataGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.geometry.Rect as ComposeRect

// Data class for media attachments
data class MediaAttachment(
    val uri: Uri,
    val type: MessageType,
    val thumbnailUri: Uri? = null,
    val isPlaceholder: Boolean = false
)

// Data class for media preview state
data class MediaPreviewState(
    val isVisible: Boolean = false,
    val mediaUris: List<Uri> = emptyList(),
    val initialIndex: Int = 0,
    val sourceViewBounds: ComposeRect? = null
)

// Data class for message draft state
data class MessageDraft(
    val text: String = "",
    val attachments: List<MediaAttachment> = emptyList()
)

@HiltViewModel
class FacebookViewModel @Inject constructor(
    private val facebookDao: FacebookDao,
    private val attachmentProcessor: AttachmentProcessorRepository,
    private val unifiedContactRepository: UnifiedContactRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    //region State Flows
    private val _facebookConversations = MutableStateFlow<List<FacebookConversationEntity>>(emptyList())
    private val _newMessages = MutableStateFlow<List<FacebookUiMessage>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isAuthenticated = MutableStateFlow(false)

    // Conversation-scoped drafts
    private val _messageDrafts = MutableStateFlow<Map<String, MessageDraft>>(emptyMap())
    val currentConversationId = MutableStateFlow<String?>(null)

    // Media preview state
    private val _mediaPreviewState = MutableStateFlow(MediaPreviewState())
    val mediaPreviewState: StateFlow<MediaPreviewState> = _mediaPreviewState.asStateFlow()

    // Dual SIM support
    private val _availableSimCards = MutableStateFlow<List<SimCard>>(emptyList())
    private val _preferredSimSlot = MutableStateFlow(0) // Default to SIM 1

    val availableSimCards: StateFlow<List<SimCard>> = _availableSimCards.asStateFlow()
    val preferredSimSlot: StateFlow<Int> = _preferredSimSlot.asStateFlow()

    // SMS Manager
    private val smsManager: SmsManager = SmsManager.getDefault()

    // Combined message draft for current conversation
    val messageDraft: StateFlow<MessageDraft> = combine(
        _messageDrafts,
        currentConversationId
    ) { drafts, currentId ->
        drafts[currentId] ?: MessageDraft()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessageDraft()
    )

    val facebookConversations: StateFlow<List<FacebookConversationEntity>> = _facebookConversations.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    //endregion

    init {
        loadFacebookConversations()
        setupRealtimeUpdates()
        setupSmsReceivers()
        initializeSimCards()

        viewModelScope.launch {
            migrateExistingData()
            delay(2000) // Wait for data to load
            debugContactLinking() // Debug contact linking
        }
    }

    //region Contact Resolution Methods
    suspend fun resolveContactIdFromConversation(conversationId: String): Long {
        println("🎯 [CONTACT RESOLUTION] Starting for: $conversationId")

        return when {
            conversationId.startsWith("sms_") -> {
                // Legacy SMS format - extract from threadId
                val conversation = facebookDao.getConversationById(conversationId)
                val threadId = conversation?.threadId ?: 0L
                println("📱 [SMS FORMAT] Thread ID: $threadId")
                threadId
            }

            conversationId.startsWith("conv_") -> {
                // PhoneNumberId format - use the proper system
                val phoneNumberId = PhoneNumberId.fromConversationId(conversationId)
                if (phoneNumberId != null) {
                    println("🔍 [PHONENUMBERID] Resolving: ${phoneNumberId.value}")

                    // Method 1: Get contact ID from conversation's linkedContactId
                    val conversation = facebookDao.getConversationByPhoneNumberId(phoneNumberId.value)
                    if (conversation?.linkedContactId != null) {
                        println("✅ [LINKED CONTACT] Found: ${conversation.linkedContactId}")
                        return conversation.linkedContactId
                    }

                    // Method 2: Try to find by phone number with normalization
                    val phoneNumber = phoneNumberId.toRawPhoneNumber()
                    if (phoneNumber != null) {
                        println("📞 [PHONE LOOKUP] Phone: $phoneNumber")
                        val normalizedPhone = normalizePhoneNumber(phoneNumber)

                        // Try exact match first
                        val conversationByPhone = facebookDao.getConversationByPhoneNumber(phoneNumber)

                        // Try normalized version if different
                        val normalizedConversation = if (phoneNumber != normalizedPhone) {
                            println("🔄 [NORMALIZED] Trying: $normalizedPhone")
                            facebookDao.getConversationByPhoneNumber(normalizedPhone)
                        } else null

                        val foundConversation = conversationByPhone ?: normalizedConversation
                        if (foundConversation?.linkedContactId != null) {
                            println("✅ [PHONE MATCH] Found contact: ${foundConversation.linkedContactId}")
                            return foundConversation.linkedContactId
                        }
                    }

                    // Method 3: Try to find in UnifiedContactRepository
                    if (phoneNumber != null) {
                        println("👥 [UNIFIED LOOKUP] Searching unified contacts for: $phoneNumber")
                        val unifiedContact = unifiedContactRepository.getContactByPhoneNumber(phoneNumber).first()
                        if (unifiedContact != null) {
                            println("✅ [UNIFIED FOUND] Contact: ${unifiedContact.id} - ${unifiedContact.displayName}")
                            // Link this conversation to the contact for future
                            linkConversationToContact(conversationId, unifiedContact.id)
                            return unifiedContact.id
                        }
                    }
                }

                println("❌ [NO CONTACT] No contact found for PhoneNumberId")
                0L
            }

            else -> {
                println("❌ [UNKNOWN FORMAT] Unknown conversation format: $conversationId")
                0L
            }
        }
    }

    suspend fun linkConversationToContact(conversationId: String, contactId: Long) {
        println("🔗 [LINKING] Conversation $conversationId → Contact $contactId")
        facebookDao.linkConversationToContact(conversationId, contactId)
    }

    suspend fun getConversationByPhoneNumberId(phoneNumberId: PhoneNumberId): FacebookConversationEntity? {
        return facebookDao.getConversationByPhoneNumberId(phoneNumberId.value)
    }

    // ADD: Phone number normalization helper
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "").trim()
    }

    // ADD: Debug method to verify data linking
    suspend fun debugContactLinking() {
        println("\n🔍 [DEBUG] Contact Linking Verification =========================")

        val allConversations = facebookDao.getFacebookConversations().first()
        val allContacts = unifiedContactRepository.getUnifiedContacts().first()

        println("📊 Total Conversations: ${allConversations.size}")
        println("📊 Total Contacts: ${allContacts.size}")

        var linkedCount = 0
        var unlinkedCount = 0

        allConversations.forEach { conversation ->
            val linkedContactId = conversation.linkedContactId
            val phoneNumber = conversation.getPhoneNumberIdObject().toRawPhoneNumber()

            println("\n💬 Conversation: ${conversation.displayName}")
            println("   📞 Phone: $phoneNumber")
            println("   🔗 Linked Contact ID: $linkedContactId")

            if (linkedContactId != null) {
                val contact = allContacts.find { it.id == linkedContactId }
                if (contact != null) {
                    println("   ✅ CONTACT FOUND: ${contact.displayName} (ID: ${contact.id})")
                    linkedCount++
                } else {
                    println("   ❌ CONTACT NOT FOUND for ID: $linkedContactId")
                    unlinkedCount++
                }
            } else {
                println("   ⚠️ NO LINKED CONTACT")
                unlinkedCount++
            }

            // Check phone number match
            if (phoneNumber != null) {
                val contactByPhone = allContacts.find { it.phoneNumber == phoneNumber }
                if (contactByPhone != null) {
                    println("   📱 Phone match found: ${contactByPhone.displayName}")
                } else {
                    // Try normalized version
                    val normalizedPhone = normalizePhoneNumber(phoneNumber)
                    val normalizedContact = allContacts.find { it.phoneNumber == normalizedPhone }
                    if (normalizedContact != null) {
                        println("   🔄 Normalized phone match: ${normalizedContact.displayName}")
                    } else {
                        println("   ❌ No phone match found")
                    }
                }
            }
        }

        println("\n📈 [SUMMARY] Linked: $linkedCount, Unlinked: $unlinkedCount")
        println("🔍 [DEBUG END] =============================================\n")
    }
    //endregion

    //region Cleanup and Refresh Methods
    fun cleanup() {
        viewModelScope.launch {
            currentConversationId.value = null
            _messageDrafts.value = emptyMap()
            _newMessages.value = emptyList()
            hideMediaPreview()
            _error.value = null

            try {
                context.unregisterReceiver(smsSentReceiver)
                context.unregisterReceiver(smsDeliveredReceiver)
            } catch (e: IllegalArgumentException) {
            }
        }
    }

    fun refreshConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val messages = facebookDao.getMessagesForConversation(conversationId).first()
                val conversation = facebookDao.getConversationById(conversationId)

                if (conversation != null) {
                    val updatedConversations = _facebookConversations.value.map {
                        if (it.id == conversationId) conversation else it
                    }
                    _facebookConversations.value = updatedConversations

                    _newMessages.update { newMessages ->
                        newMessages.filter { it.conversationId != conversationId }
                    }
                } else {
                    _error.value = "Conversation not found: $conversationId"
                }

            } catch (e: Exception) {
                _error.value = "Failed to refresh conversation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAllConversations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                loadFacebookConversations()
                unifiedContactRepository.refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to refresh conversations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    //endregion

    //region Media Attachment Methods
    suspend fun addMediaAttachments(mediaUris: List<Uri>) {
        try {
            val processedAttachments = attachmentProcessor.processMediaAttachments(mediaUris)

            _messageDrafts.update { drafts ->
                val currentId = currentConversationId.value ?: return@update drafts
                val currentDraft = drafts[currentId] ?: MessageDraft()

                val newAttachments = processedAttachments.map { processed ->
                    MediaAttachment(
                        uri = processed.uri,
                        type = processed.type
                    )
                }

                drafts + (currentId to currentDraft.copy(
                    attachments = currentDraft.attachments + newAttachments
                ))
            }
        } catch (e: Exception) {
            _error.value = "Failed to process media: ${e.message}"
        }
    }

    fun removeMediaAttachment(uri: String) {
        _messageDrafts.update { drafts ->
            val currentId = currentConversationId.value ?: return@update drafts
            val currentDraft = drafts[currentId] ?: return@update drafts

            drafts + (currentId to currentDraft.copy(
                attachments = currentDraft.attachments.filter { it.uri.toString() != uri }
            ))
        }
    }
    //endregion

    //region Media Preview Methods
    fun showMediaPreview(mediaUris: List<Uri>, initialIndex: Int = 0, sourceViewBounds: ComposeRect? = null) {
        _mediaPreviewState.value = MediaPreviewState(
            isVisible = true,
            mediaUris = mediaUris,
            initialIndex = initialIndex,
            sourceViewBounds = sourceViewBounds
        )
    }

    fun hideMediaPreview() {
        _mediaPreviewState.value = MediaPreviewState(isVisible = false)
    }

    fun shareMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
            } catch (e: Exception) {
                _error.value = "Failed to share media: ${e.message}"
            }
        }
    }

    fun downloadMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                _error.value = "Download functionality not yet implemented"
            } catch (e: Exception) {
                _error.value = "Failed to download media: ${e.message}"
            }
        }
    }

    fun androidRectToComposeRect(androidRect: Rect, density: Density): ComposeRect {
        return ComposeRect(
            left = androidRect.left / density.density,
            top = androidRect.top / density.density,
            right = androidRect.right / density.density,
            bottom = androidRect.bottom / density.density
        )
    }
    //endregion

    //region Dual SIM Methods
    private fun initializeSimCards() {
        viewModelScope.launch {
            val mockSimCards = SimCard.createMockSimCards()
            _availableSimCards.value = mockSimCards
            loadPreferredSimPreference()
        }
    }

    fun setPreferredSimSlot(slotIndex: Int) {
        if (_availableSimCards.value.any { it.slotIndex == slotIndex && it.isEnabled }) {
            _preferredSimSlot.value = slotIndex
            savePreferredSimPreference(slotIndex)
        }
    }

    fun getPreferredSimSlot(): Int = _preferredSimSlot.value

    private fun loadPreferredSimPreference() {
        _preferredSimSlot.value = 0
    }

    private fun savePreferredSimPreference(slotIndex: Int) {
    }
    //endregion

    //region SMS/MMS Sending
    fun sendRealSms(conversationId: String, text: String, simSlot: Int = _preferredSimSlot.value) {
        viewModelScope.launch {
            val conversation = getConversationById(conversationId)
            if (conversation == null) {
                _error.value = "Conversation not found"
                return@launch
            }

            val phoneNumber = conversation.getPhoneNumberIdObject().toRawPhoneNumber() ?: conversation.address ?: conversation.facebookId
            if (phoneNumber != null) {
                if (phoneNumber.isBlank() || !isValidPhoneNumber(phoneNumber)) {
                    _error.value = "Invalid phone number"
                    return@launch
                }
            }

            val timestamp = System.currentTimeMillis()
            val messageId = "sms_${timestamp}"

            try {
                val smsMessage = FacebookMessageEntity(
                    messageId = messageId,
                    conversationId = conversationId,
                    sender = "current_user_id",
                    body = text,
                    timestamp = timestamp,
                    isSentByUser = true,
                    messageType = MessageType.TEXT,
                    messageStatus = MessageStatus.SENDING,
                    syncState = SyncState.PENDING,
                    address = phoneNumber,
                    threadId = conversation.threadId,
                    smsType = SmsType.OUTBOX,
                    isMms = false,
                    simSlot = simSlot,
                    phoneNumberId = conversation.phoneNumberId
                )

                facebookDao.insertMessage(smsMessage)
                _newMessages.value = listOf(FacebookUiMessage.fromEntity(smsMessage)) + _newMessages.value

                val sentIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent("SMS_SENT").apply {
                        putExtra("message_id", messageId)
                        putExtra("conversation_id", conversationId)
                        putExtra("sim_slot", simSlot)
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )

                val deliveredIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent("SMS_DELIVERED").apply {
                        putExtra("message_id", messageId)
                        putExtra("conversation_id", conversationId)
                        putExtra("sim_slot", simSlot)
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )

                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    text,
                    sentIntent,
                    deliveredIntent
                )

                facebookDao.updateConversationLastMessage(conversationId, text, timestamp)

                val phoneNumberId = conversation.getPhoneNumberIdObject()
                unifiedContactRepository.updateRecentActivity(
                    phoneNumberId.toContactId() ?: 0L,
                    text,
                    timestamp
                )

                unifiedContactRepository.refreshData()
                loadFacebookConversations()

            } catch (e: Exception) {
                facebookDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
                _error.value = "Failed to send SMS: ${e.message}"
            }
        }
    }

    fun sendMmsMessage(conversationId: String, text: String, attachmentUris: List<Uri>, simSlot: Int = _preferredSimSlot.value) {
        viewModelScope.launch {
            val conversation = getConversationById(conversationId)
            if (conversation == null) {
                _error.value = "Conversation not found"
                return@launch
            }

            val participants = if (conversation.isSmsGroup) {
                conversation.participants
            } else {
                listOf(conversation.address ?: conversation.facebookId)
            }

            val timestamp = System.currentTimeMillis()
            val messageId = "mms_${timestamp}"

            try {
                val mmsMessage = FacebookMessageEntity(
                    messageId = messageId,
                    conversationId = conversationId,
                    sender = "current_user_id",
                    body = text.ifBlank { "[Media]" },
                    timestamp = timestamp,
                    isSentByUser = true,
                    messageType = if (attachmentUris.isNotEmpty()) getMessageTypeFromUris(attachmentUris) else MessageType.TEXT,
                    mediaUri = attachmentUris.firstOrNull()?.toString(),
                    messageStatus = MessageStatus.SENDING,
                    syncState = SyncState.PENDING,
                    address = conversation.address,
                    threadId = conversation.threadId,
                    smsType = SmsType.OUTBOX,
                    isMms = true,
                    participants = participants,
                    mmsSubject = text.ifBlank { "Shared media" },
                    simSlot = simSlot,
                    phoneNumberId = conversation.phoneNumberId
                )

                facebookDao.insertMessage(mmsMessage)
                _newMessages.value = listOf(FacebookUiMessage.fromEntity(mmsMessage)) + _newMessages.value

                delay(1000)
                facebookDao.updateMessageStatus(messageId, MessageStatus.SENT.name)

                val lastMessage = if (attachmentUris.isNotEmpty()) {
                    if (text.isNotBlank()) "[Media] $text" else "[Media]"
                } else {
                    text
                }

                facebookDao.updateConversationLastMessage(conversationId, lastMessage, timestamp)

                val phoneNumberId = conversation.getPhoneNumberIdObject()
                unifiedContactRepository.updateRecentActivity(
                    phoneNumberId.toContactId() ?: 0L,
                    lastMessage,
                    timestamp
                )

                unifiedContactRepository.refreshData()
                loadFacebookConversations()

            } catch (e: Exception) {
                facebookDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
                _error.value = "Failed to send MMS: ${e.message}"
            }
        }
    }

    private fun setupSmsReceivers() {
        val sentFilter = IntentFilter("SMS_SENT")
        val deliveredFilter = IntentFilter("SMS_DELIVERED")

        ContextCompat.registerReceiver(
            context,
            smsSentReceiver,
            sentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            context,
            smsDeliveredReceiver,
            deliveredFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    val messageId = intent.getStringExtra("message_id")
                    viewModelScope.launch {
                        messageId?.let {
                            facebookDao.updateMessageStatus(it, MessageStatus.SENT.name)
                        }
                    }
                }
                else -> {
                    val messageId = intent.getStringExtra("message_id")
                    viewModelScope.launch {
                        messageId?.let {
                            facebookDao.updateMessageStatus(it, MessageStatus.FAILED.name)
                        }
                    }
                }
            }
        }
    }

    private val smsDeliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (resultCode) {
                android.app.Activity.RESULT_OK -> {
                    val messageId = intent.getStringExtra("message_id")
                    viewModelScope.launch {
                        messageId?.let {
                            facebookDao.updateMessageStatus(it, MessageStatus.DELIVERED.name)
                        }
                    }
                }
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^[+]?[0-9\\-()\\s]{10,}$"))
    }

    private fun getMessageTypeFromUris(uris: List<Uri>): MessageType {
        return when {
            uris.any { it.toString().contains(".mp4") || it.toString().contains(".mov") } -> MessageType.VIDEO
            uris.any { it.toString().contains(".mp3") || it.toString().contains(".wav") } -> MessageType.AUDIO
            uris.any { it.toString().contains(".jpg") || it.toString().contains(".png") } -> MessageType.IMAGE
            else -> MessageType.FILE
        }
    }
    //endregion

    //region Data Loading
    fun loadFacebookConversations() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val existingConversations = facebookDao.getFacebookConversations().first()

                if (existingConversations.isEmpty()) {
                    val smsConversations = MockDataGenerator.generateSmsConversations(8)
                    val mmsGroups = MockDataGenerator.generateMmsGroupConversations(2)
                    val unknownConversations = MockDataGenerator.generateUnknownNumberConversations(3)
                    val allConversations = smsConversations + mmsGroups + unknownConversations

                    facebookDao.insertAllConversations(allConversations)

                    allConversations.forEach { conversation ->
                        val messages = MockDataGenerator.generateSmsMessages(conversation)
                        facebookDao.insertAllMessages(messages)
                    }

                    val conversationsFromDb = facebookDao.getFacebookConversations().first()
                    _facebookConversations.value = conversationsFromDb
                } else {
                    _facebookConversations.value = existingConversations

                    existingConversations.forEach { conversation ->
                        val existingMessages = facebookDao.getMessagesForConversation(conversation.id).first()
                        if (existingMessages.isEmpty()) {
                            val messages = MockDataGenerator.generateSmsMessages(conversation)
                            facebookDao.insertAllMessages(messages)
                        }
                    }
                }

            } catch (e: Exception) {
                _error.value = "Failed to load conversations: ${e.message}"
                val smsConversations = MockDataGenerator.generateSmsConversations(5)
                val unknownConversations = MockDataGenerator.generateUnknownNumberConversations(2)
                _facebookConversations.value = smsConversations + unknownConversations
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun migrateExistingData() {
        try {
            val conversationsNeedingMigration = facebookDao.getConversationsNeedingMigration()

            if (conversationsNeedingMigration.isNotEmpty()) {
                conversationsNeedingMigration.forEach { phoneNumber ->
                    val phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber)
                    val conversation = facebookDao.getConversationByPhoneNumber(phoneNumber)

                    conversation?.let {
                        facebookDao.updateConversationPhoneNumberId(it.id, phoneNumberId.value)
                        facebookDao.updateMessagesPhoneNumberId(it.id, phoneNumberId.value)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun setupRealtimeUpdates() {
        viewModelScope.launch {
            facebookDao.getFacebookConversations()
                .collect { conversations ->
                    if (!_isLoading.value) {
                        _facebookConversations.value = conversations
                    }
                }
        }
    }

    fun loadMessagesFromScraper(conversationId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            delay(1000)
            _isLoading.value = false
        }
    }
    //endregion

    //region Message Operations
    fun sendMessage(conversationId: String, text: String, simSlot: Int? = null) {
        viewModelScope.launch {
            val existingConversation = getConversationById(conversationId)
            if (existingConversation == null) {
                _error.value = "Conversation not found"
                return@launch
            }

            val draft = _messageDrafts.value[conversationId] ?: MessageDraft()
            val targetSimSlot = simSlot ?: _preferredSimSlot.value

            if (existingConversation.isSmsConversation) {
                if (draft.attachments.isEmpty()) {
                    sendRealSms(conversationId, text, targetSimSlot)
                } else {
                    sendMmsMessage(conversationId, text, draft.attachments.map { it.uri }, targetSimSlot)
                }
            } else {
                sendLegacyMessage(conversationId, text, draft)
            }

            clearCurrentDraft()
        }
    }

    private suspend fun sendLegacyMessage(conversationId: String, text: String, draft: MessageDraft) {
        val timestamp = System.currentTimeMillis()

        if (draft.attachments.isNotEmpty()) {
            val attachmentUris = draft.attachments.map { it.uri }
            attachmentProcessor.markFilesForPersistence(attachmentUris)

            draft.attachments.forEachIndexed { index, attachment ->
                val mediaMessage = FacebookMessageEntity(
                    messageId = "media_${timestamp}_$index",
                    conversationId = conversationId,
                    sender = "current_user_id",
                    body = attachment.uri.toString(),
                    timestamp = timestamp + index,
                    isSentByUser = true,
                    messageType = attachment.type,
                    messageStatus = MessageStatus.SENDING,
                    syncState = SyncState.PENDING,
                    phoneNumberId = "unknown"
                )
                facebookDao.insertMessage(mediaMessage)
                facebookDao.updateMessageStatus(mediaMessage.messageId, MessageStatus.SENT.name)
                _newMessages.value = listOf(FacebookUiMessage.fromEntity(mediaMessage)) + _newMessages.value
            }
        }

        if (text.isNotBlank()) {
            val textMessage = FacebookMessageEntity(
                messageId = "msg_${timestamp}",
                conversationId = conversationId,
                sender = "current_user_id",
                body = text,
                timestamp = timestamp + draft.attachments.size,
                isSentByUser = true,
                messageType = MessageType.TEXT,
                messageStatus = MessageStatus.SENDING,
                syncState = SyncState.PENDING,
                phoneNumberId = "unknown"
            )

            facebookDao.insertMessage(textMessage)
            facebookDao.updateMessageStatus(textMessage.messageId, MessageStatus.SENT.name)
            _newMessages.value = listOf(FacebookUiMessage.fromEntity(textMessage)) + _newMessages.value
        }

        val lastMessageText = when {
            draft.attachments.isNotEmpty() && text.isNotBlank() -> "[Media] $text"
            draft.attachments.isNotEmpty() -> if (draft.attachments.size == 1) "[Photo]" else "[${draft.attachments.size} photos]"
            else -> text
        }

        facebookDao.updateConversationLastMessage(conversationId, lastMessageText, timestamp)
        unifiedContactRepository.refreshData()
        loadFacebookConversations()
    }

    fun sendAudio(conversationId: String, audioPath: String, simSlot: Int? = null) {
        viewModelScope.launch {
            val conversation = getConversationById(conversationId)
            val isSms = conversation?.isSmsConversation == true
            val targetSimSlot = simSlot ?: _preferredSimSlot.value

            val timestamp = System.currentTimeMillis()
            val audioMessage = FacebookMessageEntity(
                messageId = "audio_${timestamp}",
                conversationId = conversationId,
                sender = "current_user_id",
                body = audioPath,
                timestamp = timestamp,
                isSentByUser = true,
                messageType = MessageType.AUDIO,
                messageStatus = if (isSms) MessageStatus.SENDING else MessageStatus.SENT,
                syncState = SyncState.PENDING,
                address = conversation?.address,
                threadId = conversation?.threadId,
                smsType = if (isSms) SmsType.OUTBOX else SmsType.SENT,
                isMms = isSms,
                simSlot = if (isSms) targetSimSlot else 0,
                phoneNumberId = conversation?.phoneNumberId ?: "unknown"
            )

            facebookDao.insertMessage(audioMessage)

            if (isSms) {
                delay(1000)
                facebookDao.updateMessageStatus(audioMessage.messageId, MessageStatus.SENT.name)
            }

            facebookDao.updateConversationLastMessage(conversationId, "[Audio message]", timestamp)
            _newMessages.value = listOf(FacebookUiMessage.fromEntity(audioMessage)) + _newMessages.value
            unifiedContactRepository.refreshData()
            loadFacebookConversations()
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<FacebookUiMessage>> {
        return combine(
            facebookDao.getMessagesForConversation(conversationId)
                .map { list -> list.map { FacebookUiMessage.fromEntity(it) } },
            _newMessages
        ) { dbMessages, newMessages ->
            (dbMessages + newMessages.filter { it.conversationId == conversationId })
                .distinctBy { it.id }
                .sortedBy { it.timestamp }
        }
    }
    //endregion

    //region Helper Functions
    private fun clearCurrentDraft() {
        val currentId = currentConversationId.value ?: return
        _messageDrafts.update { drafts ->
            drafts - currentId
        }
    }

    fun updateDraftText(text: String) {
        _messageDrafts.update { drafts ->
            val currentId = currentConversationId.value ?: return@update drafts
            val currentDraft = drafts[currentId] ?: MessageDraft()
            drafts + (currentId to currentDraft.copy(text = text))
        }
    }

    fun handleIncomingMedia(uri: Uri, mimeType: String?, text: String?) {
        viewModelScope.launch {
            try {
                val processedAttachments = attachmentProcessor.processMediaAttachments(listOf(uri))

                if (processedAttachments.isNotEmpty()) {
                    val processed = processedAttachments.first()
                    _messageDrafts.update { drafts ->
                        val currentId = currentConversationId.value ?: return@update drafts
                        val currentDraft = drafts[currentId] ?: MessageDraft()

                        val newAttachment = MediaAttachment(
                            uri = processed.uri,
                            type = processed.type
                        )

                        drafts + (currentId to currentDraft.copy(
                            attachments = currentDraft.attachments + newAttachment,
                            text = text ?: currentDraft.text
                        ))
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to process media: ${e.message}"
            }
        }
    }

    fun getConversationById(conversationId: String): FacebookConversationEntity? {
        return _facebookConversations.value.find { it.id == conversationId }
    }

    suspend fun debugGetConversation(conversationId: String): FacebookConversationEntity? {
        return facebookDao.getConversationById(conversationId)
    }

    suspend fun debugGetMessages(conversationId: String): List<FacebookMessageEntity> {
        return facebookDao.getMessagesForConversation(conversationId).first()
    }
    //endregion

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
