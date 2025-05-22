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

module.exports = router;