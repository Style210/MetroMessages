package com.metromessages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d("MmsReceiver", "Received intent: ${intent.action}, type: ${intent.type}")

            // Check if this is an MMS message
            if (intent.type != "application/vnd.wap.mms-message") {
                return
            }

            when (intent.action) {
                Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION -> {
                    handleMmsDeliver(context, intent)
                }
                Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> {
                    handleMmsReceived(context, intent)
                }
                // Handle legacy action names
                "android.provider.Telephony.WAP_PUSH_DELIVER" -> {
                    handleMmsDeliver(context, intent)
                }
                "android.provider.Telephony.WAP_PUSH_RECEIVED" -> {
                    handleMmsReceived(context, intent)
                }
            }
        } catch (e: Exception) {
            // CRITICAL: Never crash in a BroadcastReceiver
            Log.e("MmsReceiver", "Error in MMS receiver", e)
        }
    }

    private fun handleMmsDeliver(context: Context, intent: Intent) {
        // WAP_PUSH_DELIVER is only for default SMS apps
        if (!isDefaultSmsApp(context)) {
            Log.d("MmsReceiver", "Not default SMS app - ignoring WAP_PUSH_DELIVER")
            return
        }

        try {
            Log.d("MmsReceiver", "Processing WAP_PUSH_DELIVER for MMS")

            // Extract MMS data
            val data = intent.getByteArrayExtra("data")
            val contentType = intent.type

            Log.d("MmsReceiver", "MMS data size: ${data?.size ?: 0}, type: $contentType")

            // Process MMS message
            processMmsMessage(context, data, contentType, isDelivery = true)

            // Only abort if we successfully processed the MMS
            abortBroadcast()
            Log.d("MmsReceiver", "Broadcast aborted for WAP_PUSH_DELIVER")

        } catch (e: SecurityException) {
            Log.e("MmsReceiver", "SecurityException in WAP_PUSH_DELIVER", e)
            // Don't abort if we can't process the MMS
        } catch (e: Exception) {
            Log.e("MmsReceiver", "Error processing WAP_PUSH_DELIVER", e)
        }
    }

    private fun handleMmsReceived(context: Context, intent: Intent) {
        // WAP_PUSH_RECEIVED can be received by any app with permission
        try {
            Log.d("MmsReceiver", "Processing WAP_PUSH_RECEIVED for MMS")

            // Extract MMS data
            val data = intent.getByteArrayExtra("data")
            val contentType = intent.type

            Log.d("MmsReceiver", "MMS received, data size: ${data?.size ?: 0}")

            // Process MMS message
            processMmsMessage(context, data, contentType, isDelivery = false)

            // NEVER abort for WAP_PUSH_RECEIVED
            // Let the default SMS app handle it

        } catch (e: Exception) {
            Log.e("MmsReceiver", "Error processing WAP_PUSH_RECEIVED", e)
        }
    }

    private fun isDefaultSmsApp(context: Context): Boolean {
        return try {
            val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
            val isDefault = context.packageName == defaultPackage
            Log.d("MmsReceiver", "Default SMS check: $isDefault (default: $defaultPackage)")
            isDefault
        } catch (e: Exception) {
            Log.e("MmsReceiver", "Error checking default SMS app", e)
            false
        }
    }

    private fun processMmsMessage(
        context: Context,
        data: ByteArray?,
        contentType: String?,
        isDelivery: Boolean
    ) {
        Log.d("MmsReceiver", "Processing MMS: delivery=$isDelivery, type=$contentType")

        // TODO: Implement your MMS parsing logic here
        // This is where you would parse the MMS PDU data

        if (isDelivery && isDefaultSmsApp(context)) {
            // Only save to system MMS database if we're default AND this is a delivery
            try {
                saveMmsToDatabase(context, data, contentType)
            } catch (e: SecurityException) {
                Log.e("MmsReceiver", "Cannot save MMS to system - not default?", e)
            }
        }

        // Always save to your app's internal database
        saveMmsToAppDatabase(context, data, contentType)

        // Show notification
        showMmsNotification(context, isDelivery)
    }

    private fun saveMmsToDatabase(context: Context, data: ByteArray?, contentType: String?) {
        // WARNING: This requires being default SMS app
        // Writing to Telephony.Mms.CONTENT_URI throws SecurityException if not default
        Log.d("MmsReceiver", "Would save MMS to system database")
        // Implement only if you're default SMS app
    }

    private fun saveMmsToAppDatabase(context: Context, data: ByteArray?, contentType: String?) {
        // Save to YOUR app's database
        Log.d("MmsReceiver", "Saving MMS to app database")
        // TODO: Implement your app's MMS storage
    }

    private fun showMmsNotification(context: Context, isDelivery: Boolean) {
        val title = if (isDelivery) "MMS Delivered" else "MMS Received"
        Log.d("MmsReceiver", "Would show notification: $title")
        // TODO: Implement notification
    }
}
