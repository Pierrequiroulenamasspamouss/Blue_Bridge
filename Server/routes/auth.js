const express = require('express');
const router = express.Router();
const { User, DeviceToken } = require('../models');
const { v4: uuidv4 } = require('uuid');
const { sendWelcomeEmail } = require('../services/emailService');

// Helper functions
const normalizeEmail = email => email.toLowerCase().trim();
const parseJSON = str => {
    if (!str) return null;
    try {
        return JSON.parse(str);
    } catch (e) {
        console.error('JSON parse error:', e);
        return null;
    }
};
const jsonResponse = (res, status, data) => res.status(status).json(data);

// Middleware to validate token
const validateToken = async (req, res, next) => {
    const { userId, loginToken } = req.body;
    if (!userId || !loginToken) return jsonResponse(res, 401, { status: 'error', message: 'User ID and token required' });

    try {
        const user = await User.findOne({ where: { userId } });
        if (!user || user.loginToken !== loginToken) {
            return jsonResponse(res, 401, { status: 'error', message: 'Invalid token' });
        }
        req.user = user;
        next();
    } catch (error) {
        console.error('Token validation error:', error);
        jsonResponse(res, 500, { status: 'error', message: 'Authentication error' });
    }
};

// User Registration
router.post('/register', async (req, res) => {
    const { email, password, firstName, lastName, deviceToken, ...userData } = req.body;
    if (!email || !password || !firstName || !lastName) {
        return jsonResponse(res, 400, { status: 'error', message: 'Missing required fields' });
    }

    try {
        const normalizedEmail = normalizeEmail(email);
        if (await User.findOne({ where: { email: normalizedEmail } })) {
            return jsonResponse(res, 409, { status: 'error', message: 'User already exists' });
        }

        const userId = uuidv4();
        const loginToken = uuidv4();
        const user = await User.create({
            userId,
            email: normalizedEmail,
            password,
            firstName,
            lastName,
            loginToken,
            ...userData,
            location: userData.location ? JSON.stringify(userData.location) : null,
            waterNeeds: userData.waterNeeds ? JSON.stringify(userData.waterNeeds) : '[]',
            notificationPreferences: JSON.stringify(userData.notificationPreferences || {
                weatherAlerts: true,
                wellUpdates: true,
                nearbyUsers: true
            }),
            isWellOwner: userData.isWellOwner ? 1 : 0
        });

        if (deviceToken) {
            await DeviceToken.create({
                tokenId: uuidv4(),
                userId,
                token: deviceToken,
                deviceType: 'android',
                lastUsed: new Date(),
                isActive: true
            });
        }

        try { await sendWelcomeEmail(normalizedEmail, `${firstName} ${lastName}`); }
        catch (e) { console.error('Email error:', e); }

        jsonResponse(res, 201, {
            status: 'success',
            message: 'Registration successful',
            userData: {
                ...user.toJSON(),
                location: parseJSON(user.location),
                waterNeeds: parseJSON(user.waterNeeds)
            }
        });
    } catch (error) {
        console.error('Registration error:', error);
        jsonResponse(res, 500, { status: 'error', message: 'Registration failed' });
    }
});

// User Login
router.post('/login', async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        return jsonResponse(res, 400, { status: 'error', message: 'Email and password required' });
    }

    try {
        const user = await User.findOne({ where: { email: normalizeEmail(email) } });
        if (!user || user.password !== password) {
            return jsonResponse(res, 401, { status: 'error', message: 'Invalid credentials' });
        }

        user.loginToken = uuidv4();
        user.lastActive = new Date();
        await user.save();

        // Parse location and waterNeeds, providing defaults if null
        const location = parseJSON(user.location) || { latitude: 0.0, longitude: 0.0, lastUpdated: "never" };
        const waterNeeds = parseJSON(user.waterNeeds) || [];

        jsonResponse(res, 200, {
            status: 'success',
            message: 'Login successful',
            data: {
                ...user.toJSON(),
                location: location,
                waterNeeds: waterNeeds
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        jsonResponse(res, 500, { status: 'error', message: 'Login failed' });
    }
});

// Weather Endpoint (using userId instead of email)
router.post('/weather', validateToken, async (req, res) => {
    try {
        const { location } = req.body;
        if (!location) {
            return jsonResponse(res, 400, { status: 'error', message: 'Location required' });
        }

        // Here you would call your weather service with:
        // { location, userId: req.user.userId, loginToken: req.user.loginToken }

        jsonResponse(res, 200, {
            status: 'success',
            weatherData: {} // Your weather data here
        });
    } catch (error) {
        console.error('Weather error:', error);
        jsonResponse(res, 500, { status: 'error', message: 'Weather fetch failed' });
    }
});

// Other endpoints (logout, delete, validate) follow same pattern...

module.exports = router;