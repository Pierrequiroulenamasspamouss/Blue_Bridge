const admin = require('firebase-admin');
const dotenv = require('dotenv');
dotenv.config();



// Build service account object from environment variables
const serviceAccount = {
    type: process.env.FIREBASE_TYPE,
    project_id: process.env.FIREBASE_PROJECT_ID,
    private_key_id: process.env.FIREBASE_PRIVATE_KEY_ID,
    private_key: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n') || '',
    client_email: process.env.FIREBASE_CLIENT_EMAIL,
    client_id: process.env.FIREBASE_CLIENT_ID,
    auth_uri: process.env.FIREBASE_AUTH_URI,
    token_uri: process.env.FIREBASE_TOKEN_URI,
    auth_provider_x509_cert_url: process.env.FIREBASE_AUTH_PROVIDER_X509_CERT_URL,
    client_x509_cert_url: process.env.FIREBASE_CLIENT_X509_CERT_URL,
    universe_domain: process.env.FIREBASE_UNIVERSE_DOMAIN
};


console.log('Service Account:', {
    ...serviceAccount,
    private_key: serviceAccount.private_key ? '***PRIVATE_KEY_REDACTED***' : 'MISSING'
});

// Flag to track if Firebase is initialized
let isInitialized = false;

// Initialize Firebase Admin SDK
const initializeFirebase = () => {
    if (isInitialized) {
        return true;
    }

    try {
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });
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
// Send image chunks via FCM
const sendImageChunks = async (receiverToken, senderId, senderName, imageChunks, delayMs = 100) => {
    if (!isInitialized && !initializeFirebase()) {
        throw new Error('Firebase Admin SDK not initialized');
    }

    if (!receiverToken) {
        throw new Error('Receiver token not provided');
    }

    const imageId = imageChunks[0]?.imageId || Date.now();
    let successCount = 0;
    let failureCount = 0;

    console.log(`📤 Sending ${imageChunks.length} chunks for image ${imageId} to ${receiverToken.substring(0, 10)}...`);

    for (let i = 0; i < imageChunks.length; i++) {
        const chunk = imageChunks[i];

        try {
            const message = {
                token: receiverToken,
                data: {
                    type: 'image_chunk',
                    imageId: chunk.imageId.toString(),
                    totalChunks: chunk.totalChunks.toString(),
                    chunkIndex: chunk.chunkIndex.toString(),
                    data: chunk.data,
                    senderId: senderId,
                    senderName: senderName,
                    receiverId: 'receiver', // Will be set by the app
                    timestamp: Date.now().toString()
                },
                android: {
                    priority: 'high',
                    notification: {
                        title: 'Image transmission',
                        body: `Image part ${chunk.chunkIndex + 1}/${chunk.totalChunks}`,
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
            successCount++;
            console.log(`✅ Chunk ${chunk.chunkIndex + 1}/${chunk.totalChunks} sent successfully`);

            // Add delay between chunks to avoid rate limiting
            if (i < imageChunks.length - 1) {
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        } catch (error) {
            failureCount++;
            console.error(`❌ Failed to send chunk ${chunk.chunkIndex + 1}/${chunk.totalChunks}:`, error);
        }
    }

    console.log(`📊 Image ${imageId} transmission complete: ${successCount} successful, ${failureCount} failed`);
    return { successCount, failureCount, totalChunks: imageChunks.length };
};

// Send chat message via FCM (wrapper for sendPushNotification)
const sendChatMessage = async (deviceToken, senderId, senderName, content, data = {}) => {
    if (!isInitialized && !initializeFirebase()) {
        throw new Error('Firebase Admin SDK not initialized');
    }
    // Merge chat-specific data
    const messageData = {
        type: 'chat_message',
        senderId,
        senderName,
        content,
        ...data
    };
    return await sendPushNotification(
        deviceToken,
        senderName,
        content,
        messageData
    );
};

// Initialize Firebase on module load
initializeFirebase();

module.exports = {
    sendImageChunks,
    sendPushNotification,
    sendMulticastPushNotification,
    initializeFirebase,
    sendChatMessage
};