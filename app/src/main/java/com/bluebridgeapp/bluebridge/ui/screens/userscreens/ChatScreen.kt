package com.bluebridgeapp.bluebridge.ui.screens.userscreens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.ui.components.ChatConversationView
import com.bluebridgeapp.bluebridge.ui.components.ChatErrorCard
import com.bluebridgeapp.bluebridge.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CRITICAL FIX: Use collectAsStateWithLifecycle for proper lifecycle awareness
    val conversations by chatViewModel.conversations.collectAsStateWithLifecycle()
    val currentMessages by chatViewModel.currentMessages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()
    val messageInput by chatViewModel.messageInput.collectAsStateWithLifecycle()
    val searchResults by chatViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by chatViewModel.isSearching.collectAsStateWithLifecycle()

    var currentUserId by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(ChatViewModel.SearchMode.PARTIAL) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Derived state
    val conversation = remember(conversations, conversationId) {
        conversations.find { it.conversationId == conversationId }
    }

    val otherParticipantId = remember(conversation, currentUserId) {
        conversation?.participants?.find { it != currentUserId }
    }

    // DEBUG: Real-time state monitoring
    LaunchedEffect(currentMessages) {
        println("🔄 CHATSCREEN DEBUG: Messages updated - Count: ${currentMessages.size}")
        if (currentMessages.isNotEmpty()) {
            println("📱 Latest message: '${(currentMessages.last().content as? MessageContent.Text)?.text}' from ${currentMessages.last().senderName}")
        }
    }

    LaunchedEffect(conversations) {
        println("👥 CHATSCREEN DEBUG: Conversations updated - Count: ${conversations.size}")
    }

    // CRITICAL FIX: Load current user ID and messages immediately
    LaunchedEffect(Unit) {
        println("🚀 CHATSCREEN: Initializing screen for conversation: $conversationId")
        currentUserId = chatViewModel.getCurrentUserId()
        println("👤 CHATSCREEN: Current user ID: $currentUserId")

        // Load messages immediately
        chatViewModel.loadMessages(conversationId)
        println("📨 CHATSCREEN: Initial messages loaded")
    }

    // CRITICAL FIX: Reload messages when conversationId changes (when navigating between chats)
    LaunchedEffect(conversationId) {
        println("🔄 CHATSCREEN: Conversation changed to: $conversationId")
        chatViewModel.loadMessages(conversationId)
    }

    // Force UI refresh when returning to this screen
    DisposableEffect(conversationId) {
        println("🎯 CHATSCREEN: Screen focused - conversation: $conversationId")

        onDispose {
            println("📴 CHATSCREEN: Screen disposed")
        }
    }

    // Media pickers
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            otherParticipantId?.let { receiverId ->
                // TODO: Implement media sending based on type
            }
        }
    }

    // UI Components
    Column(modifier = Modifier.fillMaxSize()) {
        // DEBUG: Show real-time message count (remove in production)
        if (currentMessages.isNotEmpty()) {
            Text(
                text = "💬 Messages: ${currentMessages.size} | Latest: ${(currentMessages.last().content as? MessageContent.Text)?.text?.take(20)}...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
            )
        }

        ChatTopAppBar(
            otherParticipantId = otherParticipantId,
            onBackClick = { navController.popBackStack() },
            onMenuClick = { menuExpanded = true },
            menuExpanded = menuExpanded,
            onMenuDismiss = { menuExpanded = false },
            onSearchClick = { showSearchDialog = true },
            onDeleteClick = { showDeleteDialog = true }
        )

        errorMessage?.let { error ->
            ChatErrorCard(error = error, onDismiss = { chatViewModel.clearError() })
        }

        // Enhanced ChatConversationView with immediate state updates
        ChatConversationView(
            messages = currentMessages,
            messageInput = messageInput,
            onMessageInputChange = { newText ->
                chatViewModel.updateMessageInput(newText)
            },
            onSendMessage = { content ->
                otherParticipantId?.let { receiverId ->
                    println("📤 CHATSCREEN: Sending message: '$content' to: $receiverId")
                    chatViewModel.sendMessage(MessageContent.Text(content), receiverId)

                    // Clear input immediately for better UX
                    chatViewModel.updateMessageInput("")
                } ?: run {
                    println("❌ CHATSCREEN: Cannot send message - no receiver ID")
                    chatViewModel.setError("Cannot send message: no recipient found")
                }
            },
            currentUserId = currentUserId,
            isLoading = isLoading,
            onRefresh = {
                println("🔄 CHATSCREEN: Manual refresh triggered")
                chatViewModel.loadMessages(conversationId)
            }
        )
    }

    // Dialogs
    DeleteConversationDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            chatViewModel.deleteConversation(conversationId)
            navController.popBackStack()
        }
    )

    SearchDialog(
        showDialog = showSearchDialog,
        searchQuery = searchQuery,
        searchMode = searchMode,
        searchResults = searchResults,
        isSearching = isSearching,
        onQueryChange = {
            searchQuery = it
            chatViewModel.updateSearchQuery(it)
        },
        onModeChange = { mode ->
            searchMode = mode
            chatViewModel.updateSearchMode(mode)
        },
        onDismiss = {
            showSearchDialog = false
            chatViewModel.clearSearch()
        },
        onResultClick = {
            showSearchDialog = false
            chatViewModel.clearSearch()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    otherParticipantId: String?,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onSearchClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = otherParticipantId?.let { "User: ${it.take(8)}..." } ?: "Chat",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onMenuDismiss
                ) {
                    DropdownMenuItem(
                        text = { Text("Search Messages") },
                        onClick = {
                            onMenuDismiss()
                            onSearchClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Conversation") },
                        onClick = {
                            onMenuDismiss()
                            onDeleteClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh") },
                        onClick = {
                            onMenuDismiss()
                            onSearchClick() // Temporary - add proper refresh
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun DeleteConversationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SearchDialog(
    showDialog: Boolean,
    searchQuery: String,
    searchMode: ChatViewModel.SearchMode,
    searchResults: List<com.bluebridgeapp.bluebridge.data.model.ChatMessage>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onModeChange: (ChatViewModel.SearchMode) -> Unit,
    onDismiss: () -> Unit,
    onResultClick: (com.bluebridgeapp.bluebridge.data.model.ChatMessage) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Search Messages") },
            text = {
                SearchDialogContent(
                    searchQuery = searchQuery,
                    searchMode = searchMode,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onQueryChange = onQueryChange,
                    onModeChange = onModeChange,
                    onResultClick = onResultClick
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SearchDialogContent(
    searchQuery: String,
    searchMode: ChatViewModel.SearchMode,
    searchResults: List<com.bluebridgeapp.bluebridge.data.model.ChatMessage>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onModeChange: (ChatViewModel.SearchMode) -> Unit,
    onResultClick: (com.bluebridgeapp.bluebridge.data.model.ChatMessage) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            label = { Text("Search query") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        SearchModeSelector(searchMode, onModeChange)

        if (isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Searching...", style = MaterialTheme.typography.bodySmall)
        }

        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Found ${searchResults.size} results:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            SearchResultsList(
                results = searchResults.take(5),
                onResultClick = onResultClick
            )

            if (searchResults.size > 5) {
                Text("... and ${searchResults.size - 5} more results",
                    style = MaterialTheme.typography.bodySmall)
            }
        } else if (searchQuery.isNotEmpty() && !isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("No results found", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SearchModeSelector(
    currentMode: ChatViewModel.SearchMode,
    onModeChange: (ChatViewModel.SearchMode) -> Unit
) {
    Column {
        Text("Search mode:", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            FilterChip(
                selected = currentMode == ChatViewModel.SearchMode.PARTIAL,
                onClick = { onModeChange(ChatViewModel.SearchMode.PARTIAL) },
                label = { Text("Partial") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = currentMode == ChatViewModel.SearchMode.EXACT,
                onClick = { onModeChange(ChatViewModel.SearchMode.EXACT) },
                label = { Text("Exact") }
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<com.bluebridgeapp.bluebridge.data.model.ChatMessage>,
    onResultClick: (com.bluebridgeapp.bluebridge.data.model.ChatMessage) -> Unit
) {
    Column {
        results.forEach { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onResultClick(message) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (val content = message.content) {
                            is MessageContent.Text -> content.text
                            is MessageContent.Media -> when (content.mediaType) {
                                com.bluebridgeapp.bluebridge.data.model.MediaType.IMAGE -> "📷 Image"
                                com.bluebridgeapp.bluebridge.data.model.MediaType.VIDEO -> "🎥 Video"
                                com.bluebridgeapp.bluebridge.data.model.MediaType.AUDIO -> "🎵 Audio"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val diff = Date().time - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}