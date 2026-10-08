// MmsReader.kt
// Reads MMS rows for one thread (text + attachments) in 3 queries total, not per message.
// Parts are exposed as content://mms/part/<id>, which Coil / ContentResolver can open on modern Android.
// (The `_data` file path used by the earlier parser is not readable by apps on Android 10+.)
package com.metromessages.data.local.metromessagehub

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import android.util.Log

internal object MmsReader {
    private const val TAG = "MmsReader"
    private val PART_URI: Uri = Uri.parse("content://mms/part")
    private const val ADDR_FROM = 137      // PduHeaders.FROM

    private class Part(val mid: Long, val id: Long, val mime: String?, val text: String?, val name: String?)

    fun read(
        cr: ContentResolver,
        threadId: Long,
        resolveAddress: (rowId: Long) -> String
    ): List<MetroMessage> {
        val rows = mutableListOf<Triple<Long, Long, Int>>()          // id, dateMs, msg_box
        val read = mutableMapOf<Long, Boolean>()
        cr.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ),
            "${Telephony.Mms.THREAD_ID} = ?", arrayOf(threadId.toString()), null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                rows += Triple(id, c.getLong(1) * 1000L, c.getInt(2))  // MMS date is in seconds
                read[id] = c.getInt(3) == 1
            }
        }
        if (rows.isEmpty()) return emptyList()

        // one query for every part of every MMS in the thread
        val parts = mutableMapOf<Long, MutableList<Part>>()
        val ids = rows.joinToString(",") { it.first.toString() }
        try {
            cr.query(
                PART_URI,
                arrayOf("mid", "_id", "ct", "text", "name"),
                "mid IN ($ids)", null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val p = Part(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4))
                    parts.getOrPut(p.mid) { mutableListOf() } += p
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read MMS parts for thread $threadId", e)
        }

        return rows.map { (id, dateMs, box) ->
            val myParts = parts[id].orEmpty()
            val text = myParts.filter { it.mime?.startsWith("text/plain") == true }
                .mapNotNull { it.text }.joinToString("\n").ifBlank { null }
            // first non-text, non-SMIL part is the attachment we surface (one per bubble)
            val media = myParts.firstOrNull {
                val m = it.mime.orEmpty()
                m.isNotEmpty() && !m.startsWith("text/") && !m.contains("smil")
            }
            val type = if (box == Telephony.Mms.MESSAGE_BOX_INBOX) Telephony.Sms.MESSAGE_TYPE_INBOX
                       else Telephony.Sms.MESSAGE_TYPE_SENT
            MetroMessage(
                id = MetroMessage.MMS_ID_OFFSET + id,
                threadId = threadId,
                address = if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) resolveAddress(id) else "",
                body = text,
                date = dateMs,
                type = type,
                read = read[id] == true,
                isMms = true,
                messageType = media?.let { MessageType.fromMimeType(it.mime) } ?: MessageType.TEXT,
                mediaUri = media?.let { Uri.parse("content://mms/part/${it.id}") },
                mimeType = media?.mime,
                fileName = media?.name
            )
        }
    }

    /** Sender of an incoming MMS (type 137 = FROM). Only called for incoming rows. */
    fun senderOf(cr: ContentResolver, mmsId: Long): String? = try {
        cr.query(
            Uri.parse("content://mms/$mmsId/addr"),
            arrayOf("address", "type"), "type = ?", arrayOf(ADDR_FROM.toString()), null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) { null }
}
