const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Path to the service account JSON file
const SERVICE_ACCOUNT_PATH = path.join(__dirname, '../scripts/firebase-service-account.json');

// Flag to track if Firebase is initialized
let isInitialized = false;

// Initialize Firebase Admin SDK
const initializeFirebase = () => {
    if (isInitialized) {
        return true;
    }

    try {
        // Check if service account file exists
        if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
            console.error(`❌ Firebase initialization failed: Service account file not found at ${SERVICE_ACCOUNT_PATH}`);
            return false;
        }

        // Read service account file
        const serviceAccount = require(SERVICE_ACCOUNT_PATH);
        
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });

        console.log('✅ Firebase Admin SDK initialized successfully using service account file');
        isInitialized = true;
        return true;
    } catch (error) {
        console.error('❌ Firebase initialization error:', error);
        return false;
    }
};

// Send push notification to a single device
const sendPushNotification = async (deviceToken, title, body, data = {}) => {
    if (!isInitialized && !initializeFirebase()) {
        throw new Error('Firebase Admin SDK not initialized');
    }

    try {
        const message = {
            token: deviceToken,
            notification: {
                title,
                body
            },
            data: data,
            android: {
                priority: 'high',
                notification: {
                    sound: 'default',
                    priority: 'high',
                    channelId: 'default-channel'
                }
            },
            apns: {
                payload: {
                    aps: {
                        sound: 'default',
                        badge: 1,
                        contentAvailable: true
                    }
                }
            }
        };

        const response = await admin.messaging().send(message);
        console.log(`✅ Push notification sent successfully to ${deviceToken.substring(0, 10)}...`);
        return response;
    } catch (error) {
        console.error(`❌ Error sending push notification to ${deviceToken.substring(0, 10)}...`, error);
        throw error;
    }
};

// Send push notifications to multiple devices
const sendMulticastPushNotification = async (deviceTokens, title, body, data = {}) => {
    if (!isInitialized && !initializeFirebase()) {
        throw new Error('Firebase Admin SDK not initialized');
    }

    if (!deviceTokens || deviceTokens.length === 0) {
        throw new Error('No device tokens provided');
    }

    try {
        // Firebase has a limit of 500 tokens per multicast message
        const chunks = [];
        const chunkSize = 500;
        
        for (let i = 0; i < deviceTokens.length; i += chunkSize) {
            chunks.push(deviceTokens.slice(i, i + chunkSize));
        }
        
        let successCount = 0;
        let failureCount = 0;
        
        for (const tokensChunk of chunks) {
            // Create a multicast message with the tokens
            const message = {
                tokens: tokensChunk,
                notification: {
                    title,
                    body
                },
                data: data,
                android: {
                    priority: 'high',
                    notification: {
                        sound: 'default',
                        priority: 'high',
                        channelId: 'default-channel'
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: 'default',
                            badge: 1,
                            contentAvailable: true
                        }
                    }
                }
            };
            
            // Use sendMulticast instead of sendAll
            const response = await admin.messaging().sendMulticast(message);
            successCount += response.successCount;
            failureCount += response.failureCount;
        }
        
        console.log(`✅ Multicast push notification sent: ${successCount} successful, ${failureCount} failed`);
        return { successCount, failureCount };
    } catch (error) {
        console.error('❌ Error sending multicast push notification:', error);
        throw error;
    }
};

// Initialize Firebase on module load
initializeFirebase();

module.exports = {
    sendPushNotification,
    sendMulticastPushNotification,
    initializeFirebase
}; 