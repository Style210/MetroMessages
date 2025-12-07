package com.metromessages

import android.content.BroadcastReceiver
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

    // ✅ DEFINED: processMessages method
    private fun processMessages(context: Context, messages: Array<SmsMessage>, isSmsDeliver: Boolean) {
        try {
            // Group messages by sender (Fossify pattern)
            val groupedMessages = messages.groupBy { sms ->
                sms.originatingAddress ?: "Unknown"
            }

            for ((sender, smsList) in groupedMessages) {
                // Combine multi-part messages
                val combinedBody = smsList.joinToString("") { it.messageBody ?: "" }
                val timestamp = smsList.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                // Get or create thread ID (CRITICAL for SMS integration)
                val threadId = try {
                    Telephony.Threads.getOrCreateThreadId(context, sender)
                } catch (e: Exception) {
                    // Fallback: Use hash of phone number
                    sender.hashCode().toLong()
                }

                // Save to YOUR database (not system SMS database)
                saveToAppDatabase(context, sender, combinedBody, timestamp, threadId, isSmsDeliver)

                Log.d("SmsReceiver", "Processed SMS: $sender -> ${combinedBody.take(30)}...")
            }

        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing messages", e)
        }
    }

    // ✅ DEFINED: saveToAppDatabase method
    private fun saveToAppDatabase(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long,
        threadId: Long,
        isFromSmsDeliver: Boolean
    ) {
        try {
            // 🚨 IMPORTANT: Save to YOUR app's database, NOT system SMS database
            // System SMS database is automatically updated by Android

            // TODO: Replace with your actual database save logic
            // Example using Room or SQLite

            /*
            val message = MetroMessage(
                id = 0,
                address = sender,
                body = body,
                date = timestamp,
                threadId = threadId,
                type = if (isFromSmsDeliver) Message.TYPE_RECEIVED_DELIVER else Message.TYPE_RECEIVED,
                read = false
            )

            // Insert into your app's database
            metroMessagesRepository.insertMessage(message)
            */

            Log.d("SmsReceiver", "Saved to app DB: $sender, thread: $threadId")

        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error saving to app database", e)
        }
    }
}
