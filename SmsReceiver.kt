package com.metromessages

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                    // ✅ CRITICAL: Handle SMS_DELIVER for Android validation
                    handleSmsDeliver(context, intent)
                }
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    // Handle normal SMS received (optional for non-default apps)
                    handleSmsReceived(context, intent)
                }
            }
        } catch (e: Exception) {
            // ❌ NEVER crash a BroadcastReceiver
            // Android will blacklist your app if receivers crash
        }
    }

    private fun handleSmsDeliver(context: Context, intent: Intent) {
        try {
            // ✅ FIRST: Get messages (may throw SecurityException if not default)
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                ?: return

            // ✅ SECOND: Check if we're default SMS app
            val isDefault = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

            if (isDefault) {
                // ✅ We ARE default: Process and abort broadcast
                processMessages(context, messages, true)
                abortBroadcast()
                Log.d("SmsReceiver", "✅ SMS_DELIVER processed (default app)")
            } else {
                // ✅ We're NOT default: Just validate capability without processing
                // Simply getting here without crashing validates to Android
                Log.d("SmsReceiver", "✅ SMS_DELIVER validated (not default yet)")
                // Don't abort broadcast when not default
                // Don't process messages when not default
            }

        } catch (e: SecurityException) {
            // ✅ This is EXPECTED when we're not default SMS app
            // Android validation succeeded - we received the broadcast
            Log.d("SmsReceiver", "✅ SecurityException (not default - validation OK)")
        }
    }

    private fun handleSmsReceived(context: Context, intent: Intent) {
        try {
            // SMS_RECEIVED can be received by any app with RECEIVE_SMS permission
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                ?: return

            // Process messages (always for SMS_RECEIVED)
            processMessages(context, messages, false)

            // ❌ NEVER abort broadcast for SMS_RECEIVED_ACTION
            // This would block other apps from receiving SMS

        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in SMS_RECEIVED", e)
        }
    }

    /**
     * As the default SMS app, storing incoming messages is OUR job (the system no longer does it).
     * The old version only logged, so incoming texts were dropped.
     * SMS_RECEIVED (non-default) must never write; only SMS_DELIVER does.
     */
    private fun processMessages(context: Context, messages: Array<SmsMessage>, isSmsDeliver: Boolean) {
        if (!isSmsDeliver) return
        try {
            val bySender = messages.groupBy { it.originatingAddress ?: "Unknown" }
            for ((sender, parts) in bySender) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, sender)
                    put(Telephony.Sms.BODY, parts.joinToString("") { it.messageBody ?: "" })
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.DATE_SENT, parts.first().timestampMillis)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.SEEN, 0)
                    put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, sender))
                }
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                Log.d("SmsReceiver", "Stored SMS from $sender")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error storing SMS", e)
        }
    }
}

/** Radio result for an outgoing SMS: flip the OUTBOX row to SENT or FAILED. */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val row = intent.data ?: return
        val type = if (resultCode == android.app.Activity.RESULT_OK) Telephony.Sms.MESSAGE_TYPE_SENT
                   else Telephony.Sms.MESSAGE_TYPE_FAILED
        try {
            context.contentResolver.update(row, ContentValues().apply { put(Telephony.Sms.TYPE, type) }, null, null)
        } catch (e: Exception) {
            Log.e("SmsSentReceiver", "Could not update sent state", e)
        }
    }
}
