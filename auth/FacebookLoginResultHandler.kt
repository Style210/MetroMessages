package com.metromessages.auth

object FacebookLoginResultHandler {
    private var onSuccess: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun setCallback(
        onSuccess: (token: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        this.onSuccess = onSuccess
        this.onError = onError
    }

    fun onLoginSuccess(token: String) {
        onSuccess?.invoke(token)
        clear()
    }

    fun onLoginError(error: String) {
        onError?.invoke(error)
        clear()
    }

    private fun clear() {
        onSuccess = null
        onError = null
    }
}

