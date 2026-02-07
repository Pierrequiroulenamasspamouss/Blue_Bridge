package com.bluebridgeapp.bluebridge.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bluebridgeapp.bluebridge.MainActivity
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.RepositoryProvider
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.ChatMessage
import com.bluebridgeapp.bluebridge.data.model.MessageContent
import com.bluebridgeapp.bluebridge.data.model.MessageType
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class BluebridgeMessagingService : FirebaseMessagingService() {

    private val TAG = "BlueBridgeFCM"
    private lateinit var userRepository: UserRepository
    private lateinit var chatRepository: com.bluebridgeapp.bluebridge.data.interfaces.ChatRepository

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "FCM Service onCreate() called")
            RepositoryProvider.init(applicationContext)
            userRepository = RepositoryProvider.userRepository
            chatRepository = RepositoryProvider.chatRepository
            Log.d(TAG, "FCM Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing repositories", e)
        }
    }

    companion object {
        private val notificationIdGenerator = AtomicInteger(0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Message Type: ${remoteMessage.messageType}")
        Log.d(TAG, "Collapse Key: ${remoteMessage.collapseKey}")
        Log.d(TAG, "TTL: ${remoteMessage.ttl}")
        Log.d(TAG, "Sent Time: ${remoteMessage.sentTime}")

        // Log all data payload entries
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload received with ${remoteMessage.data.size} entries:")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "  $key: $value")
            }
        } else {
            Log.d(TAG, "No data payload in FCM message")
        }

        // Log notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification payload:")
            Log.d(TAG, "  Title: ${notification.title}")
            Log.d(TAG, "  Body: ${notification.body}")
            Log.d(TAG, "  Icon: ${notification.icon}")
            Log.d(TAG, "  Color: ${notification.color}")
            Log.d(TAG, "  Sound: ${notification.sound}")
            Log.d(TAG, "  Tag: ${notification.tag}")
            Log.d(TAG, "  Click Action: ${notification.clickAction}")
        } ?: run {
            Log.d(TAG, "No notification payload in FCM message")
        }

        // Handle data payload first (it might contain conversation info)
        if (remoteMessage.data.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "Processing data payload...")
                handleDataPayload(remoteMessage.data)
            }
        } else {
            Log.d(TAG, "No data payload to process")
        }

        // Then handle notification payload
        remoteMessage.notification?.let { notification ->
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "Processing notification payload...")
                // If we didn't handle the notification via data payload, show it now
                if (remoteMessage.data.isEmpty() || remoteMessage.data["type"] == null) {
                    Log.d(TAG, "Showing notification from notification payload")
                    val title = notification.title ?: getString(R.string.app_name)
                    val body = notification.body ?: "New notification"
                    sendNotification(title, body, "general")
                } else {
                    Log.d(TAG, "Notification already handled via data payload")
                }
            }
        }
        Log.d(TAG, "FCM message processing completed")
        Log.d(TAG, "═══════════════════════════════════════════════")
    }

    private suspend fun handleDataPayload(data: Map<String, String>) {
        val messageType = data["type"] ?: data["messageType"] ?: "general"

        Log.d(TAG, "Handling data payload with type: $messageType")

        when (messageType) {
            "chat_message", "new_message" -> {
                Log.d(TAG, "Processing as chat message")
                handleChatMessage(data)
            }
            "general", "notification" -> {
                Log.d(TAG, "Processing as general notification")
                handleGeneralNotification(data)
            }
            else -> {
                Log.d(TAG, "Unknown message type '$messageType', processing as general notification")
                handleGeneralNotification(data)
            }
        }
    }

    private suspend fun handleChatMessage(data: Map<String, String>) {
        Log.d(TAG, "Starting chat message processing...")

        // Extract sender and receiver IDs with better error handling
        val senderId = data["senderId"] ?: data["from"] ?: run {
            Log.e(TAG, "Missing senderId in FCM data, cannot process chat message")
            return
        }

        val senderName = data["senderName"] ?: data["fromName"] ?: "Unknown"
        val content = data["content"] ?: data["message"] ?: ""
        val messageId = data["messageId"] ?: UUID.randomUUID().toString()
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        // Get the current user's ID to use as receiverId
        val receiverId = try {
            userRepository.getUserId() ?: run {
                Log.e(TAG, "Cannot get current user ID from repository")
                data["receiverId"] ?: data["to"] ?: "unknown_receiver"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID from repository", e)
            data["receiverId"] ?: data["to"] ?: "unknown_receiver"
        }

        if (receiverId == "unknown_receiver") {
            Log.e(TAG, "Cannot determine receiverId, cannot process chat message")
            sendNotification(senderName, content, "general")
            return
        }

        Log.d(TAG, "Chat message details:")
        Log.d(TAG, "  Sender ID: $senderId")
        Log.d(TAG, "  Sender Name: $senderName")
        Log.d(TAG, "  Receiver ID: $receiverId")

        // Calculate conversation ID properly
        val conversationId = try {
            val convId = chatRepository.getConversationId(senderId, receiverId)
            Log.d(TAG, "Calculated conversation ID: $convId")
            convId
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating conversation ID", e)
            val sortedIds = listOf(senderId, receiverId).sorted()
            val fallbackId = "conv_${sortedIds[0]}_${sortedIds[1]}"
            Log.d(TAG, "Using fallback conversation ID: $fallbackId")
            fallbackId
        }

        try {
            Log.d(TAG, "Saving message locally...")
            val message = ChatMessage(
                messageId = messageId,
                senderId = senderId,
                senderName = senderName,
                receiverId = receiverId,
                content = MessageContent.Text(content),
                timestamp = timestamp,
                messageType = MessageType.TEXT
            )

            // Save message FIRST
            chatRepository.saveMessageLocally(message)
            Log.d(TAG, "Chat message saved locally: $messageId")

            // THEN send events - add a small delay to ensure data is persisted
            kotlinx.coroutines.delay(50L)

            Log.d(TAG, "Sending AppEvent.NewMessageReceived for conversation: $conversationId")
            AppEventChannel.sendEvent(AppEvent.NewMessageReceived(conversationId))

            kotlinx.coroutines.delay(100L)

            Log.d(TAG, "Sending AppEvent.RefreshAllConversations")
            AppEventChannel.sendEvent(AppEvent.RefreshAllConversations)

            Log.d(TAG, "Events sent successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat message or sending events", e)
        }

        // Show notification
        Log.d(TAG, "Preparing to show notification for chat message")
        sendNotification(senderName, content, conversationId)
    }
    private fun handleGeneralNotification(data: Map<String, String>) {
        Log.d(TAG, "Starting general notification processing...")

        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: data["body"] ?: "New update"
        val conversationId = data["conversationId"] ?: "general"

        Log.d(TAG, "General notification details:")
        Log.d(TAG, "  Title: $title")
        Log.d(TAG, "  Message: $message")
        Log.d(TAG, "  Conversation ID: $conversationId")

        sendNotification(title, message, conversationId)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "New FCM token received: ${token.take(10)}...")
        Log.d(TAG, "Full token length: ${token.length} characters")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Saving token locally...")
                userRepository.saveNotificationToken(token)
                Log.d(TAG, "Token saved locally successfully")

                val userId = userRepository.getUserId()
                val authToken = userRepository.getLoginToken()

                Log.d(TAG, "User ID from repository: ${userId?.take(10)}...")
                Log.d(TAG, "Auth token from repository: ${authToken?.take(10)}...")

                if (!userId.isNullOrEmpty() && !authToken.isNullOrEmpty()) {
                    Log.d(TAG, "User is logged in, registering token with server...")
                    val success = userRepository.registerNotificationToken(userId, authToken, token)

                    if (success) {
                        Log.d(TAG, "Token successfully registered with server")
                    } else {
                        Log.e(TAG, "Failed to register token with server")
                    }
                } else {
                    Log.d(TAG, "User not logged in, token will be registered on next login")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling new token", e)
            }
        }
        Log.d(TAG, "═══════════════════════════════════════════════")
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.w(TAG, "FCM messages were deleted on the server")
        Log.d(TAG, "═══════════════════════════════════════════════")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "FCM message sent successfully: $msgId")
        Log.d(TAG, "═══════════════════════════════════════════════")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.e(TAG, "FCM message send error: $msgId", exception)
        Log.d(TAG, "═══════════════════════════════════════════════")
    }

    private fun sendNotification(title: String, messageBody: String, conversationId: String = "general") {
        try {
            Log.d(TAG, "Building notification...")
            Log.d(TAG, "  Title: $title")
            Log.d(TAG, "  Body: $messageBody")
            Log.d(TAG, "  Conversation ID: $conversationId")

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("conversationId", conversationId)
                putExtra("fromNotification", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                conversationId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = getString(R.string.default_notification_channel_id)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            Log.d(TAG, "Notification channel ID: $channelId")

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "Notification manager obtained")

            createNotificationChannel(notificationManager, channelId)

            val notificationId = notificationIdGenerator.incrementAndGet()
            Log.d(TAG, "Notifying with ID: $notificationId")

            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "Notification sent successfully - ID: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Check if channel already exists
                val existingChannel = notificationManager.getNotificationChannel(channelId)
                if (existingChannel == null) {
                    Log.d(TAG, "Creating new notification channel: $channelId")
                    val channel = NotificationChannel(
                        channelId,
                        "BlueBridge Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "BlueBridge notification channel for messages and updates"
                        enableVibration(true)
                        enableLights(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        setShowBadge(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Notification channel created successfully: $channelId")
                } else {
                    Log.d(TAG, "Notification channel already exists: $channelId")
                    Log.d(TAG, "Channel importance: ${existingChannel.importance}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        } else {
            Log.d(TAG, "No need to create channel (API < O)")
        }
    }
}