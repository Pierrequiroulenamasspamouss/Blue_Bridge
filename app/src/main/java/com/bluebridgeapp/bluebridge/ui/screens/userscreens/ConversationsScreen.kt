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
    val isLoading by chatViewModel.isLoading
    val errorMessage by chatViewModel.errorMessage
    var currentUserId by remember { mutableStateOf("") }
    var showDebugInfo by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { currentUserId = chatViewModel.getCurrentUserId() }
    }

    Log.d(TAG, "ConversationsScreen state - conversations: ${conversations.size}, isLoading: $isLoading")

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect triggered - calling loadConversations()")
        chatViewModel.loadConversations()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            actions = {
                IconButton(onClick = { 
                    Log.d(TAG, "Add new chat button clicked")
                    chatViewModel.addDebugConversation()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        )

        // Error message
        errorMessage?.let { error ->
            Log.d(TAG, "Showing error message: $error")
            com.bluebridgeapp.bluebridge.ui.components.ChatErrorCard(
                error = error,
                onDismiss = { 
                    Log.d(TAG, "Error dismissed")
                    chatViewModel.clearError() 
                }
            )
        }

        // Debug info (toggleable)
        if (showDebugInfo) {
            Log.d(TAG, "Showing debug info")
            DebugChatInfo(
                conversations = conversations,
                currentMessages = emptyList(),
                currentUserId = currentUserId,
                isLoading = isLoading
            )
        }

        // Conversations list
        Log.d(TAG, "Showing conversations list")
        ConversationsListView(
            conversations = conversations,
            onConversationClick = { conversation ->
                Log.d(TAG, "Conversation clicked: ${conversation.conversationId}")
                navController.navigate("chat_screen/${conversation.conversationId}")
            },
            currentUserId = currentUserId,
            isLoading = isLoading,
            onAddNewChat = {
                Log.d(TAG, "Add new chat clicked")
                chatViewModel.addDebugConversation()
            }
        )

        // Debug buttons - More prominent
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Debug Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { 
                            Log.d(TAG, "Debug toggle button clicked")
                            showDebugInfo = !showDebugInfo 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (showDebugInfo) "Hide Debug" else "Show Debug")
                    }
                    
                    Button(
                        onClick = { 
                            Log.d(TAG, "Add debug conversation button clicked")
                            chatViewModel.addDebugConversation() 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Add Test Chat")
                    }
                    
                    Button(
                        onClick = { 
                            Log.d(TAG, "Reset conversations button clicked")
                            chatViewModel.resetConversations() 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset All")
                    }
                }
            }
        }
    }
}

