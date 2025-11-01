// File: MessageType.kt
package com.metromessages.data.local

sealed interface MessageType {
    object TEXT : MessageType
    object AUDIO : MessageType
    object IMAGE : MessageType
    object VIDEO : MessageType
    object FILE : MessageType
    object LINK : MessageType
    object MEDIA : MessageType // For "Media, files and links"
}
