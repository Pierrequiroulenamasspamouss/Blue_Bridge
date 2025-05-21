const express = require('express');
const router = express.Router();
const { getWeatherData } = require('../services/weatherService');
const { validateToken } = require('./auth');
const { User } = require('../models');

// Get weather data by location
router.get('/', validateToken, async (req, res) => {
    try {
        const { latitude, longitude } = req.query;
        
        if (!latitude || !longitude) {
            return res.status(400).json({
                status: 'error',
                message: 'Latitude and longitude are required'
            });
        }

        const weatherData = await getWeatherData(
            parseFloat(latitude),
            parseFloat(longitude)
        );

        res.json({
            status: 'success',
            data: weatherData
        });
    } catch (error) {
        console.error('Error getting weather data:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error getting weather data: ' + error.message
        });
    }
});

module.exports = router; 