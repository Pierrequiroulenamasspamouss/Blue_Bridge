package com.bluebridgeapp.bluebridge.data.interfaces

import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(content: MessageContent, receiverId: String): Boolean
    suspend fun getConversations(): Flow<List<ChatConversation>>
    suspend fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun deleteConversation(conversationId: String): Boolean
    suspend fun searchMessages(query: String, mode: com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode): List<ChatMessage>
    suspend fun markMessageAsRead(messageId: String): Boolean
    suspend fun getConversation(participantIds: List<String>): ChatConversation?
    suspend fun saveMessageLocally(message: ChatMessage)
    suspend fun getLocalMessages(conversationId: String): List<ChatMessage>
    suspend fun resetConversations()

}