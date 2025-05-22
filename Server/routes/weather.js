const express = require('express');
const router = express.Router();
const { getWeatherData } = require('../services/weatherService');
const { validateToken } = require('../middleware/auth');
const { User } = require('../models');

// Get weather data by location
router.post('/', validateToken, async (req, res) => {
    try {
        const { location, userId, loginToken } = req.body;
        
        if (!location || !userId || !loginToken) {
            return res.status(400).json({
                status: 'error',
                message: 'Location, userId, and loginToken are required'
            });
        }

        // Verify user and token
        const user = await User.findOne({
            where: {
                userId: userId,
                loginToken: loginToken
            }
        });

        if (!user) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid user ID or login token'
            });
        }

        const weatherResponse = await getWeatherData(location, userId, loginToken);
        res.json(weatherResponse);
    } catch (error) {
        console.error('Error getting weather data:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error getting weather data: ' + error.message
        });
    }
});

module.exports = router; 