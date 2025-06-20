const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;
const { validateToken } = require('../middleware/auth');
const { Op } = require('sequelize');

/**
 * Get nearby users within a specified radius using location JSON
 */
const getNearbyUsers = async (centerLat, centerLon, radius, excludeEmail) => {
    try {
        // First get all users with valid location data
        const users = await User.findAll({
            where: {
                email: { [Op.ne]: excludeEmail },
                location: { [Op.not]: null }
            },
            attributes: [
                'userId', 'username', 'firstName', 'lastName',
                'email', 'location', 'waterNeeds', //'lastActive' //TODO : fix the database to use the latest login at some point. there is a mismatch error of the type of this
            ],
            raw: true
        });

        // Filter users within radius in memory
        return users
            .map(user => {
                try {
                    const location = JSON.parse(user.location);
                    if (!location.latitude || !location.longitude) return null;

                    // Simple distance calculation (approximation)
                    const latDiff = location.latitude - centerLat;
                    const lonDiff = location.longitude - centerLon;
                    const distance = Math.sqrt(latDiff*latDiff + lonDiff*lonDiff) * 111; // Convert to km

                    return {
                        ...user,
                        distance: distance,
                        latitude: location.latitude,
                        longitude: location.longitude
                    };
                } catch (e) {
                    return null;
                }
            })
            .filter(user => user && user.distance <= radius)
            .sort((a, b) => a.distance - b.distance);

    } catch (error) {
        console.error('Error finding nearby users:', error);
        throw error;
    }
};

router.post('/', validateToken, async (req, res) => {
    try {
        const { latitude, longitude, radius = 50 } = req.body;

        if (!latitude || !longitude) {
            return res.status(400).json({
                status: 'error',
                message: 'Missing required fields: latitude and longitude'
            });
        }

        const users = await getNearbyUsers(
            parseFloat(latitude),
            parseFloat(longitude),
            parseFloat(radius),
            req.user.email
        );

        res.json({
            status: 'success',
            message: 'Nearby users retrieved successfully',
            data: users
        });
    } catch (error) {
        console.error('Error getting nearby users:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error getting nearby users'
        });
    }
});

module.exports = router;