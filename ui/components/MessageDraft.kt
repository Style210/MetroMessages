package com.metromessages.ui.components

import android.net.Uri
import com.metromessages.data.local.metromessagehub.MessageType

data class MessageDraft(
    val text: String = "",
    val attachments: List<MediaAttachment> = emptyList()
)

data class MediaAttachment(
    val uri: Uri,
    val type: MessageType,
    val thumbnailUri: Uri? = null,
    val isPlaceholder: Boolean = false)