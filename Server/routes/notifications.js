const express = require('express');
const router = express.Router();
const db = require('../models');
const { User, DeviceToken } = db;
const { sendPushNotification, sendMulticastPushNotification } = require('../services/firebaseService');
const { body, validationResult } = require('express-validator');
const validator = require('validator');

// Register device token
router.post('/register', [
    body('userId').isString().trim().escape(),
    body('loginToken').isString().trim().escape(),
    body('deviceToken').isString().trim().escape()
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ status: 'error', errors: errors.array() });
    }
  const { userId, loginToken, deviceToken } = req.body;

  try {
    // Verify user exists and loginToken is valid
    const user = await User.findOne({
      where: {
        userId,
        loginToken
      }
    });

    if (!user) {
      return res.status(401).json({
        status: 'error',
        message: 'Invalid user credentials'
      });
    }

    // Store token in database
    await DeviceToken.findOrCreate({
      where: { userId, token: deviceToken },
      defaults: {
        isActive: true,
        userId
      }
    });

    // Send a welcome notification to the newly registered token
    await sendPushNotification(
      deviceToken,
      'Welcome!',
      'Your device has been successfully registered for notifications.'
    );

    res.json({
      status: 'success',
      message: 'Token registered successfully'
    });
  } catch (error) {
    console.error('Error registering token:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to register token',
      error: error.message
    });
  }
});

// Unregister device token
router.post('/unregister', async (req, res) => {
  const { userId, loginToken, deviceToken } = req.body;

  try {
    // Verify user exists and loginToken is valid
    const user = await User.findOne({
      where: {
        userId,
        loginToken
      }
    });

    if (!user) {
      return res.status(401).json({
        status: 'error',
        message: 'Invalid user credentials'
      });
    }

    // Remove token from database
    await DeviceToken.destroy({
      where: {
        userId,
        token: deviceToken
      }
    });

    res.json({
      status: 'success',
      message: 'Token unregistered successfully'
    });
  } catch (error) {
    console.error('Error unregistering token:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to unregister token',
      error: error.message
    });
  }
});

// Send notification
router.post('/send', async (req, res) => {
  try {
    const { title, message, targetUserIds = [] } = req.body;

    // Validate required fields
    if (!title || !message) {
      return res.status(400).json({
        status: 'error',
        message: 'Title and message are required fields'
      });
    }

    // Validate targetUserIds is an array
    if (!Array.isArray(targetUserIds)) {
      return res.status(400).json({
        status: 'error',
        message: 'targetUserIds must be an array'
      });
    }

    // Find users
    const whereClause = targetUserIds.length > 0
      ? { userId: targetUserIds }
      : {};

    const users = await User.findAll({
      where: whereClause,
      include: [{
        model: DeviceToken,
        as: 'deviceTokens',
        where: { isActive: true },
        required: false
      }]
    });

    if (!users || users.length === 0) {
      return res.status(404).json({
        status: 'error',
        message: 'No matching users found'
      });
    }

    // Extract all active device tokens
    const tokens = users.flatMap(user =>
      user.deviceTokens.map(token => token.token)
    ).filter(Boolean);

    if (tokens.length === 0) {
      return res.status(404).json({
        status: 'error',
        message: 'No active device tokens found for these users'
      });
    }

    // Send notifications using your Firebase service
    let result;
    if (tokens.length === 1) {
      // Single device
      result = await sendPushNotification(tokens[0], title, message);
    } else {
      // Multiple devices
      result = await sendMulticastPushNotification(tokens, title, message);
    }

    // Prepare response
    const response = {
      status: 'success',
      message: 'Notifications processed',
      data: {
        usersTargeted: users.length,
        devicesTargeted: tokens.length
      }
    };

    // Add multicast results if available
    if (result.successCount !== undefined) {
      response.data.successCount = result.successCount;
      response.data.failureCount = result.failureCount;
    }

    res.json(response);

  } catch (error) {
    console.error('Notification route error:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to send notifications',
      error: error.message
    });
  }
});

module.exports = router;