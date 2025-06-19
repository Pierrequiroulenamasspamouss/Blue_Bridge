const express = require('express');
const router = express.Router();
const db = require('../models');
const { User, DeviceToken } = db;
const { sendPushNotification, sendMulticastPushNotification } = require('../services/firebaseService');

// Register device token
router.post('/register', async (req, res) => {
  const { email, token } = req.body;

  try {
    // Find user by email to get userId
    const user = await User.findOne({ where: { email } });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Store token in database
    await DeviceToken.findOrCreate({
      where: { userId: user.userId, token },
      defaults: { isActive: true }
    });

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
  const { email, token } = req.body;

  try {
    // Find user by email to get userId
    const user = await User.findOne({ where: { email } });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Remove token from database
    await DeviceToken.destroy({
      where: { userId: user.userId, token }
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
    const { title, message, targetEmails = [] } = req.body;

    // Validate required fields
    if (!title || !message) {
      return res.status(400).json({
        status: 'error',
        message: 'Title and message are required fields'
      });
    }

    // Validate targetEmails is an array
    if (!Array.isArray(targetEmails)) {
      return res.status(400).json({
        status: 'error',
        message: 'targetEmails must be an array'
      });
    }

    // Find users
    const whereClause = targetEmails.length > 0
      ? { email: targetEmails }
      : {};

    const users = await User.findAll({ where: whereClause });

    if (!users || users.length === 0) {
      return res.status(404).json({
        status: 'error',
        message: 'No matching users found'
      });
    }

    // Get active device tokens
    const deviceTokens = await DeviceToken.findAll({
      where: {
        userId: users.map(user => user.userId),
        isActive: true
      },
      attributes: ['token']
    });

    if (!deviceTokens || deviceTokens.length === 0) {
      return res.status(404).json({
        status: 'error',
        message: 'No active device tokens found for these users'
      });
    }

    // Extract just the token strings
    const tokens = deviceTokens.map(dt => dt.token);

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