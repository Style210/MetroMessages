// File: MessageTypeConverter.kt
package com.metromessages.data.local

import androidx.room.TypeConverter

class MessageTypeConverter {

    @TypeConverter
    fun fromMessageType(messageType: MessageType): String = when (messageType) {
        MessageType.TEXT -> "TEXT"
        MessageType.AUDIO -> "AUDIO"
        MessageType.IMAGE -> "IMAGE"
        MessageType.VIDEO -> "VIDEO"
        MessageType.FILE -> "FILE"
        MessageType.LINK -> "LINK"
        MessageType.MEDIA -> "MEDIA"
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType = when (value) {
        "TEXT" -> MessageType.TEXT
        "AUDIO" -> MessageType.AUDIO
        "IMAGE" -> MessageType.IMAGE
        "VIDEO" -> MessageType.VIDEO
        "FILE" -> MessageType.FILE
        "LINK" -> MessageType.LINK
        "MEDIA" -> MessageType.MEDIA
        else -> throw IllegalArgumentException("Unknown MessageType: $value")
    }
}

