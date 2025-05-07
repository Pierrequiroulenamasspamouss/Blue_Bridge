const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;
const { validateToken } = require('./auth');

/**
 * Get nearby users within a specified radius
 * @param {number} lat - Latitude
 * @param {number} lon - Longitude
 * @param {number} radius - Radius in kilometers
 * @param {string} excludeEmail - Email to exclude from results
 * @returns {Promise<Array>} - Array of nearby users
 */
const getNearbyUsers = async (lat, lon, radius, excludeEmail) => {
    try {
        const users = await User.findAll({
            where: {
                email: { [db.Sequelize.Op.ne]: excludeEmail },
                latitude: { [db.Sequelize.Op.ne]: null },
                longitude: { [db.Sequelize.Op.ne]: null },
                [db.Sequelize.Op.and]: [
                    db.Sequelize.literal(`
                        (6371 * acos(
                            cos(radians(${lat})) * 
                            cos(radians(latitude)) * 
                            cos(radians(longitude) - radians(${lon})) + 
                            sin(radians(${lat})) * 
                            sin(radians(latitude))
                        )) <= ${radius}
                    `)
                ]
            },
            attributes: [
                'userId',
                'username',
                'firstName',
                'lastName',
                'email',
                'latitude',
                'longitude',
                'waterNeeds',
                'lastActive'
            ]
        });

        return users.map(user => ({
            ...user.dataValues,
            distance: calculateDistance(lat, lon, user.latitude, user.longitude)
        }));
    } catch (error) {
        console.error('Error finding nearby users:', error);
        throw error;
    }
};

/**
 * Calculate distance between two points using Haversine formula
 */
const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Earth's radius in kilometers
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a = 
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * 
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
};

const toRad = (value) => {
    return value * Math.PI / 180;
};

// Get nearby users
router.get('/', validateToken, async (req, res) => {
    try {
        const { latitude, longitude, radius = 50 } = req.query;
        
        if (!latitude || !longitude) {
            return res.status(400).json({
                status: 'error',
                message: 'Latitude and longitude are required'
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
            data: users
        });
    } catch (error) {
        console.error('Error getting nearby users:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error getting nearby users: ' + error.message
        });
    }
});

module.exports = router; 