const express = require('express');
const router = express.Router();
const { User, DeviceToken } = require('../models');
const { v4: uuidv4 } = require('uuid');
const { sendWelcomeEmail } = require('../services/emailService');

// Helper functions
const handleError = (res, error, message = 'Operation failed') => {
  console.error(error);
  return res.status(500).json({ status: 'error', message });
};

const validateRequest = (res, fields) => {
  for (const [field, message] of Object.entries(fields)) {
    if (!req.body[field]) return res.status(400).json({ status: 'error', message });
  }
};

// Authentication middleware
const authenticate = async (req, res, next) => {
  try {
    const { userId, loginToken } = req.body;
    if (!userId || !loginToken) {
      return res.status(401).json({ status: 'error', message: 'Authentication required' });
    }

    const user = await User.findByPk(userId);
    if (!user || user.loginToken !== loginToken) {
      return res.status(401).json({ status: 'error', message: 'Invalid credentials' });
    }

    req.user = user;
    next();
  } catch (error) {
    handleError(res, error, 'Authentication failed');
  }
};



// Delete account
// Add this to your auth routes (replace the existing delete endpoint)
router.post('/delete-account', async (req, res) => {
  try {
    const { email, password, token } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        status: 'error',
        message: 'Email and password required'
      });
    }

    // Find user by email
    const user = await User.findOne({
      where: { email: email.toLowerCase().trim() }
    });

    // Verify password and token
    if (!user || user.password !== password || user.loginToken !== token) {
      return res.status(401).json({
        status: 'error',
        message: 'Invalid credentials'
      });
    }

    // Perform deletion in transaction
    const transaction = await User.sequelize.transaction();
    try {
      await DeviceToken.destroy({
        where: { userId: user.userId },
        transaction
      });

      await User.destroy({
        where: { userId: user.userId },
        transaction
      });

      await transaction.commit();

      return res.json({
        status: 'success',
        message: 'Account permanently deleted'
      });
    } catch (error) {
      await transaction.rollback();
      throw error;
    }
  } catch (error) {
    handleError(res, error, 'Account deletion failed');
  }
});


// User Registration
router.post('/register', async (req, res) => {
  try {
    const requiredFields = {
      email: 'Email is required',
      password: 'Password is required',
      firstName: 'First name is required',
      lastName: 'Last name is required'
    };

    validateRequest(res, requiredFields);

    const { email, password, firstName, lastName, deviceToken, ...userData } = req.body;
    const normalizedEmail = email.toLowerCase().trim();

    if (await User.findOne({ where: { email: normalizedEmail } })) {
      return res.status(409).json({ status: 'error', message: 'Email already in use' });
    }

    const user = await User.create({
      ...userData,
      email: normalizedEmail,
      password,
      firstName,
      lastName,
      loginToken: uuidv4(),
      isWellOwner: userData.isWellOwner || false
    });

    if (deviceToken) {
      await DeviceToken.create({
        userId: user.userId,
        token: deviceToken,
        deviceType: 'android'
      });
    }

    try {
      await sendWelcomeEmail(normalizedEmail, `${firstName} ${lastName}`);
    } catch (emailError) {
      console.error('Welcome email failed:', emailError);
    }

    return res.status(201).json({
      status: 'success',
      data: user.get({ plain: true })
    });

  } catch (error) {
    handleError(res, error);
  }
});

// User Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ status: 'error', message: 'Email and password required' });
    }

    const user = await User.findOne({ where: { email: email.toLowerCase().trim() } });
    if (!user || user.password !== password) {
      return res.status(401).json({ status: 'error', message: 'Invalid credentials' });
    }

    user.loginToken = uuidv4();
    user.lastActive = new Date();
    await user.save();

    return res.json({
      status: 'success',
      data: user.get({ plain: true })
    });

  } catch (error) {
    handleError(res, error);
  }
});

// Weather Endpoint
router.post('/weather', authenticate, async (req, res) => {
  try {
    const { location } = req.body;
    if (!location) {
      return res.status(400).json({ status: 'error', message: 'Location required' });
    }

    // Process weather request here
    const weatherData = await getWeatherData({
      location,
      userId: req.user.userId,
      loginToken: req.user.loginToken
    });

    return res.json({
      status: 'success',
      data: weatherData
    });

  } catch (error) {
    handleError(res, error, 'Failed to fetch weather');
  }
});

// Update user profile
router.post('/update-profile', async (req, res) => {
    try {
        const { email, token, firstName, lastName, username, location } = req.body;

        // Validate required fields
        if (!email || !token) {
            return res.status(400).json({
                status: 'error',
                message: 'Email and token are required'
            });
        }

        // Find user by email and verify token
        const user = await User.findOne({
            where: {
                email: email.toLowerCase().trim(),
                loginToken: token
            }
        });

        if (!user) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid credentials'
            });
        }

        // Prepare updates
        const updates = {};
        if (firstName) updates.firstName = firstName;
        if (lastName) updates.lastName = lastName;
        if (username) updates.username = username;

        // Handle location update
        if (location) {
            updates.location = JSON.stringify({
                latitude: parseFloat(location.latitude),
                longitude: parseFloat(location.longitude),
                lastUpdated: new Date().toISOString()
            });
        }

        // Update user profile
        await user.update(updates);

        // Return updated user data (excluding sensitive fields)
        const updatedUser = await User.findByPk(user.userId, {
            attributes: { exclude: ['password', 'loginToken'] },
            raw: true
        });

        return res.json({
            status: 'success',
            message: 'Profile updated successfully',
            data: {
                ...updatedUser,
                location: updatedUser.location ? JSON.parse(updatedUser.location) : null
            }
        });

    } catch (error) {
        console.error('Error updating profile:', error);
        return res.status(500).json({
            status: 'error',
            message: 'Failed to update profile'
        });
    }
});


module.exports = router;