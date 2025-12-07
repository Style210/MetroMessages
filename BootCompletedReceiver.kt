package com.metromessages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootCompletedReceiver", "Received: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Re-establish SMS app status after reboot
                ThreadHelper.ensureBackgroundThread {
                    Log.d("BootCompletedReceiver", "Re-registering SMS state after reboot")
                    // No action needed - Android will auto-register receivers
                    // Fossify would re-schedule alarms here
                }
            }
        }
    }
}