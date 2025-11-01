package com.metromessages.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Rect
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metromessages.data.local.MessageType
import com.metromessages.voicerecorder.VoiceRecorder
import com.metromessages.voicerecorder.VoiceRecorderState
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.mutableStateOf
import com.metromessages.viewmodel.MediaAttachment
import com.metromessages.viewmodel.MessageDraft

@Composable
fun MetroMessageInputBar(
    voiceRecorder: VoiceRecorder,
    recorderState: VoiceRecorderState,
    messageDraft: MessageDraft,
    onSendText: (String) -> Unit,
    onSendAudio: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onTextChange: (String) -> Unit,
    accentColor: Color,
    fontFamily: FontFamily,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSendAudioMessage: () -> Unit,
    onDeleteAudioPreview: () -> Unit,
    onAttachmentClick: ((Uri, Rect) -> Unit)? = null, // NEW: Attachment click handler
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val localView = LocalView.current
    val focusRequester = remember { FocusRequester() }

    // Use the draft text from the ViewModel
    val text = messageDraft.text

    // ---- Dynamic Height State ----
    var lineCount by remember { mutableIntStateOf(1) }
    val targetHeightDp =
        when {
            lineCount <= 1 -> 56.dp
            lineCount >= 5 -> 120.dp
            else -> (56 + (lineCount - 1) * 24).dp
        }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeightDp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "inputHeightAnim"
    )

    // ---- Permission launcher ----
    val recordAudioPermission = Manifest.permission.RECORD_AUDIO
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        }
    }

    fun ensureMicPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            context, recordAudioPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            onStartRecording()
        } else {
            requestPermissionLauncher.launch(recordAudioPermission)
        }
    }

    // ---- Actions ----
    val sendTextMessage: () -> Unit = {
        val trimmed = text.trim()
        if (trimmed.isNotBlank() || messageDraft.attachments.isNotEmpty()) {
            try { onSendText(trimmed) } catch (_: Exception) {}
            focusManager.clearFocus()
        }
    }

    val requestFocus: () -> Unit = {
        focusRequester.requestFocus()
    }

    // ---- UI ----
    Box(modifier = modifier.fillMaxWidth()) {
        when (recorderState) {
            is VoiceRecorderState.Idle -> {
                NormalTextInputUI(
                    text = messageDraft.text,
                    onTextChange = onTextChange,
                    onSendText = sendTextMessage,
                    onStartRecording = ::ensureMicPermissionAndStart,
                    onAttachClick = onAttachClick,
                    attachments = messageDraft.attachments,
                    onRemoveAttachment = onRemoveAttachment,
                    accentColor = accentColor,
                    fontFamily = fontFamily,
                    animatedHeight = animatedHeight,
                    focusManager = focusManager,
                    focusRequester = focusRequester,
                    requestFocus = requestFocus,
                    onLineCountChange = { count -> lineCount = count },
                    onAttachmentClick = onAttachmentClick // NEW: Pass click handler
                )
            }
            is VoiceRecorderState.Recording -> {
                RecordingUI(
                    elapsedTime = recorderState.elapsedTimeMs,
                    amplitude = recorderState.maxAmplitude,
                    onStopRecording = onStopRecording,
                    onCancelRecording = onCancelRecording,
                    accentColor = accentColor,
                    fontFamily = fontFamily
                )
            }
            is VoiceRecorderState.Preview -> {
                AudioPreviewUI(
                    audioFilePath = recorderState.audioFilePath,
                    onSendAudio = onSendAudioMessage,
                    onDeleteAudio = onDeleteAudioPreview,
                    accentColor = accentColor,
                    fontFamily = fontFamily
                )
            }
        }
    }
}

