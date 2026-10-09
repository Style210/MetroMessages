// MessageNotifier.kt
// Incoming-message notifications with inline reply and mark-as-read. Stateless on purpose: the stack of
// messages shown is rebuilt from the provider's unread rows, so it survives process death.
package com.metromessages

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput

object MessageNotifier {
    const val CHANNEL_ID = "messages"
    const val EXTRA_THREAD_ID = "thread_id"
    const val EXTRA_ADDRESS = "address"
    const val ACTION_REPLY = "com.metromessages.action.REPLY"
    const val ACTION_MARK_READ = "com.metromessages.action.MARK_READ"
    const val KEY_REPLY = "key_reply"

    /** Thread currently on screen: it is already being read, so it never notifies. */
    @Volatile var activeThreadId: Long? = null

    private fun manager(c: Context) = c.getSystemService(NotificationManager::class.java)

    fun cancel(context: Context, threadId: Long) = manager(context).cancel(threadId.toInt())

    fun displayNameFor(context: Context, number: String): String = (try {
        context.contentResolver.query(
            Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) { null }) ?: number

    fun notifyIncoming(context: Context, threadId: Long, address: String) {
        if (threadId == activeThreadId) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = manager(context)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH))
        }
        val sender = Person.Builder().setName(displayNameFor(context, address)).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
        unreadBodies(context, threadId).forEach { (body, date) -> style.addMessage(body, date, sender) }

        val code = threadId.toInt()
        val open = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_THREAD_ID, threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun action(name: String, flags: Int) = PendingIntent.getBroadcast(
            context, code,
            Intent(context, NotificationActionReceiver::class.java).setAction(name)
                .putExtra(EXTRA_THREAD_ID, threadId).putExtra(EXTRA_ADDRESS, address),
            PendingIntent.FLAG_UPDATE_CURRENT or flags
        )
        val reply = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Reply", action(ACTION_REPLY, PendingIntent.FLAG_MUTABLE)
        ).addRemoteInput(RemoteInput.Builder(KEY_REPLY).setLabel("Reply").build())
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).build()
        val markRead = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view, "Mark as read", action(ACTION_MARK_READ, PendingIntent.FLAG_IMMUTABLE)
        ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build()

        nm.notify(
            code,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(open)
                .addAction(reply).addAction(markRead)
                .setAutoCancel(true)
                .build()
        )
    }

    /** Last few unread incoming texts of the thread, oldest first. */
    private fun unreadBodies(context: Context, threadId: Long): List<Pair<String, Long>> = try {
        val out = mutableListOf<Pair<String, Long>>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.TYPE} = ?",
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 5"
        )?.use { while (it.moveToNext()) out += (it.getString(0) ?: "") to it.getLong(1) }
        out.reversed()
    } catch (e: Exception) { emptyList() }
}

/** Handles the Reply and Mark-as-read buttons on the notification. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(MessageNotifier.EXTRA_THREAD_ID, -1L)
        if (threadId <= 0) return
        when (intent.action) {
            MessageNotifier.ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(MessageNotifier.KEY_REPLY)?.toString()
                val address = intent.getStringExtra(MessageNotifier.EXTRA_ADDRESS)
                if (!text.isNullOrBlank() && address != null) SmsSender.send(context, address, text)
                SmsStore.markRead(context, threadId)
            }
            MessageNotifier.ACTION_MARK_READ -> SmsStore.markRead(context, threadId)
        }
        MessageNotifier.cancel(context, threadId)
    }
}
