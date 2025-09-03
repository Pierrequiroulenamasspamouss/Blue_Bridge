package com.bluebridgeapp.bluebridge.ui.screens.userscreens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.data.model.MediaType
import com.bluebridgeapp.bluebridge.ui.components.ChatConversationView
import com.bluebridgeapp.bluebridge.ui.components.ChatErrorCard
import com.bluebridgeapp.bluebridge.ui.components.DebugChatInfo
import com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val TAG = "ChatScreen"
    
    Log.d(TAG, "ChatScreen composable called for conversationId: $conversationId")
    
    val conversations by chatViewModel.conversations.collectAsState()
    val currentMessages by chatViewModel.currentMessages.collectAsState()
    val isLoading by chatViewModel.isLoading
    val errorMessage by chatViewModel.errorMessage
    val messageInput by chatViewModel.messageInput
    var currentUserId by remember { mutableStateOf("") }
    var showDebugInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(ChatViewModel.SearchMode.PARTIAL) }
    var showMediaOptions by remember { mutableStateOf(false) }
    val conversation = conversations.find { it.conversationId == conversationId }
    val otherParticipantId = conversation?.participants?.find { it != currentUserId }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch { currentUserId = chatViewModel.getCurrentUserId() }
    }

    // Media picker launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri ->
            Log.d(TAG, "Image selected: $imageUri")
            otherParticipantId?.let { receiverId ->
                chatViewModel.sendImageMessage(imageUri.toString(), receiverId)
            }
        }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { videoUri ->
            Log.d(TAG, "Video selected: $videoUri")
            otherParticipantId?.let { receiverId ->
                // TODO: Implement video sending
                Log.d(TAG, "Video sending not yet implemented")
            }
        }
    }
    
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { audioUri ->
            Log.d(TAG, "Audio selected: $audioUri")
            otherParticipantId?.let { receiverId ->
                // TODO: Implement audio sending
                Log.d(TAG, "Audio sending not yet implemented")
            }
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            Log.d(TAG, "File selected: $fileUri")
            otherParticipantId?.let { receiverId ->
                // TODO: Implement file sending
                Log.d(TAG, "File sending not yet implemented")
            }
        }
    }

    Log.d(TAG, "ChatScreen state - conversations: ${conversations.size}, currentMessages: ${currentMessages.size}, isLoading: $isLoading")

    LaunchedEffect(conversationId) {
        Log.d(TAG, "LaunchedEffect triggered - loading messages for conversation: $conversationId")
        chatViewModel.loadMessages(conversationId)
    }
    
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = otherParticipantId ?: "Chat",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = { 
                    Log.d(TAG, "Back button clicked")
                    navController.popBackStack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Three dots menu for conversation
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search Messages") },
                            onClick = {
                                expanded = false
                                showSearchDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Conversation") },
                            onClick = {
                                expanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        )

        // Error message
        errorMessage?.let { error ->
            Log.d(TAG, "Showing error message: $error")
            ChatErrorCard(
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
                currentMessages = currentMessages,
                currentUserId = currentUserId,
                isLoading = isLoading
            )
        }

        // Chat conversation view
        Log.d(TAG, "Showing individual chat conversation")
        Log.d(TAG, "Conversation found: ${conversation?.conversationId}, otherParticipantId: $otherParticipantId")
        
        ChatConversationView(
            messages = currentMessages,
            messageInput = messageInput,
            onMessageInputChange = { input ->
                Log.d(TAG, "Message input changed: '$input'")
                chatViewModel.updateMessageInput(input)
            },
            onSendMessage = { content ->
                Log.d(TAG, "Send message clicked with content: '$content'")
                otherParticipantId?.let { receiverId ->
                    Log.d(TAG, "Sending message to receiverId: $receiverId")
                    chatViewModel.sendMessage(MessageContent.Text(content), receiverId)
                }
            },
            onSendImage = {
                Log.d(TAG, "Send image clicked")
                imagePickerLauncher.launch("image/*")
            },
            onSendVideo = {
                Log.d(TAG, "Send video clicked")
                videoPickerLauncher.launch("video/*")
            },
            onSendAudio = {
                Log.d(TAG, "Send audio clicked")
                audioPickerLauncher.launch("audio/*")
            },
            onSendFile = {
                Log.d(TAG, "Send file clicked")
                filePickerLauncher.launch("*/*")
            },
            currentUserId = currentUserId,
            isLoading = isLoading,
            conversationTitle = otherParticipantId ?: "Unknown User"
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
                            Log.d(TAG, "Add test message button clicked")
                            chatViewModel.addDebugMessage() 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Add Test Message")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d(TAG, "Confirming delete conversation: $conversationId")
                        chatViewModel.deleteConversation(conversationId)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Search dialog
    if (showSearchDialog) {
        val searchResults by chatViewModel.searchResults.collectAsState()
        val isSearching by chatViewModel.isSearching
        
        AlertDialog(
            onDismissRequest = { 
                showSearchDialog = false
                chatViewModel.clearSearch()
            },
            title = { Text("Search Messages") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            chatViewModel.updateSearchQuery(it)
                        },
                        label = { Text("Search query") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        Text("Search mode:")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { 
                                searchMode = ChatViewModel.SearchMode.PARTIAL
                                chatViewModel.updateSearchMode(ChatViewModel.SearchMode.PARTIAL)
                            }
                        ) {
                            Text("Partial", 
                                color = if (searchMode == ChatViewModel.SearchMode.PARTIAL) 
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = { 
                                searchMode = ChatViewModel.SearchMode.EXACT
                                chatViewModel.updateSearchMode(ChatViewModel.SearchMode.EXACT)
                            }
                        ) {
                            Text("Exact",
                                color = if (searchMode == ChatViewModel.SearchMode.EXACT) 
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (isSearching) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Searching...")
                    }
                    
                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Found ${searchResults.size} results:")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        searchResults.take(5).forEach { message ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                    Log.d(TAG, "Search result clicked: ${message.messageId}")
                                    // Navigate to the specific message in the conversation
                                    showSearchDialog = false
                                    chatViewModel.clearSearch()
                                    // TODO: Implement scroll to specific message
                                    // For now, just close the dialog
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = message.senderName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (message.content) {
                                            is MessageContent.Text -> message.content.text
                                            is MessageContent.Media -> when (message.content.mediaType) {
                                                MediaType.IMAGE -> "📷 Image"
                                                MediaType.VIDEO -> "🎥 Video"
                                                MediaType.AUDIO -> "🎵 Audio"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = formatTimestamp(message.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        if (searchResults.size > 5) {
                            Text("... and ${searchResults.size - 5} more results")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSearchDialog = false
                    chatViewModel.clearSearch()
                }) {
                    Text("Close")
                }
            }
        )
    }
}

