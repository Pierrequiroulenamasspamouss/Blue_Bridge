const express = require('express');
const router = express.Router();
const db = require('../models');
const { User, sequelize } = db;
const { v4: uuidv4 } = require('uuid');
const { sendPushNotification, sendMulticastPushNotification } = require('../services/firebaseService');

/**
 * @route POST /api/notifications/register
 * @desc Register a device token for push notifications
 * @access Private (requires authentication)
 */
router.post('/register', async (req, res) => {
    console.log('💌 PUSH NOTIFICATION REGISTRATION REQUEST RECEIVED');
    console.log('Request data:', JSON.stringify(req.body, null, 2));
    
    try {
        const { email, token, deviceToken } = req.body;
        
        // Validate required fields
        if (!email || !token || !deviceToken) {
            console.log('❌ PUSH NOTIFICATION REGISTRATION FAILED: Missing required fields');
            return res.status(400).json({ 
                status: 'error', 
                message: 'Email, authentication token, and device token are required' 
            });
        }
        
        // Verify user and token
        const user = await User.findOne({ where: { email } });
        if (!user) {
            console.log(`❌ PUSH NOTIFICATION REGISTRATION FAILED: User not found for email ${email}`);
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid credentials' 
            });
        }
        
        if (user.loginToken !== token) {
            console.log(`❌ PUSH NOTIFICATION REGISTRATION FAILED: Invalid token for user ${email}`);
            console.log(`Expected: ${user.loginToken}, Received: ${token}`);
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid credentials' 
            });
        }
        
        // Store the device token
        let deviceTokens = [];
        if (user.deviceTokens) {
            try {
                deviceTokens = JSON.parse(user.deviceTokens);
                if (!Array.isArray(deviceTokens)) deviceTokens = [];
            } catch (e) {
                console.log(`⚠️ Error parsing existing device tokens for user ${email}:`, e.message);
                deviceTokens = [];
            }
        }
        
        // Add token if it doesn't exist already
        if (!deviceTokens.includes(deviceToken)) {
            deviceTokens.push(deviceToken);
            user.deviceTokens = JSON.stringify(deviceTokens);
            await user.save();
            console.log(`✅ PUSH NOTIFICATION REGISTRATION SUCCESS: Token added for user ${email}`);
            console.log(`User now has ${deviceTokens.length} registered device(s)`);
            
            // Send a test notification to verify registration
            try {
                await sendPushNotification(
                    deviceToken,
                    'Notification Registration Successful',
                    'You will now receive notifications from WellConnect'
                );
                console.log(`✅ Test notification sent to ${email}'s device`);
            } catch (error) {
                console.error(`❌ Failed to send test notification:`, error);
            }
        } else {
            console.log(`ℹ️ PUSH NOTIFICATION TOKEN ALREADY REGISTERED for user ${email}`);
        }
        
        res.json({
            status: 'success',
            message: 'Notification token registered successfully',
            deviceCount: deviceTokens.length
        });
    } catch (error) {
        console.error('❌ PUSH NOTIFICATION REGISTRATION ERROR:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Failed to register notification token: ' + error.message 
        });
    }
});

/**
 * @route POST /api/notifications/unregister
 * @desc Unregister a device token from push notifications
 * @access Private (requires authentication)
 */
router.post('/unregister', async (req, res) => {
    try {
        const { email, token, deviceToken } = req.body;
        
        // Validate required fields
        if (!email || !token || !deviceToken) {
            return res.status(400).json({ 
                status: 'error', 
                message: 'Email, authentication token, and device token are required' 
            });
        }
        
        // Verify user and token
        const user = await User.findOne({ where: { email } });
        if (!user || user.loginToken !== token) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid credentials' 
            });
        }
        
        // Remove the device token
        let deviceTokens = [];
        if (user.deviceTokens) {
            try {
                deviceTokens = JSON.parse(user.deviceTokens);
                if (!Array.isArray(deviceTokens)) deviceTokens = [];
            } catch (e) {
                deviceTokens = [];
            }
        }
        
        // Filter out the token to be removed
        deviceTokens = deviceTokens.filter(t => t !== deviceToken);
        user.deviceTokens = JSON.stringify(deviceTokens);
        await user.save();
        
        res.json({
            status: 'success',
            message: 'Notification token unregistered successfully'
        });
    } catch (error) {
        console.error('Error unregistering notification token:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Failed to unregister notification token: ' + error.message 
        });
    }
});

/**
 * @route GET /api/notifications/status
 * @desc Check notification registration status for a user
 * @access Private (requires authentication)
 */
