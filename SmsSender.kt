// SmsSender.kt
// Everything about getting an SMS out of the phone and keeping its stored state truthful.
// Platform-only (no Hilt) so receivers, the notification reply action and the "respond via message"
// service can all use it.
package com.metromessages

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log

object SmsSender {
    private const val TAG = "SmsSender"

    /** Stores the text as OUTBOX, then hands it to the radio. Returns the provider row, or null on failure. */
    fun send(context: Context, number: String, text: String): Uri? {
        if (number.isBlank() || text.isBlank()) return null
        val row = (try {
            context.contentResolver.insert(
                Telephony.Sms.Outbox.CONTENT_URI,
                ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, number)
                    put(Telephony.Sms.BODY, text)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                    put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, number))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not store outgoing SMS", e)
            null
        }) ?: return null
        dispatch(context, row, number, text)
        return row
    }

    /** Hands an already-stored row to the radio (used for first send and for retry). */
    fun dispatch(context: Context, row: Uri, number: String, text: String): Boolean {
        return try {
            val request = row.lastPathSegment?.toIntOrNull() ?: 0
            val sent = PendingIntent.getBroadcast(
                context, request, Intent(context, SmsSentReceiver::class.java).setData(row),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // delivery reports carry the PDU as an extra the system fills in, so this one must be mutable
            val delivered = PendingIntent.getBroadcast(
                context, request, Intent(context, SmsDeliveredReceiver::class.java).setData(row),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val sms = context.getSystemService(SmsManager::class.java)
            val parts = sms.divideMessage(text)
            if (parts.size > 1) {
                sms.sendMultipartTextMessage(
                    number, null, parts, ArrayList(parts.map { sent }), ArrayList(parts.map { delivered })
                )
            } else {
                sms.sendTextMessage(number, null, text, sent, delivered)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Radio refused SMS", e)
            SmsStore.setType(context, row, Telephony.Sms.MESSAGE_TYPE_FAILED)
            false
        }
    }
}

/** Small provider helpers shared by the repository and the receivers. */
object SmsStore {
    fun setType(context: Context, row: Uri, type: Int) {
        try {
            context.contentResolver.update(row, ContentValues().apply { put(Telephony.Sms.TYPE, type) }, null, null)
        } catch (e: Exception) { Log.w("SmsStore", "setType failed", e) }
    }

    fun markRead(context: Context, threadId: Long) {
        val v = ContentValues().apply { put(Telephony.Sms.READ, 1); put(Telephony.Sms.SEEN, 1) }
        val where = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0"
        val args = arrayOf(threadId.toString())
        try {
            context.contentResolver.update(Telephony.Sms.CONTENT_URI, v, where, args)
            context.contentResolver.update(Telephony.Mms.CONTENT_URI, v, where, args)
        } catch (e: Exception) { Log.w("SmsStore", "markRead failed", e) }
    }
}

/** Radio result: OUTBOX -> SENT or FAILED. Multipart-safe: a later OK never un-fails a message. */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val row = intent.data ?: return
        val ok = resultCode == Activity.RESULT_OK
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, if (ok) Telephony.Sms.MESSAGE_TYPE_SENT else Telephony.Sms.MESSAGE_TYPE_FAILED)
            if (!ok) put(Telephony.Sms.ERROR_CODE, resultCode)
        }
        try {
            if (ok) context.contentResolver.update(
                row, values, "${Telephony.Sms.TYPE} = ?", arrayOf(Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString())
            ) else context.contentResolver.update(row, values, null, null)
        } catch (e: Exception) {
            Log.e("SmsSentReceiver", "Could not update sent state", e)
        }
    }
}

/** Carrier delivery report -> Sms.STATUS (what drives a "delivered" tick). */
class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val row = intent.data ?: return
        val pdu = intent.getByteArrayExtra("pdu") ?: return
        val raw = (try { SmsMessage.createFromPdu(pdu, intent.getStringExtra("format"))?.status } catch (e: Exception) { null })
            ?: return
        val status = when {
            raw < 0x20 -> Telephony.Sms.STATUS_COMPLETE      // delivered
            raw < 0x40 -> Telephony.Sms.STATUS_PENDING       // temporary, carrier still trying
            else -> Telephony.Sms.STATUS_FAILED
        }
        try {
            context.contentResolver.update(row, ContentValues().apply { put(Telephony.Sms.STATUS, status) }, null, null)
        } catch (e: Exception) {
            Log.e("SmsDeliveredReceiver", "Could not store delivery status", e)
        }
    }
}

/**
 * "Reject call with message" and other quick replies. Android will not offer an app as the default SMS
 * app unless it has this service (it was previously declared on the activity, which doesn't count).
 */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val numbers = intent?.data?.schemeSpecificPart?.substringBefore('?')?.split(',').orEmpty()
        if (!text.isNullOrBlank()) numbers.filter { it.isNotBlank() }.forEach { SmsSender.send(this, it, text) }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
