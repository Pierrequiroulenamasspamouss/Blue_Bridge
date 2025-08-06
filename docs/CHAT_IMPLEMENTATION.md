# Chat Implementation for BlueBridge

## Overview

This document describes the chat functionality implemented in BlueBridge using Firebase Cloud Messaging (FCM) for real-time messaging between users.

## Features

- **Real-time messaging** using FCM
- **Local message storage** (no central server storage)
- **Conversation management** with user-friendly UI
- **Message notifications** with proper handling
- **Modern chat UI** with Material Design 3

## Architecture

### Components

1. **ChatMessage.kt** - Data model for chat messages
2. **ChatRepository.kt** - Interface for chat operations
3. **ChatRepositoryImpl.kt** - Implementation of chat repository
4. **ChatViewModel.kt** - ViewModel for chat state management
5. **ChatScreen.kt** - UI components for chat functionality
6. **FirebaseMessagingService.kt** - Enhanced to handle chat messages

### Data Flow

1. User sends message → ChatViewModel → ChatRepository → FCM
2. FCM delivers message → FirebaseMessagingService → Local storage
3. UI updates with new messages

## Usage

### For Users

1. Navigate to the Chat feature from the home screen
2. View conversations or start new ones
3. Send and receive messages in real-time

### For Developers

#### Testing with Python Tool

Use the provided Python tool to test FCM messaging:

```bash
cd Tools
python fcm_messaging_tool.py --server-key "YOUR_FCM_SERVER_KEY" --token "DEVICE_TOKEN" --chat --sender-name "John" --body "Hello from Python tool!"
```

#### Required Setup

1. **FCM Server Key**: Add your FCM server key to the ChatRepositoryImpl
2. **Device Tokens**: Implement proper device token retrieval from your server
3. **User Authentication**: Ensure users are logged in to access chat

## Implementation Details

### Message Structure

```kotlin
data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,
    val messageType: MessageType
)
```

### FCM Payload

Chat messages are sent with specific data payload:

```json
{
  "type": "chat_message",
  "messageId": "unique_id",
  "senderId": "user_id",
  "senderName": "User Name",
  "content": "Message content",
  "timestamp": "1234567890",
  "messageType": "TEXT"
}
```

### Local Storage

Messages are stored locally in memory (not persisted to disk). Future implementations can add:
- Room database for persistent storage
- Message encryption
- Message synchronization

## Security Considerations

1. **Authentication**: Only logged-in users can access chat
2. **Token Validation**: FCM tokens are validated
3. **Message Validation**: Messages are sanitized before sending
4. **Rate Limiting**: Consider implementing rate limiting for message sending

## Future Enhancements

1. **Message Persistence**: Store messages in local database
2. **Message Encryption**: End-to-end encryption
3. **File Sharing**: Support for images and documents
4. **Group Chats**: Multi-user conversations
5. **Message Status**: Read receipts and delivery status
6. **Push Notifications**: Enhanced notification handling

## Troubleshooting

### Common Issues

1. **Messages not sending**: Check FCM server key and device token
2. **Messages not receiving**: Verify Firebase setup and permissions
3. **UI not updating**: Check ChatViewModel state management

### Debug Tools

- Use the Python FCM tool for testing
- Check Firebase console for message delivery
- Monitor app logs for FCM-related errors

## Dependencies

- Firebase Messaging
- Kotlin Coroutines
- Jetpack Compose
- Material Design 3

## Configuration

Update the following in your implementation:

1. **FCM Server Key**: Replace `YOUR_FCM_SERVER_KEY` in ChatRepositoryImpl
2. **Device Token Retrieval**: Implement proper token fetching from your server
3. **User Management**: Ensure proper user authentication flow 