package com.metromessages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.data.local.FacebookDao
import com.metromessages.data.model.ConversationEntity
import com.metromessages.data.model.UiMessage
import com.metromessages.data.model.UiMessageConverters
import com.metromessages.util.MockDataGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FacebookViewModel @Inject constructor(
    private val facebookDao: FacebookDao
) : ViewModel() {

    private val _facebookConversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val facebookConversations: StateFlow<List<ConversationEntity>> = _facebookConversations.asStateFlow()

    init {
        println("🟡 Initializing FacebookViewModel")
        loadFacebookConversations()
    }

    private fun loadFacebookConversations() {
        viewModelScope.launch {
            facebookDao.getAllMessages()
                .map { messages ->
                    println("🔵 Raw DB messages: ${messages.size}")

                    if (messages.isEmpty()) {
                        println("🟠 No messages - generating mocks")
                        val mockConversations = MockDataGenerator.generateMockConversations(
                            count = 15,
                            isFacebook = true
                        ).also { mockList ->
                            // Insert mocks into database
                            facebookDao.insertAllMessages(
                                mockList.flatMap { conv ->
                                    UiMessageConverters.toFacebookMessages(conv)
                                }
                            )
                            println("🟢 Inserted ${mockList.size} mock conversations")
                        }
                        mockConversations
                    } else {
                        println("🔵 Processing ${messages.size} messages")
                        messages
                            .groupBy { it.conversationId }
                            .map { (conversationId, group) ->
                                val latest = group.maxByOrNull { it.timestamp }!!
                                ConversationEntity(
                                    id = conversationId,
                                    name = latest.sender ?: "Unknown",
                                    address = conversationId,
                                    lastMessage = latest.body ?: "",
                                    timestamp = latest.timestamp,
                                    isFacebook = true,
                                    contactPhotoUrl = null // Using your actual pattern
                                )
                            }
                            .sortedByDescending { it.timestamp }
                    }
                }
                .catch { e ->
                    println("🔴 ERROR: ${e.message}")
                    val fallback = MockDataGenerator.generateMockConversations(
                        count = 5,
                        isFacebook = true
                    )
                    _facebookConversations.value = fallback
                }
                .collect { convos ->
                    println("🟣 Collected ${convos.size} conversations")
                    _facebookConversations.value = convos
                }
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<UiMessage>> {
        return facebookDao.getMessagesForConversation(conversationId)
            .map { list -> list.map { UiMessageConverters.fromFacebook(it) } }
    }
}

