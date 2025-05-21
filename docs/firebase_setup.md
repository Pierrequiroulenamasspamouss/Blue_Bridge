# BlueBridge Firebase Push Notifications Setup Guide

This guide will help you set up push notifications for the BlueBridge app using Firebase Cloud Messaging.

## 1. Generate Firebase Private Key

Push notifications are failing because the Firebase private key is missing. You need to generate and configure it:

1. **Go to Firebase Console**:
   - Visit [Firebase Console](https://console.firebase.google.com/)
   - Select your project: `wellconnect-458200`

2. **Generate a Service Account Key**:
   - Click on the ⚙️ (Settings) icon in the top left sidebar
   - Select "Project settings"
   - Go to the "Service accounts" tab
   - Click on "Generate new private key"
   - Click "Generate key" on the confirmation dialog
   - Download the JSON file to a secure location

3. **Update Your .env File**:
   - Open the downloaded JSON file
   - Extract the `private_key` value
   - In your `.env` file, update the `FIREBASE_PRIVATE_KEY` with this value
   - Replace newlines with `\n` in the key
   - Update the `FIREBASE_CLIENT_EMAIL` value with the one from the JSON file

Example of how your `.env` should look like:

```
# Firebase Configuration
FIREBASE_PROJECT_ID=wellconnect-458200
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBg...[rest of your key]...C5s=\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxx@wellconnect-458200.iam.gserviceaccount.com
```

## 2. Enable Firebase Cloud Messaging API

1. **Ensure Firebase Cloud Messaging API is Enabled**:
   - In Firebase Console > Project Settings > Cloud Messaging
   - Check if "Firebase Cloud Messaging API (V1)" is enabled
   - If not, click on the three dots menu and enable it

## 3. Restart Your Server

After updating the Firebase configuration:

```bash
npm run dev
```

## 4. Test Push Notifications

After restarting your server, you should be able to send actual push notifications to devices.

## Troubleshooting

If you're still experiencing issues:

1. **Check Server Logs**: Look for any errors related to Firebase initialization
2. **Verify Token Registration**: Ensure the device token is correctly registered
3. **Check Device Permissions**: Make sure the app has notification permissions on the device
4. **Verify Firebase Configuration**: Double-check all Firebase values in your .env file

For more help, refer to the [Firebase Cloud Messaging documentation](https://firebase.google.com/docs/cloud-messaging). 