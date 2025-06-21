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
                location: { [Op.ne]: null } // Changed from [Op.not] to [Op.ne]
            },
            attributes: [
                'userId', 'username', 'firstName', 'lastName',
                'email', 'location', 'waterNeeds'
            ],
            raw: true
        });

        // Filter users within radius using Haversine formula
        return users
            .map(user => {
                try {
                    const location = typeof user.location === 'string'
                        ? JSON.parse(user.location)
                        : user.location;

                    if (!location || typeof location !== 'object' ||
                        !location.latitude || !location.longitude) {
                        return null;
                    }

                    // Convert degrees to radians
                    const toRad = x => x * Math.PI / 180;

                    // Haversine formula
                    const R = 6371; // Earth radius in km
                    const dLat = toRad(location.latitude - centerLat);
                    const dLon = toRad(location.longitude - centerLon);
                    const a =
                        Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(toRad(centerLat)) * Math.cos(toRad(location.latitude)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
                    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                    const distance = R * c;

                    return {
                        ...user,
                        distance: parseFloat(distance.toFixed(2)), // Round to 2 decimal places
                        latitude: location.latitude,
                        longitude: location.longitude
                    };
                } catch (e) {
                    console.error('Error processing user location:', e);
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