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
        console.log(`Starting nearby users search with center: ${centerLat},${centerLon} and radius: ${radius}km`);

        const users = await User.findAll({
            where: { email: { [Op.ne]: excludeEmail } },
            attributes: ['userId', 'email', 'location'],
            raw: true
        });

        console.log(`Found ${users.length} total users (excluding ${excludeEmail})`);

        const nearbyUsers = users.map(user => {
            console.log(`\nProcessing user: ${user.email}`);
            console.log(`Raw location data:`, user.location);

            try {
                if (!user.location) {
                    console.log('Skipping - no location data');
                    return null;
                }

                // Clean and parse the location string
                const cleanLocation = user.location
                    .replace(/^"+|"+$/g, '') // Remove surrounding quotes if present
                    .replace(/\\"/g, '"');    // Unescape quotes

                console.log('Cleaned location string:', cleanLocation);

                let location;
                try {
                    location = JSON.parse(cleanLocation);
                    console.log('Successfully parsed location:', location);
                } catch (e) {
                    console.log('Failed to parse location:', e);
                    return null;
                }

                // Safely extract coordinates
                const lat = parseFloat(location?.latitude);
                const lon = parseFloat(location?.longitude);
                console.log(`Extracted coordinates: ${lat},${lon}`);

                if (isNaN(lat) || isNaN(lon)) {
                    console.log('Skipping - invalid coordinates');
                    return null;
                }

                // Haversine calculation
                const R = 6371;
                const dLat = (lat - centerLat) * Math.PI / 180;
                const dLon = (lon - centerLon) * Math.PI / 180;
                const a =
                    Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(centerLat * Math.PI / 180) *
                    Math.cos(lat * Math.PI / 180) *
                    Math.sin(dLon/2) * Math.sin(dLon/2);
                const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                const distance = R * c;

                console.log(`Distance from center: ${distance.toFixed(2)} km`);

                return {
                    ...user,
                    distance: parseFloat(distance.toFixed(2)),
                    latitude: lat,
                    longitude: lon
                };
            } catch (e) {
                console.error('Error processing user:', e);
                return null;
            }
        })
        .filter(user => {
            const include = user && user.distance <= radius;
            console.log(`User ${user?.email || 'unknown'}: ${include ? 'INCLUDED' : 'EXCLUDED'} (Distance: ${user?.distance?.toFixed(2) || 'N/A'} km)`);
            return include;
        })
        .sort((a, b) => a.distance - b.distance);

        console.log(`\nFinal result: Found ${nearbyUsers.length} users within ${radius} km radius`);
        return nearbyUsers;
    } catch (error) {
        console.error('Error finding nearby users:', error);
        throw error;
    }
};


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

        const users = await getNearbyUsers(
            parseFloat(latitude),
            parseFloat(longitude),
            parseFloat(radius),
            req.user.email
        );

        // Transform the data to match the expected Kotlin class
        const formattedUsers = await Promise.all(users.map(async (user) => {
            try {
                // Try to fetch additional user details
                const fullUser = await User.findOne({
                    where: { userId: user.userId },
                    attributes: ['firstName', 'lastName', 'waterNeeds'],
                    raw: true
                });

                return {
                    userId: user.userId,
                    username: user.username || '',
                    firstName: fullUser?.firstName || 'Unknown',
                    lastName: fullUser?.lastName || 'User',
                    email: user.email,
                    waterNeeds: fullUser?.waterNeeds ? JSON.parse(fullUser.waterNeeds) : [],
                    lastActive: new Date().toISOString(), // Dummy data for lastActive
                    distance: user.distance
                };
            } catch (error) {
                console.error(`Error processing user ${user.userId}:`, error);
                // Fallback with dummy data if there's any error
                return {
                    userId: user.userId,
                    username: user.username || '',
                    firstName: 'Unknown',
                    lastName: 'User',
                    email: user.email,
                    waterNeeds: [],
                    lastActive: new Date().toISOString(), // Dummy data
                    distance: user.distance
                };
            }
        }));

        res.json({
            status: 'success',
            message: 'Nearby users retrieved successfully',
            data: formattedUsers
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