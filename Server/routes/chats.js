const express = require('express');
const router = express.Router();
const firebaseService = require('../services/firebaseService');
const db = require('../data/database_manager');

// Endpoint pour recevoir les messages et les envoyer via FCM
router.post('/', async (req, res) => {
    try {
        let senderId, receiverId, content, timestamp, receiverToken, senderName;

        console.log(`Received request:`, JSON.stringify(req.body, null, 2));

        if (req.body.data && req.body.to) {
            // FCM Messaging Tool format
            senderId = req.body.data.senderId;
            receiverId = req.body.data.receiverId || "test_receiver";
            content = req.body.data.content;
            timestamp = req.body.data.timestamp;
            receiverToken = req.body.to;
            senderName = req.body.notification?.title || "Unknown";
        } else if (req.body.data && req.body.data.senderId) {
            // Legacy app format
            senderId = req.body.data.senderId;
            receiverId = req.body.data.receiverId || "test_receiver";

            // Handle different content structures
            if (req.body.data.content && req.body.data.content.text) {
                content = req.body.data.content.text; // Extract text from nested object
            } else if (typeof req.body.data.content === 'string') {
                content = req.body.data.content;
            } else {
                content = JSON.stringify(req.body.data.content);
            }

            timestamp = req.body.data.timestamp;

            // Get receiver token from database for legacy format
            receiverToken = await getReceiverFCMToken(receiverId);
            senderName = await getSenderName(senderId);
        } else {
            console.log('❌ Unsupported request format');
            return res.status(400).json({
                success: false,
                error: 'Unsupported request format'
            });
        }

        console.log(`📨 Received message from ${senderId} to ${receiverId}`);
        console.log(`📝 Content:`, content);

        // Validation
        if (!senderId || !content ) {
        error = 'Missing required fields: senderId, content'
            console.log(error);
            return res.status(400).json({
                success: false,
                error: error
            });
        }

        // Handle text message only (for FCM Messaging Tool)
        let result;

        if (typeof content === "string" || (req.body.data && req.body.data.messageType === "TEXT")) {
            result = await firebaseService.sendChatMessage(
                receiverToken,
                senderId,
                senderName,
                content
            );
        } else {
            console.log(`❌ Unsupported content type`);
            return res.status(400).json({
                success: false,
                error: 'Unsupported content type'
            });
        }

        console.log(`✅ Message transmission result:`, result);

        res.json({
            success: true,
            message: 'Message sent successfully',
            timestamp: timestamp || Date.now()
        });

    } catch (error) {
        console.error('❌ Error processing message:', error);
        res.status(500).json({
            success: false,
            error: 'Internal server error',
            details: error.message
        });
    }
});

// Fonction pour récupérer le token FCM du destinataire depuis la base de données
async function getReceiverFCMToken(receiverId) {
    try {
        console.log(`🔍 Looking for FCM token for user: ${receiverId}`);
        
        // Essayer de récupérer depuis la base de données
        const fcmToken = await db.getUserFCMToken(receiverId);
        if (fcmToken) {
            return fcmToken;
        }
        
        // Fallback pour les tests si la base de données n'est pas disponible
        if (receiverId === "test_receiver") {
            console.log(`✅ Using fallback FCM token for test_receiver`);
            return "d9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg";
        }
        
        console.log(`❌ No FCM token found for user: ${receiverId}`);
        return null;
    } catch (error) {
        console.error('Error getting receiver FCM token:', error);
        
        // Fallback pour les tests en cas d'erreur de base de données
        if (receiverId === "test_receiver") {
            console.log(`✅ Using fallback FCM token for test_receiver (after error)`);
            return "d9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg";
        }
        
        return null;
    }
}

// Fonction pour récupérer le nom de l'expéditeur depuis la base de données
async function getSenderName(senderId) {
    try {
        console.log(`🔍 Looking for sender name for user: ${senderId}`);
        
        // Essayer de récupérer depuis la base de données
        const user = await db.getUserById(senderId);
        if (user) {
            const name = user.firstName || user.username || 'Unknown';
            console.log(`✅ Sender name found: ${name}`);
            return name;
        }
        
        // Fallback pour les tests si la base de données n'est pas disponible
        if (senderId === "bba26029-1c2f-4bae-9359-4e4e3d327fee") {
            console.log(`✅ Using fallback sender name: Test User`);
            return "Test User";
        }
        
        console.log(`✅ Sender name found: Unknown`);
        return 'Unknown';
    } catch (error) {
        console.error('Error getting sender name:', error);
        
        // Fallback pour les tests en cas d'erreur de base de données
        if (senderId === "bba26029-1c2f-4bae-9359-4e4e3d327fee") {
            console.log(`✅ Using fallback sender name: Test User (after error)`);
            return "Test User";
        }
        
        return 'Unknown';
    }
}

// Split base64 image into chunks
const splitImageIntoChunks = (base64Image, imageId, chunkSize = 3500) => {
    const chunks = [];
    const totalSize = base64Image.length;
    const totalChunks = Math.ceil(totalSize / chunkSize);

    for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize;
        const end = Math.min(start + chunkSize, totalSize);
        const chunkData = base64Image.substring(start, end);

        chunks.push({
            imageId: imageId,
            totalChunks: totalChunks,
            chunkIndex: i,
            data: chunkData
        });
    }

    return chunks;
};
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

module.exports = router;
