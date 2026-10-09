// MetroMessagesRepository.kt
// The whole provider layer. Pull-based like the original: the ViewModel asks, the repository answers.
// Everything here is one-shot except changes(), which just says "something changed, ask again".
package com.metromessages.data.local.metromessagehub

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.metromessages.MessageNotifier
import com.metromessages.SmsSender
import com.metromessages.SmsStore
import android.provider.ContactsContract
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.data.local.metropeoplehub.MetroContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetroMessagesRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val contactsRepository: MetroContactsRepository
) {
    private val TAG = "MetroMessagesRepo"
    private val prefs by lazy { context.getSharedPreferences("metro_messages", Context.MODE_PRIVATE) }

    // ---------------------------------------------------------------- state checks

    fun isDefaultSmsApp(): Boolean = try {
        context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
    } catch (e: Exception) { false }

    // ---------------------------------------------------------------- change signal

    /**
     * Emits Unit whenever SMS/MMS change. Registers on collect, unregisters when the collector goes away,
     * so there is no shared observer to start/stop. The ViewModel debounces it and re-runs its loads.
     */
    fun changes(): Flow<Unit> = callbackFlow {
        val messages = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        // Editing a contact (name, photo, number) must refresh thread names, so it is a change too.
        val contacts = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { contactsDirty = true; trySend(Unit) }
        }
        contentResolver.registerContentObserver(Telephony.MmsSms.CONTENT_URI, true, messages)
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contacts)
        awaitClose {
            contentResolver.unregisterContentObserver(messages)
            contentResolver.unregisterContentObserver(contacts)
        }
    }.conflate()

    @Volatile private var contactsDirty = false

    // ---------------------------------------------------------------- conversations

    private val addressById = HashMap<Long, String>()
    private val contactCache = HashMap<String, MetroContact?>()
    private val lookupLock = Mutex()

    /** Forget cached contact/address lookups (call from the refresh button). */
    suspend fun clearCaches() = lookupLock.withLock { contactCache.clear(); addressById.clear() }

    suspend fun getConversations(): List<MetroConversation> = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp()) return@withContext emptyList()
        lookupLock.withLock {
            if (contactsDirty) {
                contactsDirty = false
                contactCache.clear()
                contactsRepository.clearCache()
            }
            val archived = archivedIds()
            val unread = unreadByThread()
            val threadsUri = Telephony.Threads.CONTENT_URI.buildUpon()
                .appendQueryParameter("simple", "true").build()
            val out = mutableListOf<MetroConversation>()
            try {
                contentResolver.query(
                    threadsUri,
                    arrayOf(
                        Telephony.Threads._ID, Telephony.Threads.DATE, Telephony.Threads.SNIPPET,
                        Telephony.Threads.READ, Telephony.Threads.RECIPIENT_IDS
                    ),
                    null, null, "${Telephony.Threads.DATE} DESC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        val threadId = c.getLong(0)
                        if (threadId <= 0) continue
                        val numbers = recipientNumbers(c.getString(4))
                        if (numbers.isEmpty()) continue
                        val contacts = numbers.map { contactFor(it) }
                        out += MetroConversation(
                            threadId = threadId,
                            snippet = c.getString(2),
                            date = c.getLong(1),
                            read = c.getInt(3) == 1,
                            participants = numbers,
                            participantNames = numbers.mapIndexed { i, n -> contacts[i]?.displayName ?: n },
                            unreadCount = unread[threadId] ?: 0,
                            contactId = if (numbers.size == 1) contacts[0]?.id else null,
                            contactPhotoUri = if (numbers.size == 1) contacts[0]?.photoUri else null,
                            isArchived = threadId in archived
                        )
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "No SMS access", e)
            } catch (e: Exception) {
                Log.e(TAG, "getConversations failed", e)
            }
            out
        }
    }

    private fun recipientNumbers(recipientIds: String?): List<String> {
        val ids = recipientIds?.split(' ')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (ids.isEmpty()) return emptyList()
        if (ids.any { it !in addressById }) loadCanonicalAddresses()
        return ids.mapNotNull { addressById[it] }.filter { it.isNotBlank() }
    }

    private fun loadCanonicalAddresses() {
        try {
            contentResolver.query(Uri.parse("content://mms-sms/canonical-addresses"), null, null, null, null)
                ?.use { c ->
                    val idCol = c.getColumnIndexOrThrow("_id")
                    val addrCol = c.getColumnIndexOrThrow("address")
                    while (c.moveToNext()) addressById[c.getLong(idCol)] = c.getString(addrCol).orEmpty()
                }
        } catch (e: Exception) {
            Log.w(TAG, "canonical-addresses unavailable", e)
        }
    }

    private suspend fun contactFor(number: String): MetroContact? {
        val key = PhoneNumbers.key(number)
        if (contactCache.containsKey(key)) return contactCache[key]
        val found = try { contactsRepository.getContactByPhoneNumber(number) } catch (e: Exception) { null }
        contactCache[key] = found
        return found
    }

    // ---------------------------------------------------------------- messages

    suspend fun getMessages(threadId: Long): List<MetroMessage> = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp()) return@withContext emptyList()
        val all = mutableListOf<MetroMessage>()
        try {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ, Telephony.Sms.STATUS
                ),
                "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "${Telephony.Sms.DATE} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    if (id <= 0) continue
                    val body = c.getString(2)
                    val type = c.getInt(4)
                    all += MetroMessage(
                        id = id, threadId = threadId,
                        address = c.getString(1) ?: "Unknown",
                        body = body, date = c.getLong(3), type = type, read = c.getInt(5) == 1,
                        status = c.getInt(6),
                        isOTP = type == Telephony.Sms.MESSAGE_TYPE_INBOX && MetroMessage.looksLikeOtp(body)
                    )
                }
            }
            all += MmsReader.read(contentResolver, threadId) { MmsReader.senderOf(contentResolver, it) ?: "Unknown" }
        } catch (e: Exception) {
            Log.e(TAG, "getMessages($threadId) failed", e)
        }
        all.sortedBy { it.date }
    }

    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        if (isDefaultSmsApp()) SmsStore.markRead(context, threadId)
    }

    /** Screen opened / closed: the open thread never notifies, and opening clears its notification. */
    fun onThreadOpened(threadId: Long) {
        MessageNotifier.activeThreadId = threadId
        MessageNotifier.cancel(context, threadId)
    }
    fun onThreadClosed() { MessageNotifier.activeThreadId = null }

    suspend fun deleteMessage(message: MetroMessage): Boolean = withContext(Dispatchers.IO) {
        try {
            val base = if (message.isMms) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI
            contentResolver.delete(ContentUris.withAppendedId(base, message.providerId), null, null) > 0
        } catch (e: Exception) { Log.e(TAG, "delete failed", e); false }
    }

    // ---------------------------------------------------------------- archive (app-owned: Android has no archive column)

    fun archivedIds(): Set<Long> =
        prefs.getStringSet("archived", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()

    fun setArchived(threadId: Long, archived: Boolean) {
        val next = prefs.getStringSet("archived", emptySet())!!.toMutableSet()
        if (archived) next += threadId.toString() else next -= threadId.toString()
        prefs.edit().putStringSet("archived", next).apply()
    }

    // ---------------------------------------------------------------- unread

    /** Unread incoming messages per thread, SMS + MMS, in two queries. */
    private fun unreadByThread(): Map<Long, Int> {
        if (!isDefaultSmsApp()) return emptyMap()
        val counts = HashMap<Long, Int>()
        fun count(uri: Uri, threadCol: String, where: String, args: Array<String>) {
            try {
                contentResolver.query(uri, arrayOf(threadCol), where, args, null)?.use {
                    while (it.moveToNext()) counts[it.getLong(0)] = (counts[it.getLong(0)] ?: 0) + 1
                }
            } catch (e: Exception) { Log.w(TAG, "unread count failed for $uri", e) }
        }
        count(Telephony.Sms.CONTENT_URI, Telephony.Sms.THREAD_ID,
            "${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.TYPE} = ?",
            arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()))
        count(Telephony.Mms.CONTENT_URI, Telephony.Mms.THREAD_ID,
            "${Telephony.Mms.READ} = 0 AND ${Telephony.Mms.MESSAGE_BOX} = ?",
            arrayOf(Telephony.Mms.MESSAGE_BOX_INBOX.toString()))
        return counts
    }

    /** Total unread across non-archived threads: the number for a home tile or badge. Live. */
    fun unreadTotal(): Flow<Int> = changes()
        .onStart { emit(Unit) }
        .map {
            withContext(Dispatchers.IO) {
                val archived = archivedIds()
                unreadByThread().filterKeys { it !in archived }.values.sum()
            }
        }
        .distinctUntilChanged()

    // ---------------------------------------------------------------- sending

    /** Sends one SMS (stored as OUTBOX first; receivers flip it to SENT/FAILED and record delivery). */
    suspend fun sendSms(phoneNumber: String, text: String): Boolean = withContext(Dispatchers.IO) {
        isDefaultSmsApp() && SmsSender.send(context, phoneNumber, text) != null
    }

    /** Re-sends a failed SMS in place (same row, no duplicate bubble). */
    suspend fun retry(message: MetroMessage): Boolean = withContext(Dispatchers.IO) {
        if (message.isMms || !message.isFailed || message.body.isNullOrBlank()) return@withContext false
        val row = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, message.providerId)
        SmsStore.setType(context, row, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
        SmsSender.dispatch(context, row, message.address, message.body)
    }

    // ---------------------------------------------------------------- search / delete thread

    /** Case-insensitive substring search over SMS bodies, newest first. (MMS text isn't searched yet.) */
    suspend fun search(query: String, limit: Int = 200): List<MetroMessage> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty() || !isDefaultSmsApp()) return@withContext emptyList()
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val out = mutableListOf<MetroMessage>()
        try {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ),
                "${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'", arrayOf("%$escaped%"),
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { c ->
                while (c.moveToNext()) out += MetroMessage(
                    id = c.getLong(0), threadId = c.getLong(1), address = c.getString(2) ?: "Unknown",
                    body = c.getString(3), date = c.getLong(4), type = c.getInt(5), read = c.getInt(6) == 1
                )
            }
        } catch (e: Exception) { Log.e(TAG, "search failed", e) }
        out
    }

    /** Deletes the whole thread (SMS + MMS). Only ever called from an explicit user action. */
    suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.delete(ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId), null, null) > 0
        } catch (e: Exception) { Log.e(TAG, "deleteThread failed", e); false }
    }

    // ---------------------------------------------------------------- contacts <-> messages seam

    /** Every thread that involves any of this contact's numbers (one-to-one and groups). */
    suspend fun conversationsForContact(contact: MetroContact): List<MetroConversation> {
        val keys = contact.phones.map { PhoneNumbers.key(it.number) }.toSet()
        return getConversations().filter { c -> c.participants.any { PhoneNumbers.key(it) in keys } }
    }

    /** The one-to-one thread with this number, or null if you've never messaged. */
    suspend fun conversationForNumber(number: String): MetroConversation? =
        getConversations().firstOrNull { c -> c.isOneToOne && PhoneNumbers.same(c.participants[0], number) }

    /** Thread to open for a contact: their existing one-to-one thread, else a new one on their primary number. */
    suspend fun threadIdForContact(contact: MetroContact): Long? =
        conversationsForContact(contact).firstOrNull { it.isOneToOne }?.threadId
            ?: contact.primaryPhone?.let { findThreadId(it) }

    suspend fun findThreadId(phoneNumber: String): Long? = withContext(Dispatchers.IO) {
        try { Telephony.Threads.getOrCreateThreadId(context, phoneNumber) } catch (e: Exception) { null }
    }
}
