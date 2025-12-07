package com.metromessages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ThreadHelper {
    fun ensureBackgroundThread(block: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            block()
        }
    }
}
