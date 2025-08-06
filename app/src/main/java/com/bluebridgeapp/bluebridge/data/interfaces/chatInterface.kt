package com.bluebridgeapp.bluebridge.data.interfaces

import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.SendMessageRequest
import com.bluebridgeapp.bluebridge.data.model.FCMNotification
import com.bluebridgeapp.bluebridge.data.model.FCMNotificationPayload
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(request: SendMessageRequest): Boolean
    suspend fun getConversations(): Flow<List<ChatConversation>>
    suspend fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun markMessageAsRead(messageId: String): Boolean
    suspend fun getConversation(participantIds: List<String>): ChatConversation?
    suspend fun saveMessageLocally(message: ChatMessage)
    suspend fun getLocalMessages(conversationId: String): List<ChatMessage>
    suspend fun clearLocalMessages(conversationId: String)
    suspend fun getDeviceToken(userId: String): String?
    suspend fun sendFCMNotification(
        toToken: String, 
        title: String, 
        body: String, 
        data: Map<String, String>
    ): Boolean
    suspend fun deleteConversation(conversationId: String): Boolean
    suspend fun searchMessages(query: String, mode: com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode): List<ChatMessage>
    suspend fun sendImageMessage(imageUri: String, receiverId: String): Boolean
} 