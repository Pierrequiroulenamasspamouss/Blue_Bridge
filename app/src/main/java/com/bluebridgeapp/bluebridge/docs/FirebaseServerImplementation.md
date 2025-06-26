# Firebase Cloud Messaging Server Implementation Guide

This document provides specific implementation instructions for setting up the server-side components needed to support Firebase Cloud Messaging (FCM) in the BlueBridge app.

## Firebase Project Configuration

The BlueBridge app is configured with the following Firebase project:

- **Project ID:** wellconnect-458200
- **Project Number:** 110535556138
- **Applications:**
  - com.bluebridgeapp.bluebridge
  - bluebridge.wellmonitoring

## Required Keys and Certificates

For proper FCM implementation, use the following credentials:

- **Web Push Certificate Key:** `BJ9Mf0mMKZO8pnRYwL-`
- **Server Key (for authentication):** `jdWcCM2iqQUmzsgXJnIT5yjrrfrgF_2rmjkM9gUv1QbWSlLcG2cyTA93qIHmSyvx2c6o`

## Server Implementation Steps

### 1. Firebase Admin SDK Setup

Install the Firebase Admin SDK in your server environment:

```bash
# For Node.js
npm install firebase-admin

# For Python
pip install firebase-admin
```

### 2. Initialize Firebase Admin

```javascript
// Node.js example
const admin = require('firebase-admin');

// Initialize with your service account credentials
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: 'wellconnect-458200',
    clientEmail: 'firebase-adminsdk-xxxxx@wellconnect-458200.iam.gserviceaccount.com',
    privateKey: '-----BEGIN PRIVATE KEY-----\nYour private key here\n-----END PRIVATE KEY-----\n'
  }),
  databaseURL: 'https://wellconnect-458200.firebaseio.com'
});
```

```python
# Python example
import firebase_admin
from firebase_admin import credentials

cred = credentials.Certificate('path/to/serviceAccountKey.json')
firebase_admin.initialize_app(cred)
```

### 3. Implement Token Storage

Create a database table/collection to store user FCM tokens:

```sql
CREATE TABLE user_notification_tokens (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  token VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(email, token)
);
```

### 4. Create API Endpoints

Implement the `/api/notifications/register` and `/api/notifications/unregister` endpoints:

```javascript
// Node.js with Express
app.post('/api/notifications/register', authenticateUser, async (req, res) => {
  const { email, token } = req.body;
  
  try {
    // Store token in database
    await db.query(
      'INSERT INTO user_notification_tokens(email, token) VALUES($1, $2) ON CONFLICT (email, token) DO NOTHING',
      [email, token]
    );
    
    res.json({ success: true, message: 'Token registered successfully' });
  } catch (error) {
    console.error('Error registering token:', error);
    res.status(500).json({ success: false, message: 'Failed to register token' });
  }
});

app.post('/api/notifications/unregister', authenticateUser, async (req, res) => {
  const { email, token } = req.body;
  
  try {
    // Remove token from database
    await db.query(
      'DELETE FROM user_notification_tokens WHERE email = $1 AND token = $2',
      [email, token]
    );
    
    res.json({ success: true, message: 'Token unregistered successfully' });
  } catch (error) {
    console.error('Error unregistering token:', error);
    res.status(500).json({ success: false, message: 'Failed to unregister token' });
  }
});
```

### 5. Implement Notification Sending

Create a function to send notifications to users:

```javascript
// Send notification to a specific user
async function sendNotificationToUser(email, title, body, data = {}) {
  try {
    // Get user's tokens from database
    const result = await db.query(
      'SELECT token FROM user_notification_tokens WHERE email = $1',
      [email]
    );
    
    const tokens = result.rows.map(row => row.token);
    if (tokens.length === 0) {
      console.log(`No tokens found for user: ${email}`);
      return;
    }
    
    // Notification payload
    const message = {
      notification: {
        title,
        body,
      },
      data,
      tokens,
    };
    
    // Send notification
    const response = await admin.messaging().sendMulticast(message);
    console.log(`${response.successCount} messages were sent successfully`);
    
    // Handle failures and cleanup invalid tokens
    if (response.failureCount > 0) {
      const failedTokens = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          failedTokens.push(tokens[idx]);
        }
      });
      
      // Remove invalid tokens
      await cleanupInvalidTokens(failedTokens);
    }
  } catch (error) {
    console.error('Error sending notification:', error);
  }
}

// Cleanup invalid tokens
async function cleanupInvalidTokens(invalidTokens) {
  if (invalidTokens.length === 0) return;
  
  try {
    // Delete invalid tokens from database
    await db.query(
      'DELETE FROM user_notification_tokens WHERE token = ANY($1)',
      [invalidTokens]
    );
    console.log(`Removed ${invalidTokens.length} invalid tokens`);
  } catch (error) {
    console.error('Error cleaning up invalid tokens:', error);
  }
}
```

### 6. Web Push Configuration (Optional)

If you're also implementing web push notifications, use the web push certificate:

```javascript
const webpush = require('web-push');

// Configure web push with VAPID keys
webpush.setVapidDetails(
  'mailto:your-email@example.com',  // Contact information
  'BJ9Mf0mMKZO8pnRYwL-',            // Public key
  'jdWcCM2iqQUmzsgXJnIT5yjrrfrgF_2rmjkM9gUv1QbWSlLcG2cyTA93qIHmSyvx2c6o'  // Private key
);
```

## Testing Your Implementation

1. Register a test FCM token using the `/api/notifications/register` endpoint
2. Send a test notification to that token
3. Verify that the notification is received on the device

```bash
# Test loginToken registration
curl -X POST https://your-server.com/api/notifications/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER_AUTH_TOKEN" \
  -d '{"email": "test@example.com", "loginToken": "TEST_FCM_TOKEN"}'

# Send test notification (server-side script)
sendNotificationToUser('test@example.com', 'Test Notification', 'This is a test message');
```

## Security Considerations

1. Always authenticate users before registering or unregistering tokens
2. Store the Firebase Admin SDK credentials securely
3. Don't expose the server key or private key in client-side code
4. Implement rate limiting on notification endpoints
5. Regularly clean up unused/invalid tokens

## Additional Resources

1. [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
2. [FCM Server Implementation Guide](https://firebase.google.com/docs/cloud-messaging/server)
3. [Web Push Notifications](https://firebase.google.com/docs/cloud-messaging/js/client) 