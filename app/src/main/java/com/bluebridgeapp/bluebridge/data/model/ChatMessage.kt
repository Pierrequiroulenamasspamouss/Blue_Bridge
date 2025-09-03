package com.bluebridgeapp.bluebridge.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PostMessageRequest(val data: PostMessageData)

@Serializable
data class PostMessageData(
    val senderId: String = "",
    val receiverId: String = "",
    val content: MessageContent,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: MessageContent,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,

)

@Serializable
sealed class MessageContent {
    @Serializable
    data class Text(val text: String) : MessageContent()
    @Serializable
    data class Media(val base64: String, val mediaType: MediaType) : MessageContent()
}

@Serializable
enum class MessageType {
    TEXT,
}

@Serializable
data class ChatConversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val lastActivity: Long = System.currentTimeMillis()
)
/*

LEGACY WITH IMAGE CHUNKS. MIGHT RE ADD LATER ON
@Serializable
data class ImageChunk(
    val imageId: Int,
    val totalChunks: Int,
    val chunkIndex: Int,
    val data: String
)
 */

@Serializable
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}