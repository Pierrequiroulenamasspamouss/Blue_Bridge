package com.bluebridgeapp.bluebridge.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.SendMessageRequest
import com.bluebridgeapp.bluebridge.data.model.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val TAG = "ChatViewModel"
    
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()
    
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()
    
    private val _currentConversationId = mutableStateOf<String?>(null)
    val currentConversationId = _currentConversationId
    
    private val _isLoading = mutableStateOf(false)
    val isLoading = _isLoading
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage = _errorMessage
    
    private val _messageInput = mutableStateOf("")
    val messageInput = _messageInput

    // Search functionality
    private val _searchQuery = mutableStateOf("")
    val searchQuery = _searchQuery
    
    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults.asStateFlow()
    
    private val _isSearching = mutableStateOf(false)
    val isSearching = _isSearching
    
    private val _searchMode = mutableStateOf(SearchMode.PARTIAL)
    val searchMode = _searchMode

    init {
        Log.d(TAG, "ChatViewModel initialized")
        loadConversations()
    }

    enum class SearchMode {
        EXACT, PARTIAL
    }

    fun updateSearchQuery(query: String) {
        Log.d(TAG, "updateSearchQuery() called: '$query'")
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            performSearch(query, _searchMode.value)
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun updateSearchMode(mode: SearchMode) {
        Log.d(TAG, "updateSearchMode() called: $mode")
        _searchMode.value = mode
        if (_searchQuery.value.isNotEmpty()) {
            performSearch(_searchQuery.value, mode)
        }
    }

    fun performSearch(query: String, mode: SearchMode) {
        Log.d(TAG, "performSearch() called - query: '$query', mode: $mode")
        viewModelScope.launch {
            try {
                _isSearching.value = true
                val results = chatRepository.searchMessages(query, mode)
                _searchResults.value = results
                Log.d(TAG, "Search completed with ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Error performing search", e)
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        Log.d(TAG, "clearSearch() called")
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun sendImageMessage(imageUri: String, receiverId: String) {
        Log.d(TAG, "sendImageMessage() called - imageUri: $imageUri, receiverId: $receiverId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting loading to true for image sending")
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.d(TAG, "Calling chatRepository.sendImageMessage()")
                val success = chatRepository.sendImageMessage(imageUri, receiverId)
                
                if (success) {
                    Log.d(TAG, "Image message sent successfully")
                    
                    // Reload messages for current conversation to show the new message
                    val currentConversationId = _currentConversationId.value
                    if (currentConversationId != null) {
                        Log.d(TAG, "Reloading messages for current conversation: $currentConversationId")
                        val messages = chatRepository.getMessages(currentConversationId).first()
                        _currentMessages.value = messages
                        Log.d(TAG, "Updated current messages: ${messages.size}")
                    }
                } else {
                    Log.e(TAG, "Failed to send image message")
                    _errorMessage.value = "Failed to send image message"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending image message", e)
                _errorMessage.value = "Error sending image message: ${e.message}"
            } finally {
                Log.d(TAG, "Setting loading to false after image sending")
                _isLoading.value = false
            }
        }
    }

    fun loadConversations() {
        Log.d(TAG, "loadConversations() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting loading to true")
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.d(TAG, "Calling chatRepository.getConversations()")
                // Always reload from repository to get latest data
                val initialConversations = chatRepository.getConversations().first()
                Log.d(TAG, "Received initial conversations: ${initialConversations.size}")
                _conversations.value = initialConversations
                
                Log.d(TAG, "Setting loading to false")
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                _errorMessage.value = "Failed to load conversations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadMessages(conversationId: String) {
        Log.d(TAG, "loadMessages() called for conversationId: $conversationId")
        viewModelScope.launch {
            try {
                _currentConversationId.value = conversationId
                Log.d(TAG, "Calling chatRepository.getMessages()")
                // Always reload from repository to get latest messages
                val initialMessages = chatRepository.getMessages(conversationId).first()
                Log.d(TAG, "Received initial messages: ${initialMessages.size}")
                _currentMessages.value = initialMessages
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages", e)
                _errorMessage.value = "Failed to load messages: ${e.message}"
            }
        }
    }

    fun sendMessage(content: String, receiverId: String) {
        Log.d(TAG, "sendMessage() called - content: '$content', receiverId: $receiverId")
        if (content.trim().isEmpty()) {
            Log.d(TAG, "Message content is empty, returning")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting loading to true for message sending")
                _isLoading.value = true
                _errorMessage.value = null
                
                val senderId = userRepository.getUserId()
                Log.d(TAG, "Sender ID: $senderId")
                
                val request = SendMessageRequest(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content.trim(),
                    messageType = MessageType.TEXT
                )
                
                Log.d(TAG, "Calling chatRepository.sendMessage()")
                val success = chatRepository.sendMessage(request)
                
                if (success) {
                    Log.d(TAG, "Message sent successfully")
                    _messageInput.value = ""
                    
                    // Reload messages for current conversation to show the new message
                    val currentConversationId = _currentConversationId.value
                    if (currentConversationId != null) {
                        Log.d(TAG, "Reloading messages for current conversation: $currentConversationId")
                        val messages = chatRepository.getMessages(currentConversationId).first()
                        _currentMessages.value = messages
                        Log.d(TAG, "Updated current messages: ${messages.size}")
                    }
                } else {
                    Log.e(TAG, "Failed to send message")
                    _errorMessage.value = "Failed to send message"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _errorMessage.value = "Error sending message: ${e.message}"
            } finally {
                Log.d(TAG, "Setting loading to false after message sending")
                _isLoading.value = false
            }
        }
    }

    fun updateMessageInput(input: String) {
        Log.d(TAG, "updateMessageInput() called: '$input'")
        _messageInput.value = input
    }

    fun clearError() {
        Log.d(TAG, "clearError() called")
        _errorMessage.value = null
    }

    fun markMessageAsRead(messageId: String) {
        Log.d(TAG, "markMessageAsRead() called for messageId: $messageId")
        viewModelScope.launch {
            try {
                val success = chatRepository.markMessageAsRead(messageId)
                Log.d(TAG, "Message marked as read: $success")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking message as read", e)
            }
        }
    }

    suspend fun getConversationWithUser(userId: String): ChatConversation? {
        Log.d(TAG, "getConversationWithUser() called for userId: $userId")
        val currentUserId = userRepository.getUserId()
        val conversation = conversations.value.find { conversation ->
            conversation.participants.contains(userId) && 
            conversation.participants.contains(currentUserId)
        }
        Log.d(TAG, "Found conversation: ${conversation?.conversationId}")
        return conversation
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp
        
        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date) // Same day
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date) // Different day
        }
    }

    suspend fun getCurrentUserId(): String {
        Log.d(TAG, "getCurrentUserId() called")
        return try {
            // Get the actual user ID from the repository
            userRepository.getUserId()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID", e)
            "unknown_user"
        }
    }

    suspend fun getCurrentUserName(): String {
        Log.d(TAG, "getCurrentUserName() called")
        return try {
            val userName = userRepository.getUserData().first()?.firstName ?: "Unknown"
            Log.d(TAG, "Current user name: $userName")
            userName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user name", e)
            "Unknown"
        }
    }

    // Debug functions for testing
    fun addDebugConversation() {
        Log.d(TAG, "addDebugConversation() called")
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            val newConversation = ChatConversation(
                conversationId = "conv_${currentUserId}_user4_${System.currentTimeMillis()}",
                participants = listOf(currentUserId, "user4"),
                lastMessage = ChatMessage(
                    messageId = "debug_msg_${System.currentTimeMillis()}",
                    senderId = "user4",
                    senderName = "Debug User",
                    receiverId = currentUserId,
                    content = "This is a debug conversation",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                ),
                unreadCount = 1,
                lastActivity = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Adding debug conversation: ${newConversation.conversationId}")
            val currentConversations = _conversations.value.toMutableList()
            currentConversations.add(0, newConversation)
            _conversations.value = currentConversations
            Log.d(TAG, "Total conversations after adding debug: ${_conversations.value.size}")
        }
    }

    fun addDebugMessage() {
        Log.d(TAG, "addDebugMessage() called")
        viewModelScope.launch {
            val currentConversationId = _currentConversationId.value
            if (currentConversationId != null) {
                Log.d(TAG, "Adding debug message to conversation: $currentConversationId")
                val currentUserId = getCurrentUserId()
                val debugMessage = ChatMessage(
                    messageId = "debug_msg_${System.currentTimeMillis()}",
                    senderId = "user2",
                    senderName = "Debug Sender",
                    receiverId = currentUserId,
                    content = "This is a debug message sent at ${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                
                // Add to current messages
                val currentMessages = _currentMessages.value.toMutableList()
                currentMessages.add(debugMessage)
                _currentMessages.value = currentMessages
                Log.d(TAG, "Total messages after adding debug: ${_currentMessages.value.size}")
                
                // Update conversation
                val currentConversations = _conversations.value.toMutableList()
                val conversationIndex = currentConversations.indexOfFirst { it.conversationId == currentConversationId }
                if (conversationIndex != -1) {
                    currentConversations[conversationIndex] = currentConversations[conversationIndex].copy(
                        lastMessage = debugMessage,
                        lastActivity = debugMessage.timestamp
                    )
                    _conversations.value = currentConversations
                    Log.d(TAG, "Updated conversation with debug message")
                }
            } else {
                Log.w(TAG, "No current conversation selected for debug message")
            }
        }
    }

    // Debug function to reset conversations to empty
    fun resetConversations() {
        Log.d(TAG, "resetConversations() called")
        viewModelScope.launch {
            try {
                (chatRepository as? com.bluebridgeapp.bluebridge.data.repository.ChatRepositoryImpl)?.resetConversations()
                _conversations.value = emptyList()
                Log.d(TAG, "Conversations reset to empty")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting conversations", e)
            }
        }
    }

    // Function to delete a conversation
    fun deleteConversation(conversationId: String) {
        Log.d(TAG, "deleteConversation() called for conversationId: $conversationId")
        viewModelScope.launch {
            try {
                val success = chatRepository.deleteConversation(conversationId)
                if (success) {
                    Log.d(TAG, "Conversation deleted successfully")
                    // If we're currently viewing this conversation, go back to list
                    if (_currentConversationId.value == conversationId) {
                        _currentConversationId.value = null
                        _currentMessages.value = emptyList()
                    }
                    loadConversations() // Necessary to remove the displaying of the newly deleted conversation
                } else {
                    Log.e(TAG, "Failed to delete conversation")
                    _errorMessage.value = "Failed to delete conversation"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting conversation", e)
                _errorMessage.value = "Error deleting conversation: ${e.message}"
            }
        }
    }
} 