router.get('/status', async (req, res) => {
    try {
        const { email, token } = req.query;
        
        // Validate required fields
        if (!email || !token) {
            return res.status(400).json({ 
                status: 'error', 
                message: 'Email and authentication token are required' 
            });
        }
        
        // Verify user and token
        const user = await User.findOne({ where: { email } });
        if (!user || user.loginToken !== token) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid credentials' 
            });
        }
        
        // Get registered device tokens
        let deviceTokens = [];
        if (user.deviceTokens) {
            try {
                deviceTokens = JSON.parse(user.deviceTokens);
                if (!Array.isArray(deviceTokens)) deviceTokens = [];
            } catch (e) {
                deviceTokens = [];
            }
        }
        
        res.json({
            status: 'success',
            registeredTokens: deviceTokens.length,
            notificationsEnabled: deviceTokens.length > 0
        });
    } catch (error) {
        console.error('Error checking notification status:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Failed to check notification status: ' + error.message 
        });
    }
});

/**
 * @route POST /api/notifications/send
 * @desc Send push notification to all registered devices or specific users
 * @access Private (should be admin only in production)
 */
router.post('/send', async (req, res) => {
    console.log('📣 PUSH NOTIFICATION SEND REQUEST RECEIVED');
    console.log('Request data:', JSON.stringify(req.body, null, 2));
    
    try {
        const { title, message, targetEmails, data } = req.body;
        
        // Validate required fields
        if (!title || !message) {
            console.log('❌ PUSH NOTIFICATION SEND FAILED: Missing title or message');
            return res.status(400).json({ 
                status: 'error', 
                message: 'Title and message are required' 
            });
        }
        
        // Find all users with device tokens
        let userQuery = {};
        if (targetEmails && Array.isArray(targetEmails) && targetEmails.length > 0) {
            userQuery.email = targetEmails;
            console.log(`🔍 Targeting specific users: ${targetEmails.join(', ')}`);
        } else {
            console.log('🔍 Targeting all users with registered devices');
        }
        
        try {
            console.log('📊 Querying database for users...');
            const users = await User.findAll({
                where: userQuery,
                attributes: ['userId', 'email', 'deviceTokens']
            });
            
            console.log(`📊 Found ${users.length} users in the database`);
            
            // Collect all device tokens
            const deviceTokens = [];
            let userCount = 0;
            
            users.forEach(user => {
                console.log(`👤 Checking tokens for user: ${user.email}`);
                if (user.deviceTokens) {
                    try {
                        const tokens = JSON.parse(user.deviceTokens);
                        console.log(`📱 User ${user.email} has ${tokens ? tokens.length : 0} device tokens`);
                        
                        if (Array.isArray(tokens) && tokens.length > 0) {
                            deviceTokens.push(...tokens);
                            userCount++;
                            console.log(`✅ Added ${tokens.length} tokens for ${user.email}`);
                        } else {
                            console.log(`ℹ️ No valid tokens for ${user.email}`);
                        }
                    } catch (e) {
                        console.error(`❌ Error parsing device tokens for user ${user.email}:`, e);
                    }
                }
            });
            
            if (deviceTokens.length === 0) {
                console.log('❌ PUSH NOTIFICATION SEND FAILED: No valid device tokens found');
                return res.status(400).json({
                    status: 'error',
                    message: 'No registered devices found for the target users'
                });
            }
            
            console.log(`📤 Sending notification to ${deviceTokens.length} devices (${userCount} users)`);
            
            // Send notification via Firebase
            const notificationData = data || {};
            const result = await sendMulticastPushNotification(deviceTokens, title, message, notificationData);
            
            console.log('📊 Notification send result:', result);
            
            res.json({
                status: 'success',
                message: 'Notifications sent successfully',
                successCount: result.successCount,
                failureCount: result.failureCount,
                userCount: userCount,
                deviceCount: deviceTokens.length
            });
        } catch (error) {
            console.error('❌ Error sending notifications:', error);
            res.status(500).json({
                status: 'error',
                message: 'Failed to send notifications: ' + error.message
            });
        }
    } catch (error) {
        console.error('❌ PUSH NOTIFICATION SEND ERROR:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Failed to send notifications: ' + error.message 
        });
    }
});

/**
 * @route POST /api/notifications/send-to-token
 * @desc Send push notification to a specific device token
 * @access Private (requires authentication)
 */
router.post('/send-to-token', async (req, res) => {
    try {
        const { email, token, deviceToken, title, message, data } = req.body;
        
        // Validate required fields
        if (!email || !token || !deviceToken || !title || !message) {
            return res.status(400).json({ 
                status: 'error', 
                message: 'Email, authentication token, device token, title, and message are required' 
            });
        }
        
        // Verify user and token
        const user = await User.findOne({ where: { email } });
        if (!user || user.loginToken !== token) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid credentials' 
            });
        }
        
        // Send notification
        try {
            const notificationData = data || {};
            const result = await sendPushNotification(deviceToken, title, message, notificationData);
            
            res.json({
                status: 'success',
                message: 'Notification sent successfully',
                messageId: result
            });
        } catch (error) {
            console.error('Error sending notification:', error);
            res.status(500).json({
                status: 'error',
                message: 'Failed to send notification: ' + error.message
            });
        }
    } catch (error) {
        console.error('Error in send-to-token route:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Failed to process request: ' + error.message 
        });
    }
});

module.exports = router; 