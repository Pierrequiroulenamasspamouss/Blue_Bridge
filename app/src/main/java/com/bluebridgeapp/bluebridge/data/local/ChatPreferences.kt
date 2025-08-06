package com.bluebridgeapp.bluebridge.data.local

import android.content.Context
import android.content.SharedPreferences
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_CONVERSATIONS = "conversations"
        private const val KEY_MESSAGES_PREFIX = "messages_" // + conversationId
    }

    fun saveConversations(conversations: List<ChatConversation>) {
        val jsonString = json.encodeToString(conversations)
        prefs.edit().putString(KEY_CONVERSATIONS, jsonString).apply()
    }

    fun loadConversations(): List<ChatConversation> {
        val jsonString = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMessages(conversationId: String, messages: List<ChatMessage>) {
        val jsonString = json.encodeToString(messages)
        prefs.edit().putString(KEY_MESSAGES_PREFIX + conversationId, jsonString).apply()
    }

    fun loadMessages(conversationId: String): List<ChatMessage> {
        val jsonString = prefs.getString(KEY_MESSAGES_PREFIX + conversationId, null) ?: return emptyList()
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteConversation(conversationId: String) {
        prefs.edit().remove(KEY_MESSAGES_PREFIX + conversationId).apply()
        val conversations = loadConversations().filter { it.conversationId != conversationId }
        saveConversations(conversations)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

