package com.bluebridgeapp.bluebridge.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.data.model.ChatConversation
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.data.model.MediaType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Log.d("ChatComponents", "ChatErrorCard composable called with error: $error")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun ConversationsListView(
    conversations: List<ChatConversation>,
    onConversationClick: (ChatConversation) -> Unit,
    currentUserId: String,
    isLoading: Boolean,
    onAddNewChat: () -> Unit
) {
    Log.d("ChatComponents", "ConversationsListView composable called - conversations: ${conversations.size}, isLoading: $isLoading")
    
    if (isLoading) {
        Log.d("ChatComponents", "Showing loading state")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading conversations...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (conversations.isEmpty()) {
        Log.d("ChatComponents", "Showing empty state - no conversations")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No conversations yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Start chatting with other users",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddNewChat) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start New Chat")
                }
            }
        }
    } else {
        Log.d("ChatComponents", "Showing conversations list with ${conversations.size} conversations")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(conversations) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    currentUserId = currentUserId,
                    onClick = { onConversationClick(conversation) }
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ChatConversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    Log.d("ChatComponents", "ConversationItem composable called for conversation: ${conversation.conversationId}")
    
    val otherParticipantId = conversation.participants.find { it != currentUserId }
    val lastMessage = conversation.lastMessage
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { 
                Log.d("ChatComponents", "Conversation item clicked: ${conversation.conversationId}")
                onClick() 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = otherParticipantId?.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = otherParticipantId ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                lastMessage?.let { message ->
                    Text(
                        text = formatMessagePreview(message.content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Time and unread count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                lastMessage?.let { message ->
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatConversationView(
    messages: List<ChatMessage>,
    messageInput: String,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onSendAudio: () -> Unit,
    onSendFile: () -> Unit,
    currentUserId: String,
    isLoading: Boolean,
    conversationTitle: String = "Chat"
) {
    //Log.d("ChatComponents", "ChatConversationView composable called - messages: ${messages.size}, isLoading: $isLoading, conversationTitle: $conversationTitle")
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            reverseLayout = true
        ) {
            items(groupMessagesByTime(messages).reversed()) { messageGroup ->
                MessageGroup(
                    messages = messageGroup,
                    isFromCurrentUser = messageGroup.first().senderId == currentUserId,
                    onMessageAction = { action, message ->
                        when (action) {
                            MessageAction.EDIT -> {
                                // TODO: Implement edit functionality
                                Log.d("ChatComponents", "Edit message: ${message.messageId}")
                            }
                            MessageAction.DELETE -> {
                                // TODO: Implement delete functionality
                                Log.d("ChatComponents", "Delete message: ${message.messageId}")
                            }
                            MessageAction.COPY -> {
                                // TODO: Implement copy functionality
                                Log.d("ChatComponents", "Copy message: ${message.content}")
                            }
                            MessageAction.REPLY -> {
                                // TODO: Implement reply functionality
                                Log.d("ChatComponents", "Reply to message: ${message.messageId}")
                            }
                        }
                    }
                )
            }
        }
        
        // Input area
        MessageInputArea(
            messageInput = messageInput,
            onMessageInputChange = onMessageInputChange,
            onSendMessage = { content ->
                Log.d("ChatComponents", "Send message clicked with content: '$content'")
                onSendMessage(content)
                focusManager.clearFocus()
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onSendImage = onSendImage,
            onSendVideo = onSendVideo,
            onSendAudio = onSendAudio,
            onSendFile = onSendFile,
            isLoading = isLoading
        )
    }
}

enum class MessageAction {
    EDIT, DELETE, COPY, REPLY
}

@Composable
fun MessageGroup(
    messages: List<ChatMessage>,
    isFromCurrentUser: Boolean,
    onMessageAction: (MessageAction, ChatMessage) -> Unit
) {
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        messages.forEachIndexed { index, message ->
            val showTimestamp = index == 0 // Only show timestamp for first message in group
            
            var showMessageMenu by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.widthIn(max = 280.dp).then(
                    if (isFromCurrentUser) {
                        Modifier.clickable { showMessageMenu = true }
                    } else {
                        Modifier
                    }
                ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Display message content based on type
                    when (message.content) {
                        is MessageContent.Text -> {
                            Text(
                                text = formatMessageContent(message.content.text),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is MessageContent.Media -> {
                            MediaMessageContent(
                                mediaContent = message.content,
                                textColor = textColor
                            )
                        }
                    }
                    
                    if (showTimestamp) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Message actions menu (only for current user's messages)
            if (isFromCurrentUser && showMessageMenu) {
                AlertDialog(
                    onDismissRequest = { showMessageMenu = false },
                    title = { Text("Message Actions") },
                    text = { Text("What would you like to do with this message?") },
                    confirmButton = {
                        Column {
                            TextButton(
                                onClick = { 
                                    onMessageAction(MessageAction.EDIT, message)
                                    showMessageMenu = false
                                }
                            ) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { 
                                    onMessageAction(MessageAction.DELETE, message)
                                    showMessageMenu = false
                                }
                            ) {
                                Text("Delete")
                            }
                            TextButton(
                                onClick = { 
                                    onMessageAction(MessageAction.COPY, message)
                                    showMessageMenu = false
                                }
                            ) {
                                Text("Copy")
                            }
                            TextButton(
                                onClick = { 
                                    onMessageAction(MessageAction.REPLY, message)
                                    showMessageMenu = false
                                }
                            ) {
                                Text("Reply")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMessageMenu = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun MediaMessageContent(
    mediaContent: MessageContent.Media,
    textColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.widthIn(max = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon based on media type
            val icon = when (mediaContent.mediaType) {
                MediaType.IMAGE -> Icons.Default.Image
                MediaType.VIDEO -> Icons.Default.VideoFile
                MediaType.AUDIO -> Icons.Default.AudioFile
            }

            Icon(
                icon,
                contentDescription = mediaContent.mediaType.name.lowercase().capitalize(),
                modifier = Modifier.size(48.dp),
                tint = textColor
            )
            
            Text(
                text = when (mediaContent.mediaType) {
                    MediaType.IMAGE -> "Image"
                    MediaType.VIDEO -> "Video"
                    MediaType.AUDIO -> "Audio"
                },
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            
            // Show file name if available
            mediaContent.base64.substringAfterLast("/").takeIf { it.isNotEmpty() }?.let { fileName ->
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun groupMessagesByTime(messages: List<ChatMessage>): List<List<ChatMessage>> {
    if (messages.isEmpty()) return emptyList()
    
    val groups = mutableListOf<List<ChatMessage>>()
    var currentGroup = mutableListOf<ChatMessage>()
    
    messages.forEach { message ->
        if (currentGroup.isEmpty()) {
            currentGroup.add(message)
        } else {
            val lastMessage = currentGroup.last()
            val timeDiff = message.timestamp - lastMessage.timestamp
            
            // Group messages if they're from the same sender and within 1 minute
            if (message.senderId == lastMessage.senderId && timeDiff <= 60000) {
                currentGroup.add(message)
            } else {
                groups.add(currentGroup.toList())
                currentGroup = mutableListOf(message)
            }
        }
    }
    
    if (currentGroup.isNotEmpty()) {
        groups.add(currentGroup.toList())
    }
    
    return groups
}

private fun formatMessagePreview(content: MessageContent): String {
    return when (content) {
        is MessageContent.Text -> content.text
        is MessageContent.Media -> {
            when (content.mediaType) {
                MediaType.IMAGE -> "📷 Image"
                MediaType.VIDEO -> "🎥 Video"
                MediaType.AUDIO -> "🎵 Audio"
            }
        }
    }
}

private fun formatMessageContent(text: String): String {
    // Enhanced text formatting with commands
    return text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "**$1**") // Bold: **text**
        .replace(Regex("--(.*?)--"), "~~$1~~") // Strikethrough: --text--
        .replace(Regex(">(.*?)$"), "> $1") // Quote: >text
        .replace(Regex("__(.*?)__"), "<u>$1</u>") // Underline: __text__
        .replace(Regex("\\|\\|(.*?)\\|\\|"), "<code>$1</code>") // Code: ||text||
        .replace(Regex("\\^\\^(.*?)\\^\\^"), "<sup>$1</sup>") // Superscript: ^^text^^
        .replace(Regex("__(.*?)__"), "<sub>$1</sub>") // Subscript: __text__
}

@Composable
fun MessageItem(
    message: ChatMessage,
    isFromCurrentUser: Boolean
) {
    Log.d("ChatComponents", "MessageItem composable called - messageId: ${message.messageId}, isFromCurrentUser: $isFromCurrentUser")
    
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                when (message.content) {
                    is MessageContent.Text -> {
                        Text(
                            text = formatMessageContent(message.content.text),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is MessageContent.Media -> {
                        MediaMessageContent(
                            mediaContent = message.content,
                            textColor = textColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MessageInputArea(
    messageInput: String,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onSendAudio: () -> Unit,
    onSendFile: () -> Unit,
    isLoading: Boolean
) {
    Log.d("ChatComponents", "MessageInputArea composable called - messageInput: '$messageInput', isLoading: $isLoading")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Message input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { input ->
                        Log.d("ChatComponents", "Message input changed: '$input'")
                        onMessageInputChange(input)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { 
                            if (messageInput.trim().isNotEmpty()) {
                                Log.d("ChatComponents", "Send action triggered from keyboard")
                                onSendMessage(messageInput)
                            }
                        }
                    ),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Media buttons row
                Column {
                    // Image button
                    IconButton(
                        onClick = { 
                            Log.d("ChatComponents", "Image button clicked")
                            onSendImage()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Send Image")
                    }
                    
                    // Video button
                    IconButton(
                        onClick = { 
                            Log.d("ChatComponents", "Video button clicked")
                            onSendVideo()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = "Send Video")
                    }
                    
                    // Audio button
                    IconButton(
                        onClick = { 
                            Log.d("ChatComponents", "Audio button clicked")
                            onSendAudio()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = "Send Audio")
                    }
                    
                    // File button
                    IconButton(
                        onClick = { 
                            Log.d("ChatComponents", "File button clicked")
                            onSendFile()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Send File")
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                FloatingActionButton(
                    onClick = { 
                        if (messageInput.trim().isNotEmpty()) {
                            Log.d("ChatComponents", "Send FAB clicked")
                            onSendMessage(messageInput)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    //enabled = messageInput.trim().isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
            
            // Keyboard shortcuts hint
            Text(
                text = "Press Enter to send, Shift+Enter for new line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DebugChatInfo(
    conversations: List<ChatConversation>,
    currentMessages: List<ChatMessage>,
    currentUserId: String,
    isLoading: Boolean
) {
    Log.d("ChatComponents", "DebugChatInfo composable called")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Debug Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Current User ID: $currentUserId")
            Text("Loading: $isLoading")
            Text("Conversations: ${conversations.size}")
            Text("Current Messages: ${currentMessages.size}")
            conversations.forEach { conv ->
                Text("  - ${conv.conversationId}: ${conv.participants}")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
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