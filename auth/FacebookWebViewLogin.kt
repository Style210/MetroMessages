// File: FacebookWebViewLogin.kt
package com.metromessages.auth

import android.webkit.WebView
import android.webkit.WebViewClient

class FacebookWebViewLogin(private val webView: WebView) {

    fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.contains("facebook.com") == true) {
                    // Inject JavaScript to extract tokens
                    extractFacebookTokens()
                }
            }
        }
    }

    private fun extractFacebookTokens() {
        val jsCode = """
            javascript:(function() {
                // Extract access token from localStorage
                var token = localStorage.getItem('fb_access_token');
                if (token) {
                    AndroidInterface.onTokenReceived(token);
                }
                
                // Alternatively, extract from cookies
                document.cookie.split(';').forEach(cookie => {
                    if (cookie.trim().startsWith('xs=') || cookie.trim().startsWith('c_user=')) {
                        AndroidInterface.onCookieReceived(cookie.trim());
                    }
                });
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }
}

