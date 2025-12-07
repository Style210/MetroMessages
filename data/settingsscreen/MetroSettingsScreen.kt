// File: ui/screens/MetroSettingsScreen.kt
package com.metromessages.data.settingsscreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale


@Composable
fun MetroSettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Settings preferences
    val settingsPreferences = remember { SettingsPreferences(context) }

    // Archive security state - using DataStore flow
    val isArchiveSecurityEnabled by settingsPreferences.archiveSecurityFlow.collectAsState(initial = true)

    // Scroll states
    val headerScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    val currentFont by viewModel.currentFont.collectAsState()
    val currentAccent by viewModel.currentAccentColor.collectAsState()

    val isFacebookConnected by viewModel.isFacebookConnected.collectAsState()
    val isWhatsAppConnected by viewModel.isWhatsAppConnected.collectAsState()
    val isTelegramConnected by viewModel.isTelegramConnected.collectAsState()

    val isFacebookComingSoon = viewModel.isFacebookComingSoon
    val isWhatsAppComingSoon = viewModel.isWhatsAppComingSoon
    val isTelegramComingSoon = viewModel.isTelegramComingSoon

    // Permissions to request - COMPREHENSIVE SET FOR SMS/MMS REPLACEMENT + VOICE + CONTACTS WRITE
    val permissionsToRequest = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS, // ✅ CRITICAL: Added for contact editing
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.RECORD_AUDIO, // Voice recording permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        },
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            null
        },
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            null
        }
    ).filterNotNull().toTypedArray()

    // Debug function to check permission status
    fun debugPermissions() {
        println("🔍 DEBUG PERMISSIONS STATUS:")
        permissionsToRequest.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            println("   - $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
        }

        // Specifically check WRITE_CONTACTS
        val writeContactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        println("   - WRITE_CONTACTS specifically: ${if (writeContactsGranted) "✅ GRANTED" else "❌ DENIED"}")
    }

    fun hasAllPermissions() = permissionsToRequest.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
        } else {
            true // Pre-KitKat, can't check this
        }
    }

    fun requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }
    }

    var isPermissionsGranted by remember { mutableStateOf(hasAllPermissions()) }
    var isSmsDefault by remember { mutableStateOf(isDefaultSmsApp()) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSmsDefaultDialog by remember { mutableStateOf(false) }
    var showPermanentDenialDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        isPermissionsGranted = allGranted
        isSmsDefault = isDefaultSmsApp()

        // Debug the results
        println("🔍 PERMISSION REQUEST RESULTS:")
        permissions.forEach { (permission, granted) ->
            println("   - $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
        }
        println("   - All granted: $allGranted")
        println("   - Is default SMS: $isSmsDefault")

        if (!allGranted) {
            // Check if we should show rationale
            val shouldShowRationale = permissionsToRequest.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                        (context as? android.app.Activity)?.let { activity ->
                            shouldShowRequestPermissionRationale(activity, permission)
                        } ?: false
            }

            if (shouldShowRationale) {
                showPermissionRationale = true
            } else {
                // Permissions permanently denied
                showPermanentDenialDialog = true
            }
        } else if (!isSmsDefault) {
            // Permissions granted but not default SMS app
            showSmsDefaultDialog = true
        }
    }

    var showFontDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

    // Re-check SMS default status when composable recomposes
    LaunchedEffect(Unit) {
        isSmsDefault = isDefaultSmsApp()
        debugPermissions() // Debug current permission state
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header area (fixed height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onBackClick()
                    }
            ) {
                MetroHeaderCanvas(
                    text = "settings",
                    scrollState = headerScrollState,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                    metroFont = currentFont
                )
            }

            // Content area (takes remaining space and scrolls)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(contentScrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AccentDemoQuote(accentColor = currentAccent, metroFont = currentFont)

                OutlineSelectorBox(
                    title = "font",
                    currentValue = currentFont.displayName,
                    onClick = { showFontDialog = true },
                    modifier = Modifier.height(56.dp),
                    metroFont = currentFont
                )

                OutlineSelectorBox(
                    title = "accent",
                    currentValue = "",
                    onClick = { showAccentDialog = true },
                    modifier = Modifier.height(48.dp),
                    metroFont = currentFont,
                    content = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(currentAccent.copy(alpha = 1f), RectangleShape)
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Archive Security Toggle - NEW
                    ServiceToggleRow(
                        name = "archive security",
                        isEnabled = true,
                        isChecked = isArchiveSecurityEnabled,
                        onToggle = { enabled ->
                            coroutineScope.launch {
                                settingsPreferences.setArchiveSecurity(enabled)
                            }
                        },
                        metroFont = currentFont
                    )

                    ServiceToggleRow(
                        name = "facebook",
                        isEnabled = !isFacebookComingSoon,
                        isChecked = isFacebookConnected,
                        onToggle = { viewModel.onFacebookToggle(it) },
                        metroFont = currentFont
                    )
                    ServiceToggleRow(
                        name = "whatsapp",
                        isEnabled = !isWhatsAppComingSoon,
                        isChecked = isWhatsAppConnected,
                        onToggle = { viewModel.onWhatsAppToggle(it) },
                        metroFont = currentFont
                    )
                    ServiceToggleRow(
                        name = "telegram",
                        isEnabled = !isTelegramComingSoon,
                        isChecked = isTelegramConnected,
                        onToggle = { viewModel.onTelegramToggle(it) },
                        metroFont = currentFont
                    )

                    // Enhanced permission toggle with SMS default handling
                    EnhancedPermissionToggleRow(
                        name = "permissions",
                        isGranted = isPermissionsGranted,
                        isDefaultSms = isSmsDefault,
                        onRequestPermissions = {
                            println("🔄 Requesting all permissions including WRITE_CONTACTS")
                            permissionLauncher.launch(permissionsToRequest)
                        },
                        onSetDefaultSms = {
                            requestDefaultSmsApp()
                        },
                        metroFont = currentFont,
                        accentColor = currentAccent
                    )
                }
            }
        }

        // ---------------------
        // FONT DIALOG
        // ---------------------
        if (showFontDialog) {
            Dialog(onDismissRequest = { showFontDialog = false }) {
                Column(
                    modifier = Modifier
                        .background(Color.Black, RectangleShape)
                        .padding(16.dp)
                        .border(2.dp, Color(0xFF888888), RectangleShape)
                ) {
                    MetroFont.entries.forEach { font ->
                        Text(
                            text = font.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrentFont(font)
                                    showFontDialog = false
                                }
                                .padding(16.dp),
                            color = Color.White,
                            style = MetroTypography.MetroBody1(font)
                        )
                    }
                }
            }
        }

        // ---------------------
        // ACCENT DIALOG
        // ---------------------
        if (showAccentDialog) {
            Dialog(
                onDismissRequest = { showAccentDialog = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val allAccents = listOf(Color.White) +
                                viewModel.availableAccents.filter { it != Color.White }

                        // Define rows with proper spacing
                        val rows = listOf(
                            allAccents.subList(0, 4),   // Row 1: 4 swatches
                            allAccents.subList(4, 8),   // Row 2: 4 swatches
                            allAccents.subList(8, 12),  // Row 3: 4 swatches
                            allAccents.subList(12, 14)  // Row 4: 2 swatches
                        )

                        rows.forEachIndexed { rowIndex, rowColors ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowColors.forEach { color ->
                                    SimpleMetroColorSwatch(
                                        color = color,
                                        isSelected = color == currentAccent,
                                        onClick = {
                                            viewModel.setAccentColor(color)
                                            showAccentDialog = false
                                        }
                                    )
                                }

                                // Add invisible spacers to maintain equal spacing for incomplete rows
                                if (rowColors.size < 4) {
                                    repeat(4 - rowColors.size) {
                                        Spacer(modifier = Modifier.size(56.dp))
                                    }
                                }
                            }

                            // Consistent vertical spacing between all rows
                            if (rowIndex < rows.size - 1) {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // ---------------------
        // PERMISSION RATIONALE DIALOG
        // ---------------------
        if (showPermissionRationale) {
            Dialog(onDismissRequest = { showPermissionRationale = false }) {
                Column(
                    modifier = Modifier
                        .background(Color.Black, RectangleShape)
                        .padding(16.dp)
                        .border(2.dp, Color(0xFF888888), RectangleShape)
                ) {
                    Text(
                        text = "Full Messaging System Permissions",
                        style = MetroTypography.MetroBody1(currentFont),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "MetroMessages replaces your ENTIRE messaging system. We need:\n\n" +
                                "📱 SMS/MMS: Read, send, receive ALL messages\n" +
                                "📞 Phone: Caller ID, dual SIM support\n" +
                                "👥 Contacts: Read AND edit your address book\n" +
                                "🖼️ Media: Send/receive photos, videos, GIFs\n" +
                                "🎤 Audio: Voice message recording\n" +
                                "📨 Push: MMS/WAP message delivery\n" +
                                "🔔 Notifications: Message alerts\n\n" +
                                "These are the SAME permissions used by Google Messages, Samsung Messages, and other default SMS apps.",
                        style = MetroTypography.MetroBody2(currentFont),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NOT NOW",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { showPermissionRationale = false }
                                .padding(8.dp)
                        )
                        Text(
                            text = "GRANT ALL",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = currentAccent,
                            modifier = Modifier
                                .clickable {
                                    permissionLauncher.launch(permissionsToRequest)
                                    showPermissionRationale = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // ---------------------
        // SMS DEFAULT APP DIALOG
        // ---------------------
        if (showSmsDefaultDialog) {
            Dialog(onDismissRequest = { showSmsDefaultDialog = false }) {
                Column(
                    modifier = Modifier
                        .background(Color.Black, RectangleShape)
                        .padding(16.dp)
                        .border(2.dp, Color(0xFF888888), RectangleShape)
                ) {
                    Text(
                        text = "Set as Default SMS App",
                        style = MetroTypography.MetroBody1(currentFont),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "To send and receive SMS/MMS messages, MetroMessages needs to be your default SMS app.\n\n" +
                                "This allows the app to:\n" +
                                "• Send and receive all SMS messages\n" +
                                "• Handle MMS messages with media\n" +
                                "• Show message notifications\n" +
                                "• Sync with your message history\n" +
                                "• Work with other apps that send SMS",
                        style = MetroTypography.MetroBody2(currentFont),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LATER",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { showSmsDefaultDialog = false }
                                .padding(8.dp)
                        )
                        Text(
                            text = "SET DEFAULT",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = currentAccent,
                            modifier = Modifier
                                .clickable {
                                    requestDefaultSmsApp()
                                    showSmsDefaultDialog = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // ---------------------
        // PERMANENT DENIAL DIALOG
        // ---------------------
        if (showPermanentDenialDialog) {
            Dialog(onDismissRequest = { showPermanentDenialDialog = false }) {
                Column(
                    modifier = Modifier
                        .background(Color.Black, RectangleShape)
                        .padding(16.dp)
                        .border(2.dp, Color(0xFF888888), RectangleShape)
                ) {
                    Text(
                        text = "Permissions Required",
                        style = MetroTypography.MetroBody1(currentFont),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Some permissions were permanently denied. Please enable them in app settings:\n\n" +
                                "1. Go to Settings → Apps → MetroMessages\n" +
                                "2. Tap 'Permissions' \n" +
                                "3. Grant all required permissions\n" +
                                "4. Return to this app",
                        style = MetroTypography.MetroBody2(currentFont),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CANCEL",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { showPermanentDenialDialog = false }
                                .padding(8.dp)
                        )
                        Text(
                            text = "SETTINGS",
                            style = MetroTypography.MetroBody1(currentFont),
                            color = currentAccent,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                    showPermanentDenialDialog = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedPermissionToggleRow(
    name: String,
    isGranted: Boolean,
    isDefaultSms: Boolean,
    onRequestPermissions: () -> Unit,
    onSetDefaultSms: () -> Unit,
    metroFont: MetroFont,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                style = MetroTypography.MetroBody1(metroFont),
                color = Color.White
            )

            // Show detailed status
            Text(
                text = buildPermissionStatusText(isGranted, isDefaultSms),
                style = MetroTypography.MetroBody2(metroFont),
                color = getPermissionStatusColor(isGranted, isDefaultSms)
            )
        }

        // Enhanced action button
        Box(
            modifier = Modifier
                .clickable {
                    when {
                        !isGranted -> onRequestPermissions()
                        !isDefaultSms -> onSetDefaultSms()
                        // If both granted and default, do nothing (fully configured)
                    }
                }
        ) {
            val (statusText, statusColor) = getPermissionButtonState(isGranted, isDefaultSms, accentColor)

            Text(
                text = statusText,
                style = MetroTypography.MetroBody1(metroFont),
                color = statusColor
            )
        }
    }
}

private fun buildPermissionStatusText(isGranted: Boolean, isDefaultSms: Boolean): String {
    return when {
        !isGranted -> "tap to grant all permissions"
        !isDefaultSms -> "permissions granted • not default SMS app"
        else -> "fully configured ✓"
    }
}

private fun getPermissionStatusColor(isGranted: Boolean, isDefaultSms: Boolean): Color {
    return when {
        !isGranted -> Color.Yellow.copy(alpha = 0.7f)
        !isDefaultSms -> Color.Yellow.copy(alpha = 0.7f)
        else -> Color.Green
    }
}

private fun getPermissionButtonState(isGranted: Boolean, isDefaultSms: Boolean, accentColor: Color): Pair<String, Color> {
    return when {
        !isGranted -> "REQUEST" to accentColor
        !isDefaultSms -> "SET DEFAULT" to Color.Yellow
        else -> "GRANTED" to Color.Green
    }
}

@Composable
private fun SimpleMetroColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color, RectangleShape)
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun AccentDemoQuote(
    accentColor: Color,
    metroFont: MetroFont
) {
    val annotatedText = buildAnnotatedString {
        append("Being the richest man in the cemetery doesn't matter to me.  Going to bed at night saying ")
        withStyle(SpanStyle(color = accentColor)) {
            append("'we've done something wonderful' ")
        }
        append("– that's what matters to me.")
    }

    Text(
        text = annotatedText,
        style = MetroTypography.MetroBody2(metroFont),
        color = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
private fun OutlineSelectorBox(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    metroFont: MetroFont,
    content: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MetroTypography.MetroSubhead(metroFont),
            color = Color.White
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF888888), RectangleShape)
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = currentValue,
                    style = MetroTypography.MetroBody1(metroFont),
                    color = if (currentValue.isBlank())
                        Color.White.copy(alpha = 0.6f) else Color.White
                )
            }
        }
    }
}

@Composable
private fun ServiceToggleRow(
    name: String,
    isEnabled: Boolean,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    metroFont: MetroFont
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                style = MetroTypography.MetroBody1(metroFont),
                color = Color.White
            )
            if (!isEnabled) {
                Text(
                    text = "coming soon!",
                    style = MetroTypography.MetroBody2(metroFont),
                    color = Color.Gray
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { if (isEnabled) onToggle(it) },
            enabled = isEnabled
        )
    }
}
