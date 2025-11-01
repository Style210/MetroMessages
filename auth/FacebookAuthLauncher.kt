package com.metromessages.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object FacebookAuthLauncher {

    fun launch(
        context: Context,
        onSuccess: (token: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        FacebookLoginResultHandler.setCallback(onSuccess, onError)

        // Replace this with your real backend URL when you have one
        val authUrl = "https://www.facebook.com/connect".toUri()

        val intent = Intent(Intent.ACTION_VIEW, authUrl)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}


