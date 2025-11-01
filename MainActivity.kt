package com.metromessages

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.navigation.MetroNavHost
import com.metromessages.ui.theme.enterImmersiveMode
import com.metromessages.viewmodel.FacebookViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appInitializer: AppInitializer

    private val defaultSmsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when {
            isDefaultSmsApp() -> handleDefaultSmsGranted()
            result.resultCode != RESULT_CANCELED -> showDefaultSmsWarning()
        }
    }

    private val facebookViewModel: FacebookViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enterImmersiveMode(window)

        appInitializer.initialize(lifecycleScope)
        checkDefaultSmsStatus()

        setContent {
            // ✅ ADDED: Persistent gradient background at root level
            MetroMessagesAppContent() // ✅ CHANGED: Renamed to avoid conflict
        }

        handleIncomingIntent(intent)

        println("DEBUG: onCreate intent action: ${intent.action}, type: ${intent.type}")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("DEBUG: onNewIntent action: ${intent.action}, type: ${intent.type}")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                handleSendIntent(intent)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                handleSendMultipleIntent(intent)
            }
        }
    }

    private fun handleSendIntent(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val type = intent.type
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (uri != null) {
            Toast.makeText(this, "Processing content from Gboard: $type", Toast.LENGTH_SHORT).show()
            facebookViewModel.handleIncomingMedia(uri, type, text)
        } else if (!text.isNullOrBlank()) {
            facebookViewModel.updateDraftText(text)
        }
    }

    private fun handleSendMultipleIntent(intent: Intent) {
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        val type = intent.type

        if (!uris.isNullOrEmpty()) {
            Toast.makeText(this, "Processing multiple items: ${uris.size}", Toast.LENGTH_SHORT).show()
            uris.forEach { uri ->
                facebookViewModel.handleIncomingMedia(uri, type, null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode(window)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode(window)
        }
    }

    private fun checkDefaultSmsStatus() {
        if (!isDefaultSmsApp()) {
            requestDefaultSmsApp()
        } else {
            handleDefaultSmsGranted()
        }
    }

    private fun requestDefaultSmsApp() {
        try {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            defaultSmsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error requesting default SMS status",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return packageName == Telephony.Sms.getDefaultSmsPackage(this)
    }

    private fun handleDefaultSmsGranted() {
        Toast.makeText(this, "Default SMS access granted", Toast.LENGTH_SHORT).show()
    }

    private fun showDefaultSmsWarning() {
        Toast.makeText(
            this,
            "Full features require being the default SMS app",
            Toast.LENGTH_LONG
        ).show()
    }
}

// ✅ NEW: Root-level composable with persistent gradient - RENAMED to avoid conflict
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MetroMessagesAppContent() { // ✅ CHANGED: Renamed from MetroMessagesApp to MetroMessagesAppContent
    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(initial = Color(0xFF0063B1))

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ PERSISTENT GRADIENT BACKGROUND - Always there, never transitions
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.8f),    // Strong at top
                    accentColor.copy(alpha = 0.4f),    // Medium
                    accentColor.copy(alpha = 0.1f),    // Subtle
                    Color.Transparent                  // Fade out
                ),
                startY = 0f,
                endY = size.height * 0.4f // Cover top 40% of screen
            )
            drawRect(brush = gradientBrush)
        }

        // ✅ Your existing NavHost - ALL pages transition OVER the fixed gradient
        MetroNavHost(
            modifier = Modifier.fillMaxSize()
        )
    }
}
