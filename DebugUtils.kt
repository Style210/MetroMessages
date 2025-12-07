package com.metromessages

import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat


object DebugUtils {

    fun logWindowInfo(window: Window, tag: String = "WindowDebug") {
        Log.d(tag, "=== WINDOW DEBUG INFO ===")

        // Check window attributes for immersive mode flags
        val params = window.attributes
        Log.d(tag, "Window flags: ${Integer.toBinaryString(params.flags)}")

        // Check for common immersive flags
        val hasLayoutNoLimits = (params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0
        val hasLayoutInScreen = (params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0

        Log.d(tag, "FLAG_LAYOUT_NO_LIMITS: $hasLayoutNoLimits")
        Log.d(tag, "FLAG_LAYOUT_IN_SCREEN: $hasLayoutInScreen")

        Log.d(tag, "=== END WINDOW DEBUG ===")
    }

    fun checkImmersiveMode(window: Window, tag: String = "ImmersiveDebug"): Boolean {
        val decorView = window.decorView
        val rootWindowInsets = decorView.rootWindowInsets

        // ✅ NULL CHECK - rootWindowInsets is null during onCreate
        if (rootWindowInsets == null) {
            Log.d(tag, "RootWindowInsets is null - window not laid out yet")
            return false
        }

        val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)

        Log.d(tag, "=== IMMERSIVE MODE CHECK ===")
        Log.d(tag, "Status bar visible: ${insets.isVisible(WindowInsetsCompat.Type.statusBars())}")
        Log.d(tag, "Navigation bar visible: ${insets.isVisible(WindowInsetsCompat.Type.navigationBars())}")
        Log.d(tag, "System bars visible: ${insets.isVisible(WindowInsetsCompat.Type.systemBars())}")

        val isImmersive = !insets.isVisible(WindowInsetsCompat.Type.systemBars())
        Log.d(tag, "Immersive mode active: $isImmersive")
        Log.d(tag, "=== END IMMERSIVE CHECK ===")

        return isImmersive
    }

    // Simple check for decor fits system windows (manual implementation)
    fun checkDecorFitsSystemWindows(window: Window, tag: String = "DecorDebug"): Boolean {
        // This is a simplified check - the actual method isn't available in our version
        val params = window.attributes
        val hasImmersiveFlags = (params.flags and (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)) != 0

        Log.d(tag, "Decor likely fits system windows: ${!hasImmersiveFlags}")
        return !hasImmersiveFlags
    }
}