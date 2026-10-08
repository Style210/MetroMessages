// MetroMessagesRepository.kt
// The whole provider layer. Pull-based like the original: the ViewModel asks, the repository answers.
// Everything here is one-shot except changes(), which just says "something changed, ask again".
package com.metromessages.data.local.metromessagehub

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.metromessages.SmsSentReceiver
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.data.local.metropeoplehub.MetroContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        contentResolver.registerContentObserver(Telephony.MmsSms.CONTENT_URI, true, observer)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    // ---------------------------------------------------------------- conversations

    private val addressById = HashMap<Long, String>()
    private val contactCache = HashMap<String, MetroContact?>()
    private val lookupLock = Mutex()

    /** Forget cached contact/address lookups (call from the refresh button). */
    suspend fun clearCaches() = lookupLock.withLock { contactCache.clear(); addressById.clear() }

    suspend fun getConversations(): List<MetroConversation> = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp()) return@withContext emptyList()
        lookupLock.withLock {
            val archived = archivedIds()
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
        val key = number.filter { it.isDigit() }.takeLast(10).ifEmpty { number }
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
                    Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ
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
        if (!isDefaultSmsApp()) return@withContext
        val v = ContentValues().apply { put(Telephony.Sms.READ, 1); put(Telephony.Sms.SEEN, 1) }
        val where = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0"
        val args = arrayOf(threadId.toString())
        try {
            contentResolver.update(Telephony.Sms.CONTENT_URI, v, where, args)
            contentResolver.update(Telephony.Mms.CONTENT_URI, v, where, args)
        } catch (e: Exception) { Log.w(TAG, "markThreadRead failed", e) }
    }

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

    // ---------------------------------------------------------------- sending

    /**
     * Sends one SMS. As the default SMS app we are responsible for storing the message: it goes into the
     * provider as OUTBOX first, and SmsSentReceiver flips it to SENT or FAILED from the radio's result.
     */
    suspend fun sendSms(phoneNumber: String, text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp() || text.isBlank()) return@withContext false
        try {
            val row = contentResolver.insert(
                Telephony.Sms.Outbox.CONTENT_URI,
                ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, phoneNumber)
                    put(Telephony.Sms.BODY, text)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                    put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, phoneNumber))
                }
            ) ?: return@withContext false

            val sentIntent = PendingIntent.getBroadcast(
                context, row.hashCode(),
                Intent(context, SmsSentReceiver::class.java).setData(row),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val sms = context.getSystemService(SmsManager::class.java)
            val parts = sms.divideMessage(text)
            if (parts.size > 1) {
                sms.sendMultipartTextMessage(phoneNumber, null, parts, ArrayList(parts.map { sentIntent }), null)
            } else {
                sms.sendTextMessage(phoneNumber, null, text, sentIntent, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendSms failed", e)
            false
        }
    }

    suspend fun findThreadId(phoneNumber: String): Long? = withContext(Dispatchers.IO) {
        try { Telephony.Threads.getOrCreateThreadId(context, phoneNumber) } catch (e: Exception) { null }
    }
}
