package com.bluebridgeapp.bluebridge.data.repository

import android.content.Context
import android.util.Log
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.SendMessageRequest
import com.bluebridgeapp.bluebridge.data.model.FCMNotification
import com.bluebridgeapp.bluebridge.data.model.FCMNotificationPayload
import com.bluebridgeapp.bluebridge.network.ServerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import com.bluebridgeapp.bluebridge.data.local.ChatPreferences

class ChatRepositoryImpl(
    private val api: ServerApi,
    private val userRepository: UserRepository,
    private val context: Context
) : ChatRepository {
    
    private val TAG = "ChatRepository"
    private val chatPrefs = ChatPreferences(context)
    private val conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    private val messages = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    
    init {
        // Load persisted conversations
        val loadedConvs = chatPrefs.loadConversations()
        conversations.value = loadedConvs
        // Load messages for each conversation
        loadedConvs.forEach { conv ->
            val msgs = chatPrefs.loadMessages(conv.conversationId)
            messages[conv.conversationId] = MutableStateFlow(msgs)
        }
    }

    override suspend fun sendMessage(request: SendMessageRequest): Boolean {
        Log.d(TAG, "sendMessage() called - senderId: ${request.senderId}, receiverId: ${request.receiverId}, content: '${request.content}'")
        return try {
            // Send message via server API instead of FCM
            val response = api.sendMessage(request)
            val success = response.isSuccessful
            
            if (success) {
                Log.d(TAG, "Message sent successfully via server API")
                
                // Create message for local storage
                val message = ChatMessage(
                    messageId = java.util.UUID.randomUUID().toString(),
                    senderId = request.senderId,
                    senderName = userRepository.getUserData().first()?.firstName ?: "Unknown",
                    receiverId = request.receiverId,
                    content = request.content,
                    timestamp = System.currentTimeMillis(),
                    messageType = request.messageType
                )
                
                // Save message locally after successful sending
                saveMessageLocally(message)
                true
            } else {
                Log.e(TAG, "Failed to send message via server API: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }

    override suspend fun getConversations(): Flow<List<ChatConversation>> {
        Log.d(TAG, "getConversations() called")
        // Always reload from preferences
        conversations.value = chatPrefs.loadConversations()
        return conversations.asStateFlow()
    }

    override suspend fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        Log.d(TAG, "getMessages() called for conversationId: $conversationId")
        if (!messages.containsKey(conversationId)) {
            val loaded = chatPrefs.loadMessages(conversationId)
            messages[conversationId] = MutableStateFlow(loaded)
        }
        val messageFlow = messages[conversationId]!!
        Log.d(TAG, "Returning message flow with ${messageFlow.value.size} messages")
        return messageFlow.asStateFlow()
    }

    override suspend fun markMessageAsRead(messageId: String): Boolean {
        Log.d(TAG, "markMessageAsRead() called for messageId: $messageId")
        try {
            messages.forEach { (convId, messageFlow) ->
                val currentMessages = messageFlow.value.toMutableList()
                val messageIndex = currentMessages.indexOfFirst { it.messageId == messageId }
                if (messageIndex != -1) {
                    currentMessages[messageIndex] = currentMessages[messageIndex].copy(isRead = true)
                    messageFlow.value = currentMessages
                    chatPrefs.saveMessages(convId, currentMessages)
                }
            }
            Log.d(TAG, "Message marked as read successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read", e)
            return false
        }
    }

    override suspend fun getConversation(participantIds: List<String>): ChatConversation? {
        Log.d(TAG, "getConversation() called for participants: $participantIds")
        val convs = chatPrefs.loadConversations()
        val conversation = convs.find { conversation ->
            conversation.participants.containsAll(participantIds) && 
            participantIds.containsAll(conversation.participants)
        }
        Log.d(TAG, "Found conversation: ${conversation?.conversationId}")
        return conversation
    }

    override suspend fun saveMessageLocally(message: ChatMessage) {
        Log.d(TAG, "saveMessageLocally() called for message: ${message.messageId}")
        val conversationId = getConversationId(message.senderId, message.receiverId)
        Log.d(TAG, "Conversation ID: $conversationId")
        if (!messages.containsKey(conversationId)) {
            messages[conversationId] = MutableStateFlow(emptyList())
        }
        val currentMessages = messages[conversationId]!!.value.toMutableList()
        currentMessages.add(message)
        messages[conversationId]!!.value = currentMessages.sortedBy { it.timestamp }
        chatPrefs.saveMessages(conversationId, messages[conversationId]!!.value)
        Log.d(TAG, "Message saved locally, total messages in conversation: ${messages[conversationId]!!.value.size}")
        updateConversations(message, conversationId)
    }

    override suspend fun getLocalMessages(conversationId: String): List<ChatMessage> {
        Log.d(TAG, "getLocalMessages() called for conversationId: $conversationId")
        val msgs = chatPrefs.loadMessages(conversationId)
        Log.d(TAG, "Returning ${msgs.size} local messages")
        return msgs
    }

    override suspend fun clearLocalMessages(conversationId: String) {
        Log.d(TAG, "clearLocalMessages() called for conversationId: $conversationId")
        messages[conversationId]?.value = emptyList()
        chatPrefs.saveMessages(conversationId, emptyList())
        Log.d(TAG, "Local messages cleared for conversation: $conversationId")
    }

    override suspend fun getDeviceToken(userId: String): String? {
        Log.d(TAG, "getDeviceToken() called for userId: $userId")
        return try {
            val token = "dummy_device_token_$userId"
            Log.d(TAG, "Returning device token: $token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device token", e)
            null
        }
    }

    override suspend fun sendFCMNotification(
        toToken: String, 
        title: String, 
        body: String, 
        data: Map<String, String>
    ): Boolean {
        Log.d(TAG, "sendFCMNotification() called - toToken: $toToken, title: '$title', body: '$body'")
        
        return try {
            val notification = FCMNotification(
                to = toToken,
                data = data,
                notification = FCMNotificationPayload(
                    title = title,
                    body = body
                )
            )

            val jsonBody = Json.encodeToString(notification)
            Log.d(TAG, "FCM notification JSON: $jsonBody")
            
            // Use withContext to ensure this runs on a background thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=YOUR_FCM_SERVER_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(
                        "application/json".toMediaType(),
                        jsonBody
                    ))
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                if (success) {
                    Log.d(TAG, "FCM notification sent successfully")
                } else {
                    Log.e(TAG, "FCM notification failed: ${response.code} - ${response.body?.string()}")
                }
                
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
            false
        }
    }

    private fun getConversationId(userId1: String, userId2: String): String {
        val sortedIds = listOf(userId1, userId2).sorted()
        val conversationId = "conv_${sortedIds[0]}_${sortedIds[1]}"
        Log.d(TAG, "Generated conversation ID: $conversationId from users: $userId1, $userId2")
        return conversationId
    }

    private fun updateConversations(message: ChatMessage, conversationId: String) {
        Log.d(TAG, "updateConversations() called for conversationId: $conversationId")
        val currentConversations = chatPrefs.loadConversations().toMutableList()
        val existingConversationIndex = currentConversations.indexOfFirst { it.conversationId == conversationId }
        val conversation = if (existingConversationIndex != -1) {
            Log.d(TAG, "Updating existing conversation")
            currentConversations[existingConversationIndex].copy(
                lastMessage = message,
                lastActivity = message.timestamp
            )
        } else {
            Log.d(TAG, "Creating new conversation")
            ChatConversation(
                conversationId = conversationId,
                participants = listOf(message.senderId, message.receiverId),
                lastMessage = message,
                lastActivity = message.timestamp
            )
        }
        if (existingConversationIndex != -1) {
            currentConversations[existingConversationIndex] = conversation
        } else {
            currentConversations.add(conversation)
        }
        currentConversations.sortByDescending { it.lastActivity }
        conversations.value = currentConversations
        chatPrefs.saveConversations(currentConversations)
        Log.d(TAG, "Conversations updated, total conversations: ${conversations.value.size}")
    }

    override suspend fun deleteConversation(conversationId: String): Boolean {
        Log.d(TAG, "deleteConversation() called for conversationId: $conversationId")
        return try {
            chatPrefs.deleteConversation(conversationId)
            messages.remove(conversationId)
            conversations.value = chatPrefs.loadConversations()
            Log.d(TAG, "Conversation removed from list and storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
            false
        }
    }

    fun resetConversations() {
        Log.d(TAG, "resetConversations() called")
        conversations.value = emptyList()
        messages.clear()
        chatPrefs.saveConversations(emptyList())
        // Consider if you need to clear all individual message files as well
        Log.d(TAG, "All conversations and messages have been reset.")
    }

    override suspend fun searchMessages(query: String, mode: com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode): List<ChatMessage> {
        Log.d(TAG, "searchMessages() called - query: '$query', mode: $mode")
        return try {
            val allMessages = mutableListOf<ChatMessage>()
            
            // Get all messages from all conversations
            conversations.value.forEach { conversation ->
                val conversationMessages = chatPrefs.loadMessages(conversation.conversationId)
                allMessages.addAll(conversationMessages)
            }
            
            val results = when (mode) {
                com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode.EXACT -> {
                    allMessages.filter { message ->
                        message.content.equals(query, ignoreCase = true)
                    }
                }
                com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode.PARTIAL -> {
                    allMessages.filter { message ->
                        message.content.contains(query, ignoreCase = true)
                    }
                }
            }
            
            Log.d(TAG, "Search found ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching messages", e)
            emptyList()
        }
    }

    override suspend fun sendImageMessage(imageUri: String, receiverId: String): Boolean {
        Log.d(TAG, "sendImageMessage() called - imageUri: $imageUri, receiverId: $receiverId")
        return try {
            val senderId = userRepository.getUserId()
            Log.d(TAG, "Sender ID: $senderId")
            
            val message = ChatMessage(
                messageId = java.util.UUID.randomUUID().toString(),
                senderId = senderId,
                senderName = userRepository.getUserData().first()?.firstName ?: "Unknown",
                receiverId = receiverId,
                content = "[Image]",
                timestamp = System.currentTimeMillis(),
                messageType = com.bluebridgeapp.bluebridge.data.model.MessageType.IMAGE,
                imageUri = imageUri
            )
            
            Log.d(TAG, "Created image message with ID: ${message.messageId}")
            saveMessageLocally(message)
            
            // Send FCM notification for image
            val receiverToken = getDeviceToken(receiverId)
            if (receiverToken != null) {
                val success = sendFCMNotification(
                    toToken = receiverToken,
                    title = message.senderName,
                    body = "Sent an image",
                    data = mapOf(
                        "messageId" to message.messageId,
                        "senderId" to message.senderId,
                        "senderName" to message.senderName,
                        "content" to message.content,
                        "timestamp" to message.timestamp.toString(),
                        "messageType" to message.messageType.name,
                        "imageUri" to imageUri,
                        "type" to "chat_message"
                    )
                )
                Log.d(TAG, "Image message sent successfully: $success")
                success
            } else {
                Log.w(TAG, "Receiver device token not found for user: $receiverId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image message", e)
            false
        }
    }
} 