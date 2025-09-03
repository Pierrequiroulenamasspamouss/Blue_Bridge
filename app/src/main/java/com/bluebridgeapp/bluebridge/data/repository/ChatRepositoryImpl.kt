package com.bluebridgeapp.bluebridge.data.repository

import android.content.Context
import android.util.Log
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.data.model.MediaType
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.network.ServerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.bluebridgeapp.bluebridge.data.local.ChatPreferences
import com.bluebridgeapp.bluebridge.data.model.MessageType
import com.bluebridgeapp.bluebridge.data.model.PostMessageData
import com.bluebridgeapp.bluebridge.data.model.PostMessageRequest
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.utils.ImageUtils
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayInputStream

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

    override suspend fun sendMessage(content: MessageContent, receiverId: String): Boolean {
        Log.d(TAG, "sendMessage() called - content: $content, receiverId: $receiverId")
        
        return try {
            val senderId = userRepository.getUserId()
            Log.d(TAG, "Sender ID: $senderId")
            
            // Envoyer via l'API serveur
            val requestBody = PostMessageRequest(PostMessageData(
                senderId = senderId,
                receiverId = receiverId,
                content = content
            ))

            val response = api.sendMessage(requestBody)
            val success = response.isSuccessful
            
            if (success) {
                Log.d(TAG, "Message sent successfully via server API")
                
                // Créer le message pour le stockage local
                val message = ChatMessage(
                    senderId = senderId,
                    senderName = userRepository.getUserData().first()?.firstName ?: "Unknown",
                    receiverId = receiverId,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    messageType = when (content) {
                        is MessageContent.Text -> MessageType.TEXT
                        is MessageContent.Media -> TODO()
                    }
                )

                saveMessageLocally(message)
            } else {
                Log.e(TAG, "Failed to send message via server API: ${response.code()}")
            }
            
            Log.d(TAG, "Message sent successfully: $success")
            success
            
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
            // TODO: Implement mark as read functionality
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
        //TODO
        //updateConversations(message, conversationId)
    }

    override suspend fun getLocalMessages(conversationId: String): List<ChatMessage> {
        Log.d(TAG, "getLocalMessages() called for conversationId: $conversationId")
        val msgs = chatPrefs.loadMessages(conversationId)
        Log.d(TAG, "Returning ${msgs.size} local messages")
        return msgs
    }

    override suspend fun resetConversations() {
        Log.d(TAG, "resetConversations() called")
        conversations.value = emptyList()
        messages.clear()
        chatPrefs.saveConversations(emptyList())
        // Consider if you need to clear all individual message files as well
        Log.d(TAG, "All conversations and messages have been reset.")
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
    fun getConversationId(senderId: String, receiverId: String): String {
        // TODO: Implement proper conversation ID generation
        val sortedIds = listOf(senderId, receiverId).sorted()
        return "conv_${sortedIds[0]}_${sortedIds[1]}"
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
                        (message.content as? MessageContent.Text)?.text?.equals(query, ignoreCase = true) ?: false
                    }
                }
                com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel.SearchMode.PARTIAL -> {
                    allMessages.filter { message ->
                        (message.content as? MessageContent.Text)?.text?.contains(query, ignoreCase = true) ?: false
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



} 