package com.metromessages.ui.theme

import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun enterImmersiveMode(window: Window) {
    // Tell the system that Compose will handle insets itself
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Use WindowInsetsControllerCompat to hide system bars
    val controller = WindowInsetsControllerCompat(window, window.decorView)

    // Hide both status and navigation bars
    controller.hide(WindowInsetsCompat.Type.systemBars())

    // Transient bars behavior: swipe to temporarily show
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    // Optional: For API 30+ ensure the modern controller is consistent
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let { insetsController ->
            insetsController.hide(android.view.WindowInsets.Type.systemBars())
            insetsController.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
