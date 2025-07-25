// File: SmsViewModel.kt
package com.metromessages.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.data.local.ConversationDao
import com.metromessages.data.model.ConversationEntity
import com.metromessages.data.model.UiMessage
import com.metromessages.data.model.UiMessageConverters
import com.metromessages.util.MockDataGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    @ApplicationContext private val context: Context // ✅ Needed for contact lookup
) : ViewModel() {

    private val _smsConversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val smsConversations: StateFlow<List<ConversationEntity>> = _smsConversations.asStateFlow()

    init {
        loadSmsConversations()
    }

    private fun loadSmsConversations() {
        viewModelScope.launch {
            conversationDao.getSmsConversations()
                .map { conversations ->
                    if (conversations.isEmpty()) {
                        MockDataGenerator.generateMockConversations(isFacebook = false)
                    } else {
                        conversations.map { convo ->
                            val photoUri = getContactPhotoUriForAddress(convo.address)
                            ConversationEntity(
                                id = convo.id,
                                name = convo.name,
                                address = convo.address,
                                lastMessage = convo.lastMessage,
                                timestamp = convo.timestamp,
                                isFacebook = false,
                                contactPhotoUrl = photoUri
                            )
                        }
                    }
                }
                .collectLatest { conversations ->
                    println("🧪 Loaded SMS conversations: ${conversations.size}")
                    conversations.forEach {
                        println("Name=${it.name}, Address=${it.address}, LastMsg=${it.lastMessage}, Photo=${it.contactPhotoUrl}")
                    }

                    _smsConversations.value = conversations
                }
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<UiMessage>> {
        return conversationDao.getMessagesForConversation(conversationId)
            .map { it.map { msg -> UiMessageConverters.fromSms(msg) } }
    }

    private fun getContactPhotoUriForAddress(phoneNumber: String): String? {
        val uri: Uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val photoUri = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                )
                return photoUri
            }
        }
        return null
    }
}
