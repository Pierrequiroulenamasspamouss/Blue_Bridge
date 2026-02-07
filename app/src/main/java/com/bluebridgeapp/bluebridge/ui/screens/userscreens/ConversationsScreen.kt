package com.bluebridgeapp.bluebridge.ui.screens.userscreens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.ui.components.ConversationsListView
import com.bluebridgeapp.bluebridge.ui.components.DebugChatInfo
import com.bluebridgeapp.bluebridge.ui.components.NewChatScreen
import com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navController: NavController,
    chatViewModel: ChatViewModel
) {
    val TAG = "ConversationsScreen"

    Log.d(TAG, "ConversationsScreen composable called")

    val conversations by chatViewModel.conversations.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()
    var currentUserId by remember { mutableStateOf("") }
    var showDebugInfo by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { currentUserId = chatViewModel.getCurrentUserId() }
    }

    Log.d(TAG, "ConversationsScreen state - conversations: ${conversations.size}, isLoading: $isLoading")

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect triggered - calling loadConversations()")
        chatViewModel.loadConversations()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Conversations", style = MaterialTheme.typography.headlineSmall) },
            actions = {
                IconButton(onClick = { showNewChatDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        )

        errorMessage?.let { error ->
            com.bluebridgeapp.bluebridge.ui.components.ChatErrorCard(
                error = error,
                onDismiss = { chatViewModel.clearError() }
            )
        }

        if (showDebugInfo) {
            DebugChatInfo(
                conversations = conversations,
                currentMessages = emptyList(),
                currentUserId = currentUserId,
                isLoading = isLoading
            )
        }

        ConversationsListView(
            conversations = conversations,
            onConversationClick = { conversation ->
                navController.navigate("chat_screen/${conversation.conversationId}")
            },
            currentUserId = currentUserId,
            isLoading = isLoading,
            onAddNewChat = { showNewChatDialog = true }
        )

        // Debug buttons
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Debug Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { showDebugInfo = !showDebugInfo }) {
                        Text(if (showDebugInfo) "Hide Debug" else "Show Debug")
                    }
                    Button(onClick = { chatViewModel.resetConversations() }) {
                        Text("Reset All")
                    }
                }
            }
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        NewChatScreen(
            onStartChat = { userId ->
                Log.d(TAG, "Starting new chat with user: $userId")
                scope.launch {
                    try {
                        // Check if conversation already exists
                        val existingConversation = chatViewModel.getConversationWithUser(userId)
                        if (existingConversation != null) {
                            Log.d(TAG, "Conversation already exists: ${existingConversation.conversationId}")
                            navController.navigate("chat_screen/${existingConversation.conversationId}")
                        } else {
                            // Create the conversation first
                            val currentUserId = chatViewModel.getCurrentUserId()
                            val conversationId = chatViewModel.createNewConversation(userId)

                            if (conversationId != null) {
                                Log.d(TAG, "New conversation created: $conversationId")

                                // Small delay to ensure state is updated
                                kotlinx.coroutines.delay(100L)

                                // Force refresh conversations to include the new one
                                chatViewModel.loadConversations()

                                navController.navigate("chat_screen/$conversationId")
                            } else {
                                Log.e(TAG, "Failed to create conversation")
                                chatViewModel.setError("Failed to create conversation")
                            }
                        }
                        showNewChatDialog = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating new chat", e)
                        chatViewModel.setError("Error creating chat: ${e.message}")
                    }
                }
            },
            onCancel = { showNewChatDialog = false }
        )
    }
}