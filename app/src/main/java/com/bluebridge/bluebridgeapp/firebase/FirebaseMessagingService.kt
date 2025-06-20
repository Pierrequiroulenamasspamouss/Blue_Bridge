package com.bluebridge.bluebridgeapp.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bluebridge.bluebridgeapp.MainActivity
import com.bluebridge.bluebridgeapp.R
import com.bluebridge.bluebridgeapp.data.`interface`.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluebridgeMessagingService : FirebaseMessagingService() {

    private val TAG = "BlueBridgeFCM"
    private lateinit var userRepository: UserRepository


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "BlueBridge", it.body ?: "New notification")
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "BlueBridge"
            val message = remoteMessage.data["message"] ?: "New update"
            sendNotification(title, message)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed loginToken: $token")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Save the loginToken to preferences
                userRepository.saveNotificationToken(token)
                
                // Check if the user is logged in (has email and auth loginToken)
                val email = userRepository.getUserEmail()
                val authToken = userRepository.getLoginToken()
                
                if (email != null && authToken != null) {
                    // User is logged in, register the loginToken with the server
                    Log.d(TAG, "Sending refreshed loginToken to server for $email")
                    val success = userRepository.registerNotificationToken(email, authToken, token)
                    
                    if (success) {
                        Log.d(TAG, "Successfully registered refreshed loginToken with server")
                    } else {
                        Log.e(TAG, "Failed to register refreshed loginToken with server for $email")
                    }
                } else {
                    // User is not logged in, loginToken will be registered on next login
                    Log.d(TAG, "User not logged in, loginToken will be registered on next login")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving refreshed loginToken: ${e.message}", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since Android O, notification channels are required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BlueBridge Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "BlueBridge notification channel"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
} 