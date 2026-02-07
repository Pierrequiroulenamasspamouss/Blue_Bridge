package com.bluebridgeapp.bluebridge.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.local.ChatPreferences
import com.bluebridgeapp.bluebridge.data.model.*
import com.bluebridgeapp.bluebridge.network.ServerApi
import com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepositoryImpl(
    private val api: ServerApi,
    private val userRepository: UserRepository,
    private val context: Context
) : ChatRepository {

    private val TAG = "ChatRepository"
    private val chatPrefs = ChatPreferences(context)
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    // Cache flows with lazy initialization
    private val _conversations = MutableStateFlow(chatPrefs.loadConversations())
    private val _messagesCache = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    override suspend fun sendMessage(content: MessageContent, receiverId: String): Boolean {
        Log.d(TAG, "sendMessage: content=$content, receiverId=$receiverId")

        return try {
            val senderId = userRepository.getUserId()
            val conversationId = getConversationId(senderId, receiverId)

            // Send via server API
            val request = PostMessageRequest(PostMessageData(senderId, receiverId, content))
            val response = api.sendMessage(request)

            if (response.isSuccessful) {
                // Create and save message locally
                val message = createMessage(senderId, receiverId, content) // Suspend call
                saveMessageLocally(message)
                sendBroadcast(conversationId)
                true
            } else {
                Log.e(TAG, "API failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }

    override suspend fun createConversation(conversationId: String, user1Id: String, user2Id: String) {
        val conversation = ChatConversation(
            conversationId = conversationId,
            participants = listOf(user1Id, user2Id)
        )

        // Add to current conversations
        val currentConvs = chatPrefs.loadConversations().toMutableList()
        if (currentConvs.none { it.conversationId == conversationId }) {
            currentConvs.add(conversation)
            chatPrefs.saveConversations(currentConvs)
            _conversations.value = currentConvs
            Log.d(TAG, "Conversation created: $conversationId")
        }
    }

    override suspend fun getConversations(): Flow<List<ChatConversation>> = _conversations.asStateFlow()

    override suspend fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return getOrCreateMessageFlow(conversationId).asStateFlow()
    }

    override suspend fun markMessageAsRead(messageId: String): Boolean {
        // TODO: Implement mark as read functionality
        return true
    }

    override suspend fun getConversation(participantIds: List<String>): ChatConversation? {
        return _conversations.value.find { conv ->
            conv.participants.toSet() == participantIds.toSet()
        }
    }

    override suspend fun saveMessageLocally(message: ChatMessage) {
        val conversationId = getConversationId(message.senderId, message.receiverId)

        println("💾 REPOSITORY: Saving message to conversation: $conversationId")

        // Ensure conversation exists
        createConversation(conversationId, message.senderId, message.receiverId)

        // Get the message flow
        val messageFlow = getOrCreateMessageFlow(conversationId)

        // CRITICAL: Create a NEW list instance to force recomposition
        val currentMessages = messageFlow.value.toMutableList()
        currentMessages.add(message)
        val updatedMessages = currentMessages.sortedBy { it.timestamp }

        // Update the flow with a NEW instance
        messageFlow.value = ArrayList(updatedMessages) // Force new instance

        // Persist async
        chatPrefs.saveMessages(conversationId, updatedMessages)
        updateConversationLastMessage(conversationId, message)

        println("✅ REPOSITORY: Message saved. New count: ${updatedMessages.size}")
    }

    override suspend fun getLocalMessages(conversationId: String): List<ChatMessage> {
        return chatPrefs.loadMessages(conversationId)
    }

    override suspend fun resetConversations() {
        _conversations.value = emptyList()
        _messagesCache.clear()
        chatPrefs.saveConversations(emptyList())
        Log.d(TAG, "Reset all conversations")
    }

    override suspend fun deleteConversation(conversationId: String): Boolean {
        return try {
            chatPrefs.deleteConversation(conversationId)
            _messagesCache.remove(conversationId)
            _conversations.value = _conversations.value.filter { it.conversationId != conversationId }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
            false
        }
    }

    override fun getConversationId(senderId: String, receiverId: String): String {
        val sortedIds = listOf(senderId, receiverId).sorted()
        return "conv_${sortedIds[0]}_${sortedIds[1]}"
    }

    override suspend fun searchMessages(query: String, mode: ChatViewModel.SearchMode): List<ChatMessage> {
        return _conversations.value
            .flatMap { conv -> getLocalMessages(conv.conversationId) }
            .filter { message ->
                val text = (message.content as? MessageContent.Text)?.text ?: ""
                when (mode) {
                    ChatViewModel.SearchMode.EXACT -> text.equals(query, true)
                    ChatViewModel.SearchMode.PARTIAL -> text.contains(query, true)
                }
            }
    }

    // Private helper methods
    private fun getOrCreateMessageFlow(conversationId: String): MutableStateFlow<List<ChatMessage>> {
        return _messagesCache.getOrPut(conversationId) {
            val messages = chatPrefs.loadMessages(conversationId)
            Log.d(TAG, "Created message flow for $conversationId with ${messages.size} messages")
            MutableStateFlow(messages)
        }
    }

    private suspend fun updateConversations(newConversations: List<ChatConversation>) {
        _conversations.value = newConversations
        chatPrefs.saveConversations(newConversations)
    }

    private suspend fun updateConversationLastMessage(conversationId: String, message: ChatMessage) {
        val conversations = _conversations.value.toMutableList()
        val index = conversations.indexOfFirst { it.conversationId == conversationId }

        if (index != -1) {
            val lastMessageText = when (message.content) {
                is MessageContent.Text -> message.content.text
                is MessageContent.Media -> "Media message"
            }
            // Update conversation if needed
            updateConversations(conversations)
        }
    }

    private suspend fun createMessage(senderId: String, receiverId: String, content: MessageContent): ChatMessage {
        return ChatMessage(
            messageId = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = userRepository.getUserData().first()?.firstName ?: "Unknown", // Suspend call
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            messageType = when (content) {
                is MessageContent.Text -> MessageType.TEXT
                is MessageContent.Media -> MessageType.TEXT //TODO: replace when media available
            }
        )
    }

    private fun sendBroadcast(conversationId: String) {
        val intent = Intent("com.bluebridgeapp.NEW_CHAT_MESSAGE").apply {
            putExtra("conversationId", conversationId)
        }
        context.sendBroadcast(intent)
    }
}