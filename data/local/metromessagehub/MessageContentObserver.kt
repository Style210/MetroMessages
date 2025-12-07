package com.metromessages.data.local.metromessagehub

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Telephony

class MessageContentObserver(
    private val handler: Handler,
    private val onMessageChanged: () -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        // Trigger refresh when SMS content changes
        onMessageChanged()
    }

    fun register(context: Context) {
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            this
        )
    }

    fun unregister(context: Context) {
        context.contentResolver.unregisterContentObserver(this)
    }
}
