const express = require('express');
const router = express.Router();
const { User } = require('../models');
const { validateToken } = require('../middleware/auth');
const { Op } = require('sequelize');

// Helper functions
const cleanAndParseLocation = (locationString) => {
    if (!locationString) return null;

    try {
        // Remove surrounding quotes and escape characters
        let cleanStr = locationString.toString()
            .replace(/^"+|"+$/g, '')
            .replace(/\\"/g, '"');

        return JSON.parse(cleanStr);
    } catch (e) {
        console.error('Failed to parse location:', e);
        return null;
    }
};

const parseWaterNeeds = (waterNeeds) => {
    if (!waterNeeds) return [];

    try {
        const parsed = typeof waterNeeds === 'string'
            ? JSON.parse(waterNeeds)
            : waterNeeds;
        return Array.isArray(parsed) ? parsed : [];
    } catch (e) {
        console.error('Failed to parse waterNeeds:', e);
        return [];
    }
};

const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Earth radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) *
        Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
};

const getUsersWithLocations = async (excludeEmail) => {
    return await User.findAll({
        where: {
            email: { [Op.ne]: excludeEmail },
            location: { [Op.ne]: null }
        },
        attributes: [
            'userId', 'username', 'firstName', 'lastName',
            'email', 'waterNeeds', 'location'
        ],
        raw: true
    });
};

const processUserLocation = (user, centerLat, centerLon) => {
    try {
        const location = cleanAndParseLocation(user.location);
        if (!location || !location.latitude || !location.longitude) {
            return null;
        }

        const distance = calculateDistance(
            centerLat,
            centerLon,
            location.latitude,
            location.longitude
        );

        return {
            ...user,
            distance: parseFloat(distance.toFixed(2)),
            latitude: location.latitude,
            longitude: location.longitude,
            waterNeeds: parseWaterNeeds(user.waterNeeds),
            lastActive: new Date().toISOString()
        };
    } catch (error) {
        console.error(`Error processing user ${user.userId}:`, error);
        return null;
    }
};

// Routes
router.get('/debug-users', async (req, res) => {
    const users = await User.findAll({
        attributes: ['email', 'location']
    });
    res.json(users.map(u => ({
        email: u.email,
        location: u.location,
        type: typeof u.location
    })));
});

router.post('/', validateToken, async (req, res) => {
    try {
        const { latitude, longitude, radius = 50 } = req.body;

        if (!latitude || !longitude) {
            return res.status(400).json({
                status: 'error',
                message: 'Missing required fields: latitude and longitude'
            });
        }

        const users = await getUsersWithLocations(req.user.email);

        const nearbyUsers = users
            .map(user => processUserLocation(user, latitude, longitude))
            .filter(user => user !== null && user.distance <= radius)
            .sort((a, b) => a.distance - b.distance);

        res.json({
            status: 'success',
            message: 'Nearby users retrieved successfully',
            data: nearbyUsers.map(user => ({
                userId: user.userId,
                username: user.username || '',
                firstName: user.firstName || 'Unknown',
                lastName: user.lastName || 'User',
                email: user.email,
                waterNeeds: user.waterNeeds,
                lastActive: user.lastActive,
                distance: user.distance
            }))
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