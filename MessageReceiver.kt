package com.metromessages

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.util.Log

/**
 * Combined BroadcastReceiver for both SMS and MMS messages.
 */
class MessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val fullMessage = messages.joinToString(separator = "") { it.messageBody }
                val sender = messages.firstOrNull()?.originatingAddress ?: "Unknown"
                Log.d("MessageReceiver", "Received SMS from $sender: $fullMessage")

                // TODO: Insert SMS into DB, send notification, etc.
            }

            "android.provider.Telephony.WAP_PUSH_RECEIVED" -> {
                if (intent.type == "application/vnd.wap.mms-message") {
                    Log.d("MessageReceiver", "Received MMS message")

                    // TODO: Process MMS here
                }
            }
        }
    }
}

/**
 * Service stub for handling background MMS operations.
 */
class MmsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MmsService", "MMS Service started")

        // TODO: Implement MMS sending/receiving background work

        stopSelf()
        return START_NOT_STICKY
    }
}
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val fullMessage = messages.joinToString(separator = "") { it.messageBody }
            val sender = messages.firstOrNull()?.originatingAddress ?: "Unknown"

            Log.d("SmsReceiver", "Received SMS from $sender: $fullMessage")

            // TODO: Process SMS (DB insert, notification, etc.)
        }
    }
}

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.WAP_PUSH_RECEIVED" &&
            intent.type == "application/vnd.wap.mms-message") {

            Log.d("MmsReceiver", "Received MMS message")

            // TODO: Process MMS here
        }
    }
}

