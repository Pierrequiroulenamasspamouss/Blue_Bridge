package com.bluebridgeapp.bluebridge.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.AppEventHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(
    val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel(), AppEventHandler.ChatEventListener {

    private val TAG = "ChatViewModel"

    // State flows
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())

    // Mutable states
    private val _currentConversationId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _messageInput = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _searchMode = MutableStateFlow(SearchMode.PARTIAL)

    // Public state flows
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults.asStateFlow()
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    companion object {
        val currentChatPartnerId = MutableStateFlow<String?>(null)
    }

    // In ChatViewModel init
    init {
        Log.d(TAG, "ChatViewModel initialized")
        AppEventChannel.setChatEventListener(this)
        loadConversations()

        // Listen for general app events with proper error handling
        viewModelScope.launch {
            AppEventChannel.events.collect { event ->
                Log.d(TAG, "AppEvent received: ${event::class.simpleName}")
                when (event) {
                    is AppEvent.NewMessageReceived -> {
                        Log.d(TAG, "NewMessageReceived for conversation: ${event.conversationId}")
                        onNewMessageReceived(event.conversationId)
                    }
                    is AppEvent.RefreshAllConversations -> {
                        Log.d(TAG, "RefreshAllConversations event received")
                        refreshAllConversations()
                    }
                    else -> {
                        // Ignore other events
                    }
                }
            }
        }
    }
    // Improved event handlers
    override fun onNewMessageReceived(conversationId: String) {
        Log.d(TAG, "onNewMessageReceived: $conversationId")
        viewModelScope.launch {
            safeExecute {
                // Refresh conversations first
                refreshConversations()

                // If this is the current conversation, refresh messages with a small delay
                if (_currentConversationId.value == conversationId) {
                    kotlinx.coroutines.delay(100L) // Allow repository to update
                    refreshMessages(conversationId)
                    Log.d(TAG, "Messages refreshed for current conversation: $conversationId")
                } else {
                    Log.d(TAG, "Message received for different conversation: $conversationId")
                }
            }
        }
    }

    override fun onConversationUpdated(conversationId: String) {
        Log.d(TAG, "onConversationUpdated: $conversationId")
        viewModelScope.launch {
            refreshConversations()
        }
    }

    override fun onRefreshAllConversations() {
        Log.d(TAG, "onRefreshAllConversations")
        refreshAllConversations()
    }

    private fun refreshAllConversations() {
        viewModelScope.launch {
            safeExecute {
                refreshConversations()
                // Also refresh current messages if we're in a conversation
                _currentConversationId.value?.let { conversationId ->
                    kotlinx.coroutines.delay(50L)
                    refreshMessages(conversationId)
                }
            }
        }
    }

    // Improved load methods with forced refresh
    fun loadConversations() {
        viewModelScope.launch {
            safeExecute("Failed to load conversations") {
                _isLoading.value = true
                // Force refresh by clearing cache first if needed
                refreshConversations()
            }
            _isLoading.value = false
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            safeExecute("Failed to load messages") {
                _currentConversationId.value = conversationId
                currentChatPartnerId.value = getChatPartnerId(conversationId)
                refreshMessages(conversationId)
            }
        }
    }

    override fun onCleared() {
        AppEventChannel.removeChatEventListener()
        Log.d(TAG, "ChatViewModel cleared")
        super.onCleared()
    }

    enum class SearchMode { EXACT, PARTIAL }

    // Search functionality
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) performSearch(query, _searchMode.value)
        else _searchResults.value = emptyList()
    }

    fun updateSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        if (_searchQuery.value.isNotEmpty()) performSearch(_searchQuery.value, mode)
    }

    private fun performSearch(query: String, mode: SearchMode) {
        viewModelScope.launch {
            safeExecute("Search failed") {
                _isSearching.value = true
                _searchResults.value = chatRepository.searchMessages(query, mode)
                Log.d(TAG, "Search found ${_searchResults.value.size} results")
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun sendMessage(content: MessageContent, receiverId: String) {
        viewModelScope.launch {
            safeExecute("Failed to send message") {
                _isLoading.value = true

                val senderId = userRepository.getUserId()
                val conversationId = chatRepository.getConversationId(senderId, receiverId)

                // Ensure conversation exists
                ensureConversationExists(conversationId, senderId, receiverId)

                // Send message
                val success = chatRepository.sendMessage(content, receiverId)

                if (success) {
                    _messageInput.value = ""
                    refreshMessages(conversationId)
                    refreshConversations()
                } else {
                    throw Exception("Message sending failed")
                }
            }
            _isLoading.value = false
        }
    }

    fun sendImageMessage(imageUri: String, receiverId: String) {
        viewModelScope.launch {
            AppEventChannel.sendEvent(AppEvent.ShowError("Image sending not implemented yet."))
        }
    }

    // Utility functions
    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }
    suspend fun getConversationId(currentUserId: String, userId: String): String {
        return chatRepository.getConversationId(currentUserId, userId)
    }

    // Add this method to set error messages
    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            safeExecute { chatRepository.markMessageAsRead(messageId) }
        }
    }

    suspend fun getConversationWithUser(userId: String): ChatConversation? {
        val currentUserId = userRepository.getUserId()
        return _conversations.value.find { conv ->
            conv.participants.contains(userId) && conv.participants.contains(currentUserId)
        }
    }

    suspend fun getCurrentUserId(): String = userRepository.getUserId()

    fun resetConversations() {
        viewModelScope.launch {
            safeExecute {
                chatRepository.resetConversations()
                _conversations.value = emptyList()
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            safeExecute("Failed to delete conversation") {
                if (chatRepository.deleteConversation(conversationId)) {
                    if (_currentConversationId.value == conversationId) {
                        _currentConversationId.value = null
                        _currentMessages.value = emptyList()
                    }
                    loadConversations()
                } else {
                    throw Exception("Delete operation failed")
                }
            }
        }
    }

    // Private helper methods
    private suspend fun ensureConversationExists(conversationId: String, user1Id: String, user2Id: String) {
        // Check both repository and local state
        val repositoryConversation = chatRepository.getConversation(listOf(user1Id, user2Id))
        val stateConversation = _conversations.value.find { it.conversationId == conversationId }

        if (repositoryConversation == null && stateConversation == null) {
            Log.d(TAG, "Creating conversation: $conversationId")
            chatRepository.createConversation(conversationId, user1Id, user2Id)
            // Force immediate reload
            refreshConversations()
        }
        _currentConversationId.value = conversationId
    }
    suspend fun createNewConversation(otherUserId: String): String? {
        return try {
            val currentUserId = userRepository.getUserId()
            val conversationId = chatRepository.getConversationId(currentUserId, otherUserId)

            // Create the conversation in the repository
            chatRepository.createConversation(conversationId, currentUserId, otherUserId)

            // Force reload conversations immediately
            refreshConversations()

            conversationId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation", e)
            null
        }
    }
    private suspend fun getChatPartnerId(conversationId: String): String? {
        val currentUserId = userRepository.getUserId()
        return _conversations.value
            .find { it.conversationId == conversationId }
            ?.participants?.find { it != currentUserId }
    }

    private suspend fun refreshConversations() {
        _conversations.value = chatRepository.getConversations().first()
    }

    private suspend fun refreshMessages(conversationId: String) {
        _currentMessages.value = chatRepository.getMessages(conversationId).first()
    }

    private suspend fun refreshCurrentMessages() {
        _currentConversationId.value?.let { refreshMessages(it) }
    }

    private suspend fun <T> safeExecute(errorMessage: String? = null, block: suspend () -> T) {
        try {
            _errorMessage.value = null
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            _errorMessage.value = errorMessage ?: "Error: ${e.message}"
        }
    }
}