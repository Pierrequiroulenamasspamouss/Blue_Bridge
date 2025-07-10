const express = require('express');
const router = express.Router();
const { User } = require('../models');
const { validateToken } = require('../middleware/auth');
const { Op } = require('sequelize');
const { body, validationResult } = require('express-validator');
const validator = require('validator');

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
        attributes: [ // Ensure you fetch allowLocationSharing here
            'userId', 'username', 'firstName', 'lastName',
            'email', 'waterNeeds', 'location'
        ],
        raw: true
    });
};

const processUserLocation = async (user, centerLat, centerLon) => {
    try {
        // Fetch the full user data to check allowLocationSharing
        const fullUser = await User.findByPk(user.userId, {
            attributes: ['allowLocationSharing', 'location', 'waterNeeds', 'userId', 'username', 'firstName', 'lastName', 'email']
        });

        if (!fullUser || !fullUser.allowLocationSharing) return null;

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
            ...fullUser.get({ plain: true }), // Use plain object from Sequelize
            distance: parseFloat(distance.toFixed(2)),
            latitude: location.latitude,
            longitude: location.longitude,
            waterNeeds: parseWaterNeeds(fullUser.waterNeeds),
            lastActive: new Date().toISOString() // This might need to come from user.lastActive if available
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

router.post('/', [
    validateToken,
    body('latitude').isFloat({ min: -90, max: 90 }),
    body('longitude').isFloat({ min: -180, max: 180 }),
    body('radius').optional().isInt({ min: 1, max: 1000 })
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ status: 'error', errors: errors.array() });
    }
    try {
        const { latitude, longitude, radius = 50 } = req.body;

        if (!latitude || !longitude) {
            return res.status(400).json({
                status: 'error',
                message: 'Missing required fields: latitude and longitude'
            });
        }

        if (!req.user || !req.user.email) {
            return res.status(401).json({
                status: 'error',
                message: 'Authentication failed: user not found or token invalid.'
            });
        }

        const users = await getUsersWithLocations(req.user.email);
        const processedUsersPromises = users.map(user => processUserLocation(user, latitude, longitude));
        const processedUsers = (await Promise.all(processedUsersPromises)).filter(user => user !== null);

        const nearbyUsers = processedUsers
            .filter(user => user.distance <= radius)
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
                distance: user.distance,
                latitude: user.latitude, // Include if sharing is allowed
                longitude: user.longitude // Include if sharing is allowed
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