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
        
        // Diagnostic logging
        console.log('🔍 Firebase Initialization Diagnostics:');
        console.log('Service Account Project ID:', serviceAccount.project_id);
        console.log('Client Email:', serviceAccount.client_email);

        // Attempt to initialize with more verbose error handling
        try {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
        } catch (initError) {
            console.error('❌ Firebase App Initialization Error:', initError);
            
            // Additional diagnostics
            console.log('Available admin methods:', Object.keys(admin));
            console.log('Credential methods:', Object.keys(admin.credential));
            
            throw initError;
        }

        // Verify messaging methods
        const messagingMethods = Object.keys(admin.messaging());
        console.log('🔍 Available Messaging Methods:', messagingMethods);

        // Explicit method check
        const messagingInstance = admin.messaging();
        console.log('🔍 Messaging Instance Methods:', Object.keys(messagingInstance));

        console.log('✅ Firebase Admin SDK initialized successfully using service account file');
        isInitialized = true;
        return true;
    } catch (error) {
        console.error('❌ Firebase initialization comprehensive error:', error);
        
        // Log full error details
        console.error('Error Name:', error.name);
        console.error('Error Message:', error.message);
        console.error('Error Stack:', error.stack);

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

// Send push notifications to multiple devices using sendEachForMulticast
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
        const responses = [];

        for (const tokensChunk of chunks) {
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

            const response = await admin.messaging().sendEachForMulticast(message);

            response.responses.forEach((resp, index) => {
                if (resp.success) {
                    successCount++;
                } else {
                    failureCount++;
                    console.error(`❌ Failed to send to token ${tokensChunk[index].substring(0, 10)}...:`, resp.error);
                }
            });

            responses.push(response);
        }

        console.log(`✅ Multicast push notification sent: ${successCount} successful, ${failureCount} failed`);
        return { successCount, failureCount, responses };
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
