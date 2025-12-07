package com.metromessages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class TriggerSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Used for testing/debugging - triggers SMS receive flow
        if (intent.action == "com.metromessages.TRIGGER_SMS_RECEIVED") {
            // Simulate SMS received
            // Fossify uses this for automated testing
        }
    }
}
