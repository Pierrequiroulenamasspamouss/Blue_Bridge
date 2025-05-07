# Firebase Push Notifications Implementation Guide for WellConnect

This document provides a step-by-step guide on how to set up Firebase Cloud Messaging (FCM) for push notifications in the WellConnect app.

## Prerequisites

1. A Firebase project (create one at [Firebase Console](https://console.firebase.google.com/))
2. Your app already registered in Firebase

## Step 1: Firebase Project Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Download the `google-services.json` file
4. Place the file in the `app/` directory of your project

## Step 2: Add Required Dependencies

These are already added to the project, but for reference:

```gradle
// In project-level build.gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}

// In app-level build.gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.1.1')
    implementation 'com.google.firebase:firebase-messaging'
    implementation 'com.google.firebase:firebase-analytics'
}
```

## Step 3: Replace Your Firebase Project Values

Open the downloaded `google-services.json` file and ensure the following values match your Firebase project:

1. `project_number`: Your Firebase project number
2. `firebase_url`: Your Firebase database URL (if applicable)
3. `project_id`: Your Firebase project ID
4. `storage_bucket`: Your Firebase storage bucket

## Step 4: Implement FirebaseMessagingService

The app already includes `FirebaseMessagingService` implementation, which handles the following:

1. Token generation and refresh
2. Handling incoming messages
3. Creating and displaying notifications

## Step 5: Server Configuration

Ensure your server has the Firebase Admin SDK to send notifications. You'll need:

1. Server key for Firebase Admin SDK (from Firebase Console → Project Settings → Service Accounts → Generate new private key)
2. Update your server with the downloaded key
3. Endpoint to register/unregister device tokens 

## Step 6: Testing

To test your setup:

1. Send a test notification from Firebase Console
2. Test token registration with your server
3. Test receiving notifications in foreground and background modes

## Troubleshooting

Common issues:

1. **Notifications not appearing**: Check that notification permissions are granted
2. **Token not being sent to server**: Check logs for token generation and server communication
3. **Server not sending notifications**: Verify the server key and payload format

## Key Files in WellConnect

Here are the key files for FCM implementation in WellConnect:

1. `FirebaseMessagingService.kt`: Handles incoming push notifications
2. `UserViewModel.kt`: Contains methods for:
   - `registerForNotifications()`: Registers the FCM token with the server
   - `unregisterFromNotifications()`: Unregisters the FCM token
   - `getFirebaseToken()`: Gets the current FCM token
3. `UserRepository.kt`: Interface with methods for token management
4. `UserRepositoryImpl.kt`: Implementation of the repository methods
5. `ServerApi.kt`: Contains the API endpoints for token registration

## Server Endpoints 

The server needs two endpoints:

1. `/api/notifications/register`: Registers a device token for a user
2. `/api/notifications/unregister`: Unregisters a device token

Both endpoints expect a payload like:
```json
{
  "email": "user@example.com",
  "token": "firebase-token-here",
  "authToken": "user-auth-token-here"
}
``` 