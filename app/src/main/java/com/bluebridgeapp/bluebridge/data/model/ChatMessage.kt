package com.bluebridgeapp.bluebridge.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val imageUri: String? = null
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    LOCATION
}

@Serializable
data class ChatConversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val lastActivity: Long = System.currentTimeMillis()
)

@Serializable
data class SendMessageRequest(
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: MessageType = MessageType.TEXT
)

@Serializable
data class FCMNotification(
    val to: String,
    val data: Map<String, String>,
    val notification: FCMNotificationPayload
)

@Serializable
data class FCMNotificationPayload(
    val title: String,
    val body: String,
    val sound: String = "default"
) 