@Composable
private fun NormalTextInputUI(
    text: String,
    onTextChange: (String) -> Unit,
    onSendText: () -> Unit,
    onStartRecording: () -> Unit,
    onAttachClick: () -> Unit,
    attachments: List<MediaAttachment>,
    onRemoveAttachment: (String) -> Unit,
    accentColor: Color,
    fontFamily: FontFamily,
    animatedHeight: Dp,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    requestFocus: () -> Unit,
    onLineCountChange: (Int) -> Unit,
    onAttachmentClick: ((Uri, Rect) -> Unit)? = null // NEW: Attachment click handler
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Show attachment previews if any
        if (attachments.isNotEmpty()) {
            AttachmentPreviews(
                attachments = attachments,
                onRemoveAttachment = onRemoveAttachment,
                onAttachmentClick = onAttachmentClick, // NEW: Pass click handler
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Action buttons row (Metro-style floating actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Voice memo button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor, androidx.compose.foundation.shape.CircleShape)
                    .clickable { onStartRecording() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Memo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Attachment button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor, androidx.compose.foundation.shape.CircleShape)
                    .clickable { onAttachClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = "Attach File",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Text input row - Now with dynamic height
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text input - Using AndroidView EditText with dynamic height
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(animatedHeight)
                    .background(accentColor.copy(alpha = 0.15f))
                    .clickable { requestFocus() }
            ) {
                RichMessageEditText(
                    text = text,
                    onTextChange = onTextChange,
                    onSend = onSendText,
                    maxLines = 5,
                    focusRequester = focusRequester,
                    onLineCountChange = onLineCountChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // Send button - enable if there's text or attachments
            val hasContent = text.isNotBlank() || attachments.isNotEmpty()
            IconButton(
                onClick = onSendText,
                modifier = Modifier.size(44.dp),
                enabled = hasContent
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = accentColor.copy(alpha = if (hasContent) 1f else 0.5f)
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreviews(
    attachments: List<MediaAttachment>,
    onRemoveAttachment: (String) -> Unit,
    onAttachmentClick: ((Uri, Rect) -> Unit)? = null, // NEW: Attachment click handler
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            val localView = LocalView.current
            var viewBounds by remember { mutableStateOf(Rect()) }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .onGloballyPositioned { coordinates ->
                        // Calculate screen bounds for smooth transition
                        val location = IntArray(2)
                        localView.getLocationOnScreen(location)
                        viewBounds = Rect(
                            location[0] + coordinates.positionInWindow().x.roundToInt(),
                            location[1] + coordinates.positionInWindow().y.roundToInt(),
                            location[0] + coordinates.positionInWindow().x.roundToInt() + coordinates.size.width,
                            location[1] + coordinates.positionInWindow().y.roundToInt() + coordinates.size.height
                        )
                    }
                    .clickable(
                        enabled = onAttachmentClick != null,
                        onClick = {
                            onAttachmentClick?.invoke(attachment.uri, viewBounds)
                        }
                    )
            ) {
                // Show image preview for images
                if (attachment.type == MessageType.IMAGE) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(attachment.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Attachment preview",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show icon for videos and other types
                    val icon = when (attachment.type) {
                        MessageType.VIDEO -> Icons.Filled.Videocam
                        MessageType.AUDIO -> Icons.Filled.AudioFile
                        MessageType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
                        else -> Icons.Filled.Attachment
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Attachment",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }

                // Remove button
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove attachment",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clickable { onRemoveAttachment(attachment.uri.toString()) }
                        .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                        .padding(2.dp)
                )
            }
        }
    }
}

// ... rest of the MetroMessageInputBar functions remain the same ...

@Composable
private fun RecordingUI(
    elapsedTime: Long,
    amplitude: Int,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Cancel button
        IconButton(onClick = onCancelRecording) {
            Icon(Icons.Filled.Close, "Cancel", tint = Color.Red)
        }

        // Timer and waveform
        Text(
            text = formatMillis(elapsedTime),
            color = accentColor,
            fontFamily = fontFamily
        )

        // Stop button
        IconButton(onClick = onStopRecording) {
            Icon(Icons.Filled.Mic, "Stop", tint = accentColor)
        }
    }
}

@Composable
private fun AudioPreviewUI(
    audioFilePath: String,
    onSendAudio: () -> Unit,
    onDeleteAudio: () -> Unit,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Delete button
        IconButton(onClick = onDeleteAudio) {
            Icon(Icons.Filled.Delete, "Delete", tint = Color.Red)
        }

        // Audio player preview
        Text(
            text = "Audio recorded",
            color = accentColor,
            fontFamily = fontFamily
        )

        // Send button
        IconButton(onClick = onSendAudio) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = accentColor)
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}


