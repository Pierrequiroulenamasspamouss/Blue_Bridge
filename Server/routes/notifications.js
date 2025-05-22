const express = require('express');
const router = express.Router();
const db = require('../models');
const { User, DeviceToken } = db; // Import your models
const { sendNotification } = require('../services/firebaseService');
const { validateToken } = require('../middleware/auth');

// Register device token
router.post('/register', validateToken, async (req, res) => {
    try {
        const { email, token, deviceToken } = req.body;

        if (!email || !token || !deviceToken) {
            return res.status(400).json({
                status: 'error',
                message: 'Missing required fields'
            });
        }

        const user = await User.findOne({ where: { email } });
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: 'User not found'
            });
        }

        const [deviceTokenRecord, created] = await DeviceToken.findOrCreate({
            where: { userId: user.userId },
            defaults: {
                tokenId: require('uuid').v4(),
                token: deviceToken,
                deviceType: 'android',
                lastUsed: new Date(),
                isActive: true
            }
        });

        if (!created) {
            deviceTokenRecord.token = deviceToken;
            deviceTokenRecord.lastUsed = new Date();
            deviceTokenRecord.isActive = true;
            await deviceTokenRecord.save();
        }

        res.json({
            status: 'success',
            message: 'Device token registered successfully'
        });
    } catch (error) {
        console.error('Error registering device token:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error registering device token: ' + error.message
        });
    }
});

// Get all notifications for a user
router.get('/:userId', validateToken, async (req, res) => {
    try {
        const user = await User.findOne({ where: { userId: req.params.userId } });
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: 'User not found'
            });
        }

        const deviceToken = await DeviceToken.findOne({
            where: { userId: req.params.userId }
        });

        if (!deviceToken) {
            return res.status(404).json({
                status: 'error',
                message: 'Device token not found'
            });
        }

        res.json({
            status: 'success',
            data: {
                deviceToken: deviceToken.token,
                notificationsEnabled: true
            }
        });
    } catch (error) {
        console.error('Error fetching notifications:', error);
        res.status(500).json({
            status: 'error',
            message: 'Internal server error'
        });
    }
});

// Send notification to a user
router.post('/send', validateToken, async (req, res) => {
    try {
        const { userId, title, body, data } = req.body;

        const user = await User.findOne({ where: { userId } });
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: 'User not found'
            });
        }

        const deviceToken = await DeviceToken.findOne({
            where: { userId }
        });

        if (!deviceToken) {
            return res.status(404).json({
                status: 'error',
                message: 'Device token not found'
            });
        }

        await sendNotification(deviceToken.token, {
            title,
            body,
            data
        });

        res.json({
            status: 'success',
            message: 'Notification sent successfully'
        });
    } catch (error) {
        console.error('Error sending notification:', error);
        res.status(500).json({
            status: 'error',
            message: 'Internal server error'
        });
    }
});

// Update device token
router.put('/token', async (req, res) => {
    try {
        const { userId, token } = req.body;

        const user = await User.findOne({ where: { userId } });
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        const [deviceToken, created] = await DeviceToken.findOrCreate({
            where: { userId },
            defaults: { token }
        });

        if (!created) {
            deviceToken.token = token;
            await deviceToken.save();
        }

        res.json({
            status: 'success',
            message: 'Device token updated successfully'
        });
    } catch (error) {
        console.error('Error updating device token:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

module.exports = router;