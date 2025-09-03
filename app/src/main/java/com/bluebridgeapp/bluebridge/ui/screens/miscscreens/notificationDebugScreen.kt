package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationDebugScreen(
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var receiverId by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf<String?>(null) }
    var allMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentUserId by remember { mutableStateOf("") }

    // Charger l'utilisateur courant et tous les messages locaux
    LaunchedEffect(Unit) {
        currentUserId = userRepository.getUserId()
        // On charge tous les messages de toutes les conversations
        val convs = chatRepository.getConversations().firstOrNull<List<com.bluebridgeapp.bluebridge.data.model.ChatConversation>>() ?: emptyList()
        val msgs = convs.flatMap<com.bluebridgeapp.bluebridge.data.model.ChatConversation, ChatMessage> { conv -> chatRepository.getLocalMessages(conv.conversationId) }
        allMessages = msgs.sortedByDescending<ChatMessage, Long> { msg -> msg.timestamp }
    }

    // Rafraîchir la liste après envoi
    fun refreshMessages() {
        scope.launch {
            val convs = chatRepository.getConversations().firstOrNull<List<com.bluebridgeapp.bluebridge.data.model.ChatConversation>>() ?: emptyList()
            val msgs = convs.flatMap<com.bluebridgeapp.bluebridge.data.model.ChatConversation, ChatMessage> { conv -> chatRepository.getLocalMessages(conv.conversationId) }
            allMessages = msgs.sortedByDescending<ChatMessage, Long> { msg -> msg.timestamp }
        }
    }

    fun sendTextMessage() {
        if (receiverId.isBlank() || messageText.isBlank()) {
            sendResult = "Please enter receiver ID and message"
            return
        }
        scope.launch {
            isSending = true
            sendResult = null
            val success = chatRepository.sendMessage(MessageContent.Text(messageText), receiverId)
            sendResult = if (success) "Message sent!" else "Failed to send message"
            isSending = false
            messageText = ""
            refreshMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FCM Message Debug",
                fontSize = 20.sp
            )
            Button(onClick = onNavigateBack) {
                Text("Back")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Send Text Message", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = receiverId,
                    onValueChange = { receiverId = it },
                    label = { Text("Receiver ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { sendTextMessage() },
                    enabled = !isSending && receiverId.isNotBlank() && messageText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSending) "Sending..." else "Send Message")
                }
                sendResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(result, color = if (result.contains("sent")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("All Messages (in/out):", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(allMessages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("From: ${msg.senderId}  To: ${msg.receiverId}", fontSize = 12.sp)
                        Text("${formatTimestamp(msg.timestamp)}", fontSize = 10.sp)
                        when (val content = msg.content) {
                            is MessageContent.Text -> Text(content.text)
                            else -> Text("[Non-text message]", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Button(onClick = { refreshMessages() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Messages")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
}