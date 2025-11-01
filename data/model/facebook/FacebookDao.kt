package com.metromessages.data.model.facebook

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FacebookDao {

    //region Conversation Operations
    @Query("SELECT * FROM facebook_conversations ORDER BY timestamp DESC")
    fun getFacebookConversations(): Flow<List<FacebookConversationEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): FacebookConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: FacebookConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConversations(conversations: List<FacebookConversationEntity>)

    @Query("UPDATE facebook_conversations SET lastMessage = :message, timestamp = :timestamp WHERE id = :conversationId")
    suspend fun updateConversationLastMessage(conversationId: String, message: String, timestamp: Long)

    @Query("UPDATE facebook_conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updateConversationPinStatus(conversationId: String, isPinned: Boolean)

    @Query("DELETE FROM facebook_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("UPDATE facebook_conversations SET unreadCount = unreadCount + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)

    @Query("UPDATE facebook_conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationAsRead(conversationId: String)

    @Query("SELECT * FROM facebook_conversations WHERE linkedContactId = :contactId")
    suspend fun getConversationByContactId(contactId: Long): FacebookConversationEntity?

    @Query("UPDATE facebook_conversations SET linkedContactId = :contactId WHERE id = :conversationId")
    suspend fun linkConversationToContact(conversationId: String, contactId: Long)
    //endregion

    //region Message Operations
    @Query("SELECT * FROM facebook_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<FacebookMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: FacebookMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMessages(messages: List<FacebookMessageEntity>)

    @Query("DELETE FROM facebook_messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM facebook_messages WHERE conversationId = :conversationId")
    suspend fun deleteAllMessagesForConversation(conversationId: String)

    @Query("SELECT * FROM facebook_messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): FacebookMessageEntity?

    @Update
    suspend fun updateMessage(message: FacebookMessageEntity)

    @Query("UPDATE facebook_messages SET messageStatus = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE facebook_messages SET readBy = :readBy WHERE messageId = :messageId")
    suspend fun updateMessageReadBy(messageId: String, readBy: List<String>)
    //endregion

    //region SMS/MMS Specific Queries
    @Query("SELECT * FROM facebook_conversations WHERE address = :phoneNumber OR facebookId = :phoneNumber")
    suspend fun getConversationByPhoneNumber(phoneNumber: String): FacebookConversationEntity?

    @Query("SELECT * FROM facebook_conversations WHERE threadId = :threadId")
    suspend fun getConversationByThreadId(threadId: Long): FacebookConversationEntity?

    @Query("SELECT * FROM facebook_messages WHERE address = :phoneNumber ORDER BY timestamp DESC")
    fun getMessagesByPhoneNumber(phoneNumber: String): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesByThreadId(threadId: Long): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_messages WHERE smsType = :smsType ORDER BY timestamp DESC")
    fun getMessagesBySmsType(smsType: String): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE isSmsGroup = 1 ORDER BY timestamp DESC")
    fun getGroupConversations(): Flow<List<FacebookConversationEntity>>

    @Query("SELECT * FROM facebook_messages WHERE isMms = 1 ORDER BY timestamp DESC")
    fun getMmsMessages(): Flow<List<FacebookMessageEntity>>

    @Query("UPDATE facebook_conversations SET smsUnreadCount = smsUnreadCount + 1 WHERE id = :conversationId")
    suspend fun incrementSmsUnreadCount(conversationId: String)

    @Query("UPDATE facebook_conversations SET smsUnreadCount = 0 WHERE id = :conversationId")
    suspend fun markSmsConversationAsRead(conversationId: String)

    // Unknown contact queries
    @Query("SELECT * FROM facebook_conversations WHERE isUnknownContact = 1 ORDER BY timestamp DESC")
    fun getUnknownNumberConversations(): Flow<List<FacebookConversationEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE isUnknownContact = 1 AND linkedContactId IS NULL ORDER BY timestamp DESC")
    fun getUnlinkedUnknownConversations(): Flow<List<FacebookConversationEntity>>
    //endregion

    //region Advanced Queries
    @Query("SELECT COUNT(*) FROM facebook_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountForConversation(conversationId: String): Int

    @Query("SELECT * FROM facebook_messages WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getMessagesInTimeRange(startTime: Long, endTime: Long): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE facebookId LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchConversations(query: String): Flow<List<FacebookConversationEntity>>

    @Query("SELECT * FROM facebook_messages WHERE messageType = :messageType ORDER BY timestamp DESC")
    fun getMessagesByType(messageType: String): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE unreadCount > 0 ORDER BY timestamp DESC")
    fun getConversationsWithUnreadMessages(): Flow<List<FacebookConversationEntity>>

    @Query("SELECT SUM(unreadCount) FROM facebook_conversations")
    suspend fun getTotalUnreadCount(): Int

    @Query("SELECT * FROM facebook_conversations WHERE isPinned = 1 ORDER BY timestamp DESC")
    fun getPinnedConversations(): Flow<List<FacebookConversationEntity>>

    @Query("SELECT * FROM facebook_messages WHERE conversationId = :conversationId AND messageType IN (:types) ORDER BY timestamp DESC")
    fun getMessagesByTypes(conversationId: String, types: List<String>): Flow<List<FacebookMessageEntity>>

    @Query("SELECT * FROM facebook_conversations WHERE linkedContactId IS NOT NULL")
    fun getLinkedConversations(): Flow<List<FacebookConversationEntity>>

    // SMS search
    @Query("SELECT * FROM facebook_messages WHERE body LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<FacebookMessageEntity>>

    // Group conversation queries
    @Query("SELECT * FROM facebook_conversations WHERE isSmsGroup = 1 AND participants LIKE '%' || :phoneNumber || '%'")
    suspend fun getGroupConversationsContainingPhoneNumber(phoneNumber: String): List<FacebookConversationEntity>
    //endregion

    //region Bulk Operations for Performance
    @Transaction
    suspend fun insertConversationWithInitialMessage(
        conversation: FacebookConversationEntity,
        initialMessage: FacebookMessageEntity
    ) {
        insertConversation(conversation)
        insertMessage(initialMessage)
    }

    @Transaction
    suspend fun deleteConversationWithMessages(conversationId: String) {
        deleteAllMessagesForConversation(conversationId)
        deleteConversation(conversationId)
    }

    // SMS bulk operations
    @Transaction
    suspend fun insertSmsConversation(
        conversation: FacebookConversationEntity,
        messages: List<FacebookMessageEntity>
    ) {
        insertConversation(conversation)
        insertAllMessages(messages)
    }

    // Group conversation bulk operation
    @Transaction
    suspend fun insertGroupConversationWithMessages(
        conversation: FacebookConversationEntity,
        messages: List<FacebookMessageEntity>
    ) {
        insertConversation(conversation)
        insertAllMessages(messages)
    }
    //endregion

    //region PhoneNumberId based queries
    @Query("SELECT * FROM facebook_conversations WHERE phoneNumberId = :phoneNumberId")
    suspend fun getConversationByPhoneNumberId(phoneNumberId: String): FacebookConversationEntity?

    @Query("SELECT * FROM facebook_messages WHERE phoneNumberId = :phoneNumberId ORDER BY timestamp ASC")
    fun getMessagesByPhoneNumberId(phoneNumberId: String): Flow<List<FacebookMessageEntity>>

    @Query("UPDATE facebook_conversations SET phoneNumberId = :phoneNumberId WHERE id = :conversationId")
    suspend fun updateConversationPhoneNumberId(conversationId: String, phoneNumberId: String)

    @Query("UPDATE facebook_messages SET phoneNumberId = :phoneNumberId WHERE conversationId = :conversationId")
    suspend fun updateMessagesPhoneNumberId(conversationId: String, phoneNumberId: String)

    // Migration helper
    @Query("SELECT DISTINCT address FROM facebook_conversations WHERE address IS NOT NULL AND phoneNumberId IS NULL")
    suspend fun getConversationsNeedingMigration(): List<String>

    // Link unknown conversation to contact
    @Query("UPDATE facebook_conversations SET linkedContactId = :contactId, isUnknownContact = 0 WHERE id = :conversationId")
    suspend fun linkUnknownConversationToContact(conversationId: String, contactId: Long)

    // Find conversation by legacy SMS ID (for migration)
    @Query("SELECT * FROM facebook_conversations WHERE id LIKE 'sms_%' AND address = :phoneNumber")
    suspend fun getConversationByLegacySmsId(phoneNumber: String): FacebookConversationEntity?
    //endregion
}