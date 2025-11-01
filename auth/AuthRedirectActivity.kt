package com.metromessages.auth

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.metromessages.auth.FacebookLoginResultHandler

class AuthRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data != null && data.toString().startsWith("metromessages://facebook-auth")) {
            val token = data.getQueryParameter("token")
            if (token != null) {
                // Pass the token back to the ViewModel or token storage
                FacebookLoginResultHandler.onLoginSuccess(token)
            } else {
                FacebookLoginResultHandler.onLoginError("Token missing in redirect URI")
            }
        } else {
            FacebookLoginResultHandler.onLoginError("Invalid redirect URI")
        }

        finish() // No UI, just return to previous screen
    }
}

