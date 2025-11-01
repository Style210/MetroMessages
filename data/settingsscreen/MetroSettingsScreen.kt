// File: MetroSettingsScreen.kt
package com.metromessages.data.settingsscreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
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
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.components.MetroFont
import kotlinx.coroutines.CoroutineScope

@Composable
fun MetroSettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

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

    // Permissions to request
    val permissionsToRequest = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECEIVE_WAP_PUSH,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    ).toList().toTypedArray()

    fun hasAllPermissions() = permissionsToRequest.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var isPermissionsGranted by remember { mutableStateOf(hasAllPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions()
    ) { permissions ->
        isPermissionsGranted = permissions.entries.all { it.value }
    }

    var showFontDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

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

                    ServiceToggleRow(
                        name = "permissions",
                        isEnabled = true,
                        isChecked = isPermissionsGranted,
                        onToggle = { enable ->
                            if (enable && !isPermissionsGranted) {
                                permissionLauncher.launch(permissionsToRequest)
                            } else if (!enable) {
                                isPermissionsGranted = false
                            }
                        },
                        metroFont = currentFont
                    )
                }
            }
        }

        // ---------------------
        // FONT DIALOG (UNCHANGED)
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
        // FIXED ACCENT DIALOG - SIMPLE & RELIABLE
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