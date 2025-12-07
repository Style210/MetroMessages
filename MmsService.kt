package com.metromessages

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat

class MmsService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MmsService", "MMS Service started")

        if (!isDefaultSmsApp()) {
            Log.d("MmsService", "Not default SMS app, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // Create notification channel
        createNotificationChannel()

        // Build and show notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // For Phase 1: Just log and stop
        Log.d("MmsService", "MMS download placeholder (Phase 1)")

        ThreadHelper.ensureBackgroundThread {
            try {
                // TODO: Phase 2 - Implement actual MMS download
                // MmsUtils.downloadMms(this, intent)

                // Simulate download delay
                Thread.sleep(2000)

                Log.d("MmsService", "MMS download completed (simulated)")
            } catch (e: Exception) {
                Log.e("MmsService", "Error in MMS download", e)
            } finally {
                stopForeground(true)
                stopSelf()
                Log.d("MmsService", "MMS Service stopped")
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloading MMS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Downloading multimedia messages"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading MMS")
            .setContentText("Please wait…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            packageName == Telephony.Sms.getDefaultSmsPackage(this)
        } else {
            true
        }
    }

    companion object {
        private const val CHANNEL_ID = "mms_download_